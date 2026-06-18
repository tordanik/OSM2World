package org.osm2world.map_elevation.creation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * Terrain elevation data.
 * Usually obtained from a {@link TerrainEleDataSource}.
 * Contains the sites with known elevation within particular geographic bounds.
 */
public interface TerrainEleData {

	LatLonBounds bounds();

	/** returns all sites */
	Collection<LatLonEle> sites();

	/** checks whether this data contains any sites */
	default boolean isEmpty() { return sites().isEmpty(); }

	/** returns the n closest sites to the given position */
	default List<LatLonEle> findClosestSites(LatLon pos, int n) {
		Collection<LatLonEle> sites = sites();
		List<LatLonEle> siteList = sites instanceof List ? (List<LatLonEle>) sites : new ArrayList<>(sites);
		if (siteList.size() < n) { throw new IllegalArgumentException("Only " + siteList.size() + " sites available"); }
		siteList.sort(Comparator.comparingDouble(it -> pos.distanceTo(it.latLon())));
		return siteList.subList(0, n);
	}

}
