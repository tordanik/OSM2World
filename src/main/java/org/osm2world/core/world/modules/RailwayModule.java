package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.ExtrudeOption.END_CAP;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createLineBetween;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseInt;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.frontend_pbf.ModelTarget;
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

	private static class SleeperModel implements Model {

		private final double sleeperWidth;

		public SleeperModel(double sleeperWidth) {
			this.sleeperWidth = sleeperWidth;
		}

		@Override
		public void render(Target target, VectorXYZ position, double direction,
				Double height, Double width, Double length) {

			if (height == null) { height = SLEEPER_HEIGHT; }
			if (length == null) { length = SLEEPER_LENGTH; }
			if (width == null) { width = sleeperWidth; }

			SimplePolygonShapeXZ box = new AxisAlignedRectangleXZ(NULL_VECTOR, width, length);
			box = box.rotatedCW(direction);

			target.drawExtrudedShape(WOOD, box, asList(position, position.addY(height)),
					null, null, null, EnumSet.of(END_CAP));

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

		public Rail(MapWaySegment segment) {

			super(segment);

			gaugeMeters = parseInt(segment.getTags(), DEFAULT_GAUGE_MM, "gauge") / 1000.0f;
			railDist = gaugeMeters + 2 * (0.5f * RAIL_HEAD_WIDTH);

			sleeperWidth = gaugeMeters + 2 * RAIL_HEAD_WIDTH + 2 * SLEEPER_EXTRA_WIDTH;
			groundWidth = sleeperWidth + 2 * GROUND_EXTRA_WIDTH;

			if (!sleeperModelByWidth.containsKey(sleeperWidth)) {
				sleeperModelByWidth.put(sleeperWidth, new SleeperModel(sleeperWidth));
			}

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
		public void renderTo(Target target) {
			renderTo(target, 3);
		}

		public void renderTo(Target target, int lod) {

			/* draw ground */

			List<VectorXYZ> groundVs = WorldModuleGeometryUtil.createTriangleStripBetween(
					getOutline(false), getOutline(true));

			if (lod >= 4) {
				// just render the ballast (sleepers will be rendered as separate geometry)
				target.drawTriangleStrip(RAIL_BALLAST, groundVs,
						texCoordLists(groundVs, RAIL_BALLAST, GLOBAL_X_Z));
			} else {
				// use a repeating texture containing ballast, sleepers and rails
				target.drawTriangleStrip(RAILWAY, groundVs,
						texCoordLists(groundVs, RAILWAY, STRIP_FIT_HEIGHT));
			}

			/* draw rails */

			if (lod >= 3) {

				double yOffset = (lod == 4) ? SLEEPER_HEIGHT : 0;

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
					target.drawExtrudedShape(STEEL, shape, addYList(railLine, yOffset),
							nCopies(railLine.size(), Y_UNIT), null, null, extrudeOptions);
				}

			}

			/* draw railway ties/sleepers */

			if (lod >= 4) {

				List<VectorXYZ> sleeperPositions = equallyDistributePointsAlong(
						SLEEPER_DISTANCE, false, getCenterline());

				for (VectorXYZ sleeperPosition : sleeperPositions) {

					SleeperModel sleeperModel = sleeperModelByWidth.get(sleeperWidth);

					if (target instanceof ModelTarget) {

						((ModelTarget)target).drawModel(sleeperModel,
								sleeperPosition, segment.getDirection().angle(),
								null, sleeperWidth, null);

					} else {

						sleeperModel.render(target,
								sleeperPosition, segment.getDirection().angle(),
								null, sleeperWidth, null);

					}

				}

			}

		}

		@Override
		public double getWidth() {
			return groundWidth;
		}

		private long countConnectedRailSegments(MapNode node) {
			return node.getConnectedWaySegments().stream()
					.filter(it -> it.getPrimaryRepresentation() instanceof Rail)
					.count();
		}

	}

	public static class RailJunction extends JunctionNodeWorldObject<Rail> implements TerrainBoundaryWorldObject {

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
