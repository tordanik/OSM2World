package org.osm2world;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.osm.creation.JsonStringReader;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.*;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.MeshStore;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.json.JsonImplementationBrowser;
import org.osm2world.util.json.JsonUtil;
import org.osm2world.util.uri.BrowserHttpClient;
import org.osm2world.util.uri.LoadUriUtil;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSTopLevel;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.function.JSConsumer;

public class WebLibrary {

	static {
		LoadUriUtil.setClientFactory(BrowserHttpClient::new);
		JsonUtil.setImplementation(new JsonImplementationBrowser());
	}

	@JSTopLevel
	public static class O2WConfig {

		private final org.osm2world.conversion.O2WConfig config;

		O2WConfig(org.osm2world.conversion.O2WConfig config) {
			this.config = config;
		}

		private org.osm2world.conversion.O2WConfig getConfig() {
			return config;
		}

		@JSExport
		public String getProperty(String key) {
			return config.getString(key);
		}

	}

	@JSTopLevel
	public static class WebMesh {

		private final String baseColorTexture;
		private final String opacityTexture;
		private final String normalTexture;
		private final String ormTexture;
		private final String displacementTexture;

		private final boolean clampTextures;
		private final boolean transparency;
		private final float[] color;

		private final float[] positions;
		private final int[] indices;
		private final float[] normals;
		private final float[] uvs;

		@JSExport
		public String baseColorTexture() {
			return baseColorTexture;
		}

		@JSExport
		public String opacityTexture() {
			return opacityTexture;
		}

		@JSExport
		public String normalTexture() {
			return normalTexture;
		}

		@JSExport
		public String ormTexture() {
			return ormTexture;
		}

		@JSExport
		public String displacementTexture() {
			return displacementTexture;
		}

		@JSExport
		public boolean clampTextures() {
			return clampTextures;
		}
		@JSExport
		public boolean transparency() {
			return transparency;
		}
		@JSExport
		public float[] color() {
			return color;
		}

		@JSExport
		public float[] positions() {
			return positions;
		}
		@JSExport
		public int[] indices() {
			return indices;
		}
		@JSExport
		public float[] normals() {
			return normals;
		}
		@JSExport
		public float[] uvs() {
			return uvs;
		}

		public WebMesh(Material material, TriangleGeometry geom) {

			/* material fields */

			TextureLayer textureLayer = (!material.textureLayers().isEmpty())
					? material.textureLayers().get(0) : null;

			this.baseColorTexture = getTexturePath(textureLayer, l -> l.baseColorTexture);
			this.normalTexture = getTexturePath(textureLayer, l -> l.normalTexture);
			this.ormTexture = getTexturePath(textureLayer, l -> l.ormTexture);
			this.displacementTexture = getTexturePath(textureLayer, l -> l.displacementTexture);

			if (textureLayer != null && textureLayer.baseColorTexture instanceof CompositeTexture t
					&& t.mode == CompositeTexture.CompositeMode.ALPHA_FROM_A) {
				this.opacityTexture = getTexturePath(textureLayer, l -> t.textureA);
			} else {
				this.opacityTexture = null;
			}

			this.clampTextures = textureLayer != null && textureLayer.baseColorTexture.wrap != TextureData.Wrap.REPEAT;
			this.transparency = material.transparency() != Material.Transparency.FALSE;

			this.color = material.color().getColorComponents(null);

			/* geometry fields */

			List<VectorXYZ> geomVertices = geom.vertices();
			List<VectorXYZ> geomNormals = geom.normalData.normals();
			List<List<VectorXZ>> geomTexCoords = geom.texCoords;

			this.positions = new float[geomVertices.size() * 3];
			this.indices = new int[geomVertices.size()];
			this.normals = new float[geomVertices.size() * 3];
			this.uvs = new float[geomVertices.size() * 2];

			for (int i = 0; i < geomVertices.size(); i++) {

				this.positions[i * 3] = (float) geomVertices.get(i).x;
				this.positions[i * 3 + 1] = (float) geomVertices.get(i).y;
				this.positions[i * 3 + 2] = (float) geomVertices.get(i).z;

				this.indices[i] = i;

				this.normals[i * 3] = (float) geomNormals.get(i).x;
				this.normals[i * 3 + 1] = (float) geomNormals.get(i).y;
				this.normals[i * 3 + 2] = (float) geomNormals.get(i).z;

				if (!geomTexCoords.isEmpty()) {
					this.uvs[i * 2] = (float) geomTexCoords.get(0).get(i).x;
					this.uvs[i * 2 + 1] = 1.0f - (float) geomTexCoords.get(0).get(i).z;
				}

			}

		}

		private @Nullable String getTexturePath(@Nullable TextureLayer textureLayer, Function<TextureLayer, TextureData> getTexture) {
			if (textureLayer != null) {
				TextureData texture = getTexture.apply(textureLayer);
				if (texture instanceof UriTexture t) {
					return t.getUri().getPath();
				} else if (texture instanceof CompositeTexture t) {
					return getTexturePath(textureLayer, x -> t.textureB);
				}
			}
			return null;
		}

	}

