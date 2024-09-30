package org.osm2world.core.target.gltf;

import static java.lang.Boolean.TRUE;
import static org.osm2world.core.target.common.material.Materials.PLASTIC;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.gltf.data.*;
import org.osm2world.core.util.color.LColor;

import com.google.gson.Gson;

public class GltfModel implements Model {

	private final Gltf gltf;
	private final String source;

	public GltfModel(Gltf gltf, String source) {

		this.gltf = gltf;
		this.source = source;

		if (!"2.0".equals(gltf.asset.version)) {
			throw new IllegalArgumentException("Only glTF 2.0 assets supported");
		}

	}

	public String toString() {
		return source;
	}

	@Override
	public List<Mesh> buildMeshes(InstanceParameters params) {

		try {

			/* collect meshes from all nodes */

			List<Mesh> result = new ArrayList<>();

			GltfScene scene = gltf.scenes.get(gltf.scene);
			for (int n : scene.nodes) {
				result.addAll(buildMeshesForNode(gltf.nodes.get(n)));
			}

			return result;

		} catch (Exception e) {
			ConversionLog.error("Could not build meshes from glTF asset " + source, e);
			return List.of();
		}

	}

	private List<? extends Mesh> buildMeshesForNode(GltfNode node) {

		List<Mesh> result = new ArrayList<>();

		// TODO consider nodes' transform properties

		/* build this node's mesh */

		if (node.mesh != null && gltf.meshes.size() > node.mesh) {

			GltfMesh mesh = gltf.meshes.get(node.mesh);

			for (GltfMesh.Primitive primitive : mesh.primitives) {

				// construct the mesh material

				Material material;
				if (primitive.material == null) {
					// spec: "If material is undefined, then a default material MUST be used."
					material = PLASTIC;
				} else {
					var gltfMaterial = gltf.materials.get(primitive.material);
					material = convertMaterial(gltfMaterial);
				}

				// construct the mesh geometry

				if (primitive.mode == GltfMesh.TRIANGLES) {
					// TODO support strips and fans as well

					GltfAccessor positionAccessor = gltf.accessors.get(primitive.attributes.get("POSITION"));
					List<VectorXYZ> positions = readVectorsFromAccessor(VectorXYZ.class, positionAccessor);

					@Nullable List<Color> colors = null;
					if (primitive.attributes.containsKey("COLOR_0")) {
						GltfAccessor colorAccessor = gltf.accessors.get(primitive.attributes.get("COLOR_0"));
						List<VectorXYZ> colorsXYZ = readVectorsFromAccessor(VectorXYZ.class, colorAccessor);
						colors = colorsXYZ.stream().map(c -> new LColor((float)c.x, (float)c.y, (float)-c.z).toAWT()).toList();
					}

					@Nullable List<VectorXYZ> normals = null;
					if (primitive.attributes.containsKey("NORMAL")) {
						GltfAccessor normalAccessor = gltf.accessors.get(primitive.attributes.get("NORMAL"));
						normals = readVectorsFromAccessor(VectorXYZ.class, normalAccessor);
					}

					@Nullable List<VectorXZ> texCoords = null;
					if (primitive.attributes.containsKey("TEXCOORD_0")) {
						GltfAccessor texCoordAccessor = gltf.accessors.get(primitive.attributes.get("TEXCOORD_0"));
						texCoords = readVectorsFromAccessor(VectorXZ.class, texCoordAccessor);
					}

					if (primitive.indices != null) {
						GltfAccessor indexAccessor = gltf.accessors.get(primitive.indices);
						throw new UnsupportedOperationException("Indexed geometry not supported");
					}

					assert positions.size() % 3 == 0;
					assert colors == null || colors.size() == positions.size();
					assert normals == null || normals.size() == positions.size();
					assert texCoords == null || texCoords.size() == positions.size();

					var geometryBuilder = new TriangleGeometry.Builder(
							material.getNumTextureLayers(),
							null,
							normals == null ? Material.Interpolation.FLAT : null);

					List<TriangleXYZ> triangles = new ArrayList<>(positions.size() / 3);
					for (int i = 0; i < positions.size(); i += 3) {
						triangles.add(new TriangleXYZ(positions.get(i), positions.get(i + 1), positions.get(i + 2)));
					}

					geometryBuilder.addTriangles(triangles,
							texCoords == null ? null : List.of(texCoords),
							colors, normals);

					result.add(new Mesh(geometryBuilder.build(), material));

				} else {
					ConversionLog.warn("Unsupported mode " + primitive.mode);
				}

			}

		}

		/* add meshes from child nodes */

		if (node.children != null) {
			for (int child : node.children) {
				result.addAll(buildMeshesForNode(gltf.nodes.get(child)));
			}
		}

		return result;

	}

