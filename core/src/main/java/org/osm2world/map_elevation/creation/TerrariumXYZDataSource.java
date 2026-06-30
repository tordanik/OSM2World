package org.osm2world.map_elevation.creation;

import static java.lang.Math.floor;
import static java.lang.Math.max;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.scene.color.Color;
import org.osm2world.util.tiles.TileSet;

/**
 * Elevation data stored as an XYZ raster tile layer with Terrarium encoding
 */
public class TerrariumXYZDataSource implements TerrainEleDataSource {

	private static final int DEFAULT_MAX_ZOOM = 13;

	private final int maxZoom;
	private final TileSet<byte[]> tileSet;

	private final Map<TileNumber, Tile> tileCache = new HashMap<>();

	/**
	 * @param maxZoom  maximum zoom level to attempt to retrieve. If it's not available, lower zoom levels will be tried.
	 */
	public TerrariumXYZDataSource(int maxZoom, TileSet<byte[]> tileSet) {
		this.maxZoom = maxZoom;
		this.tileSet = tileSet;
	}

	public TerrariumXYZDataSource(TileSet<byte[]> tileSet) {
		this(DEFAULT_MAX_ZOOM, tileSet);
	}

	@Override
	public String toString() {
		return "TerrariumXYZData(" + tileSet + ')';
	}

	@Override
	public TerrainEleData getSites(LatLonBounds bounds) {

		List<TerrainEleDataGrid> resultGrids = new ArrayList<>();

		List<TileNumber> tiles = TileNumber.tilesForBounds(maxZoom, bounds);

		for (TileNumber tileNumber : tiles) {

			Tile tile = tileCache.get(tileNumber);
			if (tile == null) {
				tile = new Tile(tileNumber, tileSet);
				tileCache.put(tileNumber, tile);
			}

			TerrainEleDataGrid grid = tile.getSites();
			if (grid != null) {
				resultGrids.add(grid.clipped(bounds));
			}

		}

		return new TerrainEleDataGridGroup(resultGrids);

	}


	private static class Tile {

		private final TileNumber tileNumber;
		private @Nullable double[][] data;

		public Tile(TileNumber tileNumber, TileSet<byte[]> tileSet) {

			this.tileNumber = tileNumber;

			try {

				/* try to load an image for the tile number or one of its ancestors */

				TileNumber t = tileNumber;

				while (!tileSet.tileExists(t)) {
					if (t.zoom > 0) {
						t = t.ancestor(t.zoom - 1);
					} else {
						throw new IOException("Unable to find elevation data for tile or its ancestors: " + tileNumber);
					}
				}

				var imageStream = new ByteArrayInputStream(Objects.requireNonNull(tileSet.getTileData(t)));
				BufferedImage image = ImageIO.read(imageStream);

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

		public @Nullable TerrainEleDataGrid getSites() {

			var bounds = tileNumber.latLonBounds();
			double sizeLat = bounds.sizeLat();
			double sizeLon = bounds.sizeLon();

			if (data == null) return null;

			LatLonEle[][] result = new LatLonEle[data.length][data[0].length];

			for (int x = 0; x < data.length; x++) {
				result[x] = new LatLonEle[data[x].length];
				for (int y = 0; y < data[x].length; y++) {
					double lon = bounds.minlon + (0.5 + x) / data.length * sizeLon;
					double lat = bounds.maxlat - (0.5 + y) / data[x].length * sizeLat;
					result[x][data[x].length - y - 1] = new LatLonEle(lat, lon, data[x][y]);
				}
			}

			return new TerrainEleDataGrid(result);

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
