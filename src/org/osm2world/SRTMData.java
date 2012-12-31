package org.osm2world;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * SRTM data for a part of the planet
 */
public class SRTMData implements TerrainElevationData {
	
	private final File tileDirectory;
	private final MapProjection projection;
	private final SRTMTile[][] tiles;
	
	public SRTMData(File tileDirectory, MapProjection projection) {
		this.tileDirectory = tileDirectory;
		this.projection = projection;
		this.tiles = new SRTMTile[360][180];
	}
	
	//TODO: make projection reversible and switch to (projected) bounding box
	
	@Override
	public Collection<VectorXYZ> getSites(double minLon, double minLat,
			double maxLon, double maxLat) throws IOException {
		
		Collection<VectorXYZ> result = new ArrayList<VectorXYZ>();
		
		int minLonInt = (int)floor(minLon);
		int minLatInt = (int)floor(minLat);
		int maxLonInt = (int)ceil(maxLon);
		int maxLatInt = (int)ceil(maxLat);
		
		for (int lon = minLonInt; lon < maxLonInt; lon++) {
			for (int lat = minLatInt; lat < maxLatInt; lat++) {
				
				loadTileIfNecessary(lon, lat);
				
				addTileSites(result, lon, lat);
				
			}
		}
		
		return result;
		
	}
	
	private void loadTileIfNecessary(int lon, int lat) throws IOException {
		
		if (getTile(lon, lat) == null) {
			
			String fileName = tileDirectory.getPath() + File.separator;
			
			if (lat >= 0) {
				fileName += String.format("N%02d", lat);
			} else {
				fileName += String.format("S%02d", -lat);
			}

			if (lon >= 0) {
				fileName += String.format("E%03d", lon);
			} else {
				fileName += String.format("W%03d", -lon);
			}
			
			fileName += ".hgt";
			
			File file = new File(fileName);
			
			if (file.exists()) {
				setTile(lon, lat, new SRTMTile(file));
			}
			
		}
		
	}
	
	private void addTileSites(Collection<VectorXYZ> result, int tileLon, int tileLat) {
		
		SRTMTile tile = getTile(tileLon, tileLat);
		
		if (tile == null) return;
		
		/* add a site for each SRTM pixel (except last line and column,
		 * which is duplicated in adjacent tiles) */
		
		for (int x = 0; x < SRTMTile.PIXELS - 1; x++) {
			for (int y = 0; y < SRTMTile.PIXELS - 1; y++) {
				
				short value = tile.getData(x, y);
				
				double lat = tileLat + 1.0 / SRTMTile.PIXELS * y;
				double lon = tileLon + 1.0 / SRTMTile.PIXELS * x;
				
				VectorXZ pos = projection.calcPos(lat, lon);
				
				if (value != SRTMTile.BLANK_VALUE &&
						!Double.isNaN(pos.x) && !Double.isNaN(pos.z)) {
					result.add(pos.xyz(value));
				}
				
			}
		}
				
	}
	
	private SRTMTile getTile(int tileLon, int tileLat) {
		return tiles[tileLon+180][tileLat+90];
	}
	
	private void setTile(int tileLon, int tileLat, SRTMTile tile) {
		tiles[tileLon+180][tileLat+90] = tile;
	}
	
}
