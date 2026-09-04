package org.osm2world.output.gltf;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNullElse;
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
import static org.osm2world.scene.mesh.MeshWithMetadata.ElementMetadata;
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
import org.osm2world.scene.material.TextureData;
import org.osm2world.scene.material.TextureLayer;
import org.osm2world.scene.mesh.*;
import org.osm2world.scene.mesh.MeshStore.MergeMeshes.MergeOption;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.util.GlobalValues;
import org.osm2world.util.platform.json.JsonUtil;

import com.google.common.collect.Multimap;


/**
 * builds a glTF or glb (binary glTF) output file
 */
public class GltfOutput extends AbstractOutput {

	/** name of the glTF extension which allows vertex attributes to use integer components */
	public static final String KHR_MESH_QUANTIZATION = "KHR_mesh_quantization";

	private final File outputFile;
	private final GltfFlavor flavor;
	private final Compression compression;

	/** the gltf asset under construction */
	private final Gltf gltf = new Gltf();

	/** key for {@link #materialIndexMap}, see {@link #createMaterial(Material, TextureLayer, LColor)} */
	private record MaterialWithColor(Material material, @Nullable LColor color) {}

	private final Map<MaterialWithColor, Integer> materialIndexMap = new HashMap<>();
	private final Map<TextureData, Integer> textureIndexMap = new HashMap<>();

	/** data for the glb BIN chunk, only used if {@link #flavor} is {@link GltfFlavor#GLB} */
	private final List<ByteBuffer> binChunkData = new ArrayList<>();

	/** how to quantize vertex positions, or null if positions and normals are written as floats */
	private @Nullable PositionQuantization positionQuantization = null;

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

		/* if all vertices have the same color, put it into the material instead of into a vertex attribute.
		 * This is a common case, and vertex colors would take up as much space as the vertex positions. */

		@Nullable LColor constantColor = null;

		if (colors != null && !colors.isEmpty() && colors.get(0) != null
				&& colors.stream().distinct().count() == 1) {
			constantColor = colors.get(0);
			colors = null;
		}

		/* convert material */

		int materialIndex;
		if (material.textureLayers().size() == 0) {
			materialIndex = createMaterial(material, null, constantColor);
		} else {
			materialIndex = createMaterial(material, material.textureLayers().get(0), constantColor);
		}
		primitive.material = materialIndex;

		/* collect the attributes of each vertex */

		primitive.mode = GltfMesh.TRIANGLES;

		List<VectorXYZ> positions = new ArrayList<>(3 * triangles.size());
		triangles.forEach(t -> positions.addAll(t.verticesNoDup()));

		List<VectorXYZ> normals = calculateTriangleNormals(triangles, material.interpolation() == SMOOTH);

		@Nullable List<VectorXZ> texCoords = material.textureLayers().isEmpty() ? null : texCoordLists.get(0);

		@Nullable List<VectorXYZ> colorsAsVectors = colors == null ? null
				: colors.stream().map(c -> new VectorXYZ(c.red, c.green, -c.blue)).toList();

		/* use indices, so that vertices shared by multiple triangles are only stored once.
		 * Vertices are compared by the values which will actually be written, not by the original coordinates,
		 * because only those decide whether the resulting vertices are identical. */

		int[] vertexIndices = new int[positions.size()];
		var indexForVertex = new HashMap<VertexKey, Integer>();

		var uniquePositions = new ArrayList<VectorXYZ>();
		var uniqueNormals = new ArrayList<VectorXYZ>();
		@Nullable var uniqueTexCoords = texCoords == null ? null : new ArrayList<VectorXZ>();
		@Nullable var uniqueColors = colorsAsVectors == null ? null : new ArrayList<VectorXYZ>();

		for (int i = 0; i < positions.size(); i++) {

			var key = new VertexKey(positions.get(i), normals.get(i),
					texCoords == null ? null : texCoords.get(i),
					colorsAsVectors == null ? null : colorsAsVectors.get(i));

			Integer index = indexForVertex.get(key);

			if (index == null) {
				index = uniquePositions.size();
				indexForVertex.put(key, index);
				uniquePositions.add(positions.get(i));
				uniqueNormals.add(normals.get(i));
				if (uniqueTexCoords != null) { uniqueTexCoords.add(texCoords.get(i)); }
				if (uniqueColors != null) { uniqueColors.add(colorsAsVectors.get(i)); }
			}

			vertexIndices[i] = index;

		}

