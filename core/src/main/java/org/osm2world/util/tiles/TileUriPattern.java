package org.osm2world.util.tiles;

import java.net.URI;

import org.osm2world.math.geo.TileNumber;

/**
 * A URI pattern with placeholders for XYZ tile coordinates
 */
public record TileUriPattern(String pattern) {

	public TileUriPattern {

		if (!pattern.matches(".*\\{[xyz]}.*")) {
			throw new IllegalArgumentException("No placeholder in pattern: " + pattern);
		}

	}

	public URI buildURI(TileNumber tileNumber) {

		return URI.create(pattern.replace("{z}", Integer.toString(tileNumber.zoom))
				.replace("{x}", Integer.toString(tileNumber.x))
				.replace("{y}", Integer.toString(tileNumber.y)));

	}

}
