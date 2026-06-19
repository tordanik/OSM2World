package org.osm2world.map_elevation.creation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.*;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

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

		var cell = cellForPos(pos);

		return List.of(
				sites[cell.i][cell.j],
				sites[cell.i][cell.j + 1],
				sites[cell.i + 1][cell.j],
				sites[cell.i + 1][cell.j + 1]
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
				CellCoords cell = cellForPos(pos);
				int cellRange = (int)ceil(sqrt(n) / 2) - 1; // usually gets it right the first try, but not always (corners, non-square grids)
				do {
					candidates = new ArrayList<>();
					for (int i = max(0, cell.i - cellRange); i < min(numLat, cell.i + cellRange + 2); i++) {
						candidates.addAll(asList(sites[i]).subList(max(0, cell.j - cellRange), min(numLon, cell.j + cellRange + 2)));
					}
					cellRange++;
				} while (candidates.size() < n && (cellRange < numLat || cellRange < numLon));
				if (candidates.size() < n) { throw new IllegalStateException("n too large for number of sites " + n); }
			}

			candidates.sort(Comparator.comparingDouble(it -> pos.distanceTo(it.latLon())));
			return candidates.subList(0, n);

		}

	}

	@Nonnull
	private CellCoords cellForPos(LatLon pos) {

		int i = (int) floor((pos.lat - bounds.minlat) / latSize * (numLat - 1));
		int j = (int) floor((pos.lon - bounds.minlon) / lonSize * (numLon - 1));

		i = max(0, min(i, numLat - 2));
		j = max(0, min(j, numLon - 2));

		return new CellCoords(i, j);

	}

	private record CellCoords(int i, int j) {}

}
