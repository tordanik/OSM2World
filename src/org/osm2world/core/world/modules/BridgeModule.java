package org.osm2world.core.world.modules;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.Material.Lighting;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.WaterModule.Water;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.GeometryUtil.sequenceAbove;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;

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
	private static final Material BRIDGE_UNDERSIDE_MAT = new Material(Lighting.FLAT, Color.GRAY);
	private static final Material BRIDGE_PILLAR_MAT = new Material(Lighting.FLAT, Color.GRAY);
		
	private static class Bridge implements WaySegmentWorldObject,
		RenderableToAllTargets {
		
		private final MapWaySegment segment;
		private final AbstractNetworkWaySegmentWorldObject primaryRep;
		
		public Bridge(MapWaySegment segment,
				AbstractNetworkWaySegmentWorldObject primaryRepresentation) {
			this.segment = segment;
			this.primaryRep = primaryRepresentation;
		}

		@Override
		public MapElement getPrimaryMapElement() {
			return segment;
		}

		@Override
		public VectorXZ getEndPosition() {
			return primaryRep.getEndPosition();
		}

		@Override
		public VectorXZ getStartPosition() {
			return primaryRep.getStartPosition();
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}

		@Override
		public double getClearingBelow(VectorXZ pos) {
			return BRIDGE_UNDERSIDE_HEIGHT;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
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
			
			VectorXYZ[] strip1 = createVectorsForTriangleStripBetween(
					belowLeftOutline, leftOutline);
			VectorXYZ[] strip2 = createVectorsForTriangleStripBetween(
					belowRightOutline, belowLeftOutline);
			VectorXYZ[] strip3 = createVectorsForTriangleStripBetween(
					rightOutline, belowRightOutline);
			
			target.drawTriangleStrip(BRIDGE_UNDERSIDE_MAT, strip1);
			target.drawTriangleStrip(BRIDGE_UNDERSIDE_MAT, strip2);
			target.drawTriangleStrip(BRIDGE_UNDERSIDE_MAT, strip3);
			
		}

		private void drawBridgePillars(Target target) {
			
			List<VectorXZ> pillarPositions = equallyDistributePointsAlong(
					2f, false,
					primaryRep.getStartPosition(),
					primaryRep.getEndPosition());
			
			for (VectorXZ pos : pillarPositions) {
				
				//make sure that the pillar doesn't pierce anything on the ground 
				
				Collection<WorldObject> avoidedObjects = new ArrayList<WorldObject>();
				
				for (MapIntersectionWW i : segment.getIntersectionsWW()) {					
					for (WorldObject otherRep : i.getOther(segment).getRepresentations()) {
						
						if (otherRep.getGroundState() == GroundState.ON
								&& !(otherRep instanceof Water) //TODO: choose better criterion!
						) {
							avoidedObjects.add(otherRep);					
						}
					
					}										
				}
								
				if (!piercesWorldObject(pos, avoidedObjects)) {					
					drawBridgePillarAt(target, pos);					
				}
				
			}
			
		}

		private void drawBridgePillarAt(Target target, VectorXZ pos) {
		
			double eleAtPos = segment.getElevationProfile().getEleAt(pos);
			
			// TODO: start pillar at ground instead of just 100 meters below the bridge
			target.drawColumn(BRIDGE_PILLAR_MAT, null, 
					pos.xyz(eleAtPos - BRIDGE_UNDERSIDE_HEIGHT/2 - 100),
					100,
					0.2, 0.2, false, false);
			
		}
		
	}
	
}
