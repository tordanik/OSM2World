package org.osm2world.map_elevation.creation;

import static java.lang.Math.floor;
import static java.lang.Math.max;

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
import org.osm2world.util.platform.uri.LoadUriUtil;
import org.osm2world.util.tiles.TileUriPattern;

/**
 * Elevation data stored as an XYZ raster tile layer with Terrarium encoding
 */
public class TerrariumXYZDataSource implements TerrainEleDataSource {

	private static final int DEFAULT_MAX_ZOOM = 13;

	private final int maxZoom;
	private final TileUriPattern uriPattern;

	private final Map<TileNumber, Tile> tileCache = new HashMap<>();

	/**
	 * @param maxZoom  maximum zoom level to attempt to retrieve. If it's not available, lower zoom levels will be tried.
	 * @param uriPattern  tile URI with {z}, {x}, {y} placeholders
	 */
	public TerrariumXYZDataSource(int maxZoom, TileUriPattern uriPattern) {
		this.maxZoom = maxZoom;
		this.uriPattern = uriPattern;
	}

	public TerrariumXYZDataSource(TileUriPattern uriPattern) {
		this(DEFAULT_MAX_ZOOM, uriPattern);
	}

	@Override
	public String toString() {
		return "TerrariumXYZData(" + uriPattern + ')';
	}

	@Override
	public TerrainEleData getSites(LatLonBounds bounds) {

		Collection<LatLonEle> result = new ArrayList<>();

		List<TileNumber> tiles = TileNumber.tilesForBounds(maxZoom, bounds);

		for (TileNumber tileNumber : tiles) {

			Tile tile = tileCache.get(tileNumber);
			if (tile == null) {
				tile = new Tile(tileNumber, uriPattern);
				tileCache.put(tileNumber, tile);
			}

			tile.getSites().stream()
					.filter(s -> bounds.contains(s.latLon()))
					.forEach(result::add);

		}

		return new TerrainEleDataCollection(bounds, result);

	}


	private static class Tile {

		private final TileNumber tileNumber;
		private @Nullable double[][] data;

		public Tile(TileNumber tileNumber, TileUriPattern uriPattern) {

			this.tileNumber = tileNumber;

			try {

				/* try to load an image for the tile number or one of its ancestors */

				TileNumber t = tileNumber;
				URI uri = uriPattern.buildURI(tileNumber);

				while (!LoadUriUtil.checkExists(uri)) {
					if (t.zoom > 0) {
						t = t.ancestor(t.zoom - 1);
						uri = uriPattern.buildURI(t);
					} else {
						throw new IOException("Unable to find elevation data for tile or its ancestors: " + tileNumber);
					}
				}

				BufferedImage image = ImageUtil.loadImageURI(uri);

				/* determine which part of the image to load
				 * (if an ancestor tile was loaded, we want only part of the image) */

				int sizeX = image.getWidth();
				int sizeY = image.getHeight();
				double startXRelative = 0;
				double startYRelative = 0;

				if (!t.equals(tileNumber)) {
					int steps = tileNumber.zoom - t.zoom;
					sizeX = max(sizeX >> steps, 1);
					sizeY = max(sizeY >> steps, 1);
					for (int step = 1; step <= steps; step++) {
						TileNumber tAtStep = tileNumber.ancestor(t.zoom + step);
						if (tAtStep.x % 2 == 1) {
							startXRelative += 1 / Math.pow(2, step);
						}
						if (tAtStep.y % 2 == 1) {
							startYRelative += 1 / Math.pow(2, step);
						}
					}
				}

				int startX = (int) floor(startXRelative * image.getWidth());
				int startY = (int) floor(startYRelative * image.getHeight());

				/* decode the data for the relevant section of the image */

				data = new double[sizeX][sizeY];

				for (int y = 0; y < sizeY; y++) {
					for (int x = 0; x < sizeX; x++) {
						int rgb = image.getRGB(startX + x, startY + y);
						data[x][y] = TerrariumXYZDataSource.decodeValue(rgb);
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

	/** Decodes a Terrarium-encoded color pixel and returns a height in meters */
	static double decodeValue(int rgb) {
		var c = new Color(rgb);
		return (c.getRed() * 256 + c.getGreen() + c.getBlue() / 256.0) - 32768.0;
	}

	/** Encodes a height in meters as a Terrarium-encoded rgb color. Inverse of {@link #decodeValue(int)}. */
	static int encodeValue(double elevation) {
		double v = elevation + 32768;
		int r = (int)floor(v/256);
		int g = (int)floor(v % 256);
		int b = (int)floor((v - floor(v)) * 256);
		return new Color(r, g, b).getRGB();
	}
	
}
