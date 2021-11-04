package org.osm2world.core.target.common.mesh;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.mesh.TriangleGeometry.CalculatedNormals;
import org.osm2world.core.target.common.texcoord.PrecomputedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;

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

		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(null, normalMode);

		for (TriangleGeometry t : triangleGeometries) {

			if (t.colors == null) {
				if (normalMode != null) {
					builder.addTriangles(t.triangles);
				} else {
					builder.addTriangles(t.triangles,
							nCopies(t.triangles.size() * 3, builder.defaultColor),
							t.normalData.normals());
				}
			} else {
				if (normalMode != null) {
					builder.addTriangles(t.triangles, t.colors);
				} else {
					builder.addTriangles(t.triangles, t.colors, t.normalData.normals());
				}
			}

		}

		/* combine the texture coordinates */

		int numTextureLayers = triangleGeometries.get(0).texCoordFunctions.size();

		if (!triangleGeometries.stream().allMatch(it -> it.texCoordFunctions.size() == numTextureLayers)) {
			throw new IllegalArgumentException("Cannot combine geometries, incompatible number of texture layers");
		}

		List<TexCoordFunction> texCoordFunctions = new ArrayList<>(numTextureLayers);

		for (int i = 0; i < numTextureLayers; i++) {

			final int layer = i;

			TexCoordFunction firstTexCoordFunction = triangleGeometries.get(0).texCoordFunctions.get(layer);

			if (triangleGeometries.stream().allMatch(it -> it.texCoordFunctions.get(layer).equals(firstTexCoordFunction))) {

				texCoordFunctions.add(firstTexCoordFunction);

			} else {

				/* pre-compute texture coordinates and combine the results */

				List<PrecomputedTexCoordFunction> partialTexCoordFunctions = triangleGeometries.stream()
						.map(it -> new PrecomputedTexCoordFunction(it.texCoords().get(layer)))
						.collect(toList());

				texCoordFunctions.add(PrecomputedTexCoordFunction.merge(partialTexCoordFunctions));

			}

		}

		builder.setTexCoordFunctions(texCoordFunctions);

		/* build and return the result */

		return builder.build();

	}

}
