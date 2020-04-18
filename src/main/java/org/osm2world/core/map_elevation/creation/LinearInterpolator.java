package org.osm2world.core.map_elevation.creation;

import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.Collection;

import org.osm2world.core.map_elevation.creation.DelaunayTriangulation.DelaunayTriangle;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * triangulates the point set of elevation sites,
 * then interpolates linearly within each triangle
 * (i.e. treats the triangles as flat)
 */
public class LinearInterpolator implements TerrainInterpolator {

	private DelaunayTriangulation triangulation;

	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {

		AxisAlignedRectangleXZ boundingBox = bbox(sites);
		boundingBox = boundingBox.pad(100);

		triangulation = new DelaunayTriangulation(boundingBox);

		for (VectorXYZ site : sites) {
			triangulation.insert(site);
		}

	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {

		DelaunayTriangle triangle = triangulation.getEnlosingTriangle(pos);

		double ele = triangle.asTriangleXYZ().getYAt(pos);

		return pos.xyz(ele);

	}

}
