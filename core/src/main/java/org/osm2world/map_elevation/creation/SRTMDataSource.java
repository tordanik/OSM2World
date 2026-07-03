package org.osm2world.map_elevation.creation;

import static java.lang.Math.*;
import static java.util.Arrays.stream;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNullElse;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.util.platform.uri.LoadUriUtil;

/**
 * SRTM data for a part of the planet
 */
public class SRTMDataSource implements TerrainEleDataSource {

	private final File tileDirectory;
	private final SRTMTile[][] tiles;

	public SRTMDataSource(File tileDirectory) {
		this.tileDirectory = tileDirectory;
		this.tiles = new SRTMTile[360][180];
	}

	@Override
	public TerrainEleData getSites(LatLonBounds bounds) throws IOException {

		double minLon = bounds.minlon;
		double minLat = bounds.minlat;
		double maxLon = bounds.maxlon;
		double maxLat = bounds.maxlat;

		List<TerrainEleDataGrid> result = new ArrayList<>();

		int minLonInt = (int)floor(minLon);
		int minLatInt = (int)floor(minLat);
		int maxLonInt = (int)ceil(maxLon);
		int maxLatInt = (int)ceil(maxLat);

		for (int lon = minLonInt; lon < maxLonInt; lon++) {
			for (int lat = minLatInt; lat < maxLatInt; lat++) {

				loadTileIfNecessary(lon, lat);

				TerrainEleDataGrid grid = getTileSites(lon, lat, minLon, minLat, maxLon, maxLat);

				if (grid != null) {
					result.add(grid);
				}

			}
		}

		return switch (result.size()) {
			case 0 -> new TerrainEleDataCollection(bounds, List.of());
			case 1 -> result.get(0);
			default -> new TerrainEleDataGridGroup(result);
		};

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

	private TerrainEleDataGrid getTileSites(int tileLon, int tileLat,
			double minLon, double minLat, double maxLon, double maxLat) {

		SRTMTile tile = getTile(tileLon, tileLat);

		if (tile == null) return null;

		/* add a site for each SRTM pixel (except last line and column,
		 * which is duplicated in adjacent tiles) */

		int minX = max(0, (int)ceil(SRTMTile.PIXELS * (minLon - tileLon)));
		int maxX = min(SRTMTile.PIXELS - 1, (int)floor(SRTMTile.PIXELS * (maxLon - tileLon)));

		int minY = max(0, (int)ceil(SRTMTile.PIXELS * (minLat - tileLat)));
		int maxY = min(SRTMTile.PIXELS - 1, (int)floor(SRTMTile.PIXELS * (maxLat - tileLat)));

		LatLonEle[][] result = new LatLonEle[maxX - minX][maxY - minY];

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {

				short value = tile.getData(x, y);

				double lat = tileLat + 1.0 / SRTMTile.PIXELS * (y + 0.5);
				double lon = tileLon + 1.0 / SRTMTile.PIXELS * (x + 0.5);

				double ele;

				if (value == SRTMTile.BLANK_VALUE) {
					// do some very basic void filling
					ele = averageOfClosestValues(tile, x, y, 1);
				} else {
					ele = value;
				}

				result[x - minX][y - minY] = new LatLonEle(lat, lon, ele);

			}
		}

		return new TerrainEleDataGrid(result);

	}

	private static double averageOfClosestValues(SRTMTile tile, int pixelX, int pixelY, int range) {

		int nonVoidPixels = 0;
		double sumValues = 0;

		for (int x = pixelX - range; x <= pixelX + range; x++) {
			for (int y = pixelY - range; y <= pixelY + range; y++) {
				short value = tile.getData(x, y);
				if (value != SRTMTile.BLANK_VALUE) {
					nonVoidPixels++;
					sumValues += value;
				}
			}
		}

		if (nonVoidPixels > 0) {
			return sumValues / nonVoidPixels;
		} else if (range < 128) {
			return averageOfClosestValues(tile, pixelX, pixelY, range * 2);
		} else {
			return 0;
		}

	}

	private SRTMTile getTile(int tileLon, int tileLat) {
		return tiles[tileLon+180][tileLat+90];
	}

	private void setTile(int tileLon, int tileLat, SRTMTile tile) {
		tiles[tileLon+180][tileLat+90] = tile;
	}

	/**
	 * A single SRTM data tile.
	 * Multiple such tiles are used by {@link SRTMDataSource} to build coverage
	 * for larger regions.
	 */
	private static class SRTMTile {

		/** value indicating a lack of data */
		public static final short BLANK_VALUE = -32768;

		/** length of each dimension of an SRTM tile in pixels */
		static final int PIXELS = 1201;

		public final File file;
		private final ShortBuffer data;

		public SRTMTile(File file) throws IOException {

			this.file = file;

			data = loadDataFromFile(file);

		}

		private static ShortBuffer loadDataFromFile(File file) throws IOException {

			if (file.getName().endsWith(".zip")) {

				try (
						var inputStream = new FileInputStream(file);
						var bufferedInputStream = new BufferedInputStream(inputStream);
						var zipInputStream = new ZipInputStream(bufferedInputStream)
				) {

					ByteBuffer payloadData = null;

					ZipEntry zipEntry;
					while ((zipEntry = zipInputStream.getNextEntry()) != null) {
						if (!zipEntry.isDirectory()) {

							byte[] buffer = new byte[2048];
							var bos = new ByteArrayOutputStream();
							int len;
							while ((len = zipInputStream.read(buffer)) > 0) {
								bos.write(buffer,0, len);
							}
							// convert bytes to string
							byte[] zipFileBytes = bos.toByteArray();
							payloadData = ByteBuffer.wrap(zipFileBytes);

							break;

						}
					}

					if (payloadData == null) {
						throw new IOException("No hgt payload file found in zip archive " + file);
					} else {
						return loadDataFromByteBuffer(payloadData);
					}

				}

			} else {

				byte[] bytes = LoadUriUtil.fetchBinary(file.toURI());
				return loadDataFromByteBuffer(ByteBuffer.wrap(bytes));

			}

		}

		private static ShortBuffer loadDataFromByteBuffer(@Nonnull ByteBuffer data) throws IOException {

			// choose the right endianness
			ShortBuffer shortBuffer = data.order(ByteOrder.BIG_ENDIAN).asShortBuffer();

			if (shortBuffer.capacity() < PIXELS * PIXELS) {
				throw new IOException("Too few elevation values read from SRTM tile: " + shortBuffer.capacity());
			}

			return shortBuffer;

		}

		public final short getData(int x, int y) {
			assert 0 <= x && x < PIXELS && 0 <= y && y < PIXELS;
			return data.get((PIXELS - 1 - y) * PIXELS + x);
		}

		@Override
		public String toString() {
			return file.getName();
		}

	}
}
