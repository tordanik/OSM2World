package org.osm2world.core.target.common.mesh;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.math.FaceXYZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.NormalCalculationUtil;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.PrecomputedTexCoordFunction;
import org.osm2world.core.target.common.material.TexCoordFunction;

/** a geometry composed of triangles */
public class TriangleGeometry implements Geometry {

	public final List<TriangleXYZ> triangles;

	public final NormalData normalData;

	/**
	 * functions to calculate texture coordinates for each texture layer.
	 * Can be a single {@link TexCoordFunction} if there's only one layer or all layers have the same tex coords,
	 * or it can be one {@link TexCoordFunction} per layer.
	 * If you want to store already-calculated texture coordinates, use a {@link PrecomputedTexCoordFunction}.
	 */
	public final List<TexCoordFunction> texCoordFunctions;

	/** vertex colors, one for each entry in {@link #vertices()}. Each color value can be null. null if all are null. */
	public final @Nullable List</* @Nullable */ Color> colors;

	public List<VectorXYZ> vertices() {
		List<VectorXYZ> result = new ArrayList<>(triangles.size() * 3);
		for (TriangleXYZ triangle : triangles) {
			result.addAll(triangle.verticesNoDup());
		}
		return result;
	}

	/** provides the texture coordinates for each texture layer by applying the {@link #texCoordFunctions} */
	public List<List<VectorXZ>> texCoords() {
		return null; //FIXME implement
	}

	@Override
	public TriangleGeometry asTriangles() {
		return this;
	}

	private TriangleGeometry(List<TriangleXYZ> triangles, List<VectorXYZ> normals,
			List<TexCoordFunction> texCoordFunctions, @Nullable List<Color> colors) {
		this.triangles = triangles;
		this.texCoordFunctions = texCoordFunctions;
		this.normalData = new ExplicitNormals(normals);
		this.colors = colors;
	}

	/** constructor suitable for straightforward cases. Use the {@link Builder} when you need more flexibility. */
	public TriangleGeometry(List<TriangleXYZ> triangles, Interpolation normalMode,
			List<TexCoordFunction> texCoordFunctions, @Nullable List<Color> colors) {
		this.triangles = triangles;
		this.texCoordFunctions = texCoordFunctions;
		this.normalData = new CalculatedNormals(normalMode);
		this.colors = colors;
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

		public final @Nullable Color defaultColor;
		public final @Nullable Interpolation normalMode;

		private final List<TriangleXYZ> triangles = new ArrayList<>();
		private final List</* @Nullable */ Color> colors = new ArrayList<>();
		private final @Nullable List<VectorXYZ> normals;

		private List<TexCoordFunction> texCoordFunctions = null;

		/**
		 *
		 * @param defaultColor  the vertex color to use when no vertex color is passed in explicitly,
		 *   can be null for no color.
		 * @param normalMode  how to calculate normals. If set to null, normals must be provided explicitly.
		 *   Otherwise, normals must not be provided explicitly.
		 */
		public Builder(@Nullable Color defaultColor, @Nullable Interpolation normalMode) {

			this.defaultColor = defaultColor;
			this.normalMode = normalMode;

			normals = (normalMode == null) ? new ArrayList<>() : null;

		}

		public void addTriangles(List<TriangleXYZ> triangles, List<Color> colors, List<VectorXYZ> normals) {

			if (normalMode != null) {
				throw new IllegalStateException("If normal mode is set, normals must not be provided explicitly");
			} else if (colors.size() != triangles.size() * 3) {
				throw new IllegalArgumentException("there must be 3 color values for every triangle");
			} else if (normals.size() != triangles.size() * 3) {
				throw new IllegalArgumentException("there must be 3 normals for every triangle");
			}

			this.triangles.addAll(triangles);
			this.normals.addAll(normals);
			this.colors.addAll(colors);

		}

		public void addTriangles(List<TriangleXYZ> triangles, List<Color> colors) {

			if (normalMode == null) {
				throw new IllegalStateException("If normal mode is set, normals must be provided explicitly");
			} else if (colors.size() != triangles.size() * 3) {
				throw new IllegalArgumentException("there must be 3 color values for every triangle");
			}

			this.triangles.addAll(triangles);
			this.colors.addAll(colors);

		}

		public void addTriangles(List<TriangleXYZ> triangles) {
			addTriangles(triangles, nCopies(triangles.size() * 3, defaultColor));
		}

		public void addTriangles(TriangleXYZ... triangles) {
			addTriangles(asList(triangles));
		}

		public void addTriangleVs(List<VectorXYZ> vs) {

			if (vs.isEmpty() || vs.size() % 3 != 0) {
				throw new IllegalArgumentException("vertices must be provided in multiples of 3, was " + vs.size());
			}

			int numTriangles = vs.size() / 3;

			List<TriangleXYZ> triangles = new ArrayList<>(numTriangles);

			for (int i = 0; i < numTriangles; i++) {
				triangles.add(new TriangleXYZ(vs.get(3 * i), vs.get(3 * i + 1), vs.get(3 * i + 2)));
			}

			addTriangles(triangles);

		}

		public void addTriangleStrip(List<VectorXYZ> vs) {
			addTriangles(trianglesFromTriangleStrip(vs));
		}

		/**
		 * @param vs  no duplication of first/last vector
		 */
		public void addTriangleFan(List<VectorXYZ> vs) {
			addTriangles(trianglesFromTriangleFan(vs));
		}

		/**
		 * @param vs  no duplication of first/last vector
		 */
		public void addConvexPolygon(List<VectorXYZ> vs) {
			addTriangleFan(vs);
		}

		public void addFaces(FaceXYZ... faces) {
			for (FaceXYZ face : faces) {
				addConvexPolygon(face.verticesNoDup());
			}
		}

		public void setTexCoordFunctions(List<? extends TexCoordFunction> texCoordFunctions) {
			this.texCoordFunctions = new ArrayList<TexCoordFunction>(texCoordFunctions);
		}

		public TriangleGeometry build() {

			if (texCoordFunctions == null) {
				throw new IllegalStateException("texCoordFunctions has not been set yet");
			}

			@Nullable List</* @Nullable */ Color> colors = this.colors;
			if (colors.stream().allMatch(c -> c == null)) {
				colors = null;
			}

			if (normalMode == null) {
				return new TriangleGeometry(triangles, normals, texCoordFunctions, colors);
			} else {
				return new TriangleGeometry(triangles, normalMode, texCoordFunctions, colors);
			}

		}

	}

}
