package org.osm2world.output.gltf;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.osm2world.conversion.O2WConfig.ObjectMetadataType;
import static org.osm2world.math.algorithms.NormalCalculationUtil.calculateTriangleNormals;
import static org.osm2world.output.common.ResourceOutputSettings.ResourceOutputMode.EMBED;
import static org.osm2world.output.common.ResourceOutputSettings.ResourceOutputMode.REFERENCE;
import static org.osm2world.output.common.compression.Compression.*;
import static org.osm2world.output.common.compression.CompressionUtil.writeFileWithCompression;
import static org.osm2world.output.gltf.GltfFlavor.GLB;
import static org.osm2world.output.gltf.GltfFlavor.GLTF;
import static org.osm2world.scene.material.Material.Interpolation.SMOOTH;
import static org.osm2world.scene.mesh.MeshStore.*;
import static org.osm2world.scene.texcoord.TexCoordUtil.mirroredVertically;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapRelationElement;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.common.AbstractOutput;
import org.osm2world.output.common.ResourceOutputSettings;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.gltf.data.*;
import org.osm2world.output.gltf.data.GltfMaterial.NormalTextureInfo;
import org.osm2world.output.gltf.data.GltfMaterial.OcclusionTextureInfo;
import org.osm2world.output.gltf.data.GltfMaterial.PbrMetallicRoughness;
import org.osm2world.output.gltf.data.GltfMaterial.TextureInfo;
import org.osm2world.scene.Scene;
import org.osm2world.scene.color.LColor;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.material.TextureData;
import org.osm2world.scene.material.TextureLayer;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.MeshStore;
import org.osm2world.scene.mesh.MeshStore.MergeMeshes.MergeOption;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.util.GlobalValues;
import org.osm2world.util.json.JsonUtil;

import com.google.common.collect.Multimap;


/**
 * builds a glTF or glb (binary glTF) output file
 */
public class GltfOutput extends AbstractOutput {

	private final File outputFile;
	private final GltfFlavor flavor;
	private final Compression compression;

	/** the gltf asset under construction */
	private final Gltf gltf = new Gltf();

	private final Map<Material, Integer> materialIndexMap = new HashMap<>();
	private final Map<TextureData, Integer> textureIndexMap = new HashMap<>();

	/** data for the glb BIN chunk, only used if {@link #flavor} is {@link GltfFlavor#GLB} */
	private final List<ByteBuffer> binChunkData = new ArrayList<>();

	/**
	 * Sets up an output to write a scene as glTF.
	 * Uses defaults for most parameters of {@link #GltfOutput(File, GltfFlavor, Compression)}.
	 */
	public GltfOutput(File outputFile) {
		this(outputFile, null, null);
	}

	/**
	 * Sets up an output to write a scene as glTF.
	 *
	 * @param outputFile   file to write to. Existing content will be overwritten.
	 * @param flavor       type of glTF file (JSON or binary); will be guessed from filename if null
	 * @param compression  compression used for the output file; may be <code>NONE</code>;
	 *                     will be guessed from filename if null
	 */
	public GltfOutput(File outputFile, @Nullable GltfFlavor flavor, @Nullable Compression compression) {

		this.outputFile = outputFile;

		if (flavor != null && compression != null) {
			this.flavor = flavor;
			this.compression = compression;
		} else {
			Pair<GltfFlavor, Compression> fc = guessFlavorAndCompression(outputFile.getName());
			this.flavor = requireNonNullElse(flavor, fc.getLeft());
			this.compression = requireNonNullElse(compression, fc.getRight());
		}

	}

	public File outputDir() {
		return outputFile.getAbsoluteFile().getParentFile();
	}

	@Override
	public String toString() {
		return "GltfOutput(" + outputFile + ")";
	}

	@Override
	public void outputScene(Scene scene) {
		outputScene(scene.getMeshesWithMetadata(config),
				scene.getMapProjection() != null ? scene.getMapProjection().getOrigin() : null,
				scene.getBoundary());
	}

