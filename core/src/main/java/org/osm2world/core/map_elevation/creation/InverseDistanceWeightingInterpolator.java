package org.osm2world.core.map_elevation.creation;

import static java.lang.Math.*;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.Collection;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IndexGrid;


public class InverseDistanceWeightingInterpolator implements TerrainInterpolator {

	private static final double CUTOFF = 300;

	private final double negExp;
	private Collection<VectorXYZ> sites;
	private IndexGrid<VectorXYZ> siteGrid;

	public InverseDistanceWeightingInterpolator() {
		this(2);
	}

	public InverseDistanceWeightingInterpolator(double exponent) {
		this.negExp = -exponent;
	}

	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {

		if (sites.isEmpty()) {
			throw new IllegalArgumentException("No sites with elevation available");
		}

		this.sites = sites;

		siteGrid = new IndexGrid<VectorXYZ>(
				bbox(sites).pad(CUTOFF/2),
				CUTOFF, CUTOFF);

		for (VectorXYZ site : sites) {
			siteGrid.insert(site);
		}

	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {

		double weightSum = 0;
		double eleSum = 0;

		Collection<VectorXYZ>[][] cellArray = siteGrid.getCellArray();
		int cellX = siteGrid.cellXForCoord(pos.x);
		int cellZ = siteGrid.cellZForCoord(pos.z);

		for (int i = max(cellX-1, 0); i < min(cellX+2, cellArray.length); i++) {
			for (int j = max(cellZ-1, 0); j < min(cellZ+2, cellArray[i].length); j++) {

				Collection<VectorXYZ> sitesInCell = cellArray[i][j];

				if (sitesInCell == null) continue;

				for (VectorXYZ site : sitesInCell) {

					double distance = site.distanceToXZ(pos);

					if (distance < CUTOFF) {
						double weight = pow(distance, negExp);
						weightSum += weight;
						eleSum += site.y * weight;
					}

				}

			}
		}

		//System.out.println(pos + ": " + eleSum + ", " + weightSum + ", " + eleSum / weightSum);

		return pos.xyz(eleSum / weightSum);

	}

}
