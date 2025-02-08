package org.osm2world.target.gltf;

import static java.lang.Boolean.TRUE;
import static java.lang.Math.*;
import static org.osm2world.target.common.material.TextureData.Wrap;
import static org.osm2world.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.target.common.material.*;
import org.osm2world.target.common.mesh.LODRange;
import org.osm2world.target.common.mesh.Mesh;
import org.osm2world.target.common.mesh.TriangleGeometry;
import org.osm2world.target.common.model.InstanceParameters;
import org.osm2world.target.common.model.Model;
import org.osm2world.target.gltf.data.*;
import org.osm2world.util.color.LColor;
import org.osm2world.util.exception.InvalidGeometryException;

import com.google.gson.Gson;

public class GltfModel implements Model {

	/**
	 * placeholder color to be replaced with a color specific to this instance
	 * TODO: support this replacement in vertex colors, not just material base color
	 */
	private static final LColor PLACEHOLDER_COLOR = new LColor(1f, 0f, 1f);

	private final Gltf gltf;
	private final @Nullable File source;

	private final Map<Pair<GltfImage, Wrap>, TextureData> imageCache = new HashMap<>();

	public GltfModel(Gltf gltf, @Nullable File source) {

		this.gltf = gltf;
		this.source = source;

		if (!"2.0".equals(gltf.asset.version)) {
			throw new IllegalArgumentException("Only glTF 2.0 assets supported");
		}

	}

	public String toString() {
		return source != null ? source.getName() : super.toString();
	}

	@Override
	public List<Mesh> buildMeshes(InstanceParameters params) {

		try {

			@Nullable Color instanceColor = params.color();

			float rotAngle = (float) Angle.ofRadians(params.direction()).plus(Angle.ofDegrees(180)).radians;
			TransformationMatrix rootTransform = TransformationMatrix.forTRS(
					new float[] { (float)params.position().x, (float)params.position().y, (float)params.position().z },
					new float[] { 0, (float)sin(rotAngle/2), 0, (float)cos(rotAngle/2) },
					new float[] { 1, 1, 1 } // TODO provide scale
			);

			/* collect meshes from all nodes */

			List<Mesh> result = new ArrayList<>();

			GltfScene scene = gltf.scenes.get(gltf.scene);
			for (int n : scene.nodes) {
				result.addAll(buildMeshesForNode(gltf.nodes.get(n), rootTransform, params.lodRange(), instanceColor));
			}

			return result;

		} catch (Exception e) {
			ConversionLog.error("Could not build meshes from glTF asset " + source, e);
			return List.of();
		}

	}

