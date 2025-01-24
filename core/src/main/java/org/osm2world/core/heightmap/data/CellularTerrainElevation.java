package org.osm2world.core.heightmap.data;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;

/**
 * Terrain elevation data that consists of
 * points arranged in a grid structure (2d array).
 *
 * The grid
 * <ul>
 * <li>needs to be complete (points might have unknown elevation, though)
 * <li>does <em>not</em> need to have rectangular grid cells, they may be deformed
 * </ul>
 */
public interface CellularTerrainElevation extends TerrainElevation {

	/**
	 * @return  regular two-dimensional array (not jagged)
	 */
	TerrainPoint[][] getTerrainPointGrid();

	/**
	 * returns the boundary created from the first and last rows and columns
	 * of the grid.
	 * This requires that all {@link TerrainPoint}s' elevations have already
	 * been set to non-null values.
	 */
	PolygonXYZ getBoundaryPolygon();

	/**
	 * returns the boundary created from the first and last rows and columns
	 * of the grid.
	 */
	SimplePolygonXZ getBoundaryPolygonXZ();

	/**
	 * returns Iterable over cells.
	 * This is a convenience method for operations that need to be
	 * performed for all cells. The iterator cannot be used to remove cells.
	 */
	Iterable<? extends TerrainElevationCell> getCells();

}
