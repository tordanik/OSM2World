package org.osm2world.core.target.common.rendering;

import java.util.Arrays;
import java.util.List;

import static org.osm2world.core.math.AxisAlignedBoundingBoxXZ.union;

import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * calculates camera and projection information for viewing a given pair of
 * z12 tiles.
 * The viewer will look at the scene at an angle of 30°. The tiles' projection
 * on the screen will therefore be only half as high as it is wide.
 * Using a pair of z12 tiles will result in a square image.
 */
public final class OrthoTilesUtil {

	/** prevents instantiation */
	private OrthoTilesUtil() { }
	
	public static final Camera cameraForTile(MapProjection mapProjection,
			TileNumber tile, double angleDeg) {
		return cameraForBounds(boundsForTile(mapProjection, tile), angleDeg);
	}
	
	public static final Camera cameraForTiles(MapProjection mapProjection,
			List<TileNumber> tiles, double angleDeg) {		
		
		if (tiles.isEmpty()) { throw new IllegalArgumentException("empty tiles list"); }
		
		AxisAlignedBoundingBoxXZ result = boundsForTiles(mapProjection, tiles);
		
		return cameraForBounds(result, angleDeg);
		
	}

	public static final Camera cameraForBounds(
			AxisAlignedBoundingBoxXZ bounds, double angleDeg) {
		
		Camera result = new Camera();
		
		VectorXYZ lookAt = new VectorXYZ(
				bounds.minX + bounds.sizeX() / 2,
				0,
				bounds.minZ + bounds.sizeZ() / 2);		
		result.setLookAt(lookAt);
		
		double cameraDistance = Math.max(bounds.sizeX(), bounds.sizeZ());
		
		result.setPos(new VectorXYZ(
				lookAt.x,
				cameraDistance * Math.sin(Math.toRadians(angleDeg)),
				lookAt.z - cameraDistance * Math.cos(Math.toRadians(angleDeg))));
		
		return result;
	}
	
	public static final Projection projectionForTile(MapProjection mapProjection,
			TileNumber tile, double angleDeg) {
		AxisAlignedBoundingBoxXZ tileBounds = boundsForTile(mapProjection, tile);
		return projectionForBounds(tileBounds, angleDeg);
	}

	public static final Projection projectionForTiles(MapProjection mapProjection,
			List<TileNumber> tiles, double angleDeg) {
		
		if (tiles.isEmpty()) { throw new IllegalArgumentException("empty tiles list"); }
		
		AxisAlignedBoundingBoxXZ result = boundsForTiles(mapProjection, tiles);
		
		return projectionForBounds(result, angleDeg);
		
	}
	
	public static final Projection projectionForBounds(
			AxisAlignedBoundingBoxXZ bounds, double angleDeg) {
		
		double sin = Math.sin(Math.toRadians(angleDeg));
		
		return new Projection(true,
				 bounds.sizeX() / (bounds.sizeZ() * sin),
				 1, 
				 bounds.sizeZ() * sin,
				 -1000000, 1000000);
		
	}		
	
	private static final AxisAlignedBoundingBoxXZ boundsForTile(
			MapProjection mapProjection, TileNumber tile) {
		
		VectorXZ tilePos1 = mapProjection.calcPos(
				tile2lat(tile.y, tile.zoom), tile2lon(tile.x, tile.zoom));
	
		VectorXZ tilePos2 = mapProjection.calcPos(
				tile2lat(tile.y+1, tile.zoom), tile2lon(tile.x+1, tile.zoom));
		
		return new AxisAlignedBoundingBoxXZ(Arrays.asList(tilePos1, tilePos2));
		
	}

	private static final AxisAlignedBoundingBoxXZ boundsForTiles(
			MapProjection mapProjection, List<TileNumber> tiles) {
		
		AxisAlignedBoundingBoxXZ result = boundsForTile(mapProjection, tiles.get(0));
		
		for (int i=1; i<tiles.size(); i++) {
			AxisAlignedBoundingBoxXZ newBox = boundsForTile(mapProjection, tiles.get(i));
			result = union(result, newBox);
		}
		
		return result;
		
	}
	
	private static final double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}
	
	private static final double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
	
}
