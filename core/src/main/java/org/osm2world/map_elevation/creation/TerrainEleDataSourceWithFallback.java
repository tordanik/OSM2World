package org.osm2world.map_elevation.creation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * A {@link TerrainEleDataSource} with a fallback source which is used if the first has no data for a location.
 * Can be chained to allow an arbitrary number of fallbacks.
 */
public class TerrainEleDataSourceWithFallback implements TerrainEleDataSource {

	public final TerrainEleDataSource primaryDataSource;
	public final TerrainEleDataSource secondaryDataSource;

	public TerrainEleDataSourceWithFallback(TerrainEleDataSource primaryDataSource, TerrainEleDataSource secondaryDataSource) {
		this.primaryDataSource = primaryDataSource;
		this.secondaryDataSource = secondaryDataSource;
	}

	@Override
	public String toString() {
		return "(" + primaryDataSource + ", fallback: " + secondaryDataSource + ")";
	}

	@Override
	public TerrainEleData getSites(LatLonBounds bounds) throws IOException {

		TerrainEleData data = primaryDataSource.getSites(bounds);

		if (data.isEmpty()) {
			return secondaryDataSource.getSites(bounds);
		}

		if (data instanceof TerrainEleDataGridOrGridGroup gridGroup) {

			List<TerrainEleDataGrid> grids = gridGroup.grids();

			if (grids.stream().mapToDouble(it -> it.bounds().area()).sum()
					>= 0.95 * bounds.area()) {
				return data;
			} else {

				TerrainEleData data2 = secondaryDataSource.getSites(bounds);

				if (data2 instanceof TerrainEleDataGridOrGridGroup gridGroup2) {
					// combine the two grid groups
					List<TerrainEleDataGrid> resultGrids = new ArrayList<>(grids);
					resultGrids.addAll(gridGroup2.grids());
					return new TerrainEleDataGridGroup(resultGrids);
				} else {

					Collection<LatLonEle> resultSites = new ArrayList<>(data.sites());

					secondaryDataSource.getSites(bounds).sites().stream()
							.filter(it -> grids.stream().anyMatch(g -> g.bounds().contains(it.latLon())))
							.forEach(resultSites::add);

					return new TerrainEleDataCollection(bounds, resultSites);

				}

			}

		} else {

			Collection<LatLonEle> sites = data.sites();
			var siteBounds = LatLonBounds.ofPoints(sites.stream().map(LatLonEle::latLon).toList());

			if (siteBounds.area() >= 0.95 * bounds.area()) {
				return data;
			} else {

				Collection<LatLonEle> resultSites = new ArrayList<>(sites);

				secondaryDataSource.getSites(bounds).sites().stream()
						.filter(it -> !siteBounds.contains(it.latLon()))
						.forEach(resultSites::add);

				return new TerrainEleDataCollection(bounds, resultSites);

			}

		}

	}

}
