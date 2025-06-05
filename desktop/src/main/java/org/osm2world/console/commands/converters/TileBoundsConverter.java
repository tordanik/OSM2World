package org.osm2world.console.commands.converters;

import static java.util.Arrays.stream;

import java.util.List;

import org.osm2world.math.geo.TileBounds;
import org.osm2world.math.geo.TileNumber;

import picocli.CommandLine;

public class TileBoundsConverter implements CommandLine.ITypeConverter<TileBounds> {

	@Override
	public TileBounds convert(String value) throws IllegalArgumentException {

		String[] components = value.split("\\s+");

		List<TileNumber> tiles = stream(components).map(TileNumber::new).toList();

		if (tiles.isEmpty()) {
			throw new IllegalArgumentException("Not a valid tile number bounds value");
		}

		return tiles.size() == 1
				? tiles.get(0)
				: TileBounds.around(tiles);

	}

}
