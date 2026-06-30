package org.osm2world.util.tiles;

import java.io.IOException;

import org.osm2world.math.geo.TileNumber;
import org.osm2world.util.platform.uri.LoadUriUtil;

public class UriTileSet implements TileSet<byte[]> {

	private final TileUriPattern uriPattern;

	@Override
	public String toString() {
		return "UriTileSet(" + uriPattern + ")";
	}

	public UriTileSet(TileUriPattern uriPattern) {
		this.uriPattern = uriPattern;
	}

	@Override
	public boolean tileExists(TileNumber tileNumber) throws IOException {
		return LoadUriUtil.checkExists(uriPattern.buildURI(tileNumber));
	}

	@Override
	public byte[] getTileData(TileNumber tileNumber) throws IOException {
		return LoadUriUtil.fetchBinary(uriPattern.buildURI(tileNumber));
	}

}
