package org.osm2world.scene.mesh;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.NormalCalculationUtil;
import org.osm2world.math.shapes.LineSegmentXYZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.texcoord.TexCoordFunction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/** a geometry composed of triangles */
public class TriangleGeometry implements Geometry {

	public final List<TriangleXYZ> triangles;

	public final NormalData normalData;

	/** texture coordinate lists for each texture layer. Each list has the same size as {@link #vertices()}. */
	public final List<List<VectorXZ>> texCoords;

	/** vertex colors, one for each entry in {@link #vertices()}. Each color value can be null. null if all are null. */
	public final @Nullable List</* @Nullable */ Color> colors;

	public List<VectorXYZ> vertices() {
		return vertices(triangles);
	}

	private static List<VectorXYZ> vertices(List<TriangleXYZ> triangles) {
		List<VectorXYZ> result = new ArrayList<>(triangles.size() * 3);
		for (TriangleXYZ triangle : triangles) {
			result.addAll(triangle.verticesNoDup());
		}
		return result;
	}

	@Override
	public TriangleGeometry asTriangles() {
		return this;
	}

	private TriangleGeometry(List<TriangleXYZ> triangles, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoords, @Nullable List<Color> colors) {

		this.triangles = triangles;
		this.texCoords = texCoords;
		this.normalData = new ExplicitNormals(normals);
		this.colors = colors;

		validate();

	}

	/** constructor suitable for straightforward cases. Use the {@link Builder} when you need more flexibility. */
	public TriangleGeometry(List<TriangleXYZ> triangles, Interpolation normalMode,
			List<List<VectorXZ>> texCoords, @Nullable List<Color> colors) {

		this.triangles = triangles;
		this.texCoords = texCoords;
		this.normalData = new CalculatedNormals(normalMode);
		this.colors = colors;

		validate();

	}

	/* perform validation during construction */
	private void validate() {

		if (triangles.isEmpty()) {
			throw new IllegalArgumentException("empty geometry");
		}

		boolean assertionsEnabled = false;
		assert assertionsEnabled = true;

		if (assertionsEnabled) {

			List<VectorXYZ> vs = vertices(triangles);

			assert normalData.normals().size() == vs.size();
			assert colors == null || colors.size() == vs.size();
			assert texCoords.stream().allMatch(t -> t.size() == vs.size());

		}

	}

	public interface NormalData {
		public List<VectorXYZ> normals();
	}

	public class ExplicitNormals implements NormalData {
		public final List<VectorXYZ> normals;
		public ExplicitNormals(List<VectorXYZ> normals) {
			this.normals = normals;
		}
		@Override
		public List<VectorXYZ> normals() {
			return normals;
		}
		@Override
		public String toString() {
			return normals.toString();
		}
	}

	public class CalculatedNormals implements NormalData {
		public final Interpolation normalMode;
		public CalculatedNormals(Interpolation normalMode) {
			this.normalMode = normalMode;
		}
		@Override
		public List<VectorXYZ> normals() {
			return NormalCalculationUtil.calculateTriangleNormals(triangles, normalMode == Interpolation.SMOOTH);
		}
		@Override
		public String toString() {
			return "CalculatedNormals(" + normalMode + ")";
		}
	}

	/** a builder for flexibly constructing a {@link TriangleGeometry} */
	public static class Builder {

		public final int numTextureLayers;
		public final @Nullable List<? extends TexCoordFunction> defaultTexCoordFunctions;
		public final @Nullable Color defaultColor;
		public final @Nullable Interpolation normalMode;

		private final List<TriangleXYZ> triangles = new ArrayList<>();
		private final List<List<VectorXZ>> texCoords;
		private final List</* @Nullable */ Color> colors = new ArrayList<>();
		private final @Nullable List<VectorXYZ> normals;

		/**
		 *
		 * @param defaultColor  the vertex color to use when no vertex color is passed in explicitly,
		 *   can be null for no color.
		 * @param normalMode  how to calculate normals. If set to null, normals must be provided explicitly.
		 *   Otherwise, normals must not be provided explicitly.
		 */
		public Builder(int numTextureLayers, @Nullable Color defaultColor, @Nullable Interpolation normalMode) {

			this.numTextureLayers = numTextureLayers;
			this.defaultTexCoordFunctions = null;
			this.defaultColor = defaultColor;
			this.normalMode = normalMode;

			this.texCoords = new ArrayList<>(numTextureLayers);
			for (int i = 0; i < numTextureLayers; i++) {
				texCoords.add(new ArrayList<>());
			}

			normals = (normalMode == null) ? new ArrayList<>() : null;

		}

