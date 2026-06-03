package org.osm2world.map_elevation.creation;

import static java.lang.Math.*;
import static java.util.Arrays.stream;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNullElse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * SRTM data for a part of the planet
 */
public class SRTMData implements TerrainElevationData {

	private final File tileDirectory;
	private final SRTMTile[][] tiles;

	public SRTMData(File tileDirectory) {
		this.tileDirectory = tileDirectory;
		this.tiles = new SRTMTile[360][180];
	}

	@Override
	public Collection<LatLonEle> getSites(LatLonBounds bounds) throws IOException {

		double minLon = bounds.minlon;
		double minLat = bounds.minlat;
		double maxLon = bounds.maxlon;
		double maxLat = bounds.maxlat;

		Collection<LatLonEle> result = new ArrayList<>();

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

	private void loadTileIfNecessary(int lon, int lat) throws IOException {

		if (getTile(lon, lat) == null) {

			String fileNameRegex = "";

			if (lat >= 0) {
				fileNameRegex += String.format(ROOT, "N%02d", lat);
			} else {
				fileNameRegex += String.format(ROOT, "S%02d", -lat);
			}

			if (lon >= 0) {
				fileNameRegex += String.format(ROOT, "E%03d", lon);
			} else {
				fileNameRegex += String.format(ROOT, "W%03d", -lon);
			}

			fileNameRegex += "(?:\\.SRTMGL3)?\\.hgt(?:\\.zip)?";

			Pattern pattern = Pattern.compile(fileNameRegex);

			Optional<File> file = stream(requireNonNullElse(tileDirectory.listFiles(), new File[0]))
					.filter(it -> pattern.matcher(it.getName()).matches())
					.findFirst();

			if (file.isPresent()) {
				setTile(lon, lat, new SRTMTile(file.get()));
			} else {
				ConversionLog.error("Missing SRTM tile " + fileNameRegex);
			}

		}

	}

	private void addTileSites(Collection<LatLonEle> result,
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

				if (value != SRTMTile.BLANK_VALUE) {
					result.add(new LatLonEle(lat, lon, value));
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
