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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.MeshStore;
import org.osm2world.core.target.common.MeshStore.MeshMetadata;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.MeshTarget.MergeMeshes.MergeOption;
import org.osm2world.core.target.common.material.ImageFileTexture;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.RasterImageFileTexture;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
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
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.color.LColor;

import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import jakarta.xml.bind.DatatypeConverter;

public class GltfTarget extends MeshTarget {

	private final File outputFile;

	/** the gltf asset under construction */
	private final Gltf gltf = new Gltf();

	private final Map<Material, Integer> materialIndexMap = new HashMap<>();
	private final Map<String, Integer> imageIndexMap = new HashMap<>();

	public GltfTarget(File outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public String toString() {
		return "GltfTarget(" + outputFile + ")";
	}

	@Override
	public void finish() {

		boolean keepOsmElements = config.getBoolean("keepOsmElements", true);

		/* process the meshes */

		EnumSet<MergeOption> mergeOptions = EnumSet.noneOf(MergeOption.class);

		if (!keepOsmElements) {
			mergeOptions.add(MergeOption.MERGE_ELEMENTS);
		}

		MeshStore processedMeshStore = meshStore.process(asList(
				new MoveColorsToVertices(),
				new EmulateTextureLayers(),
				new GenerateTextureAtlas(),
				new MergeMeshes(mergeOptions)));

		Multimap<MeshMetadata, Mesh> meshesByMetadata = processedMeshStore.meshesByMetadata();

		/* create the basic structure of the glTF */

		gltf.asset = new GltfAsset();

		gltf.scene = 0;
		gltf.scenes = asList(new GltfScene());
		gltf.scenes.get(0).nodes = asList(0);

		gltf.accessors = new ArrayList<>();
		gltf.buffers = new ArrayList<>();
		gltf.bufferViews = new ArrayList<>();
		gltf.images = new ArrayList<>();
		gltf.materials = new ArrayList<>();
		gltf.meshes = new ArrayList<>();
		gltf.samplers = new ArrayList<>();
		gltf.textures = new ArrayList<>();

		/* generate the glTF nodes and meshes */

		gltf.nodes = new ArrayList<>();

		GltfNode rootNode = new GltfNode();
		rootNode.name = "OSM2World scene";
		gltf.nodes.add(rootNode);

		rootNode.children = new ArrayList<>();

		for (MeshMetadata objectMetadata : meshesByMetadata.keySet()) {

			List<Integer> meshNodeIndizes = new ArrayList<>(meshesByMetadata.size());

			FaultTolerantIterationUtil.forEach(meshesByMetadata.get(objectMetadata), (Mesh mesh) -> {
				int index = createNode(createMesh(mesh), null);
				meshNodeIndizes.add(index);
			});

			if (keepOsmElements) {

				if (meshNodeIndizes.size() > 1) {
					// create a parent node if this model has more than one mesh node
					int parentNodeIndex = createNode(null, new ArrayList<>(meshNodeIndizes));
					meshNodeIndizes.clear();
					meshNodeIndizes.add(parentNodeIndex);
				}

				meshNodeIndizes.forEach(index -> addMeshNameAndId(gltf.nodes.get(index), objectMetadata));

			}

			rootNode.children.addAll(meshNodeIndizes);

		}

		/* use null instead of [] when lists are empty */

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

		/* write the JSON file */

		try (FileWriter writer = new FileWriter(outputFile)) {
			new GsonBuilder().setPrettyPrinting().create().toJson(gltf, writer);
		} catch (JsonIOException | IOException e) {
			throw new RuntimeException(e);
		}

	}

	/** creates a {@link GltfNode} and returns its index in {@link Gltf#nodes} */
	private int createNode(@Nullable Integer meshIndex, @Nullable List<Integer> childNodeIndices) {

		assert childNodeIndices == null || !childNodeIndices.isEmpty();

		GltfNode node = new GltfNode();

		node.mesh = meshIndex;
		node.children = childNodeIndices;

		gltf.nodes.add(node);
		return gltf.nodes.size() - 1;

	}

	/** creates a {@link GltfMesh} and returns its index in {@link Gltf#meshes} */
	private int createMesh(Mesh mesh) {

		GltfMesh gltfMesh = new GltfMesh();

		Material material = mesh.material;

		TriangleGeometry triangleGeometry = mesh.geometry.asTriangles();
		List<? extends TriangleXYZ> triangles = triangleGeometry.triangles;
		List<List<VectorXZ>> texCoordLists = triangleGeometry.texCoords();
		List<LColor> colors = triangleGeometry.colors == null ? null
				: triangleGeometry.colors.stream().map(c -> LColor.fromAWT(c)).collect(toList());

		if (triangles.stream().anyMatch(t -> t.isDegenerateOrNaN())) {
			throw new InvalidGeometryException("degenerate triangle");
		}

		texCoordLists = flipTexCoordsVertically(texCoordLists); // move texture coordinate origin to the top left

		GltfMesh.Primitive primitive = new GltfMesh.Primitive();
		gltfMesh.primitives.add(primitive);

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

		if (colors != null) {
			List<VectorXYZ> colorsAsVectors = colors.stream().map(c -> new VectorXYZ(c.red, c.green, -c.blue)).collect(toList());
			primitive.attributes.put("COLOR_0", createAccessor(3, colorsAsVectors));
		}

		gltf.meshes.add(gltfMesh);
		return gltf.meshes.size() - 1;

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

		// whether images should be embedded in the glTF file instead of referenced using a path
		boolean alwaysEmbedTextures = config.getBoolean("alwaysEmbedTextures", true);

		String uri;
		if (alwaysEmbedTextures || !(textureData instanceof RasterImageFileTexture)) {
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

	private static void addMeshNameAndId(GltfNode node, MeshMetadata metadata) {

		MapRelation.Element mapElement = metadata.mapElement;

		if (mapElement != null) {
			Map<String, Object> extras = new HashMap<>();
			extras.put("osmId", mapElement.toString());
			node.extras = extras;
		}

		if (metadata.modelClass != null && mapElement != null) {
			TagSet tags = mapElement.getTags();
			if (tags.containsKey("name")) {
				node.name = metadata.modelClass.getSimpleName() + " " + tags.getValue("name");
			} else if (tags.containsKey("ref")) {
				node.name = metadata.modelClass.getSimpleName() + " " + tags.getValue("ref");
			} else {
				node.name = metadata.modelClass.getSimpleName() + " " + mapElement.toString();
			}
		} else {
			node.name = "Multiple elements";
		}

	}

}