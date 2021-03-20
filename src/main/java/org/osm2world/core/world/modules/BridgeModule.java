package org.osm2world.core.world.modules;


import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.parseColor;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.ClosedShapeXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.SurfaceAreaModule.SurfaceArea;
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

	public static final boolean isBridge(TagSet tags) {
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

			drawBridgePiers(target);

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

		/**
		 * draws supports for the bridge. These can be explicitly mapped with bridge:support=*.
		 * If no explicitly mapped supports are found, this method places some at equal distances.
		 */
		private void drawBridgePiers(Target target) {

			/* determine defaults */

			double defaultWidth = primaryRep.getWidth() * 0.7;
			double defaultLength = defaultWidth * 0.5;
			SimplePolygonShapeXZ defaultShape = new AxisAlignedRectangleXZ(NULL_VECTOR, defaultWidth, defaultLength);
			defaultShape = defaultShape.rotatedCW(primaryRep.getPrimaryMapElement().getDirection().angle());

			Material defaultMaterial = Materials.BRIDGE_PILLAR_DEFAULT;

			/* look for explicitly mapped supports among the way's nodes and overlapping features */

			Collection<MapElement> explicitlyMappedSupports = new ArrayList<>();

			//note: there is currently no de-duplication of bridge support nodes shared by two bridge segments

			for (MapNode node : primaryRep.segment.getStartEndNodes()) {
				if (node.getTags().containsKey("bridge:support")) {
					explicitlyMappedSupports.add(node);
				}
			}

			segment.getOverlaps().stream()
					.filter(overlap -> overlap instanceof MapOverlapWA)
					.map(overlap -> ((MapOverlapWA)overlap).e2)
					.filter(area -> area.getTags().containsKey("bridge:support"))
					.forEach(explicitlyMappedSupports::add);

			if (!explicitlyMappedSupports.isEmpty()) {

				/* draw the piers */

				for (MapElement element : explicitlyMappedSupports) {

					String bridgeSupportValue = element.getTags().getValue("bridge:support");
					if (asList("pier", "lift_pier", "pivot_pier", "pylon").contains(bridgeSupportValue)) {

						VectorXZ pos = (element instanceof MapArea)
								? ((MapArea)element).getOuterPolygon().getCenter()
								: ((MapNode)element).getPos();

						SimplePolygonShapeXZ shape = (element instanceof MapArea)
								? ((MapArea)element).getOuterPolygon()
								: defaultShape;

						Material material = null;
						if (element.getTags().containsKey("material")) {
							material = Materials.getMaterial(element.getTags().getValue("material").toUpperCase());
						}
						if (material == null) {
							material = Materials.BRIDGE_PILLAR_DEFAULT;
						}
						material = material.withColor(parseColor(element.getTags().getValue("colour"), CSS_COLORS));

						drawBridgePierAt(target, pos, shape, material);

					}

				}

			} else {

				/* no explicitly mapped supports found, distribute some equally along the bridge's length */

				double distance = min(50.0, primaryRep.getPrimaryMapElement().getLineSegment().getLength() / 2);

				List<VectorXZ> pierPositions = equallyDistributePointsAlong((float)distance, false,
						primaryRep.getStartPosition(), primaryRep.getEndPosition());

				/* make sure that the piers don't pierce anything on the ground */

				Collection<WorldObject> avoidedObjects = new ArrayList<>();

				for (MapIntersectionWW i : segment.getIntersectionsWW()) {
					for (WorldObject otherRep : i.getOther(segment).getRepresentations()) {

						if (otherRep.getGroundState() == GroundState.ON
								&& !(otherRep instanceof Water
										|| otherRep instanceof Waterway
										|| otherRep instanceof SurfaceArea) //TODO: choose better criteria!
						) {
							avoidedObjects.add(otherRep);
						}

					}
				}

				filterWorldObjectCollisions(pierPositions, avoidedObjects);

				/* draw the piers */

				for (VectorXZ pos : pierPositions) {
					drawBridgePierAt(target, pos, defaultShape, defaultMaterial);
				}

			}

		}

		private void drawBridgePierAt(Target target, VectorXZ pos, ClosedShapeXZ crossSection, Material material) {

			/* determine the bridge elevation at that point */

			PolylineXZ centerlineXZ = new PolylineXZ(primaryRep.getCenterlineXZ());
			double offset = centerlineXZ.offsetOf(centerlineXZ.closestPoint(pos));
			double ratio = offset / centerlineXZ.getLength();

			//note: there's a small inaccuracy because the length along the 3D line is different than in 2D
			VectorXYZ top = interpolateOn(primaryRep.getCenterline(), ratio);

			/* draw the pillar */

			// TODO: start pillar at ground instead of just x meters below the bridge
			VectorXYZ base = top.y(max(top.y-20, -3));
			target.drawExtrudedShape(material, crossSection,
					asList(base, top), null, null, null, EnumSet.of(ExtrudeOption.END_CAP));

		}

	}

}
