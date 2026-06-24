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

	private final int numX;
	private final int numZ;

	private final double lonSize;
	private final double latSize;

	/**
	 * @param sites  non-jagged array of sites, at least 2 in each dimension.
	 *               The first index is the x/longitude dimension, the second index is the z/latitude direction.
	 *               Smallest lon and lat at 0,0.
	 */
	public TerrainEleDataGrid(LatLonEle[][] sites) {

		checkArgument(sites != null && sites.length >= 2 && sites[0].length >= 2);

		this.numX = sites.length;
		this.numZ = sites[0].length;

		LatLonEle firstSite = sites[0][0];
		LatLonEle lastSite = sites[numX - 1][numZ - 1];

		checkArgument(firstSite.lon <= lastSite.lon);
		checkArgument(firstSite.lat <= lastSite.lat);

		this.sites = sites;

		this.bounds = LatLonBounds.ofPoints(List.of(firstSite.latLon(), lastSite.latLon()));
		this.lonSize = bounds.sizeLon();
		this.latSize = bounds.sizeLat();

	}

	@Override
	public LatLonBounds bounds() {
		return bounds;
	}

	@Override
	public int size() {
		return numX * numZ;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Collection<LatLonEle> sites() {
		List<LatLonEle> result = new ArrayList<>(numX * numZ);
		for (int x = 0; x < numX; x++) {
			result.addAll(asList(sites[x]));
		}
		return result;
	}

	/** returns a subregion of this elevation grid */
	public TerrainEleDataGrid clipped(LatLonBounds clipBounds) {

		if (clipBounds.contains(bounds)) {
			return this;
		}

		clipBounds = LatLonBounds.intersection(List.of(clipBounds, bounds));

		if (clipBounds == null) {
			throw new IllegalArgumentException("clipBounds does not intersect this grid's bounds");
		}

		int minX = max(0, (int) floor((clipBounds.minlon - bounds.minlon) / lonSize * (numX - 1)));
		int minZ = max(0, (int) floor((clipBounds.minlat - bounds.minlat) / latSize * (numZ - 1)));
		int maxX = min(numX - 1, (int) ceil((clipBounds.maxlon - bounds.minlon) / lonSize * (numX - 1)));
		int maxZ = min(numZ - 1, (int) ceil((clipBounds.maxlat - bounds.minlat) / latSize * (numZ - 1)));

		var newData = new LatLonEle[maxX - minX + 1][maxZ - minZ + 1];

		for (int x = minX; x <= maxX; x++) {
			System.arraycopy(sites[x], minZ, newData[x - minX], 0, maxZ - minZ + 1);
		}

		return new TerrainEleDataGrid(newData);

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
				sites[cell.x][cell.z],
				sites[cell.x + 1][cell.z],
				sites[cell.x][cell.z + 1],
				sites[cell.x + 1][cell.z + 1]
		);

	}

	@Override
	public List<LatLonEle> findClosestSites(LatLon pos, int n) {

		if (n == 0) {
			return List.of();
		} else {

			List<LatLonEle> candidates;

			CellCoords cell = cellForPos(pos);
			int cellRange = (n - 1) / 2; // usually gets it right the first try, but not always (near the edge)
			do {
				candidates = new ArrayList<>();
				for (int x = max(0, cell.x - cellRange); x < min(numX, cell.x + cellRange + 2); x++) {
					candidates.addAll(asList(sites[x]).subList(max(0, cell.z - cellRange), min(numZ, cell.z + cellRange + 2)));
				}
				cellRange *= 2;
			} while (candidates.size() < n && (cellRange < numX || cellRange < numZ));
			if (candidates.size() < n) { throw new IllegalStateException("n too large for number of sites " + n); }

			candidates.sort(Comparator.comparingDouble(it -> pos.distanceTo(it.latLon())));
			return candidates.subList(0, n);

		}

	}

	@Nonnull
	private CellCoords cellForPos(LatLon pos) {

		int x = (int) floor((pos.lon - bounds.minlon) / lonSize * (numX - 1));
		int z = (int) floor((pos.lat - bounds.minlat) / latSize * (numZ - 1));

		x = max(0, min(x, numX - 2));
		z = max(0, min(z, numZ - 2));

		return new CellCoords(x, z);

	}

	private record CellCoords(int x, int z) {}

}