	private List<? extends Mesh> buildMeshesForNode(GltfNode node, TransformationMatrix parentTransform,
				LODRange lodRange, @Nullable Color instanceColor) throws IOException {

		List<Mesh> result = new ArrayList<>();

		/* handle transformation matrices */

		TransformationMatrix localTransform = node.getLocalTransform();
		TransformationMatrix globalTransform = parentTransform.times(localTransform);

		/* build this node's mesh */

		if (node.mesh != null && gltf.meshes.size() > node.mesh) {

			GltfMesh mesh = gltf.meshes.get(node.mesh);

			for (GltfMesh.Primitive primitive : mesh.primitives) {

				// construct the mesh material

				GltfMaterial gltfMaterial;
				if (primitive.material == null) {
					// spec: "The default material [...] is defined to be a material with no properties specified"
					gltfMaterial = new GltfMaterial();
				} else {
					gltfMaterial = gltf.materials.get(primitive.material);
				}

				Material material = convertMaterial(gltfMaterial, instanceColor);

				// construct the mesh geometry

				int mode = primitive.mode != null ? primitive.mode : GltfMesh.TRIANGLES;

				if (mode == GltfMesh.TRIANGLES) {
					// TODO support strips and fans as well

					GltfAccessor positionAccessor = gltf.accessors.get(primitive.attributes.get("POSITION"));
					List<VectorXYZ> positions = readDataFromAccessor(VectorXYZ.class, positionAccessor);
					positions = positions.stream().map(globalTransform::applyTo).toList();

					@Nullable List<Color> colors = null;
					if (primitive.attributes.containsKey("COLOR_0")) {
						GltfAccessor colorAccessor = gltf.accessors.get(primitive.attributes.get("COLOR_0"));
						List<VectorXYZ> colorsXYZ = readDataFromAccessor(VectorXYZ.class, colorAccessor);
						colors = colorsXYZ.stream()
								.map(c -> new LColor(
										min(max(0f, (float)c.x), 1f),
										min(max(0f, (float)c.y), 1f),
										min(max(0f, (float)-c.z), 1f))
										.toAWT())
								.toList();
					}

					@Nullable List<VectorXYZ> normals = null;
					if (primitive.attributes.containsKey("NORMAL")) {
						GltfAccessor normalAccessor = gltf.accessors.get(primitive.attributes.get("NORMAL"));
						normals = readDataFromAccessor(VectorXYZ.class, normalAccessor);
						normals = normals.stream().map(globalTransform::applyToNormal).toList();
					}

					@Nullable List<VectorXZ> texCoords = null;
					if (primitive.attributes.containsKey("TEXCOORD_0")) {
						GltfAccessor texCoordAccessor = gltf.accessors.get(primitive.attributes.get("TEXCOORD_0"));
						texCoords = readDataFromAccessor(VectorXZ.class, texCoordAccessor);
					}

					@Nullable List<Integer> indices = null;
					if (primitive.indices != null) {
						GltfAccessor indexAccessor = gltf.accessors.get(primitive.indices);
						indices = readDataFromAccessor(Integer.class, indexAccessor);
					}

					assert colors == null || colors.size() == positions.size();
					assert normals == null || normals.size() == positions.size();
					assert texCoords == null || texCoords.size() == positions.size();

					/* build the geometry */

					var geometryBuilder = new TriangleGeometry.Builder(
							material.getNumTextureLayers(),
							null,
							normals == null ? material.getInterpolation() : null);

					if (indices == null) {

						assert positions.size() % 3 == 0;

						List<TriangleXYZ> triangles = new ArrayList<>(positions.size() / 3);
						for (int i = 0; i < positions.size(); i += 3) {
							triangles.add(new TriangleXYZ(positions.get(i), positions.get(i + 1), positions.get(i + 2)));
						}

						geometryBuilder.addTriangles(triangles,
								texCoords == null ? List.of() : List.of(texCoords),
								colors, normals);

					} else {

						assert indices.size() % 3 == 0;

						for (int i = 0; i < indices.size(); i += 3) {

							int i0 = indices.get(i);
							int i1 = indices.get(i + 1);
							int i2 = indices.get(i + 2);

							try {
								geometryBuilder.addTriangles(
										List.of(new TriangleXYZ(positions.get(i0), positions.get(i1), positions.get(i2))),
										texCoords == null ? List.of() : List.of(List.of(texCoords.get(i0), texCoords.get(i1), texCoords.get(i2))),
										colors == null ? null : List.of(colors.get(i0), colors.get(i1), colors.get(i2)),
										normals == null ? null : List.of(normals.get(i0), normals.get(i1), normals.get(i2))
								);
							} catch (InvalidGeometryException e) {
								ConversionLog.warn("Invalid geometry in glTF asset " + this, e);
							}

						}

					}

					result.add(new Mesh(geometryBuilder.build(), material, lodRange.min(), lodRange.max()));

				} else {
					ConversionLog.error("Unsupported mode " + mode + " in glTF asset " + this);
				}

			}

		}

		/* add meshes from child nodes */

		if (node.children != null) {
			for (int child : node.children) {
				result.addAll(buildMeshesForNode(gltf.nodes.get(child), globalTransform, lodRange, instanceColor));
			}
		}

		return result;

	}

