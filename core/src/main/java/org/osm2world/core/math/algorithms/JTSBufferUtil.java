package org.osm2world.core.math.algorithms;

import static org.osm2world.core.math.algorithms.JTSConversionUtil.polygonsFromJTS;
import static org.osm2world.core.math.algorithms.JTSConversionUtil.toJTS;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolygonWithHolesXZ;

public final class JTSBufferUtil {

	private JTSBufferUtil() {}

	/** grows or shrinks a polygon */
	public static final List<PolygonWithHolesXZ> bufferPolygon(PolygonShapeXZ polygon, double distance) {

		BufferParameters bufferParams = new BufferParameters();
		bufferParams.setJoinStyle(BufferParameters.JOIN_MITRE);
		bufferParams.setMitreLimit(BufferParameters.DEFAULT_MITRE_LIMIT);
		BufferOp op = new BufferOp(toJTS(polygon), bufferParams);

		Geometry result = op.getResultGeometry(distance);

		/* interpret the result as polygons */

		return polygonsFromJTS(result);

	}

}
