package org.osm2world.core.target.common.mesh;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

import java.util.List;

import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.mesh.TriangleGeometry.CalculatedNormals;

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

}