	/**
	 * reads scalars or vectors from a {@link GltfAccessor}
	 *
	 * @param type  can be {@link Integer}, {@link Float}, {@link VectorXZ} or {@link VectorXYZ}
	 */
	@SuppressWarnings("unchecked")
	private <T> List<T> readDataFromAccessor(Class<T> type, GltfAccessor accessor) {

		if (accessor.sparse == TRUE) {
			throw new UnsupportedOperationException("Unsupported accessor option present");
		}

		int numComponents = switch (accessor.type) {
			case "SCALAR" -> 1;
			case "VEC2" -> 2;
			case "VEC3" -> 3;
			default -> throw new UnsupportedOperationException("Unsupported accessor type " + accessor.type);
		};

		boolean normalized = accessor.normalized == TRUE;

		/* read the buffer view */

		GltfBufferView bufferView = gltf.bufferViews.get(accessor.bufferView);
		ByteBuffer byteBuffer = readBufferView(bufferView);

		int accessorByteOffset = accessor.byteOffset != null ? accessor.byteOffset : 0;
		byteBuffer.position(accessorByteOffset);

		int byteStride = bufferView.byteStride == null ? 0 : bufferView.byteStride;

		/* read the components, taking into account byteOffset and byteStride */

		int totalComponents = accessor.count * numComponents;
		List<Float> components = new ArrayList<>(totalComponents);

		for (int i = 0; i < accessor.count; i++) {
			int previousPosition = byteBuffer.position();
			for (int j = 0; j < numComponents; j++) {
				components.add(readComponent(byteBuffer, accessor.componentType, normalized));
			}
			if (bufferView.byteStride != null && i + 1 < totalComponents && (i + 1) % numComponents == 0) {
				byteBuffer.position(previousPosition + byteStride);
			}
		}

		/* build the result from the components depending on accessor type */

		switch (accessor.type) {

			case "SCALAR" -> {
				if (!type.equals(Float.class) && !type.equals(Integer.class)) throw new IllegalArgumentException("Incorrect accessor type " + type);
				assert components.size() == accessor.count;
				List<Float> result = new ArrayList<>(accessor.count);
				for (int i = 0; i < accessor.count; i++) {
					result.add(components.get(i));
				}
				if (type.equals(Integer.class)) {
					return (List<T>) result.stream().map(f -> f.intValue()).toList();
				} else {
					return (List<T>) result;
				}
			}

			case "VEC2" -> {
				if (!type.equals(VectorXZ.class)) throw new IllegalArgumentException("Incorrect accessor type " + type);
				assert components.size() == 2 * accessor.count;
				List<VectorXZ> result = new ArrayList<>(accessor.count);
				for (int i = 0; i < accessor.count; i++) {
					result.add(new VectorXZ(
							components.get(2 * i),
							-1 * components.get(2 * i + 1)));
				}
				return (List<T>) result;
			}

			case "VEC3" -> {
				if (!type.equals(VectorXYZ.class)) throw new IllegalArgumentException("Incorrect accessor type " + type);
				assert components.size() == 3 * accessor.count;
				List<VectorXYZ> result = new ArrayList<>(accessor.count);
				for (int i = 0; i < accessor.count; i++) {
					result.add(new VectorXYZ(
							components.get(3 * i),
							components.get(3 * i + 1),
							-1 * components.get(3 * i + 2)));
				}
				return (List<T>) result;
			}

			default -> throw new UnsupportedOperationException("Unsupported accessor type " + accessor.type);

		}

	}

	static float readComponent(ByteBuffer b, int componentType, boolean normalized) {
		if (normalized) throw new UnsupportedOperationException("Unsupported normalized option present");
		return switch (componentType) {
			case GltfAccessor.TYPE_BYTE -> b.get();
			case GltfAccessor.TYPE_UNSIGNED_BYTE -> b.get() & 0xff;
			case GltfAccessor.TYPE_SHORT -> b.getShort();
			case GltfAccessor.TYPE_UNSIGNED_SHORT -> b.getShort() & 0xffff;
			case GltfAccessor.TYPE_UNSIGNED_INT -> b.getInt() & 0xffffffffL;
			case GltfAccessor.TYPE_FLOAT -> b.getFloat();
			default -> throw new UnsupportedOperationException("Unsupported component type " + componentType);
		};
	}

