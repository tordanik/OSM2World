package org.osm2world.scene.mesh;

import static java.awt.Color.WHITE;
import static java.lang.Math.min;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.algorithms.GeometryUtil.isRightOf;
import static org.osm2world.scene.mesh.Geometry.combine;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapRelationElement;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.scene.material.*;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.scene.color.LColor;
import org.osm2world.world.data.WorldObject;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/** a collection of meshes along with some metadata */
public class MeshStore {

	@FunctionalInterface
	public static interface MeshProcessingStep extends Function<MeshStore, MeshStore> {}

	public record MeshMetadata(
			@Nullable MapRelationElement mapElement,
			@Nullable Class<? extends WorldObject> modelClass) {

		@Override
		public String toString() {
			return "{" + mapElement + ", " + (modelClass == null ? null : modelClass.getSimpleName()) + "}";
		}

	}

	public record MeshWithMetadata(@Nonnull Mesh mesh, @Nonnull MeshMetadata metadata) {

		public MeshWithMetadata(Mesh mesh, MeshMetadata metadata) {
			if (mesh == null || metadata == null) throw new NullPointerException();
			this.mesh = mesh;
			this.metadata = metadata;
		}

		public static MeshWithMetadata merge(List<MeshWithMetadata> meshes) {

			if (meshes.isEmpty()) throw new IllegalArgumentException();

			MeshMetadata metadata = (meshes.stream().allMatch(m -> Objects.equal(m.metadata, meshes.get(0).metadata)))
					? meshes.get(0).metadata
					: new MeshMetadata(null, null);

			Geometry mergedGeometry = combine(meshes.stream().map(m -> m.mesh.geometry).collect(toList()));
			Mesh mergedMesh = new Mesh(mergedGeometry, meshes.get(0).mesh.material);

			return new MeshWithMetadata(mergedMesh, metadata);

		}

	}

	private final List<MeshWithMetadata> meshes = new ArrayList<>();

	public MeshStore() {}

	public MeshStore(List<MeshWithMetadata> initialMeshes) {
		initialMeshes.forEach(this::addMesh);
	}

	public MeshStore(List<Mesh> initialMeshes, @Nullable MeshMetadata meshMetadata) {
		initialMeshes.forEach(mesh -> this.addMesh(mesh, meshMetadata));
	}

	public void addMesh(Mesh mesh, @Nullable MeshMetadata metadata) {
		addMesh(new MeshWithMetadata(mesh, metadata != null ? metadata : new MeshMetadata(null, null)));
	}

	public void addMesh(MeshWithMetadata meshWithMetadata) {
		meshes.add(meshWithMetadata);
	}

	public List<Mesh> meshes() {
		return meshes.stream().map(m -> m.mesh).toList();
	}

	public List<MeshWithMetadata> meshesWithMetadata() {
		return new ArrayList<>(meshes);
	}

	public Multimap<MeshMetadata, Mesh> meshesByMetadata() {
		Map<Mesh, MeshWithMetadata> metadataMap = Maps.uniqueIndex(meshes, m -> m.mesh);
		return Multimaps.index(meshes(), m -> metadataMap.get(m).metadata);
	}

	public MeshStore process(List<MeshProcessingStep> processingSteps) {
		MeshStore result = this;
		for (MeshProcessingStep processingStep : processingSteps) {
			result = processingStep.apply(result);
		}
		return result;
	}


	public static class FilterLod implements MeshProcessingStep {

		private final LevelOfDetail targetLod;

		public FilterLod(LevelOfDetail targetLod) {
			this.targetLod = targetLod;
		}

		@Override
		public MeshStore apply(MeshStore meshStore) {
			return new MeshStore(meshStore.meshesWithMetadata().stream()
					.filter(m -> m.mesh().lodRange.contains(targetLod))
					.toList());
		}

	}

	/** converts all geometry to {@link TriangleGeometry} */
	public record ConvertToTriangles(double desiredMaxError) implements MeshProcessingStep {

		public ConvertToTriangles {
			if (!Double.isFinite(desiredMaxError)) {
				throw new IllegalArgumentException("invalid parameter: " + desiredMaxError);
			}
		}

