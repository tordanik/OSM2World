package org.osm2world.core.math;

import java.util.List;
import org.osm2world.core.math.shapes.ShapeXZ;

public abstract class PolygonXZ implements ShapeXZ {
    public abstract List<VectorXZ> getVertices();

    public SimplePolygonXZ makeClockwise() {
        return (SimplePolygonXZ) makeRotationSense(true);
    }

    public PolygonXZ makeCounterclockwise() {
        return makeRotationSense(false);
    }

    protected abstract PolygonXZ makeRotationSense(boolean clockwise);
}