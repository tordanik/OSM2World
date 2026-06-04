package org.osm2world.map_elevation.creation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.scene.color.Color;
import org.osm2world.util.platform.image.ImageUtil;
import org.osm2world.util.tiles.TileUriPattern;

/**
 * Elevation data stored as an XYZ raster tile layer with Terrarium encoding
 */
public class TerrariumXYZData implements TerrainElevationData {

	private final int zoom = 13;

	private final TileUriPattern uriPattern;

	private final Map<TileNumber, Tile> tileCache = new HashMap<>();

	/**
	 * @param uriPattern  tile URIs with {z}, {x}, {y} placeholders
	 */
	public TerrariumXYZData(TileUriPattern uriPattern) {
		this.uriPattern = uriPattern;
	}

	@Override
	public String toString() {
		return "TerrariumXYZData(" + uriPattern + ')';
	}

	@Override
	public Collection<LatLonEle> getSites(LatLonBounds bounds) throws IOException {

		Collection<LatLonEle> result = new ArrayList<>();

		List<TileNumber> tiles = TileNumber.tilesForBounds(zoom, bounds);

		for (TileNumber tileNumber : tiles) {

			Tile tile = tileCache.get(tileNumber);
			if (tile == null) {
				tile = new Tile(tileNumber, uriPattern.buildURI(tileNumber));
				tileCache.put(tileNumber, tile);
			}

			tile.getSites().stream()
					.filter(s -> bounds.contains(s.latLon()))
					.forEach(result::add);

		}

		return result;

	}


	private static class Tile {

		private final TileNumber tileNumber;
		private @Nullable double[][] data;

		public Tile(TileNumber tileNumber, URI uri) throws IOException {

			this.tileNumber = tileNumber;

			try {

				BufferedImage image = ImageUtil.loadImageURI(uri);

				data = new double[image.getWidth()][image.getHeight()];

				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						data[x][y] = TerrariumXYZData.decodeValue(image.getRGB(x, y));
					}
				}

			} catch (IOException e) {
				ConversionLog.error("Unable to load elevation data for tile " + tileNumber, e);
				data = null;
			}

		}

		public Collection<LatLonEle> getSites() {

			if (data == null) return List.of();

			var bounds = tileNumber.latLonBounds();
			double sizeLat = bounds.sizeLat();
			double sizeLon = bounds.sizeLon();

			List<LatLonEle> result = new ArrayList<>();

			for (int y = 0; y < data.length; y++) {
				for (int x = 0; x < data[y].length; x++) {
					double lat = bounds.maxlat - (0.5 + y) / data.length * sizeLat;
					double lon = bounds.minlon + (0.5 + x) / data[y].length * sizeLon;
					result.add(new LatLonEle(lat, lon, data[x][y]));
				}
			}

			return result;

		}

	}

	/** decodes a Terrarium-encoded color pixel and returns a height in meters */
	static double decodeValue(int rgb) {
		var c = new Color(rgb);
		return (c.getRed() * 256 + c.getGreen() + c.getBlue() / 256.0) - 32768.0;
	}

}
