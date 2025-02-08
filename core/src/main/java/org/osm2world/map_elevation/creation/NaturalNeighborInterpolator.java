package org.osm2world.map_elevation.creation;

import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.util.Collection;

import org.osm2world.map_elevation.creation.DelaunayTriangulation.NaturalNeighbors;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * uses natural neighbor interpolation of heights
 */
public class NaturalNeighborInterpolator implements TerrainInterpolator {

	private DelaunayTriangulation triangulation;

	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {

		if (sites.isEmpty()) {
			throw new IllegalArgumentException("No sites with elevation available");
		}

		AxisAlignedRectangleXZ boundingBox = bbox(sites);
		boundingBox = boundingBox.pad(100);

		triangulation = new DelaunayTriangulation(boundingBox);

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
