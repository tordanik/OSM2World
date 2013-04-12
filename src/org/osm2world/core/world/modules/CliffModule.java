package org.osm2world.core.world.modules;

import static com.google.common.collect.Iterables.any;
import static org.osm2world.core.util.Predicates.hasType;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.globalTexCoordLists;

import java.util.List;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
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
	public void applyTo(MapData grid) {
		
		for (MapWaySegment segment : grid.getMapWaySegments()) {
			
			if (segment.getTags().contains("natural", "cliff")) {
				segment.addRepresentation(new Cliff(segment));
			}
			
		}
		
	}
	
	private static int getConnectedCliffs(MapNode node) {
		
		int result = 0;
		
		for (MapWaySegment segment : node.getConnectedWaySegments()) {
			if (any(segment.getRepresentations(), hasType(Cliff.class))) {
				result += 1;
			}
		}
		
		return result;
		
	}
	
	public static class Cliff extends AbstractNetworkWaySegmentWorldObject
			implements TerrainBoundaryWorldObject, RenderableToAllTargets {

		private static final float CLIFF_WIDTH = 1;
		
		protected Cliff(MapWaySegment segment) {
			super(segment);
		}

		@Override
		public float getWidth() {
			return CLIFF_WIDTH;
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
					
					enforcer.addMinVerticalDistanceConstraint(
							left.get(i), right.get(i), height);
					
				}
					
			}
			
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			List<VectorXYZ> groundVs = createTriangleStripBetween(
					getOutline(false), getOutline(true));
			
			target.drawTriangleStrip(Materials.EARTH, groundVs,
					globalTexCoordLists(groundVs, Materials.RAIL_BALLAST_DEFAULT, false));
			
		}
		
	}
	
}
