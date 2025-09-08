package org.osm2world.world.modules;

import static java.util.Collections.emptyList;

import java.util.List;

import org.osm2world.map_data.data.*;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
import org.osm2world.world.data.NoOutlineWaySegmentWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

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
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		protected List<String> getAttachmentTypes() {
			return emptyList();
		}

		@Override
		public List<Mesh> buildMeshes() {
			return emptyList();
		}

	}

	private static class InvisibleEleWaySegment
			extends NoOutlineWaySegmentWorldObject {

		public InvisibleEleWaySegment(MapWaySegment segment) {
			super(segment);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public List<Mesh> buildMeshes() {
			return emptyList();
		}

	}

	private static class InvisibleEleArea
			extends AbstractAreaWorldObject {

		protected InvisibleEleArea(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public List<Mesh> buildMeshes() {
			return emptyList();
		}

	}


}
