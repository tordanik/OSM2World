package org.osm2world.map_elevation.creation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * Terrain elevation consisting of multiple {@link TerrainEleDataGrid}s.
 * This is used because terrain datasets usually consist of tiles or smaller datasets which have a grid structure
 * internally. We want to exploit that local grid structure even though neighboring tiles may have a different grid.
 * Grids are checked in order, with the first having the highest priority.
 * Sites from lower-priority grids may be ignored if a higher-priority grid "overlaps" it.
 */
public class TerrainEleDataGridGroup implements TerrainEleData {

	private final LatLonBounds bounds;
	private final List<TerrainEleDataGrid> grids;

	public TerrainEleDataGridGroup(List<TerrainEleDataGrid> grids) {
		checkArgument(!grids.isEmpty());
		this.grids = grids;
		this.bounds = LatLonBounds.union(grids.stream().map(TerrainEleData::bounds).toList());
	}

	public @Nullable TerrainEleDataGrid gridAt(LatLon pos) {
		for (TerrainEleDataGrid grid : grids) {
			if (grid.bounds().contains(pos)) {
				return grid;
			}
		}
		return null;
	}

	@Override
	public LatLonBounds bounds() {
		return bounds;
	}

	@Override
	public Collection<LatLonEle> sites() {
		List<LatLonEle> sites = new ArrayList<>();
		for (TerrainEleDataGrid grid : grids) {
			sites.addAll(grid.sites());
		}
		return sites;
	}

	@Override
	public int size() {
		return grids.stream().mapToInt(TerrainEleData::size).sum();
	}

	@Override
	public boolean isEmpty() {
		return grids.stream().allMatch(TerrainEleData::isEmpty);
	}

	@Override
	public List<LatLonEle> findClosestSites(LatLon pos, int n) {

		/* try to get all sites from the highest-priority matching grid first */

		TerrainEleDataGrid grid = gridAt(pos);
		if (grid != null && grid.size() >= n) {
			return grid.findClosestSites(pos, n);
		}

		/* get the closest sites among all grids */

		List<LatLonEle> candidates = new ArrayList<>(n * grids.size());

		for (TerrainEleDataGrid g : grids) {
			candidates.addAll(g.findClosestSites(pos, min(n, g.size())));
		}

		candidates.sort(Comparator.comparingDouble(it -> pos.distanceTo(it.latLon())));
		return candidates.subList(0, n);

	}

}