		public ConvertToTriangles(LevelOfDetail lod) {
			this(switch (lod) {
				case LOD4 -> 0.01;
				case LOD3 -> 0.05;
				case LOD2 -> 0.20;
				case LOD1 -> 1.0;
				case LOD0 -> 4.0;
			});
		}

		@Override
		public MeshStore apply(MeshStore meshStore) {
			return new MeshStore(meshStore.meshesWithMetadata().stream()
					.map(m -> new MeshWithMetadata(new Mesh(applyToGeometry(m.mesh().geometry),
							m.mesh().material, m.mesh().lodRange), m.metadata()))
					.toList());
		}

		public TriangleGeometry applyToGeometry(Geometry g) {
			if (g instanceof ExtrusionGeometry eg) {
				return eg.asTriangles(desiredMaxError);
			} else {
				return g.asTriangles();
			}
		}

	}

	public static class MergeMeshes implements MeshProcessingStep {

		/** options that alter the behavior away from the default */
		public enum MergeOption {

			/**
			 * whether meshes should be merged across distinct OSM elements.
			 * If this is enabled, the result will not have any {@link MeshMetadata}.
			 */
			MERGE_ELEMENTS,

			/** whether meshes should be kept separate if they have different {@link Material#getInterpolation()} */
			SEPARATE_NORMAL_MODES,

			/** whether meshes should be kept separate if they have different {@link Material#getColor()} */
			SINGLE_COLOR_MESHES,

			/**
			 * whether specialized {@link Geometry} types (such as {@link ExtrusionGeometry} and {@link ShapeGeometry})
			 * should be preserved in separate meshes instead of being converted to {@link TriangleGeometry}
			 */
			PRESERVE_GEOMETRY_TYPES

		}

		private final Set<MergeOption> options;

		public MergeMeshes(Set<MergeOption> options) {
			this.options = options;
		}

		/** checks if two meshes should be merged according to the MergeOptions */
		public boolean shouldBeMerged(MeshWithMetadata m1, MeshWithMetadata m2) {

			if (m1.mesh().lodRange.min() != m2.mesh().lodRange.min()
					|| m1.mesh().lodRange.max() != m2.mesh().lodRange.max()) {
				return false;
			}

			if (!options.contains(MergeOption.MERGE_ELEMENTS)
					&& !java.util.Objects.equals(m1.metadata(), m2.metadata())) {
				return false;
			}

			if (options.contains(MergeOption.PRESERVE_GEOMETRY_TYPES)
					&& (!java.util.Objects.equals(m1.mesh().geometry.getClass(), TriangleGeometry.class)
					|| !java.util.Objects.equals(m2.mesh().geometry.getClass(), TriangleGeometry.class))) {
				return false;
			}

			return m1.mesh().material.equals(m2.mesh().material,
					!options.contains(MergeOption.SEPARATE_NORMAL_MODES),
					!options.contains(MergeOption.SINGLE_COLOR_MESHES));

		}

		@Override
		public MeshStore apply(MeshStore meshStore) {

			/* form sets of meshes that should be merged with each other.
			 * To improve performance, sets are indexed by the hash code of the mesh material's texture layer list. */

			Multimap<Integer, List<MeshWithMetadata>> meshSetsByHashCode = HashMultimap.create();

			meshLoop:
			for (MeshWithMetadata mesh : meshStore.meshesWithMetadata()) {

				int hashCode = mesh.mesh().material.getTextureLayers().hashCode();
				if (!options.contains(MergeOption.MERGE_ELEMENTS)) {
					hashCode ^= mesh.metadata().hashCode();
				}

				for (List<MeshWithMetadata> set : meshSetsByHashCode.get(hashCode)) {
					if (shouldBeMerged(mesh, set.get(0))) {
						set.add(mesh);
						continue meshLoop;
					}
				}

				ArrayList<MeshWithMetadata> newSet = new ArrayList<>();
				newSet.add(mesh);
				meshSetsByHashCode.put(hashCode, newSet);

			}

			/* merge meshes in the same set to produce the result */

			List<MeshWithMetadata> result = new ArrayList<>();

			for (List<MeshWithMetadata> meshSet : meshSetsByHashCode.values()) {
				result.add(MeshWithMetadata.merge(meshSet));
			}

			return new MeshStore(result);

		}

	}