		/* put geometry into buffers and set up accessors */

		primitive.indices = createIndexAccessor(vertexIndices, uniquePositions.size());

		if (positionQuantization != null) {
			primitive.attributes.put("POSITION",
					createQuantizedPositionAccessor(uniquePositions, positionQuantization));
			primitive.attributes.put("NORMAL", createQuantizedNormalAccessor(uniqueNormals));
		} else {
			primitive.attributes.put("POSITION", createAccessor(3, uniquePositions));
			primitive.attributes.put("NORMAL", createAccessor(3, uniqueNormals));
		}

		if (uniqueTexCoords != null) {
			primitive.attributes.put("TEXCOORD_0", createAccessor(2, uniqueTexCoords));
		}

		if (uniqueColors != null) {
			primitive.attributes.put("COLOR_0", createAccessor(3, uniqueColors));
		}

		gltf.meshes.add(gltfMesh);
		return gltf.meshes.size() - 1;

	}

	/** the values written to the output for a single vertex, used to identify vertices which can be shared */
	private record VertexKey(float[] componentArray) {

		VertexKey(VectorXYZ position, VectorXYZ normal, @Nullable VectorXZ texCoord, @Nullable VectorXYZ color) {

			this(new float[6 + (texCoord != null ? 2 : 0) + (color != null ? 3 : 0)]);

			System.arraycopy(components(3, position), 0, componentArray, 0, 3);
			System.arraycopy(components(3, normal), 0, componentArray, 3, 3);

			int offset = 6;

			if (texCoord != null) {
				System.arraycopy(components(2, texCoord), 0, componentArray, offset, 2);
				offset += 2;
			}

			if (color != null) {
				System.arraycopy(components(3, color), 0, componentArray, offset, 3);
			}

		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof VertexKey other && Arrays.equals(componentArray, other.componentArray);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(componentArray);
		}

	}

	/**
	 * creates an accessor for the indices of a primitive
	 *
	 * @param numVertices  number of vertices the indices refer to, decides the component type
	 */
	private int createIndexAccessor(int[] indices, int numVertices) {

		// unsigned short can be used for up to 65535 vertices (the largest value is reserved for primitive restart)
		boolean unsignedShort = numVertices <= 65535;

		int byteLengthRaw = indices.length * (unsignedShort ? 2 : 4);
		int byteLength = (byteLengthRaw + 3) / 4 * 4; // pad to multiple of 4 bytes, so later buffer views stay aligned

		ByteBuffer byteBuffer = ByteBuffer.allocate(byteLength);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		for (int index : indices) {
			if (unsignedShort) {
				byteBuffer.putShort((short) index);
			} else {
				byteBuffer.putInt(index);
			}
		}

		var accessor = new GltfAccessor(
				unsignedShort ? GltfAccessor.TYPE_UNSIGNED_SHORT : GltfAccessor.TYPE_UNSIGNED_INT,
				indices.length, "SCALAR");
		accessor.bufferView = createBufferView(byteBuffer, GltfBufferView.TARGET_ELEMENT_ARRAY_BUFFER);
		gltf.accessors.add(accessor);

		return gltf.accessors.size() - 1;

	}

	/**
	 * creates a float accessor.
	 * Can be used for any type of data (positions, normals, texture coordinates, or colors).
	 */
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

	/**
	 * parameters for representing vertex positions as 16-bit integers, see {@link #KHR_MESH_QUANTIZATION}.
	 * A quantized position q stands for the original position <code>offset + q * scale</code>,
	 * so the same offset and scale are applied to the scene's root node to undo the quantization.
	 */
	private record PositionQuantization(float[] offset, float scale) {

		private static final int MAX_VALUE = Short.MAX_VALUE;

		/** returns the quantization to use for a set of vertices */
		public static @Nullable PositionQuantization forVertices(Collection<VectorXYZ> vertices) {

			if (vertices.isEmpty()) return null;

			var min = new float[] {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
			var max = new float[] {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};

			for (VectorXYZ v : vertices) {
				float[] components = components(3, v);
				for (int i = 0; i < 3; i++) {
					min[i] = Math.min(min[i], components[i]);
					max[i] = Math.max(max[i], components[i]);
				}
			}

			float[] offset = new float[3];
			float largestExtent = 0;

			for (int i = 0; i < 3; i++) {
				offset[i] = (min[i] + max[i]) / 2;
				largestExtent = Math.max(largestExtent, max[i] - min[i]);
			}

			/* all axes share a scale: a non-uniform scale on the root node would require renderers to
			 * correct the normals, and the precision gained on the shorter axes is not needed anyway */

			float scale = largestExtent > 0 ? largestExtent / (2 * MAX_VALUE) : 1;

			return new PositionQuantization(offset, scale);

		}

		public short quantize(float value, int axis) {
			int result = Math.round((value - offset[axis]) / scale);
			return (short) Math.max(-MAX_VALUE, Math.min(MAX_VALUE, result));
		}

		public float[] scaleXYZ() {
			return new float[] {scale, scale, scale};
		}

	}

	/**
	 * creates a POSITION accessor with 16-bit integer components instead of floats,
	 * which requires the {@link #KHR_MESH_QUANTIZATION} extension
	 */
	private int createQuantizedPositionAccessor(List<VectorXYZ> vs, PositionQuantization quantization) {

		int byteStride = 8; // 3 short components, padded to the 4 byte alignment required for vertex attributes

		var min = new short[] {Short.MAX_VALUE, Short.MAX_VALUE, Short.MAX_VALUE};
		var max = new short[] {Short.MIN_VALUE, Short.MIN_VALUE, Short.MIN_VALUE};

		ByteBuffer byteBuffer = ByteBuffer.allocate(byteStride * vs.size());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		for (VectorXYZ v : vs) {

			float[] components = components(3, v);

			for (int i = 0; i < 3; i++) {
				short component = quantization.quantize(components[i], i);
				byteBuffer.putShort(component);
				min[i] = (short) Math.min(min[i], component);
				max[i] = (short) Math.max(max[i], component);
			}

			byteBuffer.putShort((short) 0); // padding

		}

		var accessor = new GltfAccessor(GltfAccessor.TYPE_SHORT, vs.size(), "VEC3");
		accessor.bufferView = createBufferView(byteBuffer, GltfBufferView.TARGET_ARRAY_BUFFER, byteStride);
		accessor.min = new float[] {min[0], min[1], min[2]};
		accessor.max = new float[] {max[0], max[1], max[2]};
		gltf.accessors.add(accessor);

		return gltf.accessors.size() - 1;

	}

	/**
	 * creates a NORMAL accessor with normalized 8-bit integer components instead of floats,
	 * which requires the {@link #KHR_MESH_QUANTIZATION} extension
	 */
	private int createQuantizedNormalAccessor(List<VectorXYZ> vs) {

		int byteStride = 4; // 3 byte components, padded to the 4 byte alignment required for vertex attributes

		ByteBuffer byteBuffer = ByteBuffer.allocate(byteStride * vs.size());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		for (VectorXYZ v : vs) {

			float[] components = components(3, v);

			for (int i = 0; i < 3; i++) {
				// a normalized byte c stands for the value max(c / 127, -1)
				byteBuffer.put((byte) Math.max(-127, Math.min(127, Math.round(components[i] * 127))));
			}

			byteBuffer.put((byte) 0); // padding

		}

		var accessor = new GltfAccessor(GltfAccessor.TYPE_BYTE, vs.size(), "VEC3");
		accessor.normalized = true;
		accessor.bufferView = createBufferView(byteBuffer, GltfBufferView.TARGET_ARRAY_BUFFER, byteStride);
		gltf.accessors.add(accessor);

		return gltf.accessors.size() - 1;

	}

	private int createBufferView(ByteBuffer byteBuffer, @Nullable Integer target) {
		return createBufferView(byteBuffer, target, null);
	}

	/**
	 * @param byteStride  distance between the elements of the buffer view in bytes,
	 *                    null if they are tightly packed
	 */
	private int createBufferView(ByteBuffer byteBuffer, @Nullable Integer target, @Nullable Integer byteStride) {

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
		view.byteStride = byteStride;

		gltf.bufferViews.add(view);
		return gltf.bufferViews.size() - 1;

	}

	/**
	 * creates a glTF material, or returns the index of an existing one if an equivalent material exists
	 *
	 * @param color  color to multiply the material's base color with, e.g. from vertex colors.
	 *               Materials with different colors cannot share a glTF material.
	 */
	private int createMaterial(Material m, @Nullable TextureLayer textureLayer, @Nullable LColor color)
			throws IOException {

		var key = new MaterialWithColor(m, color);
		if (materialIndexMap.containsKey(key)) return materialIndexMap.get(key);

		GltfMaterial material = new GltfMaterial();
		material.pbrMetallicRoughness = new PbrMetallicRoughness();
		material.pbrMetallicRoughness.roughnessFactor = 1.0f;

		if (color != null) {
			material.pbrMetallicRoughness.baseColorFactor = color.componentsRGBA();
		}

		material.name = NameUtil.getMaterialName(m, textureLayer, config);

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

		if (material.pbrMetallicRoughness.metallicRoughnessTexture == null) {
			material.pbrMetallicRoughness.metallicFactor = 0f;
		} else {
			material.pbrMetallicRoughness.metallicFactor = 1.0f;
		}

		gltf.materials.add(material);
		int index = gltf.materials.size() - 1;
		materialIndexMap.put(key, index);
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
				textureData.writeRasterImageToStream(stream, config.textureQuality(), config.maxTextureResolution());
				image.bufferView = createBufferView(asPaddedByteBuffer(stream.toByteArray(), (byte) 0x00), null);
				image.mimeType = textureData.getRasterImageFormat().mimeType();
			}
		} else {
			image.uri = switch (mode) {
				case REFERENCE -> resourceOutputSettings.buildTextureReference(textureData);
				case STORE_SEPARATELY_AND_REFERENCE -> resourceOutputSettings.storeTexture(textureData, outputDir().toURI(), config);
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
		if ("false".equals(config.exportLevels())) {
			mergeOptions.add(MergeOption.MERGE_METADATA_PROPERTIES);
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

		Multimap<ElementMetadata, MeshWithMetadata> meshesByMetadata = processedMeshStore.meshesByElementMetadata();

		/* decide whether to quantize the geometry. This makes the file smaller, but slightly less precise,
		 * so it is limited to the levels of detail which are intended to be viewed from a distance. */

		if (config.gltfExtensionWhitelistAllows(KHR_MESH_QUANTIZATION) && lod.ordinal() <= LevelOfDetail.LOD1.ordinal()) {
			positionQuantization = PositionQuantization.forVertices(processedMeshStore.meshes().stream()
					.flatMap(it -> it.geometry.asTriangles().vertices().stream())
					.toList());
		}

		/* define metadata for the scene and root node */

		var sceneMetadata = new HashMap<String, Object>();

		if (origin != null) {
			sceneMetadata.put("origin", Map.of("lat", origin.lat, "lon", origin.lon, "ele", 0));
		}

		/* create the basic structure of the glTF */

		gltf.asset = new GltfAsset();
		gltf.asset.version = "2.0";
		gltf.asset.generator = "OSM2World " + GlobalValues.VERSION_STRING;

		if (positionQuantization != null) {
			gltf.extensionsUsed = List.of(KHR_MESH_QUANTIZATION);
			gltf.extensionsRequired = List.of(KHR_MESH_QUANTIZATION);
		}

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

		if (positionQuantization != null) {
			// undo the quantization of the vertex positions for the entire scene
			rootNode.translation = positionQuantization.offset();
			rootNode.scale = positionQuantization.scaleXYZ();
		}

		rootNode.children = new ArrayList<>();

		for (ElementMetadata elementMetadata : meshesByMetadata.keySet()) {

			List<Integer> meshNodeIndizes = new ArrayList<>(meshesByMetadata.size());

			FaultTolerantIterationUtil.forEach(meshesByMetadata.get(elementMetadata), (MeshWithMetadata mesh) -> {
				try {
					int index = createNode(createMesh(mesh.mesh()), null);
					meshNodeIndizes.add(index);
					addExtrasToMeshNode(gltf.nodes.get(index), mesh.metadata(), config);
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

				assert meshNodeIndizes.size() == 1;
				addNameAndExtrasToParentNode(gltf.nodes.get(meshNodeIndizes.get(0)), elementMetadata, config);

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
			JsonUtil.toJson(gltf, writer, true);
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

	private static void addNameAndExtrasToParentNode(GltfNode node, ElementMetadata metadata, O2WConfig config) {

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

	private static void addExtrasToMeshNode(GltfNode node, MeshWithMetadata.MeshMetadata metadata, O2WConfig config) {

		Map<String, Object> extras = new HashMap<>();

		if (!"false".equals(config.exportLevels())
				&& metadata.extraProperties().containsKey("level")) {
			extras.put("level", metadata.extraProperties().get("level"));
		}

		node.extras = extras;

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