package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.closeLoop;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.math.VectorXYZ.addYList;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.ExtrudeOption.END_CAP;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.STRIP_FIT_HEIGHT;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordFunctions;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createLineBetween;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseInt;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.*;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.ModelInstance;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleGeometryUtil;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;

/**
 * adds rails to the world
 */
public class RailwayModule extends ConfigurableWorldModule {

	/** accepted values of the railway key */
	private static final List<String> RAILWAY_VALUES = asList(
			"rail", "light_rail", "tram", "subway", "disused");

	private static final int DEFAULT_GAUGE_MM = 1435;

	/** by how much the ballast goes beyond the ends of the sleeper (on each side) */
	private static final double GROUND_EXTRA_WIDTH = 0.2f;

	/** by how much the sleeper goes beyond the rail (on each side) */
	private static final double SLEEPER_EXTRA_WIDTH = 0.5f;

	private static final double SLEEPER_LENGTH = 0.26f;
	private static final double SLEEPER_HEIGHT = 0.16f * 0.4f; //extra factor to model sinking into the ballast

	private static final double SLEEPER_DISTANCE = 0.6f + SLEEPER_LENGTH;

	private static final float RAIL_HEAD_WIDTH = 0.067f; //must match RAIL_SHAPE
	private static final ShapeXZ RAIL_SHAPE;
	private static final ShapeXZ CLOSED_RAIL_SHAPE;

	static {

		List<VectorXZ> railShape = asList(
				new VectorXZ(0.45, 0), new VectorXZ(0.1, 0.1),
				new VectorXZ(0.1, 0.5), new VectorXZ(0.25, 0.55),
				new VectorXZ(0.25, 0.75), new VectorXZ(-0.25, 0.75),
				new VectorXZ(-0.25, 0.55), new VectorXZ(-0.1, 0.5),
				new VectorXZ(-0.1, 0.1), new VectorXZ(-0.45, 0));

		for (int i=0; i < railShape.size(); i++) {
			VectorXZ v = railShape.get(i);
			v = v.mult(0.1117f);
			railShape.set(i, v);
		}

		RAIL_SHAPE = new PolylineXZ(railShape);
		CLOSED_RAIL_SHAPE = new SimplePolygonXZ(closeLoop(railShape));

	}

	private record SleeperModel(double sleeperWidth) implements Model {

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			VectorXYZ position = params.position();
			Double height = params.height();
			Double width = params.width();
			Double length = params.length();

			if (height == null) {
				height = SLEEPER_HEIGHT;
			}
			if (length == null) {
				length = SLEEPER_LENGTH;
			}
			if (width == null) {
				width = sleeperWidth;
			}

			SimplePolygonShapeXZ box = new AxisAlignedRectangleXZ(NULL_VECTOR, width, length);
			box = box.rotatedCW(params.direction());

