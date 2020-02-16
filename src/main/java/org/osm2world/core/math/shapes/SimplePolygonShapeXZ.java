package org.osm2world.core.math.shapes;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.PolygonXZ;
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

	/** returns true if this polygon contains the parameter polygon */
	public default boolean contains(PolygonXZ p) {
		//FIXME: it is possible that a polygon contains all vertices of another polygon, but still not the entire polygon
		List<VectorXZ> vertexLoop = getVertexList();
		for (VectorXZ v : p.getVertices()) {
			if (!vertexLoop.contains(v) && !this.contains(v)) {
				return false;
			}
		}
		return true;
	}

	/** creates a new polygon by adding a shift vector to each vector of this */
	public default SimplePolygonShapeXZ shift(VectorXZ shiftVector) {
		return new SimplePolygonXZ(getVertexList().stream().map(shiftVector::add).collect(toList()));
	}

}
