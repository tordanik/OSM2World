package org.osm2world.world.modules;

import static java.lang.Math.PI;

import java.util.List;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolylineXZ;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.*;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
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

	private static class ExternalModelNodeWorldObject extends NoOutlineNodeWorldObject {

		private final Model model;

		protected ExternalModelNodeWorldObject(MapNode node, Model model) {
			super(node);
			this.model = model;
		}

		@Override
		public List<Mesh> buildMeshes() {
			double direction = WorldModuleParseUtil.parseDirection(node.getTags(), PI);
			var i = new ModelInstance(model, new InstanceParameters(getBase(), direction));
			return i.getMeshes();
		}

	}

	private static class ExternalModelAreaWorldObject extends AbstractAreaWorldObject {

		private final Model model;
		private EleConnector connector = null;

		protected ExternalModelAreaWorldObject(MapArea area, Model model) {
			super(area);
			this.model = model;
		}

		@Override
		public EleConnectorGroup getEleConnectors() {

			if (connector == null) {
				connector = new EleConnector(area.getOuterPolygon().getCentroid(), this, getGroundState());
			}

			var result = new EleConnectorGroup();
			result.add(connector);
			return result;

		}

		@Override
		public List<Mesh> buildMeshes() {

			VectorXYZ pos = getConnectorIfAttached() != null
					? getConnectorIfAttached().getAttachedPos()
					: connector.getPosXYZ();

			var i = new ModelInstance(model, new InstanceParameters(pos, 0));
			return i.getMeshes();

		}

	}

	private static class ExternalModelWayWorldObject implements WaySegmentWorldObject {

		private final MapWaySegment segment;
		private final Model model;

		private EleConnector connector = null;

		public ExternalModelWayWorldObject(MapWaySegment segment, Model model) {
			this.segment = segment;
			this.model = model;
		}

		@Override
		public MapWaySegment getPrimaryMapElement() {
			return segment;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {

			if (connector == null) {
				PolylineXZ polylineXZ = segment.getWay().getPolylineXZ();
				VectorXZ middlePoint = polylineXZ.pointAtOffset(0.5 * polylineXZ.getLength());
				connector = new EleConnector(middlePoint, this, getGroundState());
			}

			return List.of(connector);

		}

		@Override
		public List<Mesh> buildMeshes() {
			var i = new ModelInstance(model, new InstanceParameters(connector.getPosXYZ(), 0));
			return i.getMeshes();
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