			return singletonList(new Mesh(new ExtrusionGeometry(box, asList(position, position.addY(height)),
					null, null, null, EnumSet.of(END_CAP), WOOD.getTextureDimensions()), WOOD, LOD4));

		}
	}

	private final Map<Double, SleeperModel> sleeperModelByWidth = new HashMap<Double, SleeperModel>();

	@Override
	public void applyTo(MapData mapData) {

		for (MapWaySegment segment : mapData.getMapWaySegments()) {
			if (segment.getTags().containsAny(asList("railway"), RAILWAY_VALUES)) {
				segment.addRepresentation(new Rail(segment));
			}
		}

		for (MapNode node : mapData.getMapNodes()) {
			if (getConnectedNetworkSegments(node, Rail.class, null).size() > 2) {
				// node.addRepresentation(new RailJunction(node));
				// TODO: reactivate after implementing proper rendering for rail junctions
			}
		}

	}

	public class Rail extends AbstractNetworkWaySegmentWorldObject implements TerrainBoundaryWorldObject {

		final double gaugeMeters;
		final double railDist;

		final double sleeperWidth;
		final double groundWidth;

		final boolean ownGround;

		public Rail(MapWaySegment segment) {

			super(segment);

			gaugeMeters = parseInt(segment.getTags(), DEFAULT_GAUGE_MM, "gauge") / 1000.0f;
			railDist = gaugeMeters + 2 * (0.5f * RAIL_HEAD_WIDTH);

			sleeperWidth = gaugeMeters + 2 * RAIL_HEAD_WIDTH + 2 * SLEEPER_EXTRA_WIDTH;
			groundWidth = sleeperWidth + 2 * GROUND_EXTRA_WIDTH;

			if (!sleeperModelByWidth.containsKey(sleeperWidth)) {
				sleeperModelByWidth.put(sleeperWidth, new SleeperModel(sleeperWidth));
			}

			// tram is often part of a street, omit ground mesh
			ownGround = !segment.getTags().contains("railway", "tram");

		}

		@Override
		public GroundState getGroundState() {

			if (segment.getTags().contains("railway", "subway")
					&& !segment.getTags().contains("tunnel", "no")){
				return GroundState.BELOW;
			}
			else if ( segment.getTags().contains("tunnel", "yes"))
			{
				return GroundState.BELOW;
			}

			return super.getGroundState();

		}

		@Override
		public List<Mesh> buildMeshes() {

			List<Mesh> result = new ArrayList<>();

			/* build ground meshes */

			if (ownGround) {

				List<VectorXYZ> groundVs = WorldModuleGeometryUtil.createTriangleStripBetween(
						getOutline(false), getOutline(true));

				// just the ballast (sleepers will be rendered as separate models at this LOD)
				TriangleGeometry.Builder lod4GroundBuilder = new TriangleGeometry.Builder(
						texCoordFunctions(RAIL_BALLAST, GLOBAL_X_Z), null, Interpolation.SMOOTH);
				lod4GroundBuilder.addTriangleStrip(groundVs);
				result.add(new Mesh(lod4GroundBuilder.build(), RAIL_BALLAST, LOD4));

				// repeating texture containing ballast, sleepers and rails
				TriangleGeometry.Builder lod3GroundBuilder = new TriangleGeometry.Builder(
						texCoordFunctions(RAILWAY, STRIP_FIT_HEIGHT), null, Interpolation.SMOOTH);
				lod3GroundBuilder.addTriangleStrip(groundVs);
				result.add(new Mesh(lod3GroundBuilder.build(), RAILWAY, LOD0, LOD3));

			}

			/* build rail meshes */

			for (LevelOfDetail lod : asList(LOD3, LOD4)) {

				double yOffset = (lod == LOD4) ? SLEEPER_HEIGHT : 0;

				ShapeXZ shape = RAIL_SHAPE;

				Set<ExtrudeOption> extrudeOptions = new HashSet<>(2);
				if (countConnectedRailSegments(segment.getStartNode()) == 1) {
					extrudeOptions.add(ExtrudeOption.START_CAP);
					shape = CLOSED_RAIL_SHAPE;
				}
				if (countConnectedRailSegments(segment.getEndNode()) == 1) {
					extrudeOptions.add(ExtrudeOption.END_CAP);
					shape = CLOSED_RAIL_SHAPE;
				}

				for (List<VectorXYZ> railLine : asList(
						createLineBetween(getOutline(false), getOutline(true), ((groundWidth - railDist) / groundWidth) / 2),
						createLineBetween(getOutline(false), getOutline(true), 1 - (groundWidth - railDist) / groundWidth / 2)
				)) {
					result.add(new Mesh(new ExtrusionGeometry(shape, addYList(railLine, yOffset),
							nCopies(railLine.size(), Y_UNIT), null, null, extrudeOptions, STEEL.getTextureDimensions()),
							STEEL, lod));
				}

			}

			return result;

		}

		@Override
		public List<ModelInstance> getSubModels() {

			if (segment.getTags().contains("railway", "tram")) return List.of();

			/* return railway ties/sleeper model instances (LOD4 only) */

			SleeperModel sleeperModel = sleeperModelByWidth.get(sleeperWidth);

			List<VectorXYZ> sleeperPositions = equallyDistributePointsAlong(SLEEPER_DISTANCE, false, getCenterline());

			return sleeperPositions.stream()
					.map(it -> new ModelInstance(sleeperModel,
								new InstanceParameters(it, segment.getDirection().angle(), null, sleeperWidth, null)))
					.collect(toList());

		}

		@Override
		public double getWidth() {
			return groundWidth;
		}

		@Override
		public Collection<PolygonShapeXZ> getTerrainBoundariesXZ() {
			if (!ownGround || getOutlinePolygonXZ() == null) {
				return emptyList();
			} else {
				return singletonList(getOutlinePolygonXZ());
			}
		}

		private long countConnectedRailSegments(MapNode node) {
			return node.getConnectedWaySegments().stream()
					.filter(it -> it.getPrimaryRepresentation() instanceof Rail)
					.count();
		}

	}

	public static class RailJunction extends JunctionNodeWorldObject<Rail>
			implements TerrainBoundaryWorldObject, LegacyWorldObject {

		public RailJunction(MapNode node) {
			super(node, Rail.class);
		}

		@Override
		public void renderTo(Target target) {

			if (getOutlinePolygon() == null) return;

			/* draw ground */

			List<VectorXYZ> vectors = getOutlinePolygon().vertices();

			Material material = Materials.RAIL_BALLAST;

			target.drawConvexPolygon(material, vectors,
					texCoordLists(vectors, material, GLOBAL_X_Z));

			/* draw connection between each pair of rails */

			// TODO: implement proper rendering for railway crosses and switches

		}

	}

}
