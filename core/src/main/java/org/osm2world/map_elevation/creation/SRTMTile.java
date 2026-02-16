package org.osm2world.map_elevation.creation;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

import org.osm2world.util.platform.uri.LoadUriUtil;

/**
 * a single SRTM data tile.
 *
 * Multiple such tiles are used by {@link SRTMData} to build coverage
 * for larger regions.
 */
class SRTMTile {

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

		if (shortBuffer.capacity() < 1201 * 1201) {
			throw new IOException("Too few elevation values read from SRTM tile: " + shortBuffer.capacity());
		}

		return shortBuffer;

	}

	public final short getData(int x, int y) {
		assert 0 <= x && x < PIXELS && 0 <= y && y < PIXELS;
		return data.get((1200 - y) * 1201 + x);
	}

	@Override
	public String toString() {
		return file.getName();
	}

}
