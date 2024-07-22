package org.osm2world.core.map_elevation.creation;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
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

	public Collection<VectorXYZ> getSites(double minLon, double minLat,
			double maxLon, double maxLat) throws IOException {

		Collection<VectorXYZ> result = new ArrayList<>();

		int minLonInt = (int)floor(minLon);
		int minLatInt = (int)floor(minLat);
		int maxLonInt = (int)ceil(maxLon);
		int maxLatInt = (int)ceil(maxLat);

		for (int lon = minLonInt; lon < maxLonInt; lon++) {
			for (int lat = minLatInt; lat < maxLatInt; lat++) {

				loadTileIfNecessary(lon, lat);

				addTileSites(result, lon, lat,
						minLon, minLat, maxLon, maxLat);

			}
		}

		return result;

	}

	@Override
	public Collection<VectorXYZ> getSites(AxisAlignedRectangleXZ bounds) throws IOException {

		var latLonBounds = new LatLonBounds(
				projection.toLatLon(bounds.bottomLeft()),
				projection.toLatLon(bounds.topRight()));

		double minLon = latLonBounds.minlon;
		double minLat = latLonBounds.minlat;
		double maxLon = latLonBounds.maxlon;
		double maxLat = latLonBounds.maxlat;

		// add a small seam for robustness
		minLon -= 0.005; minLat -= 0.005;
		maxLon += 0.005; maxLat += 0.005;

		return getSites(minLon, minLat, maxLon, maxLat);

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
			} else {
				ConversionLog.error("Missing SRTM tile " + file.getName());
			}

		}

	}

	private void addTileSites(Collection<VectorXYZ> result,
			int tileLon, int tileLat,
			double minLon, double minLat, double maxLon, double maxLat) {

		SRTMTile tile = getTile(tileLon, tileLat);

		if (tile == null) return;

		/* add a site for each SRTM pixel (except last line and column,
		 * which is duplicated in adjacent tiles) */

		int minX = max(0,
				(int)ceil(SRTMTile.PIXELS * (minLon - tileLon)));
		int maxX = min(SRTMTile.PIXELS - 1,
				(int)floor(SRTMTile.PIXELS * (maxLon - tileLon)));

		int minY = max(0,
				(int)ceil(SRTMTile.PIXELS * (minLat - tileLat)));
		int maxY = min(SRTMTile.PIXELS - 1,
				(int)floor(SRTMTile.PIXELS * (maxLat - tileLat)));

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {

				short value = tile.getData(x, y);

				double lat = tileLat + 1.0 / SRTMTile.PIXELS * (y + 0.5);
				double lon = tileLon + 1.0 / SRTMTile.PIXELS * (x + 0.5);

				VectorXZ pos = projection.toXZ(lat, lon);

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
