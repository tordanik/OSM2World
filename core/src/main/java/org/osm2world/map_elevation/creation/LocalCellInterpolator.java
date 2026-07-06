package org.osm2world.map_elevation.creation;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.math3.util.MathUtils.checkNotNull;

import java.util.List;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;

/**
 * An interpolator which exploits the grid structure of many {@link TerrainEleDataSource}s
 * by determining elevation locally based on the relative position in the cell of 4 surrounding sites.
 */
public class LocalCellInterpolator implements TerrainInterpolator {

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

			List<LatLonEle> closestSites = eleData.findClosestSites(posLatLon, 8);

			return interpolateEleFromClosestSites(pos, closestSites.stream().map(projection::toXYZ).toList());

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

	static VectorXYZ interpolateEleFromSurroundingSites(VectorXZ pos, List<VectorXYZ> surroundingSites) {

		VectorXYZ bottomLeft = surroundingSites.get(0);
		VectorXYZ bottomRight = surroundingSites.get(1);
		VectorXYZ topLeft = surroundingSites.get(2);
		VectorXYZ topRight = surroundingSites.get(3);

		double minX = min(bottomLeft.x, topLeft.x);
		double maxX = max(bottomRight.x, topRight.x);
		double minZ = min(bottomLeft.z, bottomRight.z);
		double maxZ = max(topLeft.z, topRight.z);

		double impactRight = (pos.x - minX) / (maxX - minX);
		double impactTop = (pos.z - minZ) / (maxZ - minZ);

		double y = bottomLeft.y * (1 - impactRight) * (1 - impactTop)
				+ bottomRight.y * impactRight * (1 - impactTop)
				+ topLeft.y * (1 - impactRight) * impactTop
				+ topRight.y * impactRight * impactTop;

		return pos.xyz(y);

	}

}
