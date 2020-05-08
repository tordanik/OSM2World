package org.osm2world.core.math.shapes;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public interface SimplePolygonShapeXZ extends SimpleClosedShapeXZ, PolygonShapeXZ {

	/** returns the shape's outer ring. As this is already a shape without holes, it just returns the shape itself. */
	@Override
	default SimplePolygonShapeXZ getOuter() {
		return this;
	}

	/** returns the shape's holes. As this is a simple shape, the result will be empty. */
	@Override
	default Collection<? extends SimplePolygonShapeXZ> getHoles() {
		return emptyList();
	}

	/**
	 * returns the number of vertices in this polygon.
	 * The duplicated first/last vertex is <em>not</em> counted twice,
	 * so the result is equivalent to {@link #getVertexListNoDup()}.size().
	 */
	public default int size() {
		return getVertexList().size() - 1;
	}

	/** returns true if the polygon contains a given position */
	@Override
	public default boolean contains(VectorXZ v) {

		List<VectorXZ> vertexLoop = getVertexList();

		int i, j;
		boolean c = false;

		for (i = 0, j = vertexLoop.size() - 1; i < vertexLoop.size(); j = i++) {
			if (((vertexLoop.get(i).z > v.z) != (vertexLoop.get(j).z > v.z))
					&& (v.x < (vertexLoop.get(j).x - vertexLoop.get(i).x)
							* (v.z - vertexLoop.get(i).z)
							/ (vertexLoop.get(j).z - vertexLoop.get(i).z) + vertexLoop.get(i).x))
				c = !c;
		}

		return c;

	}

	/** creates a new polygon by adding a shift vector to each vector of this */
	public default SimplePolygonShapeXZ shift(VectorXZ shiftVector) {
		return new SimplePolygonXZ(getVertexList().stream().map(shiftVector::add).collect(toList()));
	}

	/** returns this polygon's area */
	@Override
	public default double getArea() {
		return new SimplePolygonXZ(this).getArea();
	}

	/** returns the centroid (or "barycenter") of the polygon */
	public default VectorXZ getCentroid() {
		return new SimplePolygonXZ(this).getCentroid();
	}

	@Override
	public default double getDiameter() {
		double maxDistance = 0;
		for (int i = 1; i < size(); i++) {
			for (int j = 0; j < i; j++) {
				double distance = getVertexList().get(i).distanceTo(getVertexList().get(j));
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
		}
		return maxDistance;
	}

}
