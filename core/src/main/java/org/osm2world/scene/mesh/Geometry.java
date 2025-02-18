package org.osm2world.scene.mesh;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.mesh.TriangleGeometry.CalculatedNormals;

public interface Geometry {

	public TriangleGeometry asTriangles();

	public static Geometry emptyGeometry() {
		return new TriangleGeometry(emptyList(), Interpolation.FLAT, emptyList(), null);
	}

	public static Geometry combine(List<Geometry> geometries) {

		switch (geometries.size()) {
		case 0: return emptyGeometry();
		case 1: return geometries.get(0);
		}

		List<TriangleGeometry> triangleGeometries = geometries.stream().map(it -> it.asTriangles()).collect(toList());

		/* verify that the number of texture layers is compatible */

		int numTextureLayers = triangleGeometries.get(0).texCoords.size();

		if (!triangleGeometries.stream().allMatch(it -> it.texCoords.size() == numTextureLayers)) {
			throw new IllegalArgumentException("Cannot combine geometries, incompatible number of texture layers");
		}

		/* determine normal mode (null unless all geometries use the same normal mode) */
		// TODO: "smooth" can alter normals even with same mode if there are adjacent triangles in different geometries

		Interpolation normalMode;

		if (!triangleGeometries.stream().allMatch(it -> it.normalData instanceof CalculatedNormals)) {
			normalMode = null;
		} else {
			Interpolation firstNormalMode = ((CalculatedNormals)triangleGeometries.get(0).normalData).normalMode;
			if (triangleGeometries.stream().allMatch(it -> ((CalculatedNormals)it.normalData).normalMode == firstNormalMode)) {
				normalMode = firstNormalMode;
			} else {
				normalMode = null;
			}
		}

		/* combine the triangles from all geometries */

		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(numTextureLayers, null, normalMode);

		for (TriangleGeometry t : triangleGeometries) {

			if (t.colors == null) {
				if (normalMode != null) {
					builder.addTriangles(t.triangles, t.texCoords);
				} else {
					builder.addTriangles(t.triangles, t.texCoords,
							nCopies(t.triangles.size() * 3, builder.defaultColor),
							t.normalData.normals());
				}
			} else {
				if (normalMode != null) {
					builder.addTriangles(t.triangles, t.texCoords, t.colors);
				} else {
					builder.addTriangles(t.triangles, t.texCoords, t.colors, t.normalData.normals());
				}
			}

		}

		/* build and return the result */

		return builder.build();

	}

	/**
	 * Transforms this geometry (first translate, then rotate, then scale).
	 *
	 * @param translation  vector to shift this geometry by
	 * @param rotation  clockwise rotation around y-axis
	 * @param scale  scaling factor, greater than 0
	 * @return  a {@link Geometry} with the transformations applied
	 */
	default Geometry transform(@Nullable VectorXYZ translation, @Nullable Angle rotation, @Nullable Double scale) {
		return asTriangles().transform(translation, rotation, scale);
	}

}
