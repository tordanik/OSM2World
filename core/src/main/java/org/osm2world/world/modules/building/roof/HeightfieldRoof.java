package org.osm2world.world.modules.building.roof;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.algorithms.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.math.algorithms.GeometryUtil.interpolateValue;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.target.common.texcoord.NamedTexCoordFunction.SLOPED_TRIANGLES;
import static org.osm2world.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.JTSTriangulationUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.target.CommonTarget;
import org.osm2world.target.common.material.Material;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;

/**
 * superclass for roofs that have exactly one height value
 * for each point within their XZ polygon
 */
abstract public class HeightfieldRoof extends Roof {

	/** if {@link #getPolygon()} has additional points inserted, this is the threshold for snapping to existing points */
	protected static final double SNAP_DISTANCE = 0.01;

	protected @Nullable AttachmentSurface attachmentSurface;

	public HeightfieldRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material);
	}

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
	public Collection<AttachmentSurface> getAttachmentSurfaces(double baseEle, int level) {

		if (attachmentSurface == null) {

			List<String> types = List.of("roof" + level, "roof", "floor" + level);

			try {
				attachmentSurface = new AttachmentSurface(types, getRoofTriangles(baseEle));
			} catch (Exception e) {
				ConversionLog.error("Suppressed exception in HeightfieldRoof attachment surface generation", e);
				PolygonXYZ flatPolygon = getPolygon().getOuter().xyz(baseEle);
				attachmentSurface = new AttachmentSurface(types, List.of(new FaceXYZ(flatPolygon.vertices())));
			}

		}

		return singleton(attachmentSurface);

	}

	@Override
	public void renderTo(CommonTarget target, double baseEle) {

		List<TriangleXYZ> trianglesXYZ = getRoofTriangles(baseEle);

		/* draw triangles */

		target.drawTriangles(material, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, material, SLOPED_TRIANGLES));

	}

	private List<TriangleXYZ> getRoofTriangles(double baseEle) {

		/* subtract attached rooftop areas (parking, helipads, pools, etc.) from the roof polygon */

		List<PolygonShapeXZ> subtractPolys = new ArrayList<>();

		if (attachmentSurface != null) {
			for (AttachmentConnector connector : attachmentSurface.getAttachedConnectors()) {
				if (connector.object != null) {
					subtractPolys.addAll(connector.object.getRawGroundFootprint());
				}
			}
		}

		subtractPolys.addAll(this.getPolygon().getHoles());

		/* triangulate the (remaining) roof polygon */

		List<TriangleXZ> trianglesXZ = JTSTriangulationUtil.triangulate(
					getPolygon().getOuter(),
					subtractPolys.stream().map(p -> asSimplePolygon(p.getOuter())).collect(toList()),
					getInnerSegments(),
					getInnerPoints());

		/* assign elevations to the triangulation */

		List<TriangleXYZ> trianglesXYZ = new ArrayList<>(trianglesXZ.size());

		for (TriangleXZ triangle : trianglesXZ) {
			TriangleXZ tCCW = triangle.makeCounterclockwise();
			trianglesXYZ.add(tCCW.xyz(v -> v.xyz(baseEle + getRoofHeightAt(v))));
		}

		return trianglesXYZ;

	}

}
