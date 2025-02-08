package org.osm2world.math.shapes;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.math.VectorXZ;

/** a closed shape. For this kind of shape, the vertices describe an area's boundary. Can have holes. */
public interface ClosedShapeXZ extends ShapeXZ {

	/** returns the shape's vertices like {@link #vertices()}, but with no duplication of the first/last vertex */
	public default List<VectorXZ> verticesNoDup() {
		List<VectorXZ> vertexLoop = vertices();
		return vertexLoop.subList(0, vertexLoop.size() - 1);
	}

	/** returns the outer ring of this shape, without holes */
	public SimpleClosedShapeXZ getOuter();

	/** returns the inner rings of this shape */
	public Collection<? extends SimpleClosedShapeXZ> getHoles();

	/** returns both the outer ring and holes */
	public default Collection<? extends SimpleClosedShapeXZ> getRings() {
		if (getHoles().isEmpty()) {
			return singletonList(getOuter());
		} else {
			List<SimpleClosedShapeXZ> result = new ArrayList<>(getHoles().size() + 1);
			result.add(getOuter());
			result.addAll(getHoles());
			return result;
		}
	}

	/** returns the shape's area */
	public double getArea();

	/**
	 * returns a decomposition of the shape into triangles.
	 * For some shapes (e.g. circles), this may be an approximation.
	 */
	public List<TriangleXZ> getTriangulation();

	public List<VectorXZ> intersectionPositions(LineSegmentXZ lineSegment);

	@Override
	public ClosedShapeXZ transform(Function<VectorXZ, VectorXZ> operation);

}
