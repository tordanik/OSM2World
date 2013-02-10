package org.osm2world;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.osm2world.DelaunayTriangulation.DelaunayTriangle;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;


public class DelaunayTriangulationTest {
	
	private static final double SIZE = 100;
	
	@Test
	public void testInsert() {
		
		List<Random> RANDOMS = asList(
				new Random(1), new Random(20), new Random(300));
		
		for (Random random : RANDOMS) {
			
			AxisAlignedBoundingBoxXZ bounds = new AxisAlignedBoundingBoxXZ(
					-SIZE, -SIZE, +SIZE, +SIZE);
			
			DelaunayTriangulation triangulation =
					new DelaunayTriangulation(bounds);
			DelaunayTriangulation triangulation2 =
					new DelaunayTriangulation(bounds);
			
			List<VectorXYZ> points = new ArrayList<VectorXYZ>();
			
			for (int insertCount = 0; insertCount < 100; insertCount++) {
				
				System.out.println(RANDOMS.indexOf(random) + ":"
						+ insertCount);
				
				double x = (random.nextDouble() * 2 * SIZE) - SIZE;
				double z = (random.nextDouble() * 2 * SIZE) - SIZE;
				
				VectorXYZ point = new VectorXYZ(x, 0, z);
				
				points.add(point);
				
				// check whether undoing works
				
				triangulation2.probe(point.xz());
				
				assertTriangulationsEqual(triangulation, triangulation2);
				
				// insert for real
				
				triangulation.insert(point);
				triangulation2.insert(point);
				
				assertTriangulationProperties(triangulation, points);
				
			}
			
		}
		
	}
	
	/**
	 * asserts that two triangulations are equal
	 */
	private static void assertTriangulationsEqual(
			DelaunayTriangulation triangulation1,
			DelaunayTriangulation triangulation2) {
		
		for (DelaunayTriangle t1 : triangulation1.getTriangles()) {
			
			DelaunayTriangle twinTriangle = null;
			
			for (DelaunayTriangle t2 : triangulation2.getTriangles()) {
				if (t1.asTriangleXZ().equals(t2.asTriangleXZ())) {
					twinTriangle = t2;
				}
			}
			
			assertNotNull("must contain " + t1, twinTriangle);
			
			for (int i = 0; i <= 2; i++) {
				
				DelaunayTriangle n1 = t1.getNeighbor(i);
				DelaunayTriangle n2 = twinTriangle.getNeighbor(i);
				
				if (n1 == null && n2 == null) {
					continue;
				} else if (n1 != null && n2 != null
						&& n1.asTriangleXZ().equals(n2.asTriangleXZ())) {
					continue;
				} else {
					fail(String.format("neighbor %d different: %s vs. %s",
							i, n1, n2));
				}
				
			}
			
		}
		
	}

	/**
	 * asserts that a triangulation confirms to a set of required properties
	 */
	private static void assertTriangulationProperties(
			DelaunayTriangulation triangulation, List<VectorXYZ> points) {
		
		for (DelaunayTriangle triangle : triangulation.getTriangles()) {
			
			/* check that neighborship relations are symmetric */
			
			for (int i = 0; i <= 2; i++) {
				if (triangle.getNeighbor(i) != null) {
					int index = triangle.getNeighbor(i).indexOfNeighbor(triangle);
					assertTrue(0 <= index && index <= 2);
				}
			}
			
			/*
			 * check that each triangle's vertices are on the
			 * circumcircle, and all other points are outside it
			 */
			
			VectorXZ center = triangle.getCircumcircleCenter();
			double radius = triangle.p0.distanceToXZ(center);
			
			assertAlmostEquals(radius, triangle.p1.distanceToXZ(center));
			assertAlmostEquals(radius, triangle.p2.distanceToXZ(center));
			
			for (VectorXYZ otherPoint : points) {
				if (triangle.p0 != otherPoint
						&& triangle.p1 != otherPoint
						&& triangle.p2 != otherPoint) {
					
					if (otherPoint.distanceToXZ(center) <= radius) {
						System.out.println(otherPoint);
						System.out.println(triangle);
					}
					assertTrue(otherPoint.distanceToXZ(center) > radius);
					
				}
			}
			
		}
		
	}
	
}
