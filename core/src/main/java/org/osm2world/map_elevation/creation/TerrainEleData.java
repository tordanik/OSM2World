package org.osm2world.map_elevation.creation;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

import com.google.common.collect.Ordering;

/**
 * Terrain elevation data.
 * Usually obtained from a {@link TerrainEleDataSource}.
 * Contains the sites with known elevation within particular geographic bounds.
 */
public interface TerrainEleData {

	LatLonBounds bounds();

	/** returns all sites */
	Collection<LatLonEle> sites();

	/** returns the number of sites */
	default int size() { return sites().size(); }

	/** checks whether this data contains any sites, i.e. whether {@link #size()} is 0 */
	default boolean isEmpty() { return size() == 0; }

	/** returns the n closest sites to the given position */
	default List<LatLonEle> findClosestSites(LatLon pos, int n) {

		Collection<LatLonEle> sites = sites();
		if (sites.size() < n) { throw new IllegalArgumentException("Only " + sites.size() + " sites available"); }

		// calculate distances only once, it's expensive
		Map<LatLonEle, Double> siteDistances = sites.stream().collect(toMap(
				it -> it, it -> pos.distanceTo(it.latLon())));

		var ordering = Ordering.<LatLonEle>from(Comparator.comparingDouble(siteDistances::get));
		return ordering.leastOf(sites, n);

	}

}
