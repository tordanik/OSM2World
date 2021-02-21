package org.osm2world.core.world.modules;

import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MIN;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.List;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * adds cliffs and retaining walls to the world.
 * Their common property is that they offset terrain elevation.
 */
public class CliffModule extends ConfigurableWorldModule {

	@Override
	public void applyTo(MapData mapData) {

		for (MapWaySegment segment : mapData.getMapWaySegments()) {

			if (segment.getTags().contains("natural", "cliff")) {
				segment.addRepresentation(new Cliff(segment));
			} else if (segment.getTags().contains("barrier", "retaining_wall")) {
				segment.addRepresentation(new RetainingWall(segment));
			}

		}

	}

	private static int getConnectedCliffs(MapNode node) {

		int result = 0;

		for (MapWaySegment segment : node.getConnectedWaySegments()) {
			if (segment.getRepresentations().stream().anyMatch(r -> r instanceof Cliff)) {
				result += 1;
			}
		}

		return result;

	}

	private abstract static class AbstractCliff extends AbstractNetworkWaySegmentWorldObject
			implements TerrainBoundaryWorldObject {

		protected AbstractCliff(MapWaySegment segment) {
			super(segment);
		}

		protected abstract double getDefaultWidth();

		protected abstract Material getMaterial();

		@Override
		public double getWidth() {
			return parseWidth(segment.getTags(), getDefaultWidth());
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {

			double height = parseHeight(segment.getTags(), 5);

			if (isBroken()) return;

			/* add vertical offset between left and right connectors */

			List<EleConnector> center = getCenterlineEleConnectors();
			List<EleConnector> left = connectors.getConnectors(getOutlineXZ(false));
			List<EleConnector> right = connectors.getConnectors(getOutlineXZ(true));

			for (int i = 0; i < center.size(); i++) {

				// the ends of the cliff may be much lower
				if ((i != 0 || getConnectedCliffs(segment.getStartNode()) > 1)
						&& (i != center.size() - 1 || getConnectedCliffs(segment.getEndNode()) > 1)) {

					enforcer.requireVerticalDistance(
							MIN, height, left.get(i), right.get(i));

				}

			}

		}

		@Override
		public void renderTo(Target target) {

			List<VectorXYZ> groundVs = createTriangleStripBetween(
					getOutline(false), getOutline(true));

			target.drawTriangleStrip(getMaterial(), groundVs,
					texCoordLists(groundVs, getMaterial(), GLOBAL_X_Z));

		}

	}

	public static class Cliff extends AbstractCliff {

		protected Cliff(MapWaySegment segment) {
			super(segment);
		}

		@Override
		protected double getDefaultWidth() {
			return 1.0;
		}

		@Override
		protected Material getMaterial() {
			return Materials.ROCK;
		}

	}

	public static class RetainingWall extends AbstractCliff {

		protected RetainingWall(MapWaySegment segment) {
			super(segment);
		}

		@Override
		protected double getDefaultWidth() {
			return 1.0;
		}

		@Override
		protected Material getMaterial() {
			return Materials.CONCRETE;
		}

	}

}
