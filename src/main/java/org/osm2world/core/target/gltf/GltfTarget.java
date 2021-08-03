package org.osm2world.core.target.gltf;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.algorithms.NormalCalculationUtil.calculateTriangleNormals;
import static org.osm2world.core.target.TargetUtil.flipTexCoordsVertically;
import static org.osm2world.core.target.common.material.Material.Interpolation.SMOOTH;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.material.ImageFileTexture;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.target.gltf.data.Gltf;
import org.osm2world.core.target.gltf.data.GltfAccessor;
import org.osm2world.core.target.gltf.data.GltfAsset;
import org.osm2world.core.target.gltf.data.GltfBuffer;
import org.osm2world.core.target.gltf.data.GltfBufferView;
import org.osm2world.core.target.gltf.data.GltfImage;
import org.osm2world.core.target.gltf.data.GltfMaterial;
import org.osm2world.core.target.gltf.data.GltfMaterial.NormalTextureInfo;
import org.osm2world.core.target.gltf.data.GltfMaterial.OcclusionTextureInfo;
import org.osm2world.core.target.gltf.data.GltfMaterial.PbrMetallicRoughness;
import org.osm2world.core.target.gltf.data.GltfMaterial.TextureInfo;
import org.osm2world.core.target.gltf.data.GltfMesh;
import org.osm2world.core.target.gltf.data.GltfNode;
import org.osm2world.core.target.gltf.data.GltfSampler;
import org.osm2world.core.target.gltf.data.GltfScene;
import org.osm2world.core.target.gltf.data.GltfTexture;
import org.osm2world.core.world.data.WorldObject;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import jakarta.xml.bind.DatatypeConverter;

public class GltfTarget extends AbstractTarget {

	// TODO expose as option - whether images should be embedded in the glTF file instead of referenced using a path
	private final boolean alwaysEmbedTextures = true;

	private final File outputFile;

	/** the gltf asset under construction */
	private final Gltf gltf;

	private GltfMesh currentMesh = null;

	private final Map<Material, Integer> materialIndexMap = new HashMap<>();
	private final Map<String, Integer> imageIndexMap = new HashMap<>();

	public GltfTarget(File outputFile) {
		this.outputFile = outputFile;

		/* create the basic structure of the glTF */

		gltf = new Gltf();
		gltf.asset = new GltfAsset();

		gltf.scene = 0;
		gltf.scenes = asList(new GltfScene());
		gltf.scenes.get(0).nodes = asList(0);
		gltf.nodes = new ArrayList<>();
		gltf.nodes.add(new GltfNode());
		gltf.nodes.get(0).name = "OSM2World scene";

		gltf.accessors = new ArrayList<>();
		gltf.buffers = new ArrayList<>();
		gltf.bufferViews = new ArrayList<>();
		gltf.images = new ArrayList<>();
		gltf.materials = new ArrayList<>();
		gltf.meshes = new ArrayList<>();
		gltf.samplers = new ArrayList<>();
		gltf.textures = new ArrayList<>();

	}

	@Override
	public void beginObject(WorldObject object) {

		if (currentMesh != null) {
			finishCurrentObject();
		}

		GltfNode currentNode = new GltfNode();
		gltf.nodes.add(currentNode);

		if (object != null) {

			MapElement osmElement = object.getPrimaryMapElement();
			TagSet tags = osmElement.getTags();
			Object elementWithId = osmElement instanceof MapWaySegment ? ((MapWaySegment)osmElement).getWay() : osmElement;

			if (tags.containsKey("name")) {
				currentNode.name = object.getClass().getSimpleName() + " " + tags.getValue("name");
			} else if (tags.containsKey("ref")) {
				currentNode.name = object.getClass().getSimpleName() + " " + tags.getValue("ref");
			} else {
				currentNode.name = object.getClass().getSimpleName() + " " + elementWithId.toString();
			}

			Map<String, Object> extras = new HashMap<>();
			extras.put("osmId", elementWithId.toString());
			currentNode.extras = extras;

		}

		currentMesh = new GltfMesh();
		gltf.meshes.add(currentMesh);
		currentNode.mesh = gltf.meshes.size() - 1;

	}