	/** replaces meshes that have multiple layers of textures with multiple meshes, each of which have only one layer */
	public static class EmulateTextureLayers implements MeshProcessingStep {

		private static final double OFFSET_PER_LAYER = 5e-2;

		/** maximum number of layers for which geometry is created, additional ones are omitted */
		private final int maxLayers;

		public EmulateTextureLayers(int maxLayers) {
			this.maxLayers = maxLayers;
		}

		public EmulateTextureLayers() {
			this.maxLayers = Integer.MAX_VALUE;
		}

		@Override
		public MeshStore apply(MeshStore meshStore) {

			/* replace any multi-layer meshes with multiple meshes */

			List<MeshWithMetadata> result = new ArrayList<>();

			for (MeshWithMetadata meshWithMetadata : meshStore.meshesWithMetadata()) {

				Mesh mesh = meshWithMetadata.mesh();

				if (mesh.material.getNumTextureLayers() <= 1) {
					result.add(meshWithMetadata);
				} else {

					TriangleGeometry tg = mesh.geometry.asTriangles();

					for (int layer = 0; layer < min(maxLayers, mesh.material.getNumTextureLayers()); layer++) {

						double offset = layer * OFFSET_PER_LAYER;

						TriangleGeometry.Builder builder = new TriangleGeometry.Builder(1, null, null);
						List<TriangleXYZ> offsetTriangles = tg.triangles.stream()
								.map(t -> t.shift(t.getNormal().mult(offset)))
								.collect(toList());
						List<List<VectorXZ>> texCoords = List.of(tg.texCoords.get(layer));
						builder.addTriangles(offsetTriangles, texCoords, tg.colors, tg.normalData.normals());
						TriangleGeometry newGeometry = builder.build();

						Material singleLayerMaterial = mesh.material
								.withTransparency(layer > 0 ? Material.Transparency.BINARY : null)
								.withLayers(List.of(mesh.material.getTextureLayers().get(layer)));

						Mesh newMesh = new Mesh(newGeometry, singleLayerMaterial, mesh.lodRange);

						result.add(new MeshWithMetadata(newMesh, meshWithMetadata.metadata()));

					}

				}
			}

			return new MeshStore(result);

		}

	}

	/** adds the {@link Material}'s colors directly to the {@link Mesh} as vertex colors */
	public static class MoveColorsToVertices implements MeshProcessingStep {

		@Override
		public MeshStore apply(MeshStore meshStore) {

			List<MeshWithMetadata> result = new ArrayList<>();

			for (MeshWithMetadata meshWithMetadata : meshStore.meshesWithMetadata()) {

				if (!meshWithMetadata.mesh().material.getTextureLayers().isEmpty()
						&& meshWithMetadata.mesh().material.getTextureLayers().stream().noneMatch(it -> it.colorable)) {
					result.add(meshWithMetadata);
					continue;
				}

				Mesh mesh = meshWithMetadata.mesh();
				Material newMaterial = mesh.material.withColor(WHITE);
				Geometry newGeometry;

				if (mesh.geometry instanceof TriangleGeometry tg) {

					List<Color> colors = (tg.colors != null) ? tg.colors
							: nCopies(tg.vertices().size(), mesh.material.getColor());

					TriangleGeometry.Builder builder = new TriangleGeometry.Builder(tg.texCoords.size(), null, null);
					builder.addTriangles(tg.triangles, tg.texCoords, colors, tg.normalData.normals());
					newGeometry = builder.build();

				} else if (mesh.geometry instanceof ShapeGeometry sg) {

					LColor existingColor = sg.color == null ? LColor.WHITE : LColor.fromAWT(sg.color);
					LColor newColor = existingColor.multiply(mesh.material.getLColor());

					newGeometry = new ShapeGeometry(sg.shape, sg.point, sg.frontVector, sg.upVector, sg.scaleFactor,
							newColor.toAWT(), sg.normalMode, sg.textureDimensions);

				} else if (mesh.geometry instanceof ExtrusionGeometry eg) {

					LColor existingColor = eg.color == null ? LColor.WHITE : LColor.fromAWT(eg.color);
					LColor newColor = existingColor.multiply(LColor.fromAWT(mesh.material.getColor()));

					newGeometry = new ExtrusionGeometry(eg.shape, eg.path, eg.upVectors, eg.scaleFactors,
							newColor.toAWT(), eg.options, eg.textureDimensions);

				} else {
					throw new Error("unsupported geometry type: " + mesh.geometry.getClass());
				}

				result.add(new MeshWithMetadata(new Mesh(newGeometry, newMaterial), meshWithMetadata.metadata()));

			}

			return new MeshStore(result);

		}

	}

