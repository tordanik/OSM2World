package org.osm2world.map_elevation.creation;

import static org.apache.commons.math3.util.MathUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;

/**
 * An interpolator that only considers the 4 surrounding sites in a grid.
 * (Has some fallback logic for non-grid data or positions between grids.)
 */
public abstract class AbstractGridLocalInterpolator implements TerrainInterpolator {

	private TerrainEleData eleData;
	private MapProjection projection;

	@Override
	public void setKnownSites(TerrainEleData eleData, MapProjection projection) {

		checkNotNull(eleData);
		checkNotNull(projection);

		this.eleData = eleData;
		this.projection = projection;

		if (!(eleData instanceof TerrainEleDataGridOrGridGroup)) {
			ConversionLog.warn(getClass().getSimpleName() + " is intended for terrain ele data with a grid structure");
		}

	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {

		LatLon posLatLon = projection.toLatLon(pos);

		/* try to find and use surrounding sites in the current grid */

		List<LatLonEle> surroundingSites = null;

		if (eleData instanceof TerrainEleDataGridOrGridGroup grids) {
			TerrainEleDataGrid grid = grids.gridAt(posLatLon);
			if (grid != null) {
				surroundingSites = grid.findSurroundingSites(posLatLon);
			}
		}

		if (surroundingSites != null) {
			return interpolateEleFromSurroundingSites(pos, projectSurroundingSites(surroundingSites));
		} else {

			/* get closest sites, attempt to get a box of 4 surrounding sites */

			List<LatLonEle> closestSites = eleData.findClosestSites(posLatLon, 8);
			List<VectorXYZ> closestSitesXYZ = closestSites.stream().map(projection::toXYZ).toList();

			List<List<VectorXYZ>> sitesByQuadrant = new ArrayList<>(4);
			for (int i = 0; i < 4; i++) { sitesByQuadrant.add(new ArrayList<>()); }

			for (VectorXYZ site : closestSitesXYZ) {
				if (site.z < pos.z) {
					if (site.x < pos.x) {
						sitesByQuadrant.get(0).add(site);
					} else {
						sitesByQuadrant.get(1).add(site);
					}
				} else {
					if (site.x < pos.x) {
						sitesByQuadrant.get(2).add(site);
					} else {
						sitesByQuadrant.get(3).add(site);
					}
				}
			}

			if (sitesByQuadrant.stream().noneMatch(List::isEmpty)) {

				Comparator<VectorXYZ> comparator = Comparator.comparingDouble(it -> it.distanceToXZ(pos));
				List<VectorXYZ> surroundingSitesXYZ = List.of(
						sitesByQuadrant.get(0).stream().min(comparator).orElseThrow(IllegalStateException::new),
						sitesByQuadrant.get(1).stream().min(comparator).orElseThrow(IllegalStateException::new),
						sitesByQuadrant.get(2).stream().min(comparator).orElseThrow(IllegalStateException::new),
						sitesByQuadrant.get(3).stream().min(comparator).orElseThrow(IllegalStateException::new)
				);

				return interpolateEleFromSurroundingSites(pos, surroundingSitesXYZ);

			} else {
				return interpolateEleFromClosestSites(pos, closestSitesXYZ);
			}

		}

	}

	private List<VectorXYZ> projectSurroundingSites(List<LatLonEle> surroundingSites) {
		assert surroundingSites.size() == 4;
		return List.of(
				projection.toXYZ(surroundingSites.get(0)),
				projection.toXYZ(surroundingSites.get(1)),
				projection.toXYZ(surroundingSites.get(2)),
				projection.toXYZ(surroundingSites.get(3))
		);
	}

	private VectorXYZ interpolateEleFromClosestSites(VectorXZ pos, List<VectorXYZ> closestSites) {

		double weightSum = 0;
		double weightedEleSum = 0;

		for (VectorXYZ site : closestSites) {

			double distance = pos.distanceTo(site.xz());
			double weight = 1 / distance;
			weightSum += weight;
			weightedEleSum += site.y * weight;

		}

		return pos.xyz(weightedEleSum / weightSum);

	}

	protected abstract VectorXYZ interpolateEleFromSurroundingSites(VectorXZ pos, List<VectorXYZ> surroundingSites);

}