	@Override
	public void drawTriangles(Material material, List<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {

		if (triangles.stream().anyMatch(t -> t.isDegenerateOrNaN())) {
			throw new InvalidGeometryException("degenerate triangle");
		}

		texCoordLists = flipTexCoordsVertically(texCoordLists); // move texture coordinate origin to the top left

		// TODO merge sets of triangles using the same material into a single primitive

		GltfMesh.Primitive primitive = new GltfMesh.Primitive();
		currentMesh.primitives.add(primitive);

		/* convert material */

		int materialIndex;
		if (material.getNumTextureLayers() == 0) {
			materialIndex = materialIndexMap.containsKey(material)
					? materialIndexMap.get(material)
					: createMaterial(material, null);
		} else {
			materialIndex = materialIndexMap.containsKey(material)
					? materialIndexMap.get(material)
					: createMaterial(material, material.getTextureLayers().get(0));
			// TODO: handle additional material layers by having a similar method that gets a @Nullable TextureLayer and offset distance passed in
			// TODO (continued) ... but calculate normal vectors only once.
		}
		primitive.material = materialIndex;

		/* put geometry into buffers and set up accessors */
		// TODO consider using indices

		primitive.mode = GltfMesh.TRIANGLES;

		List<VectorXYZ> positions = new ArrayList<>(3 * triangles.size());
		triangles.forEach(t -> positions.addAll(t.verticesNoDup()));
		primitive.attributes.put("POSITION", createAccessor(3, positions));

		List<VectorXYZ> normals = calculateTriangleNormals(triangles, material.getInterpolation() == SMOOTH);
		primitive.attributes.put("NORMAL", createAccessor(3, normals));

		if (material.getNumTextureLayers() > 0) {
			primitive.attributes.put("TEXCOORD_0", createAccessor(2, texCoordLists.get(0)));
		}

	}

	@Override
	public void finish() {

		if (currentMesh != null) {
			finishCurrentObject();
		}

		gltf.nodes.get(0).children = IntStream.range(1, gltf.nodes.size()).boxed().collect(toList());

		if (gltf.accessors.isEmpty()) {
			gltf.accessors = null;
		}

		if (gltf.buffers.isEmpty()) {
			gltf.buffers = null;
		}

		if (gltf.bufferViews.isEmpty()) {
			gltf.bufferViews = null;
		}

		if (gltf.images.isEmpty()) {
			gltf.images = null;
		}

		if (gltf.materials.isEmpty()) {
			gltf.materials = null;
		}

		if (gltf.meshes.isEmpty()) {
			gltf.meshes = null;
		}

		if (gltf.samplers.isEmpty()) {
			gltf.samplers = null;
		}

		if (gltf.textures.isEmpty()) {
			gltf.textures = null;
		}

		try (FileWriter writer = new FileWriter(outputFile)) {
			new GsonBuilder().setPrettyPrinting().create().toJson(gltf, writer);
		} catch (JsonIOException | IOException e) {
			throw new RuntimeException(e);
		}

	}

	private void finishCurrentObject() {
		if (currentMesh.primitives.isEmpty()) {
			gltf.nodes.remove(gltf.nodes.size() - 1);
			gltf.meshes.remove(gltf.meshes.size() - 1);
		}
	}


	private int createAccessor(int numComponents, List<? extends Vector3D> vs) {

		String type;

		switch (numComponents) {
		case 2: type = "VEC2"; break;
		case 3: type = "VEC3"; break;
		default: throw new UnsupportedOperationException("invalid numComponents: " + numComponents);
		}

		float[] min = new float[numComponents];
		float[] max = new float[numComponents];

		Arrays.fill(min, Float.POSITIVE_INFINITY);
		Arrays.fill(max, Float.NEGATIVE_INFINITY);

		int byteLength = 4 /* FLOAT */ * numComponents * vs.size();

		ByteBuffer byteBuffer = ByteBuffer.allocate(byteLength);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		for (Vector3D v : vs) {

			float[] components = components(numComponents, v);

			for (int i = 0; i < numComponents; i++) {
				byteBuffer.putFloat(components[i]);
				min[i] = Math.min(min[i], components[i]);
				max[i] = Math.max(max[i], components[i]);
			}

		}

	    String dataUri = "data:application/gltf-buffer;base64,"
	    		+ DatatypeConverter.printBase64Binary(byteBuffer.array());

		GltfBuffer buffer = new GltfBuffer(byteLength);
		buffer.uri = dataUri;
		gltf.buffers.add(buffer);
		int bufferIndex = gltf.buffers.size() - 1;

		GltfBufferView view = new GltfBufferView(bufferIndex, byteLength);
		gltf.bufferViews.add(view);

		GltfAccessor accessor = new GltfAccessor(GltfAccessor.TYPE_FLOAT, vs.size(), type);
		accessor.bufferView = gltf.bufferViews.size() - 1;
		accessor.min = min;
		accessor.max = max;
		gltf.accessors.add(accessor);

		return gltf.accessors.size() - 1;

	}

	private static float[] components(int numComponents, Vector3D v) {
		if (numComponents == 2) {
			return new float[] {
					(float)((VectorXZ)v).x,
					(float)((VectorXZ)v).z
			};
		} else {
			assert numComponents == 3;
			return new float[] {
					(float)((VectorXYZ)v).x,
					(float)((VectorXYZ)v).y,
					(float)((VectorXYZ)v).z * -1
			};
		}
	}

	private int createMaterial(Material m, @Nullable TextureLayer textureLayer) {

		GltfMaterial material = new GltfMaterial();
		material.pbrMetallicRoughness = new PbrMetallicRoughness();

		String name = Materials.getUniqueName(m);
		if (textureLayer != null && m.getNumTextureLayers() > 1) {
			name += "_layer" + m.getTextureLayers().indexOf(textureLayer);
		}
		material.name = name;

		switch (m.getTransparency()) {
		case FALSE:
			material.alphaMode = "OPAQUE";
			break;
		case BINARY:
			material.alphaMode = "MASK";
			break;
		case TRUE:
			material.alphaMode = "BLEND";
			break;
		}

		material.doubleSided = m.isDoubleSided();

		if (textureLayer == null || textureLayer.colorable) {
			material.pbrMetallicRoughness.baseColorFactor = new float[] {
				m.getColor().getRed() / 255f,
				m.getColor().getGreen() / 255f,
				m.getColor().getBlue() / 255f,
				1.0f
			};
		}

		if (textureLayer != null) {

			if (textureLayer.baseColorTexture != null) {

				int baseColorTextureIndex = createTexture(textureLayer.baseColorTexture);

				material.pbrMetallicRoughness.baseColorTexture = new TextureInfo();
				material.pbrMetallicRoughness.baseColorTexture.index = baseColorTextureIndex;

			}

			if (textureLayer.ormTexture != null) {

				int ormTextureIndex = createTexture(textureLayer.ormTexture);

				material.occlusionTexture = new OcclusionTextureInfo();
				material.occlusionTexture.index = ormTextureIndex;

				material.pbrMetallicRoughness.metallicRoughnessTexture = new TextureInfo();
				material.pbrMetallicRoughness.metallicRoughnessTexture.index = ormTextureIndex;

			}

			if (textureLayer.normalTexture != null) {

				int normalTextureIndex = createTexture(textureLayer.normalTexture);

				material.normalTexture = new NormalTextureInfo();
				material.normalTexture.index = normalTextureIndex;

			}

		}

		gltf.materials.add(material);
		int index = gltf.materials.size() - 1;
		materialIndexMap.put(m, index);
		return index;

	}

	private int createTexture(TextureData textureData) {

		String uri;
		if (alwaysEmbedTextures
				|| !(textureData instanceof ImageFileTexture)
				|| ((ImageFileTexture)textureData).getFile().getPath().endsWith(".svg")) {
			uri = textureData.getDataUri();
		} else {
			uri = ((ImageFileTexture)textureData).getFile().getPath();
		}

		int imageIndex = imageIndexMap.containsKey(uri) ? imageIndexMap.get(uri) : createImage(uri);

		GltfSampler sampler = new GltfSampler();
		switch (textureData.wrap) {
		case CLAMP:
		case CLAMP_TO_BORDER:
			sampler.wrapS = GltfSampler.WRAP_CLAMP_TO_EDGE;
			sampler.wrapT = GltfSampler.WRAP_CLAMP_TO_EDGE;
			break;
		case REPEAT:
			sampler.wrapS = GltfSampler.WRAP_REPEAT;
			sampler.wrapT = GltfSampler.WRAP_REPEAT;
			break;
		}
		gltf.samplers.add(sampler);
		int samplerIndex = gltf.samplers.size() - 1;

		GltfTexture texture = new GltfTexture();
		texture.source = imageIndex;
		texture.sampler = samplerIndex;

		gltf.textures.add(texture);
		return gltf.textures.size() - 1;

	}

	private int createImage(String uri) {

		GltfImage image = new GltfImage();
		image.uri = uri;

		gltf.images.add(image);
		int index = gltf.images.size() - 1;

		imageIndexMap.put(uri, index);

		return index;

	}

}