		/**
		 * @param defaultTexCoordFunctions  the {@link TexCoordFunction} that are used to calculate texture coords
		 *  if none are passed in explicitly
		 */
		public Builder(List<? extends TexCoordFunction> defaultTexCoordFunctions, @Nullable Color defaultColor,
				@Nullable Interpolation normalMode) {

			this.numTextureLayers = defaultTexCoordFunctions.size();
			this.defaultTexCoordFunctions = defaultTexCoordFunctions;
			this.defaultColor = defaultColor;
			this.normalMode = normalMode;

			this.texCoords = new ArrayList<>(numTextureLayers);
			for (int i = 0; i < numTextureLayers; i++) {
				texCoords.add(new ArrayList<>());
			}

			normals = (normalMode == null) ? new ArrayList<>() : null;

		}

		public void addTriangles(List<TriangleXYZ> triangles, @Nullable List<List<VectorXZ>> texCoords,
				@Nullable List<Color> colors, @Nullable List<VectorXYZ> normals) {

			if (colors == null) {
				colors = nCopies(triangles.size() * 3, defaultColor);
			}

			if (texCoords == null) {
				texCoords = applyDefaultTexCoordFunctions(vertices(triangles));
			}

			if (texCoords.size() != numTextureLayers) {
				throw new IllegalAccessError(texCoords.size() + " texCoord lists, expected " +  numTextureLayers);
			} else if (colors.size() != triangles.size() * 3) {
				throw new IllegalArgumentException("there must be 3 color values for every triangle");
			} else if (texCoords.stream().anyMatch(tcs -> tcs.size() != triangles.size() * 3)) {
				throw new IllegalArgumentException("there must be 3 tex coord values for every triangle");
			}

			this.triangles.addAll(triangles);
			this.colors.addAll(colors);

			for (int layer = 0; layer < numTextureLayers; layer ++) {
				this.texCoords.get(layer).addAll(texCoords.get(layer));
			}

			if (normals != null) {

				if (normalMode != null) {
					throw new IllegalStateException("If normal mode is set, normals must not be provided explicitly");
				} else if (normals.size() != triangles.size() * 3) {
					throw new IllegalArgumentException("there must be 3 normals for every triangle");
				}

				this.normals.addAll(normals);

			} else if (normalMode == null) {
				throw new IllegalStateException("If normal mode is not set, normals must be provided explicitly");
			}

		}

		public void addTriangles(List<TriangleXYZ> triangles, @Nullable List<List<VectorXZ>> texCoords,
				@Nullable List<Color> colors) {
			addTriangles(triangles, texCoords, colors, null);
		}

		public void addTriangles(List<TriangleXYZ> triangles, @Nullable List<List<VectorXZ>> texCoords) {
			addTriangles(triangles, texCoords, null);
		}

		public void addTriangleVs(List<VectorXYZ> vs, @Nullable List<List<VectorXZ>> texCoords) {

			if (vs.isEmpty() || vs.size() % 3 != 0) {
				throw new IllegalArgumentException("vertices must be provided in multiples of 3, was " + vs.size());
			}

			int numTriangles = vs.size() / 3;

			List<TriangleXYZ> triangles = new ArrayList<>(numTriangles);

			for (int i = 0; i < numTriangles; i++) {
				triangles.add(new TriangleXYZ(vs.get(3 * i), vs.get(3 * i + 1), vs.get(3 * i + 2)));
			}

			addTriangles(triangles, texCoords);

		}

		public void addTriangles(List<TriangleXYZ> triangles) {
			addTriangles(triangles, null);
		}

		public void addTriangles(TriangleXYZ... triangles) {
			addTriangles(asList(triangles));
		}

		public void addTriangleStrip(List<VectorXYZ> vs, List<List<VectorXZ>> texCoords) {
			addTriangles(trianglesFromTriangleStrip(vs),
					texCoords.stream().map(it -> triangleVertexListFromTriangleStrip(it)).collect(toList()));
		}

