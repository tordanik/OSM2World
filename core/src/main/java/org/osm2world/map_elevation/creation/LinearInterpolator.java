package org.osm2world.map_elevation.creation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.TriangleXYZ;

/**
 * triangulates the point set of elevation sites,
 * then interpolates linearly within each triangle
 * (i.e. treats the triangles as flat)
 */
public class LinearInterpolator implements TerrainInterpolator {

	private TerrainInterpolator implementation;

	@Override
	public void setKnownSites(TerrainEleData eleData, MapProjection projection) {
		if (eleData instanceof TerrainEleDataGridOrGridGroup) {
			implementation = new GridImplementation();
		} else {
			implementation = new GeneralImplementation();
		}
		implementation.setKnownSites(eleData, projection);
	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {
		return implementation.interpolateEle(pos);
	}

	/** implementation that works for data no matter how the sites are distributed */
	private static class GeneralImplementation implements TerrainInterpolator {

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

			DelaunayTriangulation.DelaunayTriangle triangle = triangulation.getEnclosingTriangle(pos);

			double ele = triangle.asTriangleXYZ().getYAt(pos);

			return pos.xyz(ele);

		}

	}

	/** implementation specifically for data with a grid structure */
	private static class GridImplementation implements TerrainInterpolator {

		private TerrainEleDataGridOrGridGroup eleData;
		private MapProjection projection;

		public void setKnownSites(TerrainEleData eleData, MapProjection projection) {

			this.projection = projection;

			if (eleData instanceof TerrainEleDataGridOrGridGroup grid) {
				this.eleData = grid;
			} else {
				throw new IllegalArgumentException("Unsupported TerrainEleData type: " + eleData.getClass());
			}

		}

		@Override
		public VectorXYZ interpolateEle(VectorXZ pos) {

			LatLon posLatLon = projection.toLatLon(pos);

			/* find the correct grid */

			TerrainEleDataGrid grid = eleData.gridAt(posLatLon);

			if (grid != null) {

				List<LatLonEle> surroundingSites = grid.findSurroundingSites(posLatLon);

				TriangleXYZ t = new TriangleXYZ(
						projection.toXYZ(surroundingSites.get(0)),
						projection.toXYZ(surroundingSites.get(1)),
						projection.toXYZ(surroundingSites.get(2)));

				if (!t.xz().contains(pos)) {
					t = new TriangleXYZ(
							projection.toXYZ(surroundingSites.get(1)),
							projection.toXYZ(surroundingSites.get(2)),
							projection.toXYZ(surroundingSites.get(3)));
				}

				return pos.xyz(t.getYAt(pos));

			} else {

				List<LatLonEle> closestSites = eleData.findClosestSites(posLatLon, 3);

				// FIXME implement proper interpolation between closest sites
				return pos.xyz(closestSites.get(0).ele());

			}

		}

	}

}
