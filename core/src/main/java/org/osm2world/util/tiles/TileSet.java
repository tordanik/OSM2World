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

	/**
	 * Returns the tile itself if it exists.
	 * If it doesn't exist, goes up the ancestor chain and returns the first ancestor which exists.
	 * If none exist, returns null.
	 */
	default @Nullable TileNumber firstExistingAncestor(TileNumber tileNumber) throws IOException {

		TileNumber t = tileNumber;

		while (!tileExists(t)) {
			if (t.zoom > 0) {
				t = t.ancestor(t.zoom - 1);
			} else {
				return null;
			}
		}

		return t;

	}

}
