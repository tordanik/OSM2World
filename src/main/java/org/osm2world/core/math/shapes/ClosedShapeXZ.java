package org.osm2world.core.math.shapes;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.VectorXZ;

/** a closed shape. For this kind of shape, the vertices describe an area's boundary. Can have holes. */
public interface ClosedShapeXZ extends ShapeXZ {

	/** returns the shape's vertices like {@link #getVertexList()}, but with no duplication of the first/last vertex */
	public default List<VectorXZ> getVertexListNoDup() {
		List<VectorXZ> vertexLoop = getVertexList();
		return vertexLoop.subList(0, vertexLoop.size() - 1);
	}

	/** returns the outer ring of this shape, without holes */
	public SimpleClosedShapeXZ getOuter();

	/** returns the inner rings of this shape */
	public Collection<? extends SimpleClosedShapeXZ> getHoles();

}