	/**
	 * @param bounds  the boundary to be used for the output file.
	 *                Has an effect if some options such as clipping to bounds are used.
	 */
	public void outputScene(List<MeshWithMetadata> meshesWithMetadata, @Nullable LatLon origin,
			@Nullable SimpleClosedShapeXZ bounds) {

		MeshStore meshStore = new MeshStore(meshesWithMetadata);

		writeFileWithCompression(outputFile, compression, outputStream -> {

			try {
				if (flavor == GltfFlavor.GLTF) {
					writeJson(meshStore, origin, bounds, outputStream);
				} else {
					try (var jsonChunkOutputStream = new ByteArrayOutputStream()) {
						writeJson(meshStore, origin, bounds, jsonChunkOutputStream);
						ByteBuffer jsonChunkData = asPaddedByteBuffer(jsonChunkOutputStream.toByteArray(), (byte) 0x20);
						writeGlb(outputStream, jsonChunkData, binChunkData);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		});

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
	private int createMesh(Mesh mesh) throws IOException {

		GltfMesh gltfMesh = new GltfMesh();

		Material material = mesh.material;

		TriangleGeometry triangleGeometry = mesh.geometry.asTriangles();
		List<? extends TriangleXYZ> triangles = triangleGeometry.triangles;
		List<List<VectorXZ>> texCoordLists = triangleGeometry.texCoords;
		List<LColor> colors = triangleGeometry.colors == null ? null
				: triangleGeometry.colors.stream().map(LColor::fromRGB).toList();

		texCoordLists = mirroredVertically(texCoordLists); // move texture coordinate origin to the top left

		GltfMesh.Primitive primitive = new GltfMesh.Primitive();
		gltfMesh.primitives.add(primitive);

		/* convert material */

		int materialIndex;
		if (material.textureLayers().size() == 0) {
			materialIndex = createMaterial(material, null);
		} else {
			materialIndex = createMaterial(material, material.textureLayers().get(0));
		}
		primitive.material = materialIndex;

		/* put geometry into buffers and set up accessors */
		// TODO consider using indices

		primitive.mode = GltfMesh.TRIANGLES;

		List<VectorXYZ> positions = new ArrayList<>(3 * triangles.size());
		triangles.forEach(t -> positions.addAll(t.verticesNoDup()));
		primitive.attributes.put("POSITION", createAccessor(3, positions));

		List<VectorXYZ> normals = calculateTriangleNormals(triangles, material.interpolation() == SMOOTH);
		primitive.attributes.put("NORMAL", createAccessor(3, normals));

		if (material.textureLayers().size() > 0) {
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

		String type = switch (numComponents) {
			case 2 -> "VEC2";
			case 3 -> "VEC3";
			default -> throw new UnsupportedOperationException("invalid numComponents: " + numComponents);
		};

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

		GltfAccessor accessor = new GltfAccessor(GltfAccessor.TYPE_FLOAT, vs.size(), type);
		accessor.bufferView = createBufferView(byteBuffer, GltfBufferView.TARGET_ARRAY_BUFFER);
		accessor.min = min;
		accessor.max = max;
		gltf.accessors.add(accessor);

		return gltf.accessors.size() - 1;

	}

	private int createBufferView(ByteBuffer byteBuffer, @Nullable Integer target) {

		GltfBufferView view = switch (flavor) {
			case GLTF -> {

				String dataUri = "data:application/gltf-buffer;base64,"
						+ Base64.getEncoder().encodeToString(byteBuffer.array());

				GltfBuffer buffer = new GltfBuffer(byteBuffer.capacity());
				buffer.uri = dataUri;
				gltf.buffers.add(buffer);
				int bufferIndex = gltf.buffers.size() - 1;

				yield new GltfBufferView(bufferIndex, byteBuffer.capacity());

			}
			case GLB -> {
				int byteOffset = binChunkData.stream().mapToInt(ByteBuffer::capacity).sum();
				binChunkData.add(byteBuffer);
				var binBufferView = new GltfBufferView(0, byteBuffer.capacity());
				binBufferView.byteOffset = byteOffset;
				yield binBufferView;
			}
		};

		view.target = target;

		gltf.bufferViews.add(view);
		return gltf.bufferViews.size() - 1;

	}

	private int createMaterial(Material m, @Nullable TextureLayer textureLayer) throws IOException {

		if (materialIndexMap.containsKey(m)) return materialIndexMap.get(m);

		GltfMaterial material = new GltfMaterial();
		material.pbrMetallicRoughness = new PbrMetallicRoughness();

		material.name = getMaterialName(m, textureLayer);

		material.alphaMode = switch (m.transparency()) {
			case FALSE -> "OPAQUE";
			case BINARY -> "MASK";
			case TRUE -> "BLEND";
		};

		material.doubleSided = m.doubleSided();

		if (textureLayer != null) {

			/* textureLayer.baseColorTexture != null */ {

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

	private int createTexture(TextureData textureData) throws IOException {

		if (textureIndexMap.containsKey(textureData)) return textureIndexMap.get(textureData);

		GltfSampler sampler = new GltfSampler();
		sampler.magFilter = GltfSampler.LINEAR;
		sampler.minFilter = GltfSampler.LINEAR_MIPMAP_LINEAR;
		switch (textureData.wrap) {
			case CLAMP -> {
				sampler.wrapS = GltfSampler.WRAP_CLAMP_TO_EDGE;
				sampler.wrapT = GltfSampler.WRAP_CLAMP_TO_EDGE;
			}
			case REPEAT -> {
				sampler.wrapS = GltfSampler.WRAP_REPEAT;
				sampler.wrapT = GltfSampler.WRAP_REPEAT;
			}
		}
		gltf.samplers.add(sampler);
		int samplerIndex = gltf.samplers.size() - 1;

		GltfTexture texture = new GltfTexture();
		texture.source = createImage(textureData);
		texture.sampler = samplerIndex;

		gltf.textures.add(texture);
		int index = gltf.textures.size() - 1;
		textureIndexMap.put(textureData, index);
		return index;

	}

	private int createImage(TextureData textureData) throws IOException {

		ResourceOutputSettings resourceOutputSettings = getResourceOutputSettings();
		ResourceOutputSettings.ResourceOutputMode mode = resourceOutputSettings.modeForTexture(textureData);

		GltfImage image = new GltfImage();

		if (flavor == GltfFlavor.GLB && mode == EMBED) {
			try (var stream = new ByteArrayOutputStream()) {
				textureData.writeRasterImageToStream(stream, config.textureQuality());
				image.bufferView = createBufferView(asPaddedByteBuffer(stream.toByteArray(), (byte) 0x00), null);
				image.mimeType = textureData.getRasterImageFormat().mimeType();
			}
		} else {
			image.uri = switch (mode) {
				case REFERENCE -> resourceOutputSettings.buildTextureReference(textureData);
				case STORE_SEPARATELY_AND_REFERENCE -> resourceOutputSettings.storeTexture(textureData, outputDir().toURI());
				case EMBED -> textureData.getDataUri();
			};
		}

		gltf.images.add(image);
		return gltf.images.size() - 1;

	}

	/**
	 * constructs the JSON document after all parts of the glTF have been created
	 * and outputs it to an {@link OutputStream}
	 */
	private void writeJson(MeshStore meshStore, @Nullable LatLon origin, SimpleClosedShapeXZ bounds,
			OutputStream outputStream) throws IOException {

		boolean keepOsmElements = config.keepOsmElements();
		boolean clipToBounds = config.clipToBounds();

		/* process the meshes */

		EnumSet<MergeOption> mergeOptions = EnumSet.noneOf(MergeOption.class);

		if (!keepOsmElements) {
			mergeOptions.add(MergeOption.MERGE_ELEMENTS);
		}

		LevelOfDetail lod = config.lod();

		List<MeshProcessingStep> processingSteps = new ArrayList<>(asList(
				new FilterLod(lod),
				new ConvertToTriangles(lod),
				new EmulateTextureLayers(lod.ordinal() <= 1 ? 1 : Integer.MAX_VALUE),
				new MoveColorsToVertices(), // after EmulateTextureLayers because colorable is per layer
				new ReplaceTexturesWithAtlas(t -> getResourceOutputSettings().modeForTexture(t) == REFERENCE),
				new MergeMeshes(mergeOptions)));

		if (clipToBounds && bounds != null) {
			processingSteps.add(1, new ClipToBounds(bounds, true));
		}

		MeshStore processedMeshStore = meshStore.process(processingSteps);

		Multimap<MeshMetadata, Mesh> meshesByMetadata = processedMeshStore.meshesByMetadata();

		/* define metadata for the scene and root node */

		var sceneMetadata = new HashMap<String, Object>();

		if (origin != null) {
			sceneMetadata.put("origin", Map.of("lat", origin.lat, "lon", origin.lon, "ele", 0));
		}

		/* create the basic structure of the glTF */

		gltf.asset = new GltfAsset();
		gltf.asset.version = "2.0";
		gltf.asset.generator = "OSM2World " + GlobalValues.VERSION_STRING;

		gltf.scene = 0;
		gltf.scenes = List.of(new GltfScene());
		gltf.scenes.get(0).nodes = List.of(0);
		gltf.scenes.get(0).extras = sceneMetadata;

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
		rootNode.extras = sceneMetadata;
		gltf.nodes.add(rootNode);

		rootNode.children = new ArrayList<>();

		for (MeshMetadata objectMetadata : meshesByMetadata.keySet()) {

			List<Integer> meshNodeIndizes = new ArrayList<>(meshesByMetadata.size());

			FaultTolerantIterationUtil.forEach(meshesByMetadata.get(objectMetadata), (Mesh mesh) -> {
				try {
					int index = createNode(createMesh(mesh), null);
					meshNodeIndizes.add(index);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			if (keepOsmElements) {

				if (meshNodeIndizes.size() > 1) {
					// create a parent node if this model has more than one mesh node
					int parentNodeIndex = createNode(null, new ArrayList<>(meshNodeIndizes));
					meshNodeIndizes.clear();
					meshNodeIndizes.add(parentNodeIndex);
				}

				meshNodeIndizes.forEach(index -> addMeshNameAndExtras(gltf.nodes.get(index), objectMetadata, config));

			}

			rootNode.children.addAll(meshNodeIndizes);

		}

		/* add a buffer for the BIN chunk */

		if (flavor == GltfFlavor.GLB) {
			gltf.buffers.add(0, new GltfBuffer(binChunkData.stream().mapToInt(ByteBuffer::capacity).sum()));
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

		try (var writer = new OutputStreamWriter(outputStream)) {
			JsonUtil.serialize(gltf, writer, true);
		}

	}

	/** writes a binary glTF */
	private static void writeGlb(OutputStream outputStream, ByteBuffer jsonChunkData, List<ByteBuffer> binChunkData)
			throws IOException {

		int jsonChunkDataLength = jsonChunkData.capacity();
		int binChunkDataLength = binChunkData.stream().mapToInt(ByteBuffer::capacity).sum();

		int length = 12 // header
				+ 8 + jsonChunkDataLength // JSON chunk header + JSON chunk data
				+ 8 + binChunkDataLength; // BIN chunk header + BIN chunk data

		ByteBuffer result = ByteBuffer.allocate(length);
		result.order(ByteOrder.LITTLE_ENDIAN);

		/* write the header */

		result.putInt(0x46546C67); // magic number
		result.putInt(2); // version
		result.putInt(length);

		/* write the JSON chunk */

		result.putInt(jsonChunkDataLength);
		result.putInt(0x4E4F534A); // chunk type "JSON"
		result.put(jsonChunkData.array());

		/* write the BIN chunk */

		result.putInt(binChunkDataLength);
		result.putInt(0x004E4942); // chunk type "BIN"
		binChunkData.forEach(it -> result.put(it.array()));

		/* output the result */

		outputStream.write(result.array());

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

	/**
	 * returns a ByteBuffer containing an input array, which is padded (if necessary) to be a multiple of 4 bytes in
	 * length. This is used to fulfil the glTF spec requirement of alignment to 4-byte boundaries.
	 *
	 * @param paddingChar  byte value which is used to fill the padding
	 * @return a {@link ByteBuffer} which either wraps the input array or contains a copy of the bytes and some padding
	 */
	private static ByteBuffer asPaddedByteBuffer(byte[] bytes, byte paddingChar) {
		if (bytes.length % 4 == 0) {
			return ByteBuffer.wrap(bytes);
		} else {
			int padding = 4 - (bytes.length % 4);
			ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + padding);
			byteBuffer.put(bytes);
			for (int i = 0; i < padding; i++) {
				byteBuffer.put(paddingChar);
			}
			return byteBuffer;
		}
	}

	private @Nullable String getMaterialName(Material m, @Nullable TextureLayer textureLayer) {

		String name = Materials.getUniqueName(m);

		if (name == null) {
			if (textureLayer != null) {
				if (textureLayer.toString().startsWith("TextureAtlas")) {
					name = "TextureAtlas " + Integer.toHexString(m.hashCode());
				} else if (!textureLayer.toString().contains(",")) {
					name = textureLayer.toString();
				}
			}
		} else {
			if (textureLayer != null && m.textureLayers().size() > 1) {
				name += "_layer" + m.textureLayers().indexOf(textureLayer);
			}
		}

		return name;

	}

	private static void addMeshNameAndExtras(GltfNode node, MeshMetadata metadata, O2WConfig config) {

		MapRelationElement mapElement = metadata.mapElement();

		if (mapElement != null) {
			Map<String, Object> extras = new HashMap<>();
			if (config.exportMetadata().contains(ObjectMetadataType.ID)) {
				extras.put("osmId", mapElement.toString());
			}
			if (config.exportMetadata().contains(ObjectMetadataType.TAGS)) {
				extras.put("osmTags", mapElement.getTags().stream().collect(toMap(t -> t.key, t -> t.value)));
			}
			node.extras = extras;

		}

		if (metadata.modelClass() != null && mapElement != null) {
			TagSet tags = mapElement.getTags();
			if (tags.containsKey("name")) {
				node.name = metadata.modelClass().getSimpleName() + " " + tags.getValue("name");
			} else if (tags.containsKey("ref")) {
				node.name = metadata.modelClass().getSimpleName() + " " + tags.getValue("ref");
			} else {
				node.name = metadata.modelClass().getSimpleName() + " " + mapElement;
			}
		} else {
			node.name = "Multiple elements";
		}

	}

	private Pair<GltfFlavor, Compression> guessFlavorAndCompression(String fileName) {
		if (fileName.endsWith(".gltf")) {
			return Pair.of(GLTF, NONE);
		} else if (fileName.endsWith(".glb")) {
			return Pair.of(GLB, NONE);
		} else if (fileName.endsWith(".gltf.gz")) {
			return Pair.of(GLTF, GZ);
		} else if (fileName.endsWith(".glb.gz")) {
			return Pair.of(GLB, GZ);
		} else if (fileName.endsWith(".gltf.zip")) {
			return Pair.of(GLTF, ZIP);
		} else if (fileName.endsWith(".glb.zip")) {
			return Pair.of(GLB, ZIP);
		} else {
			throw new Error("unsupported extension: " + fileName);
		}
	}

	private ResourceOutputSettings getResourceOutputSettings() {
		File textureDir = new File(outputDir(), FilenameUtils.removeExtension(outputFile.getName()) + "_textures");
		return ResourceOutputSettings.fromConfig(config, textureDir.toURI(), true);
	}

}