package org.osm2world;

import static java.lang.Math.*;

import java.util.Collection;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionGrid;


public class InverseDistanceWeightingInterpolator implements TerrainInterpolator {
	
	private static final double CUTOFF = 300;
	
	private final double negExp;
	private Collection<VectorXYZ> sites;
	private IntersectionGrid<VectorXYZ> siteGrid; //TODO: rename IntersectionGrid to something more generic
	
	public InverseDistanceWeightingInterpolator(double exponent) {
		this.negExp = -exponent;
	}
	
	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {
		
		this.sites = sites;
		
		siteGrid = new IntersectionGrid<VectorXYZ>(
				new AxisAlignedBoundingBoxXZ(sites).pad(CUTOFF/2),
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
		int cellX = siteGrid.cellXForCoord(pos.x, pos.z);
		int cellZ = siteGrid.cellZForCoord(pos.x, pos.z);
		
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
