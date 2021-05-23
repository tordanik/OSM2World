package org.osm2world.core.math;

import static java.lang.Math.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.VectorXYZ.*;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;

/**
 * a simple 3D polygon where all vertices are in the same plane.
 * {@link FaceXYZ} is the most general implementation.
 */
public interface FlatSimplePolygonShapeXYZ {

	/**
	 * returns the polygon's vertices. First and last vertex are equal.
	 */
	public List<VectorXYZ> vertices();

	/**
	 * returns the polygon's vertices.
	 * Unlike {@link #vertices()}, there is no duplication of the first/last vertex.
	 */
	public List<VectorXYZ> verticesNoDup();

	/** returns the normalized normal vector. The vector points "up" for a counterclockwise polygon. */
	public VectorXYZ getNormal();

	/**
	 * returns the average of all vertex coordinates.
	 * The result is not necessarily contained by this polygon.
	 * @see SimplePolygonXZ#getCenter()
	 */
	default public VectorXYZ getCenter() {
		double x=0, y=0, z=0;
		int numberVertices = verticesNoDup().size();
		for (VectorXYZ vertex : verticesNoDup()) {
			x += vertex.x / numberVertices;
			y += vertex.y / numberVertices;
			z += vertex.z / numberVertices;
			/* single division per coordinate after loop would be faster,
			 * but might cause numbers to get too large */
		}
		return new VectorXYZ(x, y, z);
	}

	/** returns the closest point on this face to a given point in 3D space */
	default public VectorXYZ closestPoint(VectorXYZ v) {

		VectorXZ pointInPlane = toFacePlane(v);
		SimplePolygonXZ polygonInPlane = toFacePlane(this);

		VectorXZ closestPointInPlane = polygonInPlane.closestPoint(pointInPlane);

		return fromFacePlane(closestPointInPlane);

	}

	default public double distanceTo(VectorXYZ p) {
		return closestPoint(p).distanceTo(p);
	}

	default public double distanceToXZ(Vector3D p) {
		VectorXZ pXZ = p.xz();
		if (abs(this.getNormal().y) > 0.001) { // not vertical
			return new PolygonXYZ(vertices()).getSimpleXZPolygon().closestPoint(pXZ).distanceTo(pXZ);
		} else {
			throw new NotImplementedException("only implemented for faces that are not vertical");
		}
	}

	/**
	 * returns the y coord value at a {@link VectorXZ} within the face's 2D footprint.
	 *
	 * @throws InvalidGeometryException  if this face is vertical
	 */
	default public double getYAt(VectorXZ v) {
		return triangleOnFace(vertices()).getYAt(v);
	}

	/* TODO: make private in Java 9 */
	default VectorXZ toFacePlane(VectorXYZ v) {
		return rotateToOrFromFacePlane(v, true).xz();
	}

	/* TODO: make private in Java 9 */
	default SimplePolygonXZ toFacePlane(FlatSimplePolygonShapeXYZ p) {
		List<VectorXZ> xzLoop = p.vertices().stream().map(v -> toFacePlane(v)).collect(toList());
		return new SimplePolygonXZ(xzLoop);
	}

	/* TODO: make private in Java 9 */
	default VectorXYZ fromFacePlane(VectorXZ v) {
		return rotateToOrFromFacePlane(v.xyz(getCenter().y), false);
	}

	/* TODO: make private in Java 9 */
	default FaceXYZ fromFacePlane(SimplePolygonXZ p) {
		List<VectorXYZ> xyzLoop = p.vertices().stream().map(v -> fromFacePlane(v)).collect(toList());
		return new FaceXYZ(xyzLoop);
	}

	/* TODO: make private in Java 9 */
	default VectorXYZ rotateToOrFromFacePlane(VectorXYZ v, boolean to) {

		VectorXYZ faceNormal = getNormal();

		if (faceNormal.distanceTo(Y_UNIT) < 0.001) {

			return v;

		} else if (faceNormal.distanceTo(Y_UNIT.invert()) < 0.001) {

			return v.rotateVec(PI, getCenter(), Z_UNIT);

		} else {

			VectorXYZ xzNormal = Y_UNIT;
			VectorXYZ rotAxis = faceNormal.crossNormalized(xzNormal);
			VectorXYZ rotOrigin = getCenter();

			double angle = faceNormal.angleTo(xzNormal);

			return v.rotateVec(to ? angle : -angle, rotOrigin, rotAxis);

		}

	}

	/* TODO: make protected in Java 9 */
	/** returns a triangle consisting of three polygon vertices that are not collinear */
	public static TriangleXYZ triangleOnFace(List<VectorXYZ> vertexLoop) throws InvalidGeometryException {

		final VectorXYZ v1 = vertexLoop.get(0);
		final VectorXYZ v2 = vertexLoop.get(1);

		VectorXYZ v3 = null;
		for (int i = 2; i < vertexLoop.size() - 1; i++) {
			VectorXYZ vTest = vertexLoop.get(i);
			if (vTest.subtract(v1).angleTo(vTest.subtract(v2)) > 0.001) {
				v3 = vTest;
			}
		}

		if (v3 == null) {
			throw new InvalidGeometryException("Points of face are collinear: " + vertexLoop);
		}

		return new TriangleXYZ(v1, v2, v3);

	}

}