	public static class ReplaceTexturesWithAtlas implements MeshProcessingStep {

		/** a group of {@link TextureAtlas}es, one for each texture type in a {@link TextureLayer} */
		public static class TextureAtlasGroup {

			public final TextureAtlas baseColorAtlas, normalAtlas, ormAtlas, displacementAtlas;

			private TextureAtlasGroup(Set<TextureLayer> textureLayers) {

				Map<TextureLayer.TextureType, List<TextureData>> map = new HashMap<>();

				for (TextureLayer.TextureType type : TextureLayer.TextureType.values()) {
					map.put(type, textureLayers.stream()
							.map(l -> l.getTexture(type))
							.map(t -> t != null ? t : BlankTexture.INSTANCE)
							.collect(toList()));
				}

				this.baseColorAtlas = new TextureAtlas(map.get(TextureLayer.TextureType.BASE_COLOR));
				this.normalAtlas = new TextureAtlas(map.get(TextureLayer.TextureType.NORMAL));
				this.ormAtlas = new TextureAtlas(map.get(TextureLayer.TextureType.ORM));
				this.displacementAtlas = new TextureAtlas(map.get(TextureLayer.TextureType.DISPLACEMENT));

			}

			public TextureAtlas getTextureAtlas(TextureLayer.TextureType type) {
				switch (type) {
					case BASE_COLOR: return baseColorAtlas;
					case NORMAL: return normalAtlas;
					case ORM: return ormAtlas;
					case DISPLACEMENT: return displacementAtlas;
					default: throw new Error();
				}
			}

			public boolean canReplaceLayer(TextureLayer layer) {

				int index = baseColorAtlas.textures.indexOf(layer.baseColorTexture);

				return index >= 0 && stream(TextureLayer.TextureType.values()).allMatch(type -> layer.getTexture(type) == null
						|| index == getTextureAtlas(type).textures.indexOf(layer.getTexture(type)));

			}

		}

		public final @Nullable TextureAtlasGroup textureAtlasGroup;
		public final Predicate<TextureData> excludeFromAtlas;

		/**
		 * @param textureAtlasGroup  a pre-existing group of texture atlases that will be used.
		 *   If none is provided, such a group will be generated separately for each {@link MeshStore}
		 */
		public ReplaceTexturesWithAtlas(@Nullable TextureAtlasGroup textureAtlasGroup) {
			this.textureAtlasGroup = textureAtlasGroup;
			this.excludeFromAtlas = x -> false;
		}

		/**
		 * @param excludeFromAtlas  identifies texture layers which should never be included in a texture atlas
		 */
		public ReplaceTexturesWithAtlas(Predicate<TextureData> excludeFromAtlas) {
			this.textureAtlasGroup = null;
			this.excludeFromAtlas = excludeFromAtlas;
		}

		public ReplaceTexturesWithAtlas() {
			this((TextureAtlasGroup) null);
		}

		@Override
		public MeshStore apply(MeshStore meshStore) {

			TextureAtlasGroup atlasGroup = textureAtlasGroup != null
					? textureAtlasGroup
					: generateTextureAtlasGroup(List.of(meshStore), excludeFromAtlas);

			if (atlasGroup == null) {
				return meshStore;
			} else {
				return replaceTexturesWithAtlas(meshStore, atlasGroup);
			}

		}

