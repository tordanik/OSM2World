package org.osm2world;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.TextureData;
import org.osm2world.scene.material.TextureLayer;
import org.osm2world.scene.material.UriTexture;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.MeshStore;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.uri.BrowserHttpClient;
import org.osm2world.util.uri.LoadUriUtil;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSTopLevel;

public class WebLibrary {

	static {
		LoadUriUtil.setClientFactory(BrowserHttpClient::new);
	}

	@JSTopLevel
	public static class WebMesh {

		private final String baseColorTexture;
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

			if (textureLayer != null && textureLayer.baseColorTexture instanceof UriTexture t) {
				this.baseColorTexture = t.getUri().getPath();
			} else {
				this.baseColorTexture = null;
			}

			if (textureLayer != null && textureLayer.normalTexture instanceof UriTexture t) {
				this.normalTexture = t.getUri().getPath();
			} else {
				this.normalTexture = null;
			}

			if (textureLayer != null && textureLayer.ormTexture instanceof UriTexture t) {
				this.ormTexture = t.getUri().getPath();
			} else {
				this.ormTexture = null;
			}

			if (textureLayer != null && textureLayer.displacementTexture instanceof UriTexture t) {
				this.displacementTexture = t.getUri().getPath();
			} else {
				this.displacementTexture = null;
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

	}

	// TODO: add a config object

	@JSExport
	public static WebMesh[] convert(String elementType, String[] tags) {

		if (tags.length % 2 != 0) {
			throw new IllegalArgumentException("tags must have an even number of strings (key-value pairs)");
		}

		var o2w = new O2WConverterImpl(new O2WConfig(Map.of("lod", "4")), List.of());

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

		} catch (Exception e) {
			return null;
		}

	}

}