	@JSExport
	public static void loadConfig(String uri, JSObject extraProperties,
			JSConsumer<O2WConfig> onSuccess, @Nullable JSConsumer<String> onError) {

		JSConsumer<String> handleError = (onError != null) ? onError : System.err::println;

		new Thread(() -> {
			try {
				Map<String, ?> properties = jsObjectToMap(extraProperties);
				var config = new org.osm2world.conversion.O2WConfig(properties, new URI(uri));
				onSuccess.accept(new O2WConfig(config));
			} catch (URISyntaxException e) {
				handleError.accept(e.getMessage());
			}
		}).start();

	}

	@JSBody(params = {"obj"}, script = "return Object.keys(obj);")
	private static native String[] getKeys(JSObject obj);

	@JSBody(params = {"obj", "key"}, script = "return obj[key];")
	private static native String getProperty(JSObject obj, String key);

	private static Map<String, String> jsObjectToMap(JSObject obj) {
		Map<String, String> map = new HashMap<>();
		for (String key : getKeys(obj)) {
			String value = getProperty(obj, key);
			if (value != null) {
				map.put(key, value);
			}
		}
		return map;
	}

	@JSExport
	public static void convert(@Nullable O2WConfig config, String elementType, String[] tags,
			JSConsumer<JSArray<WebMesh>> onSuccess, @Nullable JSConsumer<String> onError) {

		if (tags.length % 2 != 0) {
			throw new IllegalArgumentException("tags must have an even number of strings (key-value pairs)");
		}

		new Thread(() -> {

			var o2w = new O2WConverterImpl(config != null ? config.getConfig() : null, List.of());

			var builder = new MapDataBuilder();

			switch (elementType) {
				case "node" -> builder.createNode(0, 0, TagSet.of(tags));
				case "way" -> {
					var wayNodes = List.of(
							builder.createNode(-7.5, -5.0),
							builder.createNode(7.5, 5.0)
					);
					builder.createWay(wayNodes, TagSet.of(tags));
				}
				case "area" -> {
					var wayNodes = List.of(
							builder.createNode(-7.5, -5.0),
							builder.createNode(7.5, -5.0),
							builder.createNode(7.5, 5.0),
							builder.createNode(-7.5, 5.0)
					);
					builder.createWayArea(wayNodes, TagSet.of(tags));
				}
				default -> throw new IllegalArgumentException("elementType must be 'node', 'way' or 'area'");
			}

			MapData mapData = builder.build();

			try {

				Scene scene = o2w.convert(mapData, null);

				WebMesh[] meshArray = sceneToMeshArray(scene);
				onSuccess.accept(JSArray.of(meshArray));

			} catch (Exception e) {
				if (onError != null) {
					onError.accept(e.getMessage());
				}
			}

		}).start();

	}

	/**
	 * Converts OSM data in JSON format to 3D geometry.
	 * Uses the OSM JSON dialect supported by Overpass API.
	 *
	 * @param osmJson  a JSON string
	 * @param config  OSM2World configuration received from loadConfig, can be null
	 * @param onSuccess  callback which will receive an array of WebMesh objects if the conversion succeeds
	 * @param onError   optional error callback, can be null
	 */
	@JSExport
	public static void convertJson(String osmJson, @Nullable O2WConfig config,
			JSConsumer<JSArray<WebMesh>> onSuccess, @Nullable JSConsumer<String> onError) {

		var osmReader = new JsonStringReader(osmJson);

		new Thread(() -> {

			var o2w = new O2WConverterImpl(config != null ? config.getConfig() : null, List.of());

			try {

				Scene scene = o2w.convert(osmReader, null, null);

				WebMesh[] meshArray = sceneToMeshArray(scene);
				onSuccess.accept(JSArray.of(meshArray));

			} catch (Exception e) {
				if (onError != null) {
					onError.accept(e.getMessage());
				}
			}

		}).start();

	}

	private static WebMesh[] sceneToMeshArray(Scene scene) {

		var meshStore = new MeshStore(scene.getMeshesWithMetadata());

		meshStore = meshStore.process(List.of(
				new MeshStore.EmulateDoubleSidedMaterials(),
				new MeshStore.EmulateTextureLayers(),
				new MeshStore.MergeMeshes(EnumSet.of(MeshStore.MergeMeshes.MergeOption.SINGLE_COLOR_MESHES))
		));
		List<Mesh> meshes = meshStore.meshes();

		List<WebMesh> webMeshes = new ArrayList<>();

		for (Mesh mesh : meshes) {
			webMeshes.add(new WebMesh(mesh.material, mesh.geometry.asTriangles()));
		}

		return webMeshes.toArray(WebMesh[]::new);

	}

}