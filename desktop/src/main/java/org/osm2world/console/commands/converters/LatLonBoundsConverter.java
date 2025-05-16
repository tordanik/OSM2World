package org.osm2world.console.commands.converters;

import static java.util.Arrays.stream;

import java.util.List;

import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;

import picocli.CommandLine;

public class LatLonBoundsConverter implements CommandLine.ITypeConverter<LatLonBounds> {

	@Override
	public LatLonBounds convert(String value) throws IllegalArgumentException {

		String[] components = value.split("\\s+");

		List<LatLon> coords = stream(components).map(LatLon::new).toList();

		if (coords.size() < 2) {
			throw new IllegalArgumentException("Bounds require at least two latitude-longitude tuples");
		}

		return LatLonBounds.ofPoints(coords);

	}

}