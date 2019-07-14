package org.osm2world.core.target.common;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.FaceTarget.Face;
import org.osm2world.core.target.common.FaceTarget.IsolatedTriangle;


public class FaceTargetTest {

	@Test
	public void testCombineTrianglesToFaces() {
		
		VectorXYZ bottomLeft = new VectorXYZ(-1, 0, 0);
		VectorXYZ bottomCenter = new VectorXYZ(0, 0, 0);
		VectorXYZ bottomRight = new VectorXYZ(+1, 0, 0);
		VectorXYZ centerLeft = new VectorXYZ(-1, 1, 0);
		VectorXYZ center = new VectorXYZ(0, 1, 0);
		VectorXYZ centerRight = new VectorXYZ(+1, 1, 0);
		VectorXYZ topCenter = new VectorXYZ(0, 2, 0);
		
		List<IsolatedTriangle> isolatedTriangles = new ArrayList<IsolatedTriangle>();
		
		isolatedTriangles.add(triangle(centerLeft, bottomLeft, center));
		isolatedTriangles.add(triangle(bottomLeft, bottomCenter, center));
		isolatedTriangles.add(triangle(center, bottomCenter, centerRight));
		isolatedTriangles.add(triangle(bottomCenter, bottomRight, centerRight));
		isolatedTriangles.add(triangle(centerLeft, center, topCenter));
		isolatedTriangles.add(triangle(topCenter, center, centerRight));
		
		Collection<Face> faces =
				FaceTarget.combineTrianglesToFaces(isolatedTriangles);
		
		assertSame(1, faces.size());
		assertSame(6, faces.iterator().next().vs.size());
		
	}
	
	private static final IsolatedTriangle triangle(
			VectorXYZ v1, VectorXYZ v2, VectorXYZ v3) {
		
		TriangleXYZ triangleXYZ = new TriangleXYZ(v1, v2, v3);
		
		return new IsolatedTriangle(
				triangleXYZ, triangleXYZ.getNormal(),
				0, Collections.<List<VectorXZ>>emptyList());
		
	}
	
}