		/**
		 * finds suitable textures in one or more {@link MeshStore}s and creates a {@link TextureAtlasGroup} for them.
		 * @param excludeFromAtlas  identifies texture layers which should never be included in a texture atlas
		 * @return  the {@link TextureAtlasGroup} with all suitable textures, or null if no suitable textures exist
		 */
		public static @Nullable TextureAtlasGroup generateTextureAtlasGroup(
				Iterable<MeshStore> meshStores, Predicate<TextureData> excludeFromAtlas) {

			Set<TextureLayer> textureLayersForAtlas = new HashSet<>();
			for (MeshStore meshStore : meshStores) {
				for (Mesh mesh : meshStore.meshes()) {
					for (TextureLayer textureLayer : mesh.material.getTextureLayers()) {
						if (textureLayer.textures().stream().noneMatch(excludeFromAtlas)) {
							textureLayersForAtlas.add(textureLayer);
						}
					}
				}
			}

			for (MeshStore meshStore : meshStores) {
				for (Mesh mesh : meshStore.meshes()) {
					List<List<VectorXZ>> texCoordLists = mesh.geometry.asTriangles().texCoords;
					for (int layer = 0; layer < mesh.material.getNumTextureLayers(); layer++) {
						if (texCoordLists.get(layer).stream().anyMatch(t -> t.x < 0 || t.x > 1 || t.z < 0 || t.z > 1)) {
							// texture is accessed at a tex coordinate outside the [0;1] range, it should not be in an atlas
							textureLayersForAtlas.remove(mesh.material.getTextureLayers().get(layer));
						}
					}
				}
			}

			if (textureLayersForAtlas.isEmpty()) {
				return null;
			} else {
				return new TextureAtlasGroup(textureLayersForAtlas);
			}

		}

		/** replaces textures with the atlas texture and translate texture coordinates */
		private static MeshStore replaceTexturesWithAtlas(MeshStore meshStore, TextureAtlasGroup atlasGroup) {

			List<MeshWithMetadata> result = new ArrayList<>();

			for (MeshWithMetadata meshWithMetadata : meshStore.meshesWithMetadata()) {

				Mesh mesh = meshWithMetadata.mesh();

				if (!mesh.material.getTextureLayers().stream().anyMatch(atlasGroup::canReplaceLayer)) {
					result.add(meshWithMetadata);
				} else {

					TriangleGeometry tg = mesh.geometry.asTriangles();

					List<TextureLayer> newTextureLayers = new ArrayList<>(mesh.material.getTextureLayers());
					List<List<VectorXZ>> newTexCoords = new ArrayList<>(tg.texCoords);

					for (int layer = 0; layer < newTextureLayers.size(); layer ++) {

						TextureLayer oldLayer = newTextureLayers.get(layer);

						if (atlasGroup.canReplaceLayer(oldLayer)) {
							TextureLayer newLayer = new TextureLayer(atlasGroup.baseColorAtlas,
									oldLayer.normalTexture == null ? null : atlasGroup.normalAtlas,
									oldLayer.ormTexture == null ? null : atlasGroup.ormAtlas,
									oldLayer.displacementTexture == null ? null : atlasGroup.displacementAtlas,
									true);
							newTextureLayers.set(layer, newLayer);
							newTexCoords.set(layer, atlasGroup.baseColorAtlas.mapTexCoords(
									oldLayer.baseColorTexture, newTexCoords.get(layer)));
						}

					}

					TriangleGeometry.Builder builder = new TriangleGeometry.Builder(tg.texCoords.size(), null, null);
					builder.addTriangles(tg.triangles, newTexCoords, tg.colors, tg.normalData.normals());

					Material newMaterial = mesh.material.withLayers(newTextureLayers);
					Mesh newMesh = new Mesh(builder.build(), newMaterial, mesh.lodRange);

					result.add(new MeshWithMetadata(newMesh, meshWithMetadata.metadata()));

				}
			}

			return new MeshStore(result);

		}

	}

