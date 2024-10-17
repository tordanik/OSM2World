package org.osm2world.core.target.gltf;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.osm2world.core.math.algorithms.NormalCalculationUtil.*;
import static org.osm2world.core.target.TargetUtil.*;
import static org.osm2world.core.target.common.ResourceOutputSettings.ResourceOutputMode.*;
import static org.osm2world.core.target.common.material.Material.Interpolation.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.cesiumjs.WGS84Util;
import org.osm2world.core.GlobalValues;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.target.TargetUtil.Compression;
import org.osm2world.core.target.common.MeshStore;
import org.osm2world.core.target.common.MeshStore.MeshMetadata;
import org.osm2world.core.target.common.MeshStore.MeshProcessingStep;
import org.osm2world.core.target.common.MeshStore.MeshWithMetadata;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.MeshTarget.MergeMeshes.MergeOption;
import org.osm2world.core.target.common.ResourceOutputSettings;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
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
import org.osm2world.core.target.gltf.tiles_data.TilesetAsset;
import org.osm2world.core.target.gltf.tiles_data.TilesetParentEntry;
import org.osm2world.core.target.gltf.tiles_data.TilesetRoot;
import org.osm2world.core.util.ConfigUtil;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.color.LColor;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

/**
 * builds a glTF or glb (binary glTF) output file
 */
public class GltfTarget extends MeshTarget {

	//TODO: Make configurable
	private static final int NUM_MESHES_FOR_SUBDIVISION_TOP = 100;

	public enum GltfFlavor { GLTF, GLB }

	private final File outputFile;
	private final GltfFlavor flavor;
	private final Compression compression;
	private final MapProjection mapProjection;
	private final @Nullable SimpleClosedShapeXZ bounds;

	/** the gltf asset under construction */
	private final Gltf gltf = new Gltf();

	private final Map<Material, Integer> materialIndexMap = new HashMap<>();
	private final Map<TextureData, Integer> textureIndexMap = new HashMap<>();

	/** data for the glb BIN chunk, only used if {@link #flavor} is {@link GltfFlavor#GLB} */
	private final List<ByteBuffer> binChunkData = new ArrayList<>();

	public GltfTarget(File outputFile, GltfFlavor flavor, Compression compression,
					  MapProjection mapProjection,
					  @Nullable SimpleClosedShapeXZ bounds) {
		this.outputFile = outputFile;
		this.flavor = flavor;
		this.compression = compression;
		this.mapProjection = mapProjection;
		this.bounds = bounds;
	}

	public static final class MinMax {
		public double min = Double.MAX_VALUE;
		public double max = Double.MIN_VALUE;
	}

	public File outputDir() {
		return outputFile.getParentFile();
	}

	@Override
	public String toString() {
		return "GltfTarget(" + outputFile + ")";
	}

	@Override
	public void finish() {
		MeshStore processedMeshStore = processMeshStore();

		List<MeshWithMetadata> meshesWithMetadata = processedMeshStore.meshesWithMetadata();
		meshesWithMetadata.sort(new MeshHeightAndSizeComparator());

		MinMax ymm = new MinMax();
		meshesWithMetadata.forEach(m -> {
			m.mesh().geometry.asTriangles().vertices().forEach(v -> {
				ymm.min = Double.min(ymm.min, v.y);
				ymm.max = Double.max(ymm.max, v.y);
			});
		});

		List<File> writtenFiles = new ArrayList<>();

		if (config.getBoolean("subdivideTiles", false)) {
			List<MeshWithMetadata> topMeshes = meshesWithMetadata.subList(0, Math.min(meshesWithMetadata.size(), NUM_MESHES_FOR_SUBDIVISION_TOP));
			File p = this.outputFile.getParentFile();

			String fname = this.outputFile.getName();
			String ext = getExt(fname);
			String baseName = fname.replace(ext, "");

			File out = new File(p, baseName + "_0" + ext);
			writeToFile(out, this.compression, new MeshStore(topMeshes));

			writtenFiles.add(out);

			List<MeshWithMetadata> restMeshes = meshesWithMetadata.subList(Math.min(meshesWithMetadata.size(), 100), meshesWithMetadata.size());
			writeToFile(this.outputFile, this.compression, new MeshStore(restMeshes));
			writtenFiles.add(this.outputFile);
		}
		else {
			writeToFile(this.outputFile, this.compression, processedMeshStore);
		}

		if (config.getBoolean("writeTilesetJson", false)) {
			File p = this.outputFile.getParentFile();
			String fname = this.outputFile.getName();
			String ext = getExt(fname);
			String baseName = fname.replace(ext, "");
			writeTileset(new File(p, baseName + ".tileset.json"), writtenFiles, mapProjection.getOrigin(), bounds, ymm.min, ymm.max);
		}
	}

