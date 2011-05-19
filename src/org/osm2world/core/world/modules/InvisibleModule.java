package org.osm2world.core.world.modules;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.data.NoOutlineWaySegmentWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * creates invisible world objects that are not rendered,
 * but nevertheless contain important information (such as elevation at a point)
 */
public class InvisibleModule extends AbstractModule {
	
	@Override
	protected void applyToNode(MapNode node) {
		if (node.getTags().containsKey("ele")
				&& node.getRepresentations().isEmpty()) {
			
			boolean isInGroundSegment = false;
			for (MapSegment segment : node.getConnectedSegments()) {
				MapElement element;
				if (segment instanceof MapWaySegment) {
					element = (MapWaySegment)segment;
				} else {
					element = ((MapAreaSegment)segment).getArea();
				}
				if (element.getPrimaryRepresentation() != null
						&& element.getPrimaryRepresentation().getGroundState()
							== GroundState.ON) {
					isInGroundSegment = true;
					break;
				}
			}
			
			if (node.getConnectedSegments().isEmpty() || isInGroundSegment) {
				node.addRepresentation(new InvisibleEleNode(node));
			}
			
		}
	}
	
	@Override
	protected void applyToWaySegment(MapWaySegment segment) {
		if (segment.getTags().containsKey("ele")
				&& segment.getRepresentations().isEmpty()) {
			segment.addRepresentation(new InvisibleEleWaySegment(segment));
		}
	}
	
	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().containsKey("ele")
				&& area.getRepresentations().isEmpty()) {
			area.addRepresentation(new InvisibleEleArea(area));
		}
	}
	
	private static class InvisibleEleNode extends NoOutlineNodeWorldObject {
		
		public InvisibleEleNode(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
	}
	
	private static class InvisibleEleWaySegment
			extends NoOutlineWaySegmentWorldObject {
		
		public InvisibleEleWaySegment(MapWaySegment segment) {
			super(segment);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
	}
	
	private static class InvisibleEleArea
			extends AbstractAreaWorldObject {
		
		protected InvisibleEleArea(MapArea area) {
			super(area);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
	}
	
	
}
