package org.osm2world.core.world.modules;


import static java.lang.Math.max;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
import org.osm2world.core.world.modules.WaterModule.Water;
import org.osm2world.core.world.modules.WaterModule.Waterway;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.modules.common.BridgeOrTunnel;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * adds bridges to the world.
 *
 * Needs to be applied <em>after</em> all the modules that generate
 * whatever runs over the bridge.
 */
public class BridgeModule extends AbstractModule {

	public static final boolean isBridge(TagGroup tags) {
		return tags.containsKey("bridge")
			&& !"no".equals(tags.getValue("bridge"));
	}

	public static final boolean isBridge(MapWaySegment segment) {
		return isBridge(segment.getTags());
	}

	@Override
	protected void applyToWaySegment(MapWaySegment segment) {

		WaySegmentWorldObject primaryRepresentation =
			segment.getPrimaryRepresentation();

		if (primaryRepresentation instanceof AbstractNetworkWaySegmentWorldObject
				&& isBridge(segment)) {

			segment.addRepresentation(new Bridge(segment,
					(AbstractNetworkWaySegmentWorldObject) primaryRepresentation));

		}

	}

	public static final double BRIDGE_UNDERSIDE_HEIGHT = 0.2f;

	private static class Bridge extends BridgeOrTunnel {

		public Bridge(MapWaySegment segment,
				AbstractNetworkWaySegmentWorldObject primaryWO) {
			super(segment, primaryWO);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			// TODO EleConnectors for pillars
			return super.getEleConnectors();
		}

		@Override
		public void renderTo(Target target) {

			drawBridgeUnderside(target);

			drawBridgePillars(target);

		}

		private void drawBridgeUnderside(Target target) {

			List<VectorXYZ> leftOutline = primaryRep.getOutline(false);
			List<VectorXYZ> rightOutline = primaryRep.getOutline(true);

			List<VectorXYZ> belowLeftOutline = sequenceAbove(leftOutline, -BRIDGE_UNDERSIDE_HEIGHT);
			List<VectorXYZ> belowRightOutline = sequenceAbove(rightOutline, -BRIDGE_UNDERSIDE_HEIGHT);

			List<VectorXYZ> strip1 = createTriangleStripBetween(
					belowLeftOutline, leftOutline);
			List<VectorXYZ> strip2 = createTriangleStripBetween(
					belowRightOutline, belowLeftOutline);
			List<VectorXYZ> strip3 = createTriangleStripBetween(
					rightOutline, belowRightOutline);

			target.drawTriangleStrip(Materials.BRIDGE_DEFAULT, strip1, null);
			target.drawTriangleStrip(Materials.BRIDGE_DEFAULT, strip2, null);
			target.drawTriangleStrip(Materials.BRIDGE_DEFAULT, strip3, null);

		}

		private void drawBridgePillars(Target target) {

			List<VectorXZ> pillarPositions = equallyDistributePointsAlong(
					2f, false,
					primaryRep.getStartPosition(),
					primaryRep.getEndPosition());

			//make sure that the pillars doesn't pierce anything on the ground

			Collection<WorldObjectWithOutline> avoidedObjects = new ArrayList<>();

			for (MapIntersectionWW i : segment.getIntersectionsWW()) {
				for (WorldObject otherRep : i.getOther(segment).getRepresentations()) {

					if (otherRep.getGroundState() == GroundState.ON
							&& otherRep instanceof WorldObjectWithOutline
							&& !(otherRep instanceof Water || otherRep instanceof Waterway) //TODO: choose better criterion!
					) {
						avoidedObjects.add((WorldObjectWithOutline) otherRep);
					}

				}
			}

			filterWorldObjectCollisions(pillarPositions, avoidedObjects, null);

			//draw the pillars

			for (VectorXZ pos : pillarPositions) {
				drawBridgePillarAt(target, pos);
			}

		}

		private void drawBridgePillarAt(Target target, VectorXZ pos) {

			/* determine the bridge elevation at that point */

			VectorXYZ top = null;

			List<VectorXYZ> vs = primaryRep.getCenterline();

			for (int i = 0; i + 1 < vs.size(); i++) {

				if (isBetween(pos, vs.get(i).xz(), vs.get(i+1).xz())) {
					top = interpolateElevation(pos, vs.get(i), vs.get(i+1));
                                        break;
				}

			}

			/* draw the pillar */

			// TODO: start pillar at ground instead of just x meters below the bridge
			VectorXYZ base = top.y(max(top.y-20, -3));
			target.drawColumn(Materials.BRIDGE_PILLAR_DEFAULT, null,
					base,
					top.y - base.y,
					0.2, 0.2, false, false);

		}

	}

}
