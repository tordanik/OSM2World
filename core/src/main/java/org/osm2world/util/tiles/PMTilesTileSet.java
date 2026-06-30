package org.osm2world.util.tiles;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.math.geo.TileNumber;

import io.tileverse.pmtiles.PMTilesReader;
import io.tileverse.rangereader.RangeReader;
import io.tileverse.rangereader.file.FileRangeReader;
import io.tileverse.rangereader.http.HttpRangeReader;
import io.tileverse.tiling.pyramid.TileIndex;

public class PMTilesTileSet implements TileSet<byte[]> {

	private static final @Nullable String API_TOKEN = null;

	private final @Nonnull URI uri;

	/**
	 * @param uri  HTTP or file URI
	 */
	public PMTilesTileSet(@Nonnull URI uri) {
		checkNotNull(uri);
		this.uri = uri;
	}

	@Override
	public String toString() {
		return "PMTilesTileSet(" + uri + ")";
	}

	@Override
	public @Nullable byte[] getTileData(TileNumber tileNumber) throws IOException {

		try (PMTilesReader reader = buildReader()) {

			var tileIndex = TileIndex.zxy(tileNumber.zoom, tileNumber.x, tileNumber.y);
			Optional<ByteBuffer> tileData = reader.getTile(tileIndex);

			return tileData.map(ByteBuffer::array).orElse(null);
		}

	}

	@Nonnull
	private PMTilesReader buildReader() throws IOException {

		if (uri.getScheme().equals("file")) {

			RangeReader rangeReader = FileRangeReader.builder()
					.path(uri.getPath())
					.build();

			return new PMTilesReader(rangeReader);

		} else if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {

			var rangeReaderBuilder = HttpRangeReader.builder().uri(uri);

			if (API_TOKEN != null) {
				rangeReaderBuilder.bearerToken(API_TOKEN);
			}

			return new PMTilesReader(rangeReaderBuilder.build());

		} else {
			throw new UnsupportedOperationException("URI scheme not supported: " + uri.getScheme());
		}

	}

}
