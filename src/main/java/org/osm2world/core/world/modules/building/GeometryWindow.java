package org.osm2world.core.world.modules.building;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.interpolateBetween;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.JTSBufferUtil;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material;

public class GeometryWindow implements Window {

	private static final double DEPTH = 0.10;
	private static final double OUTER_FRAME_WIDTH = 0.1;
	private static final double INNER_FRAME_WIDTH = 0.05;
	private static final double OUTER_FRAME_THICKNESS = 0.05;
	private static final double INNER_FRAME_THICKNESS = 0.03;

	private final VectorXZ position;
	private final WindowParameters params;

	private final SimplePolygonXZ outline;
	private final SimplePolygonXZ paneOutline;
	private final List<List<VectorXZ>> innerFramePaths;

	private final boolean transparent;

	public GeometryWindow(VectorXZ position, WindowParameters params, boolean transparent) {

		this.position = position;
		this.params = params;
		this.transparent = transparent;

		switch (params.windowShape) {
		case CIRCLE:
			outline = circleShape(position, params.width, params.height);
			paneOutline = circleShape(position.add(new VectorXZ(0, OUTER_FRAME_WIDTH / 2)),
					params.width - OUTER_FRAME_WIDTH, params.height - OUTER_FRAME_WIDTH);
			break;
		case RECTANGLE:
		default:
			outline = rectangleShape(position, params.width, params.height);
			paneOutline = rectangleShape(position.add(new VectorXZ(0, OUTER_FRAME_WIDTH / 2)),
					params.width - OUTER_FRAME_WIDTH, params.height - OUTER_FRAME_WIDTH);
			break;
		}

		innerFramePaths = new ArrayList<>();

		AxisAlignedRectangleXZ paneBbox = paneOutline.boundingBox();

		VectorXZ windowBottom = paneBbox.center().add(0, -paneBbox.sizeZ() / 2);
		VectorXZ windowTop = paneBbox.center().add(0, +paneBbox.sizeZ() / 2);
		for (int vertFrameI = 0; vertFrameI < params.panesVertical - 1; vertFrameI ++) {
			VectorXZ center = interpolateBetween(windowBottom, windowTop, (vertFrameI + 1.0)/params.panesVertical);
			LineSegmentXZ intersectionSegment = new LineSegmentXZ(
					center.add(-paneBbox.sizeX(), 0), center.add(+paneBbox.sizeX(), 0));
			List<VectorXZ> is = paneOutline.intersectionPositions(intersectionSegment);
			innerFramePaths.add(asList(
					Collections.min(is, Comparator.comparingDouble(v -> v.x)),
					Collections.max(is, Comparator.comparingDouble(v -> v.x))));
		}

		VectorXZ windowLeft = paneBbox.center().add(-paneBbox.sizeX() / 2, 0);
		VectorXZ windowRight = paneBbox.center().add(+paneBbox.sizeX() / 2, 0);
		for (int horizFrameI = 0; horizFrameI < params.panesHorizontal - 1; horizFrameI ++) {
			VectorXZ center = interpolateBetween(windowLeft, windowRight, (horizFrameI + 1.0)/params.panesHorizontal);
			LineSegmentXZ intersectionSegment = new LineSegmentXZ(
					center.add(0, -paneBbox.sizeZ()), center.add(0, +paneBbox.sizeZ()));
			List<VectorXZ> is = paneOutline.intersectionPositions(intersectionSegment);
			innerFramePaths.add(asList(
					Collections.min(is, Comparator.comparingDouble(v -> v.z)),
					Collections.max(is, Comparator.comparingDouble(v -> v.z))));
		}

	}

	private SimplePolygonXZ rectangleShape(VectorXZ position, double width, double height) {
		return asSimplePolygon(new AxisAlignedRectangleXZ(-width/2, 0, +width/2, height).shift(position));
	}

	private SimplePolygonXZ circleShape(VectorXZ position, double width, double height) {
		return asSimplePolygon(new CircleXZ(position.add(0, height / 2), max(height, width)/2));
	}

	@Override
	public SimplePolygonXZ outline() {
		return outline;
	}

	@Override
	public Double insetDistance() {
		return DEPTH - OUTER_FRAME_THICKNESS;
	}

	@Override
	public void renderTo(Target target, WallSurface surface) {

		VectorXYZ windowNormal = surface.normalAt(outline().getCentroid());

		VectorXYZ toBack = windowNormal.mult(-DEPTH);
		VectorXYZ toOuterFrame = windowNormal.mult(-DEPTH + OUTER_FRAME_THICKNESS);

		/* draw the window pane */

		Material paneMaterial = transparent ? params.transparentWindowMaterial : params.opaqueWindowMaterial;

		List<TriangleXZ> paneTrianglesXZ = paneOutline.getTriangulation();
		List<TriangleXYZ> paneTriangles = paneTrianglesXZ.stream()
				.map(t -> surface.convertTo3D(t).shift(toBack))
				.collect(toList());
		target.drawTriangles(paneMaterial, paneTriangles,
				triangleTexCoordLists(paneTriangles, paneMaterial, surface::texCoordsGlobal));

		/* draw outer frame */

		List<SimplePolygonShapeXZ> innerOutlines = JTSBufferUtil.bufferPolygon(outline, -OUTER_FRAME_WIDTH)
				.stream().map(p -> p.getOuter()).collect(toList());

		List<TriangleXZ> frontFaceTriangles = triangulate(outline, innerOutlines);
		List<TriangleXYZ> frontFaceTrianglesXYZ = frontFaceTriangles.stream()
				.map(t -> surface.convertTo3D(t))
				.map(t -> t.shift(toOuterFrame))
				.collect(toList());
		target.drawTriangles(params.frameMaterial, frontFaceTrianglesXYZ,
				triangleTexCoordLists(frontFaceTrianglesXYZ, params.frameMaterial, surface::texCoordsGlobal));

		Material frameSideMaterial = params.frameMaterial;
		if (params.windowShape == WindowParameters.WindowShape.CIRCLE) {
			frameSideMaterial = frameSideMaterial.makeSmooth();
		}

		for (SimplePolygonShapeXZ innerOutline : innerOutlines) {
			PolygonXYZ innerOutlineXYZ = surface.convertTo3D(innerOutline);
			List<VectorXYZ> vsFrameSideStrip = createTriangleStripBetween(
					innerOutlineXYZ.add(toOuterFrame).vertices(),
					innerOutlineXYZ.add(toBack).vertices());
			target.drawTriangleStrip(frameSideMaterial, vsFrameSideStrip,
					texCoordLists(vsFrameSideStrip, frameSideMaterial, STRIP_WALL));
		}

		/* draw inner frame elements with shape extrusion along paths */

		ShapeXZ innerFrameShape = new AxisAlignedRectangleXZ(
				-INNER_FRAME_WIDTH/2, -INNER_FRAME_THICKNESS/2,
				+INNER_FRAME_WIDTH/2, +INNER_FRAME_THICKNESS/2);

		for (List<VectorXZ> framePath : innerFramePaths) {

			List<VectorXYZ> framePathXYZ = framePath.stream()
					.map(v -> surface.convertTo3D(v))
					.map(v -> v.add(toBack))
					.collect(toList());

			target.drawExtrudedShape(params.frameMaterial, innerFrameShape,
					framePathXYZ, nCopies(framePathXYZ.size(), windowNormal),
					null, null, EnumSet.noneOf(ExtrudeOption.class));

		}

	}

}