	private <T extends Vector3D> List<T> readVectorsFromAccessor(Class<T> type, GltfAccessor accessor) {

		switch (accessor.type) {
			case "VEC2" -> {
				if (!type.equals(VectorXZ.class)) throw new IllegalArgumentException("Incorrect vector type " + type);
			}
			case "VEC3" -> {
				if (!type.equals(VectorXYZ.class)) throw new IllegalArgumentException("Incorrect vector type " + type);
			}
			default -> throw new UnsupportedOperationException("Unsupported accessor type " + accessor.type);
		}

		if (accessor.componentType != GltfAccessor.TYPE_FLOAT) {
			throw new UnsupportedOperationException("Unsupported component type " + accessor.componentType);
		} else if (accessor.normalized == TRUE || accessor.sparse == TRUE
				|| (accessor.byteOffset != null && accessor.byteOffset != 0)) {
			throw new UnsupportedOperationException("Unsupported accessor option present");
		}

		GltfBufferView bufferView = gltf.bufferViews.get(accessor.bufferView);

		ByteBuffer byteBuffer = readBufferView(bufferView);

		List<Vector3D> result = new ArrayList<>(accessor.count);

		for (int i = 0; i < accessor.count; i++) {
			if ("VEC2".equals(accessor.type)) {
				result.add(new VectorXZ(
						byteBuffer.getFloat(),
						byteBuffer.getFloat()));
			} else {
				result.add(new VectorXYZ(
						byteBuffer.getFloat(),
						byteBuffer.getFloat(),
						-1 * byteBuffer.getFloat()));
			}
		}

		return (List<T>) result;

	}

	private ByteBuffer readBufferView(GltfBufferView bufferView) {

		if (bufferView.byteStride != null) {
			throw new UnsupportedOperationException("Unsupported bufferView option present");
		}

		int byteOffset = bufferView.byteOffset == null ? 0 : bufferView.byteOffset;
		int byteLength = bufferView.byteLength;

		GltfBuffer buffer = gltf.buffers.get(bufferView.buffer);

		var pattern = Pattern.compile("data:application/gltf-buffer;base64,(.+)");
		var matcher = pattern.matcher(buffer.uri);

		if (matcher.matches()) {

			// load data URI
			String encodedData = matcher.group(1);
			byte[] data = Base64.getDecoder().decode(encodedData);
			ByteBuffer byteBuffer = ByteBuffer.wrap(data);
			byteBuffer.slice(byteOffset, byteLength);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			return byteBuffer;

		} else {

			// TODO implement
			throw new IllegalArgumentException("Only data uris supported");

		}

	}

	private Material convertMaterial(GltfMaterial gltfMaterial) {
		// TODO implement
		return PLASTIC;
	}

	public static GltfModel loadFromFile(File gltfFile) throws IOException {

		try (var reader = new FileReader(gltfFile)) {

			Gltf gltf = new Gson().fromJson(reader, Gltf.class);

			return new GltfModel(gltf, gltfFile.getName());

		} catch (Exception e) {
			throw new IOException("Could not read glTF model at " + gltfFile, e);
		}

	}

}
