package org.osm2world.util.tiles;

import java.io.IOException;

import javax.annotation.Nullable;

import org.osm2world.math.geo.TileNumber;

/**
 * a set of XYZ tiles in which each tile is accessed using a {@link TileNumber}
 *
 * @param <P> payload data type
 */
public interface TileSet<P> {

	default boolean tileExists(TileNumber tileNumber) throws IOException {
		return getTileData(tileNumber) != null;
	}

	@Nullable P getTileData(TileNumber tileNumber) throws IOException;

}
