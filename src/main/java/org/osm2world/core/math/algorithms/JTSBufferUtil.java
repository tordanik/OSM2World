package org.osm2world.core.math.algorithms;

import static org.osm2world.core.math.JTSConversionUtil.*;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;

public final class JTSBufferUtil {

	private JTSBufferUtil() {}

	/** grows or shrinks a polygon */
	public static final List<PolygonWithHolesXZ> bufferPolygon(PolygonShapeXZ polygon, double distance) {

		BufferParameters bufferParams = new BufferParameters();
		bufferParams.setJoinStyle(BufferParameters.JOIN_MITRE);
		bufferParams.setMitreLimit(BufferParameters.DEFAULT_MITRE_LIMIT);
		BufferOp op = new BufferOp(polygonXZToJTSPolygon(polygon), bufferParams);

		Geometry result = op.getResultGeometry(distance);

		/* interpret the result as polygons */

		return polygonsXZFromJTSGeometry(result);

	}

}