	private ByteBuffer readBufferView(GltfBufferView bufferView) {

		GltfBuffer buffer = gltf.buffers.get(bufferView.buffer);
		ByteBuffer result;

		if (buffer.uri == null) {
			throw new UnsupportedOperationException("No uri present in bufferView");
		}

		var pattern = Pattern.compile("data:application/(?:gltf-buffer|octet-stream);base64,(.+)");
		var matcher = pattern.matcher(buffer.uri);

		if (matcher.matches()) {

			// load data URI
			String encodedData = matcher.group(1);
			byte[] data = Base64.getDecoder().decode(encodedData);
			result = ByteBuffer.wrap(data);

		} else {

			// load external .bin file
			try {
				URI bufferUri = new URI(buffer.uri);
				if (source != null) {
					bufferUri = source.toURI().resolve(bufferUri);
				}
				try (InputStream inputStream = bufferUri.toURL().openStream()) {
					result = ByteBuffer.wrap(inputStream.readAllBytes());
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		int byteOffset = bufferView.byteOffset == null ? 0 : bufferView.byteOffset;
		int byteLength = bufferView.byteLength;
		result = result.slice(byteOffset, byteLength);

		result.order(ByteOrder.LITTLE_ENDIAN);

		return result;

	}

	private Material convertMaterial(GltfMaterial m, @Nullable Color instanceColor) throws IOException {

		// TODO factors other than baseColorFactor, e.g. emissiveFactor

		/* check for unsupported features */

		if (m.emissiveTexture != null) {
			ConversionLog.warn("Unsupported emissive texture option present in glTF asset " + this);
		}
		if (m.occlusionTexture != null && m.occlusionTexture.strength != null && m.occlusionTexture.strength != 1) {
			ConversionLog.warn("Unsupported occlusion strength option present in glTF asset " + this);
		}

		/* read the textures */

		Color color = Color.WHITE;
		@Nullable TextureLayer textureLayer;

		if (m.pbrMetallicRoughness != null) {

			float[] c = m.pbrMetallicRoughness.baseColorFactor;
			if (c != null) {
				var lColor = new LColor(c[0], c[1], c[2]);
				if (PLACEHOLDER_COLOR.equals(lColor) && instanceColor != null) {
					color = instanceColor;
				} else {
					color = lColor.toAWT();
				}
			}

			@Nullable TextureData baseColorTexture = readTexture(m.pbrMetallicRoughness.baseColorTexture);

			@Nullable TextureData mrTexture = readTexture(m.pbrMetallicRoughness.metallicRoughnessTexture);
			@Nullable TextureData oTexture = readTexture(m.occlusionTexture);

			@Nullable TextureData ormTexture = (mrTexture != null) ? mrTexture : oTexture;
			if (mrTexture != null && oTexture != null && !mrTexture.equals(oTexture)) {
				ConversionLog.warn("Separate occlusion texture is ignored for glTF asset " + this);
			}

			@Nullable TextureData normalTexture = readTexture(m.normalTexture);
			requireOrUnsupported(m.normalTexture == null || m.normalTexture.scale == null || m.normalTexture.scale == 1);

			if (baseColorTexture == null) {
				textureLayer = null;
			} else {
				textureLayer = new TextureLayer(baseColorTexture, normalTexture, ormTexture, null, true);
			}

		} else {
			textureLayer = null;
		}

		/* build the material */

		Material.Transparency transparency = switch (m.alphaMode != null ? m.alphaMode : "OPAQUE") {
			case "OPAQUE" -> Material.Transparency.FALSE;
			case "MASK" -> Material.Transparency.BINARY;
			case "BLEND" -> Material.Transparency.TRUE;
			default -> throw new IllegalArgumentException("Unsupported alphaMode " + m.alphaMode);
		};

		return new ImmutableMaterial(
				Material.Interpolation.FLAT,
				color,
				m.doubleSided != null && m.doubleSided,
				transparency,
				Material.Shadow.TRUE,
				Material.AmbientOcclusion.TRUE,
				textureLayer == null ? List.of() : List.of(textureLayer));

	}

	private @Nullable TextureData readTexture(@Nullable GltfMaterial.TextureInfo textureInfo) throws IOException {

		if (textureInfo == null) {
			return null;
		}

		/* read the texture sampler parameters */

		requireOrUnsupported(textureInfo.texCoord == null || textureInfo.texCoord == 0);

		GltfTexture texture = gltf.textures.get(textureInfo.index);

		Wrap wrap = Wrap.REPEAT;
		if (texture.sampler != null) {
			GltfSampler sampler = gltf.samplers.get(texture.sampler);
			requireOrUnsupported(Objects.equals(sampler.wrapS, sampler.wrapT));
			if (sampler.wrapS != null) {
				wrap = switch (sampler.wrapS) {
					case GltfSampler.WRAP_CLAMP_TO_EDGE -> Wrap.CLAMP;
					case GltfSampler.WRAP_REPEAT -> Wrap.REPEAT;
					default -> throw new UnsupportedOperationException("Wrap mode " + sampler.wrapS);
				};
			}
		}

		/* read and return the actual texture image */

		requireOrUnsupported(texture.source != null);
		GltfImage image = gltf.images.get(texture.source);

		return readImage(image, wrap);

	}

	private TextureData readImage(GltfImage image, Wrap wrap) throws IOException {

		if (!imageCache.containsKey(Pair.of(image, wrap))) {

			if ((image.uri == null) == (image.bufferView == null)) {
				throw new IllegalArgumentException("Image must use either uri or bufferView");
			} else if (image.bufferView != null && image.mimeType == null) {
				throw new IllegalArgumentException("Image with bufferView requires mimeType");
			}

			var dimensions = new TextureDataDimensions(1, 1);

			requireOrUnsupported(image.uri != null);

			TextureData textureData;

			if (image.uri.startsWith("data:")) {
				textureData = new DataUriTexture(image.uri, dimensions, wrap, GLOBAL_X_Z);
			} else {
				try {
					URI imageUri = new URI(image.uri);
					if (source != null) {
						imageUri = source.toURI().resolve(imageUri);
					}
					textureData = new UriTexture(imageUri, dimensions, wrap, GLOBAL_X_Z);
					textureData.getBufferedImage();
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			}

			imageCache.put(Pair.of(image, wrap), textureData);

		}

		return imageCache.get(Pair.of(image, wrap));

	}

	private static void requireOrUnsupported(boolean b) {
		if (!b) {
			throw new UnsupportedOperationException();
		}
	}

	public static GltfModel loadFromFile(File gltfFile) throws IOException {

		try (var reader = new FileReader(gltfFile)) {

			Gltf gltf = new Gson().fromJson(reader, Gltf.class);

			return new GltfModel(gltf, gltfFile);

		} catch (Exception e) {
			throw new IOException("Could not read glTF model at " + gltfFile, e);
		}

	}

}
