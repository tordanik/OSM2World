package org.osm2world.core.math.geo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * a region on the globe described by a range of {@link TileNumber}s.
 */
public interface TileBounds extends GeoBounds {

	Set<TileNumber> getTiles();

	@Override
	default LatLonBounds latLonBounds() {
		List<LatLonBounds> tileBounds = getTiles().stream().map(TileNumber::latLonBounds).toList();
		return LatLonBounds.union(tileBounds);
	}

	/**
	 * returns the smallest TileBounds which fit around all tiles from a collection
	 * @param tiles  at least one {@link TileNumber}
	 */
	static TileBounds around(Collection<TileNumber> tiles) {

		if (tiles.size() == 1) {
			return tiles.iterator().next();
		} else {
			int zoom = tiles.stream().mapToInt(it -> it.zoom).max().orElseThrow();
			return around(tiles, zoom);
		}
	}

	/**
	 * variant of {@link #around(Collection)} which explicitly sets the zoom level of the result
	 */
	static TileBounds around(Collection<TileNumber> tiles, int zoom) {

		class TileRect implements TileBounds {

			private final Set<TileNumber> tiles;
			private final LatLonBounds latLonBounds;

			TileRect(Set<TileNumber> tiles) {
				this.tiles = tiles;
				this.latLonBounds = LatLonBounds.union(tiles.stream().map(TileNumber::latLonBounds).toList());
			}

			@Override
			public Set<TileNumber> getTiles() {
				return tiles;
			}

			@Override
			public LatLonBounds latLonBounds() {
				return latLonBounds;
			}

		}

		var bounds = LatLonBounds.union(tiles.stream().map(TileNumber::latLonBounds).toList());
		// shrink bounds a tiny bit to prevent the neighboring tiles from being generated as well
		bounds = new LatLonBounds(bounds.minlat + 1e-5, bounds.minlon + 1e-5,
				bounds.maxlat - 1e-5, bounds.maxlon - 1e-5);
		Set<TileNumber> allTiles = new HashSet<>(TileNumber.tilesForBounds(zoom, bounds));

		return new TileRect(allTiles);

	}

}
