package org.osm2world.map_elevation.creation;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.scene.color.Color;
import org.osm2world.util.IntRange;
import org.osm2world.util.tiles.TileSet;

/**
 * Elevation data stored as an XYZ raster tile layer with Terrarium encoding
 */
public class TerrariumXYZDataSource implements TerrainEleDataSource {

	public static final int DEFAULT_MAX_ZOOM = 13;

	private final int maxZoom;
	private final TileSet<byte[]> tileSet;
	private final boolean hasFallback;

	private final Map<TileNumber, Tile> tileCache = new HashMap<>();

	/**
	 * @param maxZoom  maximum zoom level to attempt to retrieve. If it's not available, lower zoom levels will be tried.
	 * @param hasFallback  whether this data source has a fallback
	 *                     (and it's therefore expected rather than an error that data may sometimes be missing)
	 */
	public TerrariumXYZDataSource(int maxZoom, TileSet<byte[]> tileSet, boolean hasFallback) {
		this.maxZoom = maxZoom;
		this.tileSet = tileSet;
		this.hasFallback = hasFallback;
	}

	public TerrariumXYZDataSource(int maxZoom, TileSet<byte[]> tileSet) {
		this(maxZoom, tileSet, false);
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

		/* find all tiles overlapping the bounds */

		List<TileNumber> tileNumbers = TileNumber.tilesForBounds(maxZoom, bounds);
		List<Tile> tiles = new ArrayList<>();

		for (TileNumber tileNumber : tileNumbers) {
			tiles.add(getOrLoadTile(tileNumber));
		}

		/* if possible, merge all tiles to get a single uninterrupted grid (makes later queries easier and faster) */

		if (!tiles.isEmpty() && tiles.size() > 1) {
			double[][] data0 = tiles.get(0).data;
 			if (data0 != null && tiles.stream().allMatch(t -> t.data != null && t.data.length == data0.length
						&& t.data[0].length == data0[0].length)) {
				return mergedGridForTiles(tileNumbers, bounds);
			}
		}

		return gridGroupFromTiles(bounds, tiles);

	}

	private Tile getOrLoadTile(TileNumber tileNumber) {
		Tile tile = tileCache.get(tileNumber);
		if (tile == null) {
			tile = new Tile(tileNumber);
			tileCache.put(tileNumber, tile);
		}
		return tile;
	}

	@Nonnull
	private static TerrainEleData gridGroupFromTiles(LatLonBounds bounds, List<Tile> tiles) {

		List<TerrainEleDataGrid> resultGrids = new ArrayList<>();

		for (Tile tile : tiles) {

			TerrainEleDataGrid grid = tile.getSites();
			if (grid != null) {
				resultGrids.add(grid.clipped(bounds));
			}

		}

		return switch (resultGrids.size()) {
			case 0 -> new TerrainEleDataCollection(bounds, List.of());
			case 1 -> resultGrids.get(0);
			default -> new TerrainEleDataGridGroup(resultGrids);
		};

	}

	private TerrainEleData mergedGridForTiles(List<TileNumber> tileNumbers, LatLonBounds bounds) {

		IntRange xRange = IntRange.around(tileNumbers, it -> it.x);
		IntRange yRange = IntRange.around(tileNumbers, it -> it.y);

		double[][] mergedData = null;

		for (int tileX : xRange) {
			for (int tileY : yRange) {

				TileNumber tileNumber = new TileNumber(tileNumbers.get(0).zoom, tileX, tileY);
				Tile tile = getOrLoadTile(tileNumber);

				double[][] tileData = tile.data;
				if (tileData == null) throw new IllegalStateException("No data for tile " + tileNumber);

				if (mergedData == null) {
					mergedData = new double[tileData.length * xRange.size()][tileData[0].length * yRange.size()];
				}

				for (int x = 0; x < tileData.length; x++) {
					System.arraycopy(tileData[x], 0,
							mergedData[(tileX - xRange.min()) * tileData.length + x],
							(tileY - yRange.min()) * tileData[0].length, tileData[x].length);
				}

			}
		}

		LatLonBounds mergedBounds = LatLonBounds.union(tileNumbers.stream().map(TileNumber::latLonBounds).toList());
		Tile tempMergedTile = new Tile(mergedBounds, mergedData);
		return requireNonNull(tempMergedTile.getSites()).clipped(bounds);

	}

	private class Tile {

		private final LatLonBounds bounds;
		private @Nullable double[][] data;

		public Tile(TileNumber tileNumber) {

			this.bounds = tileNumber.latLonBounds();

			try {

				/* try to load an image for the tile number or one of its ancestors */

				TileNumber t = tileSet.firstExistingAncestor(tileNumber);

				if (t == null) {
					throw new IOException("Unable to find elevation data for tile or its ancestors: " + tileNumber);
				}

				var imageStream = new ByteArrayInputStream(requireNonNull(tileSet.getTileData(t)));
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
				if (!hasFallback) {
					ConversionLog.error("Unable to load elevation data for tile " + tileNumber, e);
				}
				data = null;
			}

		}

		public Tile(LatLonBounds bounds,  @Nullable double[][] data) {
			this.bounds = bounds;
			this.data = data;
		}

		public @Nullable TerrainEleDataGrid getSites() {

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
