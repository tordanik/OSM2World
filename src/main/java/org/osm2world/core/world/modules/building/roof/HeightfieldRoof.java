package org.osm2world.core.world.modules.building.roof;

import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.SLOPED_TRIANGLES;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.Poly2TriUtil;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.util.exception.TriangulationException;

/**
 * superclass for roofs that have exactly one height value
 * for each point within their XZ polygon
 */
abstract public class HeightfieldRoof extends Roof {

	public HeightfieldRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		super(originalPolygon, tags, height, material);
	}

	/** returns segments within the roof polygon that define ridges or edges of the roof */
	public abstract Collection<LineSegmentXZ> getInnerSegments();

	/** returns segments within the roof polygon that define apex nodes of the roof */
	public abstract Collection<VectorXZ> getInnerPoints();

	/**
	 * returns roof height at a position.
	 * Only required to work for positions that are part of the polygon, segments or points for the roof.
	 *
	 * @return  height, null if unknown
	 */
	protected abstract Double getRoofHeightAt_noInterpolation(VectorXZ pos);

	@Override
	public double getRoofHeightAt(VectorXZ v) {

		Double ele = getRoofHeightAt_noInterpolation(v);

		if (ele != null) {
			return ele;
		} else {

			// get all segments from the roof

			//TODO (performance): avoid doing this for every node

			Collection<LineSegmentXZ> segments = new ArrayList<>();

			segments.addAll(this.getInnerSegments());
			segments.addAll(this.getPolygon().getOuter().getSegments());
			for (SimplePolygonXZ hole : this.getPolygon().getHoles()) {
				segments.addAll(hole.getSegments());
			}

			// find the segment with the closest distance to the node

			LineSegmentXZ closestSegment = null;
			double closestSegmentDistance = Double.MAX_VALUE;

			for (LineSegmentXZ segment : segments) {
				double segmentDistance = distanceFromLineSegment(v, segment);
				if (segmentDistance < closestSegmentDistance) {
					closestSegment = segment;
					closestSegmentDistance = segmentDistance;
				}
			}

			// use that segment for height interpolation

			return interpolateValue(v,
					closestSegment.p1,
					getRoofHeightAt_noInterpolation(closestSegment.p1),
					closestSegment.p2,
					getRoofHeightAt_noInterpolation(closestSegment.p2));

		}
	}

	@Override
	public void renderTo(Target target, double baseEle) {

		/* create the triangulation of the roof */

		Collection<TriangleXZ> triangles;

		try {

			triangles = Poly2TriUtil.triangulate(
					getPolygon().getOuter(),
				    getPolygon().getHoles(),
				    getInnerSegments(),
				    getInnerPoints());

		} catch (TriangulationException e) {

			triangles = JTSTriangulationUtil.triangulate(
					getPolygon().getOuter(),
					getPolygon().getHoles(),
					getInnerSegments(),
					getInnerPoints());

		}

		List<TriangleXYZ> trianglesXYZ = new ArrayList<>(triangles.size());

		for (TriangleXZ triangle : triangles) {
			TriangleXZ tCCW = triangle.makeCounterclockwise();
			VectorXZ v = tCCW.v1;
			VectorXZ v1 = tCCW.v2;
			VectorXZ v2 = tCCW.v3;
			trianglesXYZ.add(new TriangleXYZ(
					v.xyz(baseEle + getRoofHeightAt(v)),
					v1.xyz(baseEle + getRoofHeightAt(v1)),
					v2.xyz(baseEle + getRoofHeightAt(v2))));
		}

		/* draw triangles */

		target.drawTriangles(material, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, material, SLOPED_TRIANGLES));

	}

}