	/** removes all geometry outside a bounding shape */
	public record ClipToBounds(SimpleClosedShapeXZ bounds, boolean splitTriangles) implements MeshProcessingStep {

		@Override
		public MeshStore apply(MeshStore meshStore) {

			List<MeshWithMetadata> result = new ArrayList<>();

			for (MeshWithMetadata meshWithMetadata : meshStore.meshesWithMetadata()) {

				Mesh mesh = meshWithMetadata.mesh();
				TriangleGeometry tg = mesh.geometry.asTriangles();

				Map<TriangleXYZ, Collection<TriangleXYZ>> trianglesToReplace = new HashMap<>();

				if (!splitTriangles) {

					/* mark triangles outside the bounds for removal */

					for (TriangleXYZ t : tg.triangles) {
						if (!bounds.contains(t.getCenter().xz())) {
							trianglesToReplace.put(t, emptyList());
						}
					}

				} else {

					List<LineSegmentXZ> boundingSegments = getSegmentsCCW(bounds);

					// TODO: speed up by calculating an inner box for bounds
					// -> if it contains tBbox, the triangle is safely inside the bounds
					// var tBbox = AxisAlignedRectangleXZ.bbox(t.vertices());

					for (TriangleXYZ originalTriangle : tg.triangles) {
						Collection<TriangleXYZ> splitTriangles = clipToBounds(originalTriangle, boundingSegments);
						if (splitTriangles.size() != 1 || !splitTriangles.contains(originalTriangle)) {
							trianglesToReplace.put(originalTriangle, splitTriangles);
						}
					}

				}

				/* build a new mesh without the triangles outside the bounds */

				if (trianglesToReplace.isEmpty()) {
					result.add(meshWithMetadata);
				} else {

					List<VectorXYZ> normals = tg.normalData.normals();

					List<TriangleXYZ> newTriangles = new ArrayList<>();
					List<Color> newColors = tg.colors == null ? null : new ArrayList<>();
					List<VectorXYZ> newNormals = new ArrayList<>();
					List<List<VectorXZ>> newTexCoords = new ArrayList<>(tg.texCoords.size());

					for (int layer = 0; layer < tg.texCoords.size(); layer++) {
						newTexCoords.add(new ArrayList<>());
					}

					for (int i = 0; i < tg.triangles.size(); i++) {

						TriangleXYZ triangle = tg.triangles.get(i);

						if (!trianglesToReplace.containsKey(triangle)) {

							newTriangles.add(triangle);

							for (int j = 0; j <= 2; j++) {

								if (newColors != null) {
									newColors.add(tg.colors.get(3 * i + j));
								}

								newNormals.add(normals.get(3 * i + j));

								for (int layer = 0; layer < tg.texCoords.size(); layer ++) {
									newTexCoords.get(layer).add(tg.texCoords.get(layer).get(3 * i + j));
								}

							}

						} else if (!trianglesToReplace.get(triangle).isEmpty()) {

							/* get the triangle's original vertex attributes */

							LColor[] origColors = newColors == null ? null : new LColor[3];
							VectorXYZ[] origNormals = new VectorXYZ[3];
							List<VectorXZ[]> origTexCoords = new ArrayList<>(tg.texCoords.size());

							for (int layer = 0; layer < tg.texCoords.size(); layer++) {
								origTexCoords.add(new VectorXZ[3]);
							}

							for (int j = 0; j <= 2; j++) {

								if (origColors != null) {
									origColors[j] = LColor.fromAWT(tg.colors.get(3 * i + j));
								}

								origNormals[j] = normals.get(3 * i + j);

								for (int layer = 0; layer < tg.texCoords.size(); layer ++) {
									origTexCoords.get(layer)[j] = tg.texCoords.get(layer).get(3 * i + j);
								}

							}

							/* determine the new triangles' vertex attributes by interpolating on the original triangle */

							TriangleXZ projectedTriangle = new TriangleXZ(
									triangle.toFacePlane(triangle.v1),
									triangle.toFacePlane(triangle.v2),
									triangle.toFacePlane(triangle.v3)
							);

							for (TriangleXYZ newTriangle : trianglesToReplace.get(triangle)) {

								newTriangles.add(newTriangle);

								for (int j = 0; j <= 2; j++) {

									VectorXZ projectedV = triangle.toFacePlane(newTriangle.vertices().get(j));

									if (origColors != null) {
										newColors.add(GeometryUtil.interpolateOnTriangle(projectedV, projectedTriangle,
												origColors[0], origColors[1], origColors[2]).toAWT());
									}

									newNormals.add(GeometryUtil.interpolateOnTriangle(projectedV, projectedTriangle,
											origNormals[0], origNormals[1], origNormals[2]));

									for (int layer = 0; layer < tg.texCoords.size(); layer ++) {
										newTexCoords.get(layer).add(
												GeometryUtil.interpolateOnTriangle(projectedV, projectedTriangle,
														origTexCoords.get(layer)[0],
														origTexCoords.get(layer)[1],
														origTexCoords.get(layer)[2])
										);
									}

								}

							}

						}

					}

					if (!newTriangles.isEmpty()) {
						TriangleGeometry.Builder builder = new TriangleGeometry.Builder(newTexCoords.size(), null, null);
						builder.addTriangles(newTriangles, newTexCoords, newColors, newNormals);
						result.add(new MeshWithMetadata(new Mesh(builder.build(), mesh.material), meshWithMetadata.metadata()));
					}

				}

			}

			return new MeshStore(result);

		}

