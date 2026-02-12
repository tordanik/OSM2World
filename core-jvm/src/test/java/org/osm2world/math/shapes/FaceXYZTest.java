package org.osm2world.math.shapes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;

public class FaceXYZTest {

	@Test
	public void testToFacePlane_xzParallel() {

		FaceXYZ face = new FaceXYZ(closeLoop(
				new VectorXYZ(-1, 20, -1),
				new VectorXYZ(+1, 20, -1),
				new VectorXYZ(+1, 20, +1),
				new VectorXYZ(-1, 20, +1)
				));

		SimplePolygonXZ faceInPlane = face.toFacePlane(face);
		assertAlmostEquals(face.getSimpleXZPolygon(), faceInPlane);
		assertAlmostEquals(new VectorXZ(10, 22), face.toFacePlane(new VectorXYZ(10, 40, 22)));

		PolygonXYZ faceBackInXYZ = face.fromFacePlane(faceInPlane);
		assertAlmostEquals(face, faceBackInXYZ);

	}

	@Test
	public void testToFacePlane_xzParallelFlipped() {

		FaceXYZ face = new FaceXYZ(closeLoop(
				new VectorXYZ(-1, -3.5, -1),
				new VectorXYZ(-1, -3.5, +1),
				new VectorXYZ(+1, -3.5,  0)
				));

		SimplePolygonXZ faceInPlane = face.toFacePlane(face);
		PolygonXYZ faceBackInXYZ = face.fromFacePlane(faceInPlane);
		assertAlmostEquals(face, faceBackInXYZ);

	}

	@Test
	public void testToFacePlane_verticalFace() {

		FaceXYZ face = new FaceXYZ(closeLoop(
				new VectorXYZ(-1, -1, 42),
				new VectorXYZ(+1, -1, 42),
				new VectorXYZ(+1, +1, 42),
				new VectorXYZ(-1, +1, 42)
				));

		SimplePolygonXZ faceInPlane = face.toFacePlane(face);
		PolygonXYZ faceBackInXYZ = face.fromFacePlane(faceInPlane);
		assertAlmostEquals(face, faceBackInXYZ);

	}

	@Test
	public void testClosestPoint_verticalFace() {

		FaceXYZ face = new FaceXYZ(closeLoop(
				new VectorXYZ(-1, -1, 22),
				new VectorXYZ(-1,  0, 22),
				new VectorXYZ(-1, +1, 22),
				new VectorXYZ(+1, +1, 22),
				new VectorXYZ(+1, -1, 22)));

			// within face
			assertAlmostEquals(new VectorXYZ(0.2, 0.2, 22), face.closestPoint(new VectorXYZ(0.2, 0.2, 0)));
			assertAlmostEquals(new VectorXYZ(-1, -1, 22), face.closestPoint(new VectorXYZ(-1, -1, 50)));

			// outside face
			assertAlmostEquals(new VectorXYZ(-1, -1, 22), face.closestPoint(new VectorXYZ(-10, -10, 22)));
			assertAlmostEquals(new VectorXYZ(1, 0.5, 22), face.closestPoint(new VectorXYZ(26, 0.5, 33)));

	}

	@Test
	public void testCreateDiagonalFace() {
		new FaceXYZ(closeLoop(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(2, 0, 0),
				new VectorXYZ(1, 5, 1)));
	}

	@Test
	public void testClosestPoint_diagonalFace() {

		FaceXYZ face = new FaceXYZ(closeLoop(
				new VectorXYZ(-10, 0, 0),
				new VectorXYZ(+10, 0, 0),
				new VectorXYZ(0, 20, 20)));

		assertAlmostEquals(0, 15, 15, face.closestPoint(new VectorXYZ(0, 10, 20)));

	}

	@Test
	public void testDistanceXZ_verticalFace() {

		FaceXYZ face = new FaceXYZ(closeLoop(
				new VectorXYZ(-1, -1, 22),
				new VectorXYZ(-1,  0, 22),
				new VectorXYZ(-1, +1, 22),
				new VectorXYZ(+1, +1, 22),
				new VectorXYZ(+1, -1, 22)));

		// within face
		assertEquals(22.0, face.distanceToXZ(new VectorXZ(0.2, 0)), 0.1);
		assertEquals(28.0, face.distanceToXZ(new VectorXZ(-1, 50)), 0.1);

		// outside face
		assertEquals(9.0, face.distanceToXZ(new VectorXZ(-10, 22)), 0.1);
		assertEquals(9.0, face.distanceToXZ(new VectorXYZ(-10, -10, 22)), 0.1);
		assertEquals(11.0, face.distanceToXZ(new VectorXZ(0, 33)), 0.1);

	}

	@Test
	public void testGetNonTouchingSegments_Backtrack() {

		VectorXYZ A = new VectorXYZ(0, 0, 0);
		VectorXYZ B = new VectorXYZ(3, 0, 0);
		VectorXYZ C = new VectorXYZ(3, 0, 1);
		VectorXYZ D = new VectorXYZ(1, 0, 1);
		VectorXYZ E = new VectorXYZ(3, 0, 3);
		VectorXYZ F = new VectorXYZ(0, 0, 3);

		FaceXYZ face = new FaceXYZ(closeLoop(A, B, C, D, C, E, F));

		assertEquals(7, face.getSegments().size());

		var segments = face.getNonTouchingSegments();

		assertEquals(5, segments.size());
		assertTrue(segments.contains(new LineSegmentXYZ(A, B)));
		assertTrue(segments.contains(new LineSegmentXYZ(B, C)));
		assertTrue(segments.contains(new LineSegmentXYZ(C, E)));
		assertTrue(segments.contains(new LineSegmentXYZ(E, F)));
		assertTrue(segments.contains(new LineSegmentXYZ(F, A)));

	}

	@Test
	public void testGetNonTouchingSegments_Ring() {

		VectorXYZ A = new VectorXYZ(0, 0, 0);
		VectorXYZ B = new VectorXYZ(3, 0, 0);
		VectorXYZ C = new VectorXYZ(3, 0, 1.5);
		VectorXYZ D = new VectorXYZ(2, 0, 1.5);
		VectorXYZ E = new VectorXYZ(2, 0, 1);
		VectorXYZ F = new VectorXYZ(1, 0, 1);
		VectorXYZ G = new VectorXYZ(1, 0, 2);
		VectorXYZ H = new VectorXYZ(2, 0, 2);
		VectorXYZ I = D;
		VectorXYZ J = C;
		VectorXYZ K = new VectorXYZ(3, 0, 3);
		VectorXYZ L = new VectorXYZ(0, 0, 3);

		FaceXYZ face = new FaceXYZ(closeLoop(A, B, C, D, C, E, F, G, H, I, J, K, L));

		assertEquals(13, face.getSegments().size());
		assertEquals(11, face.getNonTouchingSegments().size());

	}

}
