package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;

public final class NormalCalculationUtil {

	/** prevents instantiation */
	private NormalCalculationUtil() {}
	
	/**
	 * calculates normals for a collection of triangles
	 */
	public static final List<VectorXYZ> calculateTriangleNormals(
			List<VectorXYZ> vertices, boolean smooth) {

		assert vertices.size() % 3 == 0;
		
		VectorXYZ[] normals = new VectorXYZ[vertices.size()];
		
		//TODO: implement smooth case
		if (/*!smooth*/ true) { //flat
				
			for (int triangle = 0; triangle < vertices.size() / 3; triangle++) {
				
				int i = triangle * 3 + 1;
				
				VectorXYZ vBefore = vertices.get(i-1);
				VectorXYZ vAt = vertices.get(i);
				VectorXYZ vAfter = vertices.get(i+1);
				
				normals[i] = (vBefore.subtract(vAt)).cross((vAfter.subtract(vAt))).normalize();
				
				normals[i-1] = normals[i];
				normals[i+1] = normals[i];
									
			}
			
		}
		
		return asList(normals);
		
	}


	public static final List<VectorXYZ> calculateTriangleStripNormals(
			List<VectorXYZ> vertices, boolean smooth) {

		assert vertices.size() > 3;
		
		VectorXYZ[] normals = calculateTriangleFanNormals(vertices, false)
				.toArray(new VectorXYZ[0]);
		
		//TODO: implement smooth case
		if (/*!smooth*/ true) { //flat
			
			for (int triangle = 0; triangle < vertices.size() - 2; triangle++) {
				if (triangle % 2 == 1) {
					normals[2 + triangle] = normals[2 + triangle].invert();
				}
			}
			
		}
			
		return asList(normals);
		
	}
	
	public static final List<VectorXYZ> calculateTriangleFanNormals(
			List<VectorXYZ> vertices, boolean smooth) {
		
		assert vertices.size() > 3;

		VectorXYZ[] normals = new VectorXYZ[vertices.size()];
			
		//TODO: implement smooth case
		if (/*!smooth*/ true) { //flat
				            
			for (int triangle = 0; triangle < vertices.size() - 2; triangle++) {
				
				int i = triangle + 1;
				
				VectorXYZ vBefore = vertices.get(i-1);
				VectorXYZ vAt = vertices.get(i);
				VectorXYZ vAfter = vertices.get(i+1);
				
				normals[i+1] = (vBefore.subtract(vAt)).cross((vAfter.subtract(vAt))).normalize();
							
			}
			
			normals[0] = VectorXYZ.NULL_VECTOR;
			normals[1] = VectorXYZ.NULL_VECTOR;
		
		}
		
		return asList(normals);
		
	}

	private static final double MAX_ANGLE_RADIANS = Math.toRadians(75);
	
	/**
	 * calculates normals for vertices that are shared by multiple triangles.
	 */
	public static final Collection<TriangleXYZWithNormals> calculateTrianglesWithNormals(
			Collection<TriangleXYZ> triangles) {
		
		Map<VectorXYZ, List<TriangleXYZ>> adjacentTriangles =
			calculateAdjacentTriangles(triangles);
		
		Collection<TriangleXYZWithNormals> result =
			new ArrayList<TriangleXYZWithNormals>(triangles.size());
		
		for (TriangleXYZ triangle : triangles) {
			
			result.add(new TriangleXYZWithNormals(triangle,
					calculateNormal(triangle.v1, triangle, adjacentTriangles),
					calculateNormal(triangle.v2, triangle, adjacentTriangles),
					calculateNormal(triangle.v3, triangle, adjacentTriangles)));
			
		}
		
		return result;
	
	}

	private static VectorXYZ calculateNormal(VectorXYZ v, TriangleXYZ triangle,
			Map<VectorXYZ, List<TriangleXYZ>> adjacentTrianglesMap) {
		
		/* find adjacent triangles whose normals are close enough to that of t
		 * and save their normal vectors */

		List<VectorXYZ> relevantNormals = new ArrayList<VectorXYZ>();
		
		for (TriangleXYZ t2 : adjacentTrianglesMap.get(v)) {
			
			if (triangle == t2 ||
					triangle.getNormal().angleTo(t2.getNormal()) <= MAX_ANGLE_RADIANS) {

				//add, unless one of the existing normals is very similar

				boolean notCoplanar = true;
				for (VectorXYZ n : relevantNormals) {
					if (n.angleTo(t2.getNormal()) < 0.01) {
						notCoplanar = false;
						break;
					}
				}

				if (notCoplanar) {
					relevantNormals.add(t2.getNormal());
				}

			}
		}
		
		/* calculate sum of relevant normals,
		 * normalize it and set the result as normal for the vertex */

		VectorXYZ normal = new VectorXYZ(0, 0, 0);
		for (VectorXYZ addNormal : relevantNormals) {
			normal = normal.add(addNormal);
		}

		return normal.normalize();

	}

	private static Map<VectorXYZ, List<TriangleXYZ>> calculateAdjacentTriangles(
			Collection<TriangleXYZ> triangles) {
		
		Map<VectorXYZ, List<TriangleXYZ>> result =
			new HashMap<VectorXYZ, List<TriangleXYZ>>();
		
		for (TriangleXYZ triangle : triangles) {
			for (VectorXYZ vertex : triangle.getVertices()) {
				List<TriangleXYZ> triangleList = result.get(vertex);
				if (triangleList == null) {
					triangleList = new ArrayList<TriangleXYZ>();
					result.put(vertex, triangleList);
				}
				triangleList.add(triangle);
			}
		}
		
		return result;
	}
	
}
