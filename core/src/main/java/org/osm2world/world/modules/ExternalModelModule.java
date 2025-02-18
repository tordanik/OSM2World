package org.osm2world.world.modules;

import static java.util.Collections.emptyList;

import java.util.List;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.*;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.world.creation.WorldModule;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.NodeWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;
import org.osm2world.world.data.WorldObject;

public class ExternalModelModule implements WorldModule {

	@Override
	public void applyTo(MapData mapData) {

		for (MapElement element : mapData.getMapElements()) {
			if (element.getPrimaryRepresentation() == null
					&& element.getTags().containsKey("3dmr")) {

				if (element instanceof MapNode) {
					((MapNode)element).addRepresentation(new NodePlaceholder((MapNode) element));
				} else if (element instanceof MapWaySegment) {
					((MapWaySegment)element).addRepresentation(new WayPlaceholder((MapWaySegment) element));
				} else if (element instanceof MapArea) {
					((MapArea)element).addRepresentation(new AreaPlaceholder((MapArea) element));
				}

			}
		}

	}

	@Override
	public void setConfiguration(O2WConfig config) {}

	/** temporary placeholder, to be replaced with an actual 3dmr model by the Target */
	private abstract static class ExternalModelPlaceholder<T extends MapElement> implements WorldObject {

		protected final T primaryMapElement;

		protected ExternalModelPlaceholder(T primaryMapElement) {
			this.primaryMapElement = primaryMapElement;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return emptyList();
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

		@Override
		public T getPrimaryMapElement() {
			return primaryMapElement;
		}

		@Override
		public List<Mesh> buildMeshes() {
			// no rendering
			return List.of();
		}

	}

	private static class NodePlaceholder extends ExternalModelPlaceholder<MapNode> implements NodeWorldObject {

		public NodePlaceholder(MapNode primaryMapElement) {
			super(primaryMapElement);
		}

	}

	private static class WayPlaceholder extends ExternalModelPlaceholder<MapWaySegment> implements WaySegmentWorldObject {

		public WayPlaceholder(MapWaySegment primaryMapElement) {
			super(primaryMapElement);
		}

	}

	private static class AreaPlaceholder extends ExternalModelPlaceholder<MapArea> implements AreaWorldObject {

		public AreaPlaceholder(MapArea primaryMapElement) {
			super(primaryMapElement);
		}

	}


}
