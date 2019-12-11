package org.osm2world.core.math.shapes;

import java.util.Collection;

/** a closed shape. For this kind of shape, the vertices describe an area's boundary. Can have holes. */
public interface ClosedShapeXZ extends ShapeXZ {

	/** returns the outer ring of this shape, without holes */
	public SimpleClosedShapeXZ getOuter();

	/** returns the inner rings of this shape */
	public Collection<? extends SimpleClosedShapeXZ> getHoles();

}
