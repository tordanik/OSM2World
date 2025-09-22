package org.osm2world.world.modules;

import static java.lang.Math.PI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolylineXZ;
import org.osm2world.output.gltf.GltfFlavor;
import org.osm2world.output.gltf.GltfModel;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.*;
import org.osm2world.util.ValueParseUtil;
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
					Integer id = ValueParseUtil.parseInt(element.getTags().getValue("3dmr"));
					if (id != null) {
						model = loadModelFrom3dmr(id, element.getElementWithId());
					}
				}

				if (model != null) {

					if (element instanceof MapNode n) {
						n.addRepresentation(new ExternalModelNodeWorldObject(n, model));
					} else if (element instanceof MapArea a) {
						a.addRepresentation(new ExternalModelAreaWorldObject(a, model));
					} else if (element instanceof MapWaySegment s) {
						if (s.getIndexInWay() == 0) {
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

	/**
	 * attempt to load a model from the 3D model repository (3DMR)
	 */
	private @Nullable GltfModel loadModelFrom3dmr(long id, @Nullable MapRelationElement element) {

		/* First, try to load from a local directory if available */

		File modelDir = config.model3dmrDir();

		if (modelDir != null) {

			File modelFile = new File(modelDir, id + ".glb");

			if (!modelFile.exists()) {
				Pattern filenamePattern = Pattern.compile("^" + id + "(_[0-9]+)\\.glb$");
				File[] files = modelDir.listFiles((dir, name) -> filenamePattern.matcher(name).matches());
				if (files != null && files.length > 0) {
					Arrays.sort(files, Comparator.comparingLong(f -> Long.parseLong(filenamePattern.matcher(f.getName()).group(1))));
					modelFile = files[files.length - 1];
				}
			}

			if (modelFile.exists()) {
				try {
					return GltfModel.loadFromFile(modelFile);
				} catch (IOException e) {
					ConversionLog.error("Error loading 3DMR model from local file: " + modelFile, element);
				}
			}

		}

		/* If a local file not found or couldn't be loaded, try to download from URL */

		String urlPrefix = config.model3dmrUrl();

		if (urlPrefix != null) {
			try {

				URL url = new URL(urlPrefix + id);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");

				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					try (InputStream inputStream = connection.getInputStream()) {
						 return GltfModel.loadFromStream(inputStream, GltfFlavor.GLB,
								 new ExternalModelSource.External3DMRSource(id));
					}
				} else {
					ConversionLog.error("Response code " + connection.getResponseCode()
							+ " retrieving 3DMR model '"  + id + "' from " + urlPrefix, element);
				}

			} catch (IOException e) {
				ConversionLog.error("Error loading 3DMR model '"  + id + "' from " + urlPrefix, element);
			}
		}

		/* Return null if the model couldn't be loaded */

		return null;

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
