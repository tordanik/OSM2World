package org.osm2world.map_elevation.creation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.util.List;

import org.osm2world.map_elevation.creation.DelaunayTriangulation.DelaunayTriangle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * triangulates the point set of elevation sites,
 * then interpolates linearly within each triangle
 * (i.e. treats the triangles as flat)
 */
public class LinearInterpolator implements TerrainInterpolator {

	private DelaunayTriangulation triangulation;

	public void setKnownSites(TerrainEleData eleData, MapProjection projection) {

		checkNotNull(eleData);
		checkNotNull(projection);

		List<VectorXYZ> sites = eleData.sites().stream().map(projection::toXYZ).toList();

		AxisAlignedRectangleXZ boundingBox = bbox(sites).pad(100);

		triangulation = new DelaunayTriangulation(boundingBox);

		for (VectorXYZ site : sites) {
			triangulation.insert(site);
		}

	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {

		DelaunayTriangle triangle = triangulation.getEnclosingTriangle(pos);

		double ele = triangle.asTriangleXYZ().getYAt(pos);

		return pos.xyz(ele);

	}

}