		/** Like {@link #addTriangleStrip(List, List)}, but uses {@link #defaultTexCoordFunctions} */
		public void addTriangleStrip(List<VectorXYZ> vs) {
			addTriangleStrip(vs, applyDefaultTexCoordFunctions(vs));
		}

		/**
		 * @param vs  no duplication of first/last vector
		 */
		public void addTriangleFan(List<VectorXYZ> vs, List<List<VectorXZ>> texCoords) {
			addTriangles(trianglesFromTriangleFan(vs),
					texCoords.stream().map(it -> triangleVertexListFromTriangleFan(it)).collect(toList()));
		}

		/** Like {@link #addTriangleFan(List, List)}, but uses {@link #defaultTexCoordFunctions} */
		public void addTriangleFan(List<VectorXYZ> vs) {
			addTriangleFan(vs, applyDefaultTexCoordFunctions(vs));
		}

		public TriangleGeometry build() {

			/* set colors to null if all values are null */

			@Nullable List</* @Nullable */ Color> colors = this.colors;
			if (colors.stream().allMatch(c -> c == null)) {
				colors = null;
			}

			/* build and return the result */

			if (normalMode == null) {
				return new TriangleGeometry(triangles, normals, texCoords, colors);
			} else {
				return new TriangleGeometry(triangles, normalMode, texCoords, colors);
			}

		}

		private List<List<VectorXZ>> applyDefaultTexCoordFunctions(List<VectorXYZ> vertices) {

			if (numTextureLayers == 0) {
				return emptyList();
			}

			if (defaultTexCoordFunctions == null) {
				throw new IllegalStateException("No default functions for calculating tex coords are available");
			}

			return defaultTexCoordFunctions.stream()
					.map(texCoordFunction -> texCoordFunction.apply(vertices))
					.collect(toList());

		}

	}

	public TriangleGeometry transform(@Nullable VectorXYZ translation, @Nullable Angle rotation, @Nullable Double scale) {

		VectorXYZ t = (translation != null) ? translation : NULL_VECTOR;
		Angle r = (rotation != null) ? rotation : Angle.ofRadians(0);
		double s = (scale != null) ? scale : 1.0;

		if (s <= 0) throw new IllegalArgumentException("Illegal scale: " + scale);

		if (t.equals(NULL_VECTOR) && r.radians == 0.0 && s == 1.0) return this;

		List<TriangleXYZ> newTriangles = triangles.stream()
				.map(it -> it.shift(t))
				.map(it -> it.rotateY(r.radians))
				.map(it -> it.scale(NULL_VECTOR, s))
				.toList();

		if (normalData instanceof CalculatedNormals calculatedNormals) {
			return new TriangleGeometry(newTriangles, calculatedNormals.normalMode, texCoords, colors);
		} else {
			List<VectorXYZ> newNormals = normalData.normals().stream()
					.map(it -> it.rotateY(r.radians))
					.toList();
			return new TriangleGeometry(newTriangles, newNormals, texCoords, colors);
		}

	}

	/**
	 * Returns all edges where this geometry is not flat.
	 * That is, this method returns all triangle segments in this geometry where the two triangles sharing the segment
	 * have different normals.
	 */
	public Set<LineSegmentXYZ> edges() {

		Set<LineSegmentXYZ> result = new HashSet<>();

		Multimap<LineSegmentXYZ, TriangleXYZ> adjacentTriangles = HashMultimap.create();

		for (TriangleXYZ triangle : triangles) {
			for (LineSegmentXYZ segment : triangle.segments()) {
				LineSegmentXYZ canonicalSegment = (segment.getDirection().y > 0
						|| (segment.getDirection().y == 0 && segment.getDirection().xz().angle() >= Math.PI))
						? segment : segment.reverse();
				adjacentTriangles.put(canonicalSegment, triangle);
			}
		}

		for (LineSegmentXYZ segment : adjacentTriangles.keySet()) {
			Collection<TriangleXYZ> triangles = adjacentTriangles.get(segment);
			VectorXYZ n = triangles.iterator().next().getNormal();
			if (triangles.size() == 1
					|| triangles.stream().anyMatch(it -> it.getNormal().distanceTo(n) > 0.01)) {
				result.add(segment);
			}
		}

		return result;

	}

}
