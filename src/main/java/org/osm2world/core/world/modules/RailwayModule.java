package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.target.common.material.Materials.RAIL_DEFAULT;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseInt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
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

	static {

		List<VectorXZ> railShape = asList(
				new VectorXZ(-0.45, 0), new VectorXZ(-0.1, 0.1),
				new VectorXZ(-0.1, 0.5), new VectorXZ(-0.25, 0.55),
				new VectorXZ(-0.25, 0.75), new VectorXZ(+0.25, 0.75),
				new VectorXZ(+0.25, 0.55), new VectorXZ(+0.1, 0.5),
				new VectorXZ(+0.1, 0.1), new VectorXZ(+0.45, 0));

		for (int i=0; i < railShape.size(); i++) {
			VectorXZ v = railShape.get(i);
			v = v.mult(0.1117f);
			v = new VectorXZ(-v.x, v.z + SLEEPER_HEIGHT);
			railShape.set(i, v);
		}

		RAIL_SHAPE = new PolylineXZ(railShape);

	}

	private static class SleeperModel implements Model {

		private final double sleeperWidth;

		public SleeperModel(double sleeperWidth) {
			this.sleeperWidth = sleeperWidth;
		}

		@Override
		public void render(Target<?> target, VectorXYZ position, double direction,
				Double height, Double width, Double length) {

			if (height == null) { height = SLEEPER_HEIGHT; }
			if (length == null) { length = SLEEPER_LENGTH; }
			if (width == null) {width = sleeperWidth; }

			target.drawBox(Materials.RAIL_SLEEPER_DEFAULT,
					position, VectorXZ.fromAngle(direction),
					height, width, length);

		}
	}

	private final Map<Double, SleeperModel> sleeperModelByWidth = new HashMap<Double, SleeperModel>();

	@Override
	public void applyTo(MapData grid) {

		for (MapWaySegment segment : grid.getMapWaySegments()) {
			if (segment.getTags().containsAny("railway", RAILWAY_VALUES)) {
				segment.addRepresentation(new Rail(segment));
			}
		}

		//TODO: the following for loop is copied from water module and should be in a common superclass
		for (MapNode node : grid.getMapNodes()) {

			int connectedRails = 0;

			for (MapWaySegment segment : node.getConnectedWaySegments()) {
				if (segment.getRepresentations().stream().anyMatch(r -> r instanceof Rail)) {
					connectedRails += 1;
				}
			}

			if (connectedRails > 2) {
				// node.addRepresentation(new RailJunction(node));
				// TODO: reactivate after implementing proper rendering for rail junctions
			}

		}

	}

	private class Rail extends AbstractNetworkWaySegmentWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {

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
		public void renderTo(Target<?> target) {

			/* draw ground */

			List<VectorXYZ> groundVs = WorldModuleGeometryUtil.createTriangleStripBetween(
					getOutline(false), getOutline(true));

			target.drawTriangleStrip(Materials.RAIL_BALLAST_DEFAULT, groundVs,
					texCoordLists(groundVs, Materials.RAIL_BALLAST_DEFAULT, GLOBAL_X_Z));


			/* draw rails */

			@SuppressWarnings("unchecked")
			List<VectorXYZ>[] railLines = new List[2];

			railLines[0] = WorldModuleGeometryUtil.createLineBetween(
					getOutline(false), getOutline(true),
					(float) ((groundWidth - railDist) / groundWidth) / 2);

			railLines[1] = WorldModuleGeometryUtil.createLineBetween(
					getOutline(false), getOutline(true),
					(float) (1 - ((groundWidth - railDist) / groundWidth) / 2));

			for (List<VectorXYZ> railLine : railLines) {

				target.drawExtrudedShape(RAIL_DEFAULT, RAIL_SHAPE, railLine,
						nCopies(railLine.size(), Y_UNIT), null, null, null);

			}


			/* draw railway ties/sleepers */

			List<VectorXYZ> sleeperPositions = equallyDistributePointsAlong(
					SLEEPER_DISTANCE, false, getCenterline());

			for (VectorXYZ sleeperPosition : sleeperPositions) {

				SleeperModel sleeperModel = sleeperModelByWidth.get(sleeperWidth);

				if (target instanceof ModelTarget<?>) {

					((ModelTarget<?>)target).drawModel(sleeperModel,
							sleeperPosition, segment.getDirection().angle(),
							null, sleeperWidth, null);

				} else {

					sleeperModel.render(target,
							sleeperPosition, segment.getDirection().angle(),
							null, sleeperWidth, null);

				}

			}

		}

		@Override
		public float getWidth() {
			return (float)groundWidth;
		}

	}

	public static class RailJunction
		extends JunctionNodeWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {

		public RailJunction(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			//TODO (code duplication): copied from RoadModule
			GroundState currentGroundState = null;
			checkEachLine: {
				for (MapWaySegment line : this.node.getConnectedWaySegments()) {
					if (line.getPrimaryRepresentation() == null) continue;
					GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
					if (currentGroundState == null) {
						currentGroundState = lineGroundState;
					} else if (currentGroundState != lineGroundState) {
						currentGroundState = GroundState.ON;
						break checkEachLine;
					}
				}
			}
			return currentGroundState;
		}

		@Override
		public void renderTo(Target<?> target) {

			if (getOutlinePolygon() == null) return;

			/* draw ground */

			List<VectorXYZ> vectors = getOutlinePolygon().getVertexLoop();

			Material material = Materials.RAIL_BALLAST_DEFAULT;

			target.drawConvexPolygon(material, vectors,
					texCoordLists(vectors, material, GLOBAL_X_Z));

			/* draw connection between each pair of rails */

			/* TODO: use node.getConnectedLines() instead?
			 * (allows access to information from there,
			 *  such as getOutline!)
			 */

			for (int i=0; i<cutCenters.size(); i++) {
				for (int j=0; j<i; j++) {

					/* connect those rails with an obtuse angle between them */


				}
			}

		}

	}

}
