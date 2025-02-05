package org.osm2world.core.target.common.rendering;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.shapes.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.core.math.shapes.AxisAlignedRectangleXZ.union;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.geo.LatLonBounds;
import org.osm2world.core.math.geo.MapProjection;
import org.osm2world.core.math.geo.TileNumber;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;

/**
 * calculates camera and projection information for orthographic tiles.
 */
public final class OrthoTilesUtil {

	/** 4 cardinal directions, can be used for camera placement */
	public static enum CardinalDirection {

		N, E, S, W;

		/**
		 * returns the closest cardinal direction for an angle
		 * @param angle  angle to north direction in radians;
		 *               consistent with {@link VectorXZ#angle()}
		 */
		public static CardinalDirection closestCardinal(double angle) {
			angle = angle % (2 * PI);
			if (angle < PI / 4) { return N; }
			else if (angle < 3 * PI / 4) { return E; }
			else if (angle < 5 * PI / 4) { return S; }
			else if (angle < 7 * PI / 4) { return W; }
			else { return N; }
		}

		public boolean isOppositeOf(CardinalDirection other) {
			return this == N && other == S
					|| this == E && other == W
					|| this == S && other == N
					|| this == W && other == E;
		}

	}

	/** prevents instantiation */
	private OrthoTilesUtil() { }

	public static final Camera cameraForTile(MapProjection mapProjection,
			TileNumber tile, double angleDeg, CardinalDirection from) {
		return cameraForBounds(boundsForTile(mapProjection, tile),
				angleDeg, from);
	}

	public static final Camera cameraForTiles(MapProjection mapProjection,
			List<TileNumber> tiles, double angleDeg, CardinalDirection from) {

		if (tiles.isEmpty()) { throw new IllegalArgumentException("empty tiles list"); }

		AxisAlignedRectangleXZ result = boundsForTiles(mapProjection, tiles);

		return cameraForBounds(result, angleDeg, from);

	}

	public static final Camera cameraForBounds(
			AxisAlignedRectangleXZ bounds, double angleDeg,
			CardinalDirection from) {

		Camera result = new Camera();

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

		result.setCamera(lookAt.x + cameraOffsetX,
						 cameraDistance * Math.sin(Math.toRadians(angleDeg)),
						 lookAt.z + cameraOffsetZ,
						 lookAt.x, lookAt.y, lookAt.z);

		return result;
	}

	public static final Projection projectionForTile(MapProjection mapProjection,
			TileNumber tile, double angleDeg, CardinalDirection from) {
		AxisAlignedRectangleXZ tileBounds = boundsForTile(mapProjection, tile);
		return projectionForBounds(tileBounds, angleDeg, from);
	}

	public static final Projection projectionForTiles(MapProjection mapProjection,
			List<TileNumber> tiles, double angleDeg, CardinalDirection from) {

		if (tiles.isEmpty()) { throw new IllegalArgumentException("empty tiles list"); }

		AxisAlignedRectangleXZ result = boundsForTiles(mapProjection, tiles);

		return projectionForBounds(result, angleDeg, from);

	}

	public static final Projection projectionForBounds(
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

		return new Projection(true,
				 sizeX / (sizeZ * sin),
				 Double.NaN,
				 sizeZ * sin,
				 -10000, 10000);

	}

	public static final AxisAlignedRectangleXZ boundsForTile(MapProjection mapProjection, TileNumber tile) {
		LatLonBounds bounds = tile.latLonBounds();
		return bbox(asList(mapProjection.toXZ(bounds.getMin()), mapProjection.toXZ(bounds.getMax())));
	}

	public static final AxisAlignedRectangleXZ boundsForTiles(MapProjection mapProjection, List<TileNumber> tiles) {

		AxisAlignedRectangleXZ result = boundsForTile(mapProjection, tiles.get(0));

		for (int i=1; i<tiles.size(); i++) {
			AxisAlignedRectangleXZ newBox = boundsForTile(mapProjection, tiles.get(i));
			result = union(result, newBox);
		}

		return result;

	}

}
