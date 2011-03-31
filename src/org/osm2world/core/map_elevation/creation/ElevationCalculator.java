package org.osm2world.core.map_elevation.creation;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.ElevationProfile;

/**
 * calculates elevations using information from {@link MapData}
 * and terrain elevation data
 */
public interface ElevationCalculator {

	/**
	 * provides elevation information for all elements in the {@link MapData}.
	 * 
	 * Implementations of this method need to <ul>
	 * <li>set {@link ElevationProfile}s for all {@link MapElement}s</li>
	 * <li>determine elevation of all {@link TerrainPoint}s with unknown
	 * elevation by using {@link TerrainPoint#setEle(float)}.</li>
	 * </ul>
	 * 
	 * @param mapData  map data without elevation profiles; != null
	 * @param eleData  terrain elevation data, possibly with points
	 *                 with unknown information; != null
	 */
	public void calculateElevations(MapData mapData,
			CellularTerrainElevation eleData);

}