	private String getExt(String fname) {
		String ext = "";

		if (fname.endsWith(".gz")) {
			ext = ".gz";
		}
		if (fname.endsWith(".zip")) {
			ext = ".zip";
		}

		if (fname.endsWith(".gltf" + ext)) {
			ext = ".gltf" + ext;
		}
		if (fname.endsWith(".glb" + ext)) {
			ext = ".glb" + ext;
		}

		return ext;
	}

	private void writeToFile(File outputFile, Compression compression, MeshStore processedMeshStore) {
		writeFileWithCompression(outputFile, compression, outputStream -> {
			try {
				if (flavor == GltfFlavor.GLTF) {
					writeJson(processedMeshStore, outputStream);
				} else {
					try (var jsonChunkOutputStream = new ByteArrayOutputStream()) {
						writeJson(processedMeshStore, jsonChunkOutputStream);
						ByteBuffer jsonChunkData = asPaddedByteBuffer(jsonChunkOutputStream.toByteArray(), (byte) 0x20);
						writeGlb(outputStream, jsonChunkData, binChunkData);
					}
				}
			} catch (IOException | JsonIOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private MeshStore processMeshStore() {
		boolean keepOsmElements = config.getBoolean("keepOsmElements", true);
		boolean clipToBounds = config.getBoolean("clipToBounds", false);

		/* process the meshes */

		EnumSet<MergeOption> mergeOptions = EnumSet.noneOf(MergeOption.class);

		if (!keepOsmElements) {
			mergeOptions.add(MergeOption.MERGE_ELEMENTS);
		}

		LevelOfDetail lod = ConfigUtil.readLOD(config);

		// TODO: split into subtiles eighter here as one step
		// (good option save tile in meta),
		// or one step up by calling processMeshStore
		// multiple times with different bboxes (bad option)

		List<MeshProcessingStep> processingSteps = new ArrayList<>(asList(
				new FilterLod(lod),
				new EmulateTextureLayers(lod.ordinal() <= 1 ? 1 : Integer.MAX_VALUE),
				new MoveColorsToVertices(), // after EmulateTextureLayers because colorable is per layer
				new ReplaceTexturesWithAtlas(t -> getResourceOutputSettings().modeForTexture(t) == REFERENCE),
				new MergeMeshes(mergeOptions)));

		if (clipToBounds && bounds != null) {
			processingSteps.add(1, new ClipToBounds(bounds, true));
		}

		return meshStore.process(processingSteps);
	}

	/*
	Working example for tileset
	{
		"asset" : {
			"version": "1.0"
		},
		"root": {
			"content": {
				"uri": "14_5298_5916_0.glb"
			},
			"refine": "ADD",
			"geometricError": 25,
			"boundingVolume": {
				"region": [-1.1098350999480917,0.7790694465970149,-1.1094516048185785,0.779342292568195,0.0,100]
			},
			"transform": [
				0.895540041198885,    0.4449809373551852,  0.0,                0.0,
				-0.31269461895546163, 0.6293090971636892,  0.7114718093525005, 0.0,
				0.3165913926274654,   -0.6371514934593837, 0.7027146394495273, 0.0,
				2022609.150078308,    -4070573.2078238726, 4459382.83869308,   1.0
			],
			"children": [{
				"boundingVolume": {
					"region": [-1.1098350999480917,0.7790694465970149,-1.1094516048185785,0.779342292568195,0.0,97.49999999999997]
				},
				"geometricError": 0,
				"content": {
					"uri": "14_5298_5916.glb"
				}
			}]
		}
	}*/
	private void writeTileset(File outFile, List<File> tileContentFiles, LatLon origin, SimpleClosedShapeXZ fullBBOX, double minY, double maxY) {

		AxisAlignedRectangleXZ bbox = fullBBOX.boundingBox();
		
		VectorXYZ cartesianOrigin = WGS84Util.cartesianFromLatLon(origin, 0.0);
		double[] transform = WGS84Util.eastNorthUpToFixedFrame(cartesianOrigin);

		// left top
		LatLon westNorth = this.mapProjection.toLatLon(new VectorXZ(bbox.minX, bbox.maxZ));
		// right bottom
		LatLon eastSouth = this.mapProjection.toLatLon(new VectorXZ(bbox.maxX, bbox.minZ));

		double[] boundingRegion = new double[] {
			Math.toRadians(westNorth.lon), // west
			Math.toRadians(eastSouth.lat), // south
			Math.toRadians(eastSouth.lon), // east
			Math.toRadians(westNorth.lat), // north
			minY,
			maxY
		};

		TilesetRoot tileset = new TilesetRoot();
		tileset.setAsset(new TilesetAsset("1.0"));

		TilesetParentEntry root = new TilesetParentEntry();
		tileset.setRoot(root);

		root.setTransform(transform);
		root.setGeometricError(25);
		root.setBoundingVolume(boundingRegion);
		root.setContent(tileContentFiles.get(0).getName());

		if (tileContentFiles.size() > 1) {
			root.addChild(tileContentFiles.get(1).getName());
		}

		Gson gson = new GsonBuilder().create();
		String out = gson.toJson(tileset);

		try (PrintWriter metaWriter = new PrintWriter(new FileOutputStream(outFile))) {
			metaWriter.println(out);
			metaWriter.flush();
		}
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
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
	private int createMesh(Mesh mesh) throws IOException {

		GltfMesh gltfMesh = new GltfMesh();

		Material material = mesh.material;

		TriangleGeometry triangleGeometry = mesh.geometry.asTriangles();
		List<? extends TriangleXYZ> triangles = triangleGeometry.triangles;
		List<List<VectorXZ>> texCoordLists = triangleGeometry.texCoords;
		List<LColor> colors = triangleGeometry.colors == null ? null
				: triangleGeometry.colors.stream().map(LColor::fromAWT).toList();

		texCoordLists = flipTexCoordsVertically(texCoordLists); // move texture coordinate origin to the top left

		GltfMesh.Primitive primitive = new GltfMesh.Primitive();
		gltfMesh.primitives.add(primitive);

		/* convert material */

		int materialIndex;
		if (material.getNumTextureLayers() == 0) {
			materialIndex = createMaterial(material, null);
		} else {
			materialIndex = createMaterial(material, material.getTextureLayers().get(0));
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

		material.alphaMode = switch (m.getTransparency()) {
			case FALSE -> "OPAQUE";
			case BINARY -> "MASK";
			case TRUE -> "BLEND";
		};

		material.doubleSided = m.isDoubleSided();

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
				textureData.writeRasterImageToStream(stream, config.getFloat("textureQuality", 0.75f));
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
	 * @param processedMeshStore
	 */
	private void writeJson(MeshStore processedMeshStore, OutputStream outputStream) throws IOException {
		boolean keepOsmElements = config.getBoolean("keepOsmElements", true);

		/* create the basic structure of the glTF */

		gltf.asset = new GltfAsset();
		gltf.asset.version = "2.0";
		gltf.asset.generator = "OSM2World " + GlobalValues.VERSION_STRING;

		gltf.scene = 0;
		gltf.scenes = List.of(new GltfScene());
		gltf.scenes.get(0).nodes = List.of(0);

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

		Multimap<MeshMetadata, Mesh> meshesByMetadata = processedMeshStore.meshesByMetadata();
		// int top = 100;

		for (MeshMetadata objectMetadata : meshesByMetadata.keySet()) {

			List<Integer> meshNodeIndizes = new ArrayList<>(meshesByMetadata.size());

			FaultTolerantIterationUtil.forEach(meshesByMetadata.get(objectMetadata), (Mesh mesh) -> {
				try {
					int index = createNode(createMesh(mesh), null);
					meshNodeIndizes.add(index);
				} catch (JsonIOException | IOException e) {
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

				meshNodeIndizes.forEach(index -> addMeshNameAndId(gltf.nodes.get(index), objectMetadata));

			}

			rootNode.children.addAll(meshNodeIndizes);

			// if (top-- == 0) {
			// 	break;
			// }
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
			new GsonBuilder().setPrettyPrinting().create().toJson(gltf, writer);
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
		} else if (textureLayer != null && m.getNumTextureLayers() > 1) {
			name += "_layer" + m.getTextureLayers().indexOf(textureLayer);
		}

		return name;

	}

	private static void addMeshNameAndId(GltfNode node, MeshMetadata metadata) {

		MapRelation.Element mapElement = metadata.mapElement();

		if (mapElement != null) {
			Map<String, Object> extras = new HashMap<>();
			extras.put("osmId", mapElement.toString());
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

	private ResourceOutputSettings getResourceOutputSettings() {
		File textureDir = new File(outputDir(), FilenameUtils.removeExtension(outputFile.getName()) + "_textures");
		return ResourceOutputSettings.fromConfig(config, textureDir.toURI(), true);
	}

}