package org.osm2world;

import java.util.Collection;

import org.osm2world.DelaunayTriangulation.NaturalNeighbors;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * uses natural neighbor interpolation of heights
 */
public class NaturalNeighborStrategy implements EleInterpolationStrategy {

	private DelaunayTriangulation triangulation;
	
	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {
		
		AxisAlignedBoundingBoxXZ boundingBox = new AxisAlignedBoundingBoxXZ(sites);
		boundingBox = boundingBox.pad(100);
		
		triangulation = new DelaunayTriangulation(boundingBox.polygonXZ().getVertices());
		
		int i = 0; //TODO remove
		int total = sites.size();
		long startTime = System.currentTimeMillis();
				
		for (VectorXYZ site : sites) {
			if (++i % 1000 == 0) System.out.println("KS: " + i + "/" + total
					+ " after " + ((System.currentTimeMillis() - startTime) / 1e3));
			triangulation.insert(site);
			
		}
		
	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {
	
		NaturalNeighbors nn = triangulation.probe(pos);
		
		double ele = 0;
		
		for (int i = 0; i < nn.neighbors.length; i++) {
			ele += nn.neighbors[i].y * nn.relativeWeights[i];
		}
		
		return pos.xyz(ele);
		
	}
	
}
