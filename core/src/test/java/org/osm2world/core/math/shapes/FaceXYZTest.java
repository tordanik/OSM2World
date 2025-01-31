package org.osm2world.core.math.shapes;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

public class FaceXYZTest {

	@Test
	public void testToFacePlane_xzParallel() {

		FaceXYZ face = new FaceXYZ(closeLoop(asList(
				new VectorXYZ(-1, 20, -1),
				new VectorXYZ(+1, 20, -1),
				new VectorXYZ(+1, 20, +1),
				new VectorXYZ(-1, 20, +1)
				)));

		SimplePolygonXZ faceInPlane = face.toFacePlane(face);
		assertAlmostEquals(face.getSimpleXZPolygon(), faceInPlane);
		assertAlmostEquals(new VectorXZ(10, 22), face.toFacePlane(new VectorXYZ(10, 40, 22)));

		PolygonXYZ faceBackInXYZ = face.fromFacePlane(faceInPlane);
		assertAlmostEquals(face, faceBackInXYZ);

	}

	@Test
	public void testToFacePlane_xzParallelFlipped() {

		FaceXYZ face = new FaceXYZ(closeLoop(asList(
				new VectorXYZ(-1, -3.5, -1),
				new VectorXYZ(-1, -3.5, +1),
				new VectorXYZ(+1, -3.5,  0)
				)));

		SimplePolygonXZ faceInPlane = face.toFacePlane(face);
		PolygonXYZ faceBackInXYZ = face.fromFacePlane(faceInPlane);
		assertAlmostEquals(face, faceBackInXYZ);

	}

	@Test
	public void testToFacePlane_verticalFace() {

		FaceXYZ face = new FaceXYZ(closeLoop(asList(
				new VectorXYZ(-1, -1, 42),
				new VectorXYZ(+1, -1, 42),
				new VectorXYZ(+1, +1, 42),
				new VectorXYZ(-1, +1, 42)
				)));

		SimplePolygonXZ faceInPlane = face.toFacePlane(face);
		PolygonXYZ faceBackInXYZ = face.fromFacePlane(faceInPlane);
		assertAlmostEquals(face, faceBackInXYZ);

	}

	@Test
	public void testClosestPoint_verticalFace() {

		FaceXYZ face = new FaceXYZ(closeLoop(asList(
				new VectorXYZ(-1, -1, 22),
				new VectorXYZ(-1,  0, 22),
				new VectorXYZ(-1, +1, 22),
				new VectorXYZ(+1, +1, 22),
				new VectorXYZ(+1, -1, 22),
				new VectorXYZ(-1, -1, 22))));

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

}
