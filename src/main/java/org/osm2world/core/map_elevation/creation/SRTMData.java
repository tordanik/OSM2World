package org.osm2world.core.map_elevation.creation;

import static java.lang.Double.isNaN;
import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
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

				addTileSites(result, lon, lat,
						minLon, minLat, maxLon, maxLat);

			}
		}

		return result;

	}

	/**
	 * variant of getSites which calculates minimum and maximum lat/lon
	 * from the bounds of a {@link MapData} instance
	 *
	 * TODO: make projection reversible, then replace both getSites methods
	 *       with a single getSite(AxisAlignedBoundingBox dataBounds) method
	 */
	@Override
	public Collection<VectorXYZ> getSites(MapData mapData) throws IOException {

		double minLon = Double.POSITIVE_INFINITY;
		double minLat = Double.POSITIVE_INFINITY;
		double maxLon = Double.NEGATIVE_INFINITY;
		double maxLat = Double.NEGATIVE_INFINITY;

		/* find the minimum and maximum lat/lon in the data */

		for (MapNode mapNode : mapData.getMapNodes()) {

			double lon = mapNode.getOsmNode().getLongitude();
			double lat = mapNode.getOsmNode().getLatitude();

			if (!isNaN(lat) && !isNaN(lon)) {
				minLon = min(minLon, lon);
				minLat = min(minLat, lat);
				maxLon = max(maxLon, lon);
				maxLat = max(maxLat, lat);
			}

		}

		/* add a small seam for robustness */

		minLon -= 0.02; minLat -= 0.02;
		maxLon += 0.02; maxLat += 0.02;

		/* TODO: the seam could be smaller - such as this - if empty terrain nodes did have lat/lon
		minLon -= 0.005; minLat -= 0.005;
		maxLon += 0.005; maxLat += 0.005;
		*/

		/* retrieve the sites for the query */

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
				System.err.println("warning: missing SRTM tile " + file.getName());
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
