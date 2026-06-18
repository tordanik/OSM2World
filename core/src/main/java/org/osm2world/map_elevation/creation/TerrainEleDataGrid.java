package org.osm2world.map_elevation.creation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.*;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * Terrain elevation data with a grid structure.
 * Knowing this overall structure can sometimes be exploited for better performance.
 */
public class TerrainEleDataGrid implements TerrainEleData {

	private final LatLonBounds bounds;
	private final LatLonEle[][] sites;

	private final int numLat;
	private final int numLon;

	private final double latSize;
	private final double lonSize;

	public TerrainEleDataGrid(LatLonEle[][] sites) {

		checkArgument(sites != null && sites.length != 0 && sites[0].length != 0);

		this.numLat = sites.length;
		this.numLon = sites[0].length;

		LatLonEle firstSite = sites[0][0];
		LatLonEle lastSite = sites[numLat - 1][numLon - 1];

		checkArgument(firstSite.lat <= lastSite.lat);
		checkArgument(firstSite.lon <= lastSite.lon);

		this.sites = sites;

		this.bounds = LatLonBounds.ofPoints(List.of(firstSite.latLon(), lastSite.latLon()));
		this.latSize = bounds.sizeLat();
		this.lonSize = bounds.sizeLon();

	}

	@Override
	public LatLonBounds bounds() {
		return bounds;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Collection<LatLonEle> sites() {
		List<LatLonEle> result = new ArrayList<>(numLat * numLon);
		for (int i = 0; i < numLat; i++) {
			result.addAll(asList(sites[i]));
		}
		return result;
	}

	/**
	 * Returns the 4 sites surrounding the given position in this grid.
	 *
	 * @param pos  a position within the bounds of this grid
	 * @return  the 4 sites, ordered from bottom-left to top-right
	 */
	public List<LatLonEle> findSurroundingSites(LatLon pos) {

		int i = (int) floor((pos.lat - bounds.minlat) / latSize * (numLat - 1));
		int j = (int) floor((pos.lon - bounds.minlon) / lonSize * (numLon - 1));

		i = max(0, min(i, numLat - 2));
		j = max(0, min(j, numLon - 2));

		return List.of(
				sites[i][j],
				sites[i][j + 1],
				sites[i + 1][j],
				sites[i + 1][j + 1]
		);

	}

	@Override
	public List<LatLonEle> findClosestSites(LatLon pos, int n) {

		if (n == 4) {
			return findSurroundingSites(pos);
		} else {

			List<LatLonEle> candidates;

			if (n < 4) {
				candidates = new ArrayList<>(findSurroundingSites(pos));
			} else {
				// TODO implement a faster solution which narrows down the candidates first
				return TerrainEleData.super.findClosestSites(pos, n);
			}

			candidates.sort(Comparator.comparingDouble(it -> pos.distanceTo(it.latLon())));
			return candidates.subList(0, n);

		}

	}
}