		static List<LineSegmentXZ> getSegmentsCCW(SimpleClosedShapeXZ bounds) {
			List<LineSegmentXZ> boundingSegments = bounds.getSegments();
			if (bounds.isClockwise()) {
				boundingSegments = boundingSegments.stream().map(LineSegmentXZ::reverse).toList();
			}
			return boundingSegments;
		}

		static Collection<TriangleXYZ> clipToBounds(TriangleXYZ triangle, List<LineSegmentXZ> boundingSegments) {

			Collection<TriangleXYZ> splitTriangles = List.of(triangle);

			for (LineSegmentXZ segment : boundingSegments) {

				Collection<TriangleXYZ> newSplitTriangles = new ArrayList<>();

				FaultTolerantIterationUtil.forEach(splitTriangles, t -> {
					newSplitTriangles.addAll(t.split(segment.toLineXZ()));
				});

				newSplitTriangles.removeIf(it -> isRightOf(it.getCenter().xz(), segment.p1, segment.p2));

				splitTriangles = newSplitTriangles;

			}

			return splitTriangles;

		}

	}

	/** replaces meshes' {@link Material}s with equivalents that omit certain texture types */
	public record RemoveTextures(EnumSet<TextureLayer.TextureType> textureTypesToRemove) implements MeshProcessingStep {

		public RemoveTextures {
			if (textureTypesToRemove.contains(TextureLayer.TextureType.BASE_COLOR)) {
				throw new IllegalArgumentException("Base color textures cannot be removed");
			}
		}

		@Override
		public MeshStore apply(MeshStore meshStore) {

			List<MeshWithMetadata> result = new ArrayList<>();

			for (MeshWithMetadata m : meshStore.meshesWithMetadata()) {

				Material oldMaterial = m.mesh().material;

				Material newMaterial = oldMaterial.withLayers(oldMaterial.getTextureLayers().stream().map(
						l -> new TextureLayer(
								l.baseColorTexture,
								textureTypesToRemove.contains(TextureLayer.TextureType.NORMAL) ? null : l.normalTexture,
								textureTypesToRemove.contains(TextureLayer.TextureType.ORM) ? null : l.ormTexture,
								textureTypesToRemove.contains(TextureLayer.TextureType.DISPLACEMENT) ? null : l.displacementTexture,
								l.colorable
						)
				).toList());

				result.add(new MeshWithMetadata(new Mesh(m.mesh().geometry, newMaterial), m.metadata()));

			}

			return new MeshStore(result);

		}

	}

	// TODO: implement additional processing steps
	// * EmulateDoubleSidedMaterials
	// * ReplaceAlmostBlankTextures(threshold)
	// * BakeDisplacement

}