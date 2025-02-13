package org.osm2world.output.common.rendering;

import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.union;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.*;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * calculates camera and projection information for orthographic rendering.
 */
public final class OrthographicUtil {

	/** prevents instantiation */
	private OrthographicUtil() { }

	public static MutableCamera cameraForBounds(MapProjection mapProjection,
			GeoBounds bounds, double angleDeg, CardinalDirection from) {

		AxisAlignedRectangleXZ result = boundsXZ(mapProjection, bounds.latLonBounds());

		return cameraForBounds(result, angleDeg, from);

	}

	public static MutableCamera cameraForBounds(
			AxisAlignedRectangleXZ bounds, double angleDeg, CardinalDirection from) {

		MutableCamera result = new MutableCamera();

		VectorXYZ lookAt = new VectorXYZ(
				bounds.minX + bounds.sizeX() / 2,
				0,
				bounds.minZ + bounds.sizeZ() / 2);

		// calculate camera position (start with position for view from south,
		// then modify it depending on parameters)

		double cameraDistance = Math.max(bounds.sizeX(), bounds.sizeZ());

		double cameraOffsetX = 0;
		double cameraOffsetZ = - cameraDistance * Math.cos(Math.toRadians(angleDeg));

		if (from == CardinalDirection.W || from == CardinalDirection.E) {
			double temp = cameraOffsetX;
			cameraOffsetX = cameraOffsetZ;
			cameraOffsetZ = temp;
		}

		if (from == CardinalDirection.N || from == CardinalDirection.E) {
			cameraOffsetX = -cameraOffsetX;
			cameraOffsetZ = -cameraOffsetZ;
		}

		VectorXYZ pos = new VectorXYZ(
				lookAt.x + cameraOffsetX,
				cameraDistance * Math.sin(Math.toRadians(angleDeg)),
				lookAt.z + cameraOffsetZ);
		result.setCamera(pos, lookAt);

		return result;
	}

	public static OrthographicProjection projectionForBounds(MapProjection mapProjection,
			GeoBounds bounds, double angleDeg, CardinalDirection from) {

		AxisAlignedRectangleXZ result = boundsXZ(mapProjection, bounds.latLonBounds());

		return projectionForBounds(result, angleDeg, from);

	}

	public static OrthographicProjection projectionForBounds(
			AxisAlignedRectangleXZ bounds, double angleDeg,
			CardinalDirection from) {

		double sin = Math.sin(Math.toRadians(angleDeg));

		double sizeX = bounds.sizeX();
		double sizeZ = bounds.sizeZ();

		if (from == CardinalDirection.W || from == CardinalDirection.E) {
			double temp = sizeX;
			sizeX = sizeZ;
			sizeZ = temp;
		}

		return new OrthographicProjection(
				 sizeX / (sizeZ * sin),
				 sizeZ * sin);

	}

	public static AxisAlignedRectangleXZ boundsForTile(MapProjection mapProjection, TileNumber tile) {
		LatLonBounds bounds = tile.latLonBounds();
		return boundsXZ(mapProjection, bounds);
	}

	public static AxisAlignedRectangleXZ boundsForTiles(MapProjection mapProjection, List<TileNumber> tiles) {

		AxisAlignedRectangleXZ result = boundsForTile(mapProjection, tiles.get(0));

		for (int i=1; i<tiles.size(); i++) {
			AxisAlignedRectangleXZ newBox = boundsForTile(mapProjection, tiles.get(i));
			result = union(result, newBox);
		}

		return result;

	}

	private static AxisAlignedRectangleXZ boundsXZ(MapProjection mapProjection, LatLonBounds bounds) {
		return bbox(List.of(mapProjection.toXZ(bounds.getMin()), mapProjection.toXZ(bounds.getMax())));
	}

}
