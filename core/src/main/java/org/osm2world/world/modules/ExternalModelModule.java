package org.osm2world.world.modules;

import static java.lang.Math.PI;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolylineXZ;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.*;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentUtil;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.NodeWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.common.ConfigurableWorldModule;
import org.osm2world.world.modules.common.WorldModuleParseUtil;

/**
 * Represents features using models loaded from external resources, such as {@link org.osm2world.output.gltf.GltfModel}.
 * The models can be defined in the {@link O2WConfig}, or they can be linked in OSM tags.
 */
public class ExternalModelModule extends ConfigurableWorldModule {

	@Override
	public void applyTo(MapData mapData) {

		for (MapElement element : mapData.getMapElements()) {
			if (element.getPrimaryRepresentation() == null) {

				Model model = Models.getModel(element.getElementWithId().toString());

				if (model == null && element.getTags().containsKey("3dmr")) {
					model = new ExternalResourceModel("3dmr:" + element.getTags().getValue("3dmr"));
				}

				if (model != null) {

					if (element instanceof MapNode n) {
						n.addRepresentation(new ExternalModelNodeWorldObject(n, model));
					} else if (element instanceof MapArea a) {
						a.addRepresentation(new ExternalModelAreaWorldObject(a, model));
					} else if (element instanceof MapWaySegment s) {
						if (s.getWay().getWaySegments().indexOf(s) == 0) {
							s.addRepresentation(new ExternalModelWayWorldObject(s, model));
							for (int i = 1; i < s.getWay().getWaySegments().size(); i++) {
								MapWaySegment segment = s.getWay().getWaySegments().get(i);
								segment.addRepresentation(new EmptyWaySegmentWorldObject(segment));
							}
						}
					}

				}

			}
		}

	}

	private static abstract class ExternalModelWorldObject<E extends MapElement> implements WorldObject {

		private final E element;
		private final Model model;

		private final EleConnector eleConnector;
		private final @Nullable AttachmentConnector attachmentConnector;

		protected ExternalModelWorldObject(E element, Model model) {

			this.element = element;
			this.model = model;

			VectorXZ pos = null;

			if (element instanceof MapNode n) {
				pos = n.getPos();
			} else if (element instanceof MapArea a) {
				pos = a.getOuterPolygon().getCentroid();
			} else if (element instanceof MapWaySegment s) {
				PolylineXZ polylineXZ = s.getWay().getPolylineXZ();
				pos = polylineXZ.pointAtOffset(0.5 * polylineXZ.getLength());
			}

			eleConnector = new EleConnector(pos, this, getGroundState());

			List<String> attachmentTypes = AttachmentUtil.getCompatibleSurfaceTypes(element);

			if (attachmentTypes.isEmpty()) {
				attachmentConnector = null;
			} else {
				attachmentConnector = new AttachmentConnector(attachmentTypes,
						pos.xyz(0), this, 0, true);
			}

		}

		@Override
		public E getPrimaryMapElement() {
			return element;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return List.of(eleConnector);
		}

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			return attachmentConnector != null ? List.of(attachmentConnector) : List.of();
		}

		@Override
		public GroundState getGroundState() {
			if (attachmentConnector != null && attachmentConnector.isAttached()) {
				return GroundState.ATTACHED;
			} else {
				return GroundState.ON;
			}
		}


		@Override
		public List<Mesh> buildMeshes() {

			VectorXYZ pos = eleConnector.getPosXYZ();
			if (attachmentConnector != null && attachmentConnector.isAttached()) {
				pos = attachmentConnector.getAttachedPos();
			}

			double direction = element instanceof MapNode
					? WorldModuleParseUtil.parseDirection(element.getTags(), PI)
					: 0;

			var i = new ModelInstance(model, new InstanceParameters(pos, direction));
			return i.getMeshes();

		}

	}

	private static class ExternalModelNodeWorldObject extends ExternalModelWorldObject<MapNode>
			implements NodeWorldObject {
		protected ExternalModelNodeWorldObject(MapNode node, Model model) {
			super(node, model);
		}
	}

	private static class ExternalModelAreaWorldObject extends ExternalModelWorldObject<MapArea>
			implements AreaWorldObject {
		protected ExternalModelAreaWorldObject(MapArea area, Model model) {
			super(area, model);
		}
	}

	private static class ExternalModelWayWorldObject extends ExternalModelWorldObject<MapWaySegment>
			implements WaySegmentWorldObject {
		protected ExternalModelWayWorldObject(MapWaySegment element, Model model) {
			super(element, model);
		}
	}

	/**
	 * only exists so that way segments other than the first still have a primary {@link WorldObject}.
	 * (The first one will have an {@link ExternalModelWayWorldObject}.)
	 */
	private record EmptyWaySegmentWorldObject(MapWaySegment segment) implements WaySegmentWorldObject {

		@Override
		public MapWaySegment getPrimaryMapElement() {
			return segment;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return List.of();
		}

		@Override
		public List<Mesh> buildMeshes() {
			return List.of();
		}

	}

}
