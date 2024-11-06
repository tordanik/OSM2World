package org.osm2world.core.world.modules.building.roof;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.distanceFromLineSegment;
import static org.osm2world.core.math.GeometryUtil.interpolateValue;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.SLOPED_TRIANGLES;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.*;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.CommonTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;

/**
 * superclass for roofs that have exactly one height value
 * for each point within their XZ polygon
 */
abstract public class HeightfieldRoof extends Roof {

	/** if {@link #getPolygon()} has additional points inserted, this is the threshold for snapping to existing points */
	protected static final double SNAP_DISTANCE = 0.01;

	protected @Nullable AttachmentSurface attachmentSurface;

	public HeightfieldRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, double height, Material material) {
		super(originalPolygon, tags, height, material);
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

			String[] types = new String[] {"roof" + level, "roof", "floor" + level};

			try {
				AttachmentSurface.Builder builder = new AttachmentSurface.Builder(types);
				this.renderTo(builder, baseEle);
				attachmentSurface = builder.build();
			} catch (Exception e) {
				ConversionLog.error("Suppressed exception in HeightfieldRoof attachment surface generation", e);
				PolygonXYZ flatPolygon = getPolygon().getOuter().xyz(baseEle);
				attachmentSurface = new AttachmentSurface(asList(types), asList(new FaceXYZ(flatPolygon.vertices())));
			}

		}

		return singleton(attachmentSurface);

	}

	@Override
	public void renderTo(CommonTarget target, double baseEle) {

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

		/* draw triangles */

		target.drawTriangles(material, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, material, SLOPED_TRIANGLES));

	}

}
