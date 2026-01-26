package org.osm2world.world.modules.building;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.algorithms.FaceDecompositionUtil.splitPolygonIntoFaces;
import static org.osm2world.math.algorithms.GeometryUtil.*;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.output.common.ExtrudeOption.END_CAP;
import static org.osm2world.scene.material.Materials.STEEL;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD0;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD4;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.world.modules.building.WindowParameters.WindowRegion.CENTER;
import static org.osm2world.world.modules.building.WindowParameters.WindowRegion.TOP;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;

import java.util.*;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.JTSBufferUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.common.ExtrudeOption;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.mesh.LODRange;
import org.osm2world.util.enums.LeftRightBoth;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.building.WindowParameters.RegionProperties;
import org.osm2world.world.modules.building.WindowParameters.WindowRegion;

public class GeometryWindow implements Window {

	private static final double DEPTH = 0.10;
	private static final double OUTER_FRAME_WIDTH = 0.1;
	private static final double INNER_FRAME_WIDTH = 0.05;
	private static final double OUTER_FRAME_THICKNESS = 0.05;
	private static final double INNER_FRAME_THICKNESS = 0.03;
	private static final double SHUTTER_THICKNESS = 0.03;

	private final WindowParameters params;

	private final SimpleClosedShapeXZ outline;
	private final SimpleClosedShapeXZ paneOutline;
	private final List<PolylineShapeXZ> innerFramePaths;

	private final boolean transparent;

	public GeometryWindow(VectorXZ position, WindowParameters params, boolean transparent) {

		this.params = params;
		this.transparent = transparent;

		/* build the outline, either as a simple shape or as a combination of multiple window regions */

		Map<WindowRegion, LineSegmentXZ> regionBorderSegments = new EnumMap<>(WindowRegion.class);
		Map<WindowRegion, SimpleClosedShapeXZ> regionOutlines = new EnumMap<>(WindowRegion.class);

		// TODO: implement the other regions: LEFT, RIGHT and BOTTOM
		boolean useRegions = params.regionProperties.containsKey(CENTER) && params.regionProperties.containsKey(TOP);

		if (!useRegions) {

			outline = params.overallProperties.shape.buildShapeXZ(position,
					params.overallProperties.width, params.overallProperties.height);

		} else {

			regionOutlines.put(CENTER, params.regionProperties.get(CENTER).shape.buildShapeXZ(position,
					params.regionProperties.get(CENTER).width, params.regionProperties.get(CENTER).height));
			SimpleClosedShapeXZ centerOutline = regionOutlines.get(CENTER);

			LineSegmentXZ topSegment = centerOutline.intersections(new LineSegmentXZ(
					centerOutline.getCentroid(), centerOutline.getCentroid().add(0, 1000)))
					.stream().findAny().get().segment();
			regionBorderSegments.put(TOP, topSegment);

			RegionProperties properties = params.regionProperties.get(TOP);

			double topHeight = params.regionProperties.get(TOP).height;
			regionOutlines.put(TOP, properties.shape.buildShapeXZ(topSegment, topHeight));

			assert !centerOutline.isClockwise();
			assert !regionOutlines.get(TOP).isClockwise();

			List<VectorXZ> newOutline = new ArrayList<>();

			List<VectorXZ> centerVs = centerOutline.verticesNoDup();
			for (int i = centerVs.indexOf(topSegment.p2);
					i != centerVs.indexOf(topSegment.p1);
					i = (i + 1) % centerVs.size()) {
				newOutline.add(centerOutline.vertices().get(i));
			}

			List<VectorXZ> topVs = regionOutlines.get(TOP).verticesNoDup();

			VectorXZ start = topVs.stream().min(Comparator.comparingDouble(topSegment.p1::distanceTo)).get();
			VectorXZ end = topVs.stream().min(Comparator.comparingDouble(topSegment.p2::distanceTo)).get();

			for (int i = topVs.indexOf(start);
					i != topVs.indexOf(end);
					i = (i + 1) % topVs.size()) {
				newOutline.add(topVs.get(i));
			}

			outline = new SimplePolygonXZ(closeLoop(newOutline));

		}

		/* calculate the border for the actual glass pane */

		paneOutline = paneOutlineFromOutline(outline);

		/* place borders */

		if (params.overallProperties.panes != null) {

			int panesVertical = params.overallProperties.panes.panesVertical;
			int panesHorizontal = params.overallProperties.panes.panesHorizontal;

			innerFramePaths = (params.overallProperties.panes.radialPanes)
					? innerPaneBorderPathsRadial(paneOutline, null, panesHorizontal, panesVertical)
					: innerPaneBorderPaths(paneOutline, panesHorizontal, panesVertical);

		} else if (!regionOutlines.isEmpty() && params.regionProperties.values().stream().anyMatch(it -> it.panes != null)) {

			innerFramePaths = new ArrayList<>();

			for (WindowRegion region : regionOutlines.keySet()) {

				RegionProperties properties = params.regionProperties.get(region);

				if (properties != null) {

					SimpleClosedShapeXZ regionPaneOutline = paneOutlineFromOutline(regionOutlines.get(region));

					if (properties.panes != null) {

						innerFramePaths.addAll((properties.panes.radialPanes)
								? innerPaneBorderPathsRadial(regionPaneOutline, regionBorderSegments.get(region),
										properties.panes.panesHorizontal, properties.panes.panesVertical)
								: innerPaneBorderPaths(regionPaneOutline,
										properties.panes.panesHorizontal, properties.panes.panesVertical));

					}

				}

			}

			innerFramePaths.addAll(regionBorderSegments.values());

		} else {
			innerFramePaths = emptyList();
		}

	}

	private static SimpleClosedShapeXZ paneOutlineFromOutline(SimpleClosedShapeXZ outline) {
		// scaling is a "good enough" simplification; polygon buffering would be accurate
		double scaleFactor = max(0.1, 1 - (INNER_FRAME_WIDTH / outline.getDiameter()));
		return outline.scale(scaleFactor);
	}

	private static List<PolylineShapeXZ> innerPaneBorderPaths(SimpleClosedShapeXZ paneOutline,
			int panesHorizontal, int panesVertical) {

		List<PolylineShapeXZ> result = new ArrayList<>();

		AxisAlignedRectangleXZ paneBbox = paneOutline.boundingBox();

		VectorXZ windowBottom = paneBbox.center().add(0, -paneBbox.sizeZ() / 2);
		VectorXZ windowTop = paneBbox.center().add(0, +paneBbox.sizeZ() / 2);
		for (int vertFrameI = 0; vertFrameI < panesVertical - 1; vertFrameI ++) {
			VectorXZ center = interpolateBetween(windowBottom, windowTop, (vertFrameI + 1.0)/panesVertical);
			LineSegmentXZ intersectionSegment = new LineSegmentXZ(
					center.add(-paneBbox.sizeX(), 0), center.add(+paneBbox.sizeX(), 0));
			List<VectorXZ> is = paneOutline.intersectionPositions(intersectionSegment);
			result.add(new LineSegmentXZ(
					Collections.min(is, Comparator.comparingDouble(v -> v.x)),
					Collections.max(is, Comparator.comparingDouble(v -> v.x))));
		}

		VectorXZ windowLeft = paneBbox.center().add(-paneBbox.sizeX() / 2, 0);
		VectorXZ windowRight = paneBbox.center().add(+paneBbox.sizeX() / 2, 0);
		for (int horizFrameI = 0; horizFrameI < panesHorizontal - 1; horizFrameI ++) {
			VectorXZ center = interpolateBetween(windowLeft, windowRight, (horizFrameI + 1.0)/panesHorizontal);
			LineSegmentXZ intersectionSegment = new LineSegmentXZ(
					center.add(0, -paneBbox.sizeZ()), center.add(0, +paneBbox.sizeZ()));
			List<VectorXZ> is = paneOutline.intersectionPositions(intersectionSegment);
			result.add(new LineSegmentXZ(
					Collections.min(is, Comparator.comparingDouble(v -> v.z)),
					Collections.max(is, Comparator.comparingDouble(v -> v.z))));
		}

		return result;

	}

	@SuppressWarnings("unused") // TODO use panesVertical for "rings" around the center
	private static List<PolylineShapeXZ> innerPaneBorderPathsRadial(SimpleClosedShapeXZ paneOutline,
			LineSegmentXZ regionBorderSegment, int panesHorizontal, int panesVertical) {

		List<PolylineShapeXZ> result = new ArrayList<>();

		VectorXZ center = paneOutline.getCentroid();
		if (regionBorderSegment != null) {
			center = regionBorderSegment.getCenter();
		}

		Angle minAngle, step;

		if (regionBorderSegment == null) {
			minAngle = Angle.ofDegrees(0);
			step = Angle.ofDegrees(360.0 / panesHorizontal);
		} else {
			// TODO: to support other regions than TOP, use regionBorderSegment's direction
			minAngle = Angle.ofDegrees(270);
			Angle maxAngle = Angle.ofDegrees(90);
			step = maxAngle.minus(minAngle).div(panesHorizontal);
		}

		for (int paneH = 0; paneH < panesHorizontal; paneH ++) {
			if (paneH > 0 || regionBorderSegment == null) {
				Angle angle = minAngle.plus(step.times(paneH));
				LineSegmentXZ intersectionSegment = new LineSegmentXZ(center,
						center.add(VectorXZ.fromAngle(angle).mult(2 * paneOutline.getDiameter())));
				VectorXZ intersectionPoint = paneOutline.intersectionPositions(intersectionSegment).get(0);
				result.add(new PolylineXZ(center, intersectionPoint));
			}
		}

		return result;

	}

	@Override
	public SimplePolygonXZ outline() {
		return asSimplePolygon(outline);
	}

	@Override
	public double insetDistance() {
		return DEPTH - OUTER_FRAME_THICKNESS;
	}

	@Override
	public void renderTo(CommonTarget target, WallSurface surface, O2WConfig config) {

		VectorXYZ windowNormal = surface.normalAt(outline().getCentroid());

		VectorXYZ toBack = windowNormal.mult(-DEPTH);
		VectorXYZ toOuterFrame = windowNormal.mult(-DEPTH + OUTER_FRAME_THICKNESS);

		/* draw the window pane */

		List<TriangleXZ> paneTrianglesXZ = paneOutline.getTriangulation();
		List<TriangleXYZ> paneTriangles = paneTrianglesXZ.stream()
				.map(t -> surface.convertTo3D(t).shift(toBack))
				.collect(toList());

		if (transparent) {
			LODRange previousLodRange = null;
			boolean skip = false;
			if (target instanceof ProceduralWorldObject.Target t) {
				previousLodRange = t.getCurrentLodRange();
				// intersect with previousLodRange to avoid loosening LOD, e.g. if GeometryWindow is only used at LOD4
				var lodRange = LODRange.intersection(previousLodRange, new LODRange(BuildingPart.INDOOR_MIN_LOD, LOD4));
				t.setCurrentLodRange(lodRange);
				skip = (lodRange == null);
			}
			if (!skip) {
				Material transparentMaterial = params.transparentWindowMaterial(config.mapStyle());
				target.drawTriangles(transparentMaterial, paneTriangles,
						triangleTexCoordLists(paneTriangles, transparentMaterial, surface::texCoordFunction));
			}
			if (target instanceof ProceduralWorldObject.Target t) {
				t.setCurrentLodRange(LOD0, BuildingPart.INDOOR_MIN_LOD);
				t.drawTriangles(params.opaqueWindowMaterial(), paneTriangles,
						triangleTexCoordLists(paneTriangles, params.opaqueWindowMaterial(), surface::texCoordFunction));
				t.setCurrentLodRange(previousLodRange);
			}
		} else {
			target.drawTriangles(params.opaqueWindowMaterial(), paneTriangles,
					triangleTexCoordLists(paneTriangles, params.opaqueWindowMaterial(), surface::texCoordFunction));
		}

		/* draw outer frame */

		SimplePolygonXZ outlinePolygon = asSimplePolygon(outline);

		List<SimplePolygonShapeXZ> innerOutlines = JTSBufferUtil.bufferPolygon(outlinePolygon, -OUTER_FRAME_WIDTH)
				.stream().map(p -> p.getOuter()).collect(toList());

		List<TriangleXZ> frontFaceTriangles = triangulate(outlinePolygon, innerOutlines);
		List<TriangleXYZ> frontFaceTrianglesXYZ = frontFaceTriangles.stream()
				.map(t -> surface.convertTo3D(t))
				.map(t -> t.shift(toOuterFrame))
				.collect(toList());
		target.drawTriangles(params.frameMaterial, frontFaceTrianglesXYZ,
				triangleTexCoordLists(frontFaceTrianglesXYZ, params.frameMaterial, surface::texCoordFunction));

		Material frameSideMaterial = params.frameMaterial;
		if (params.overallProperties.shape == WindowParameters.WindowShape.CIRCLE) {
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

		for (PolylineShapeXZ framePath : innerFramePaths) {

			List<VectorXYZ> framePathXYZ = framePath.vertices().stream()
					.map(v -> surface.convertTo3D(v))
					.map(v -> v.add(toBack))
					.collect(toList());

			target.drawExtrudedShape(params.frameMaterial, innerFrameShape,
					framePathXYZ, nCopies(framePathXYZ.size(), windowNormal),
					null, EnumSet.noneOf(ExtrudeOption.class));

		}

		/* draw shutters */

		if (params.shutterSide != null) {

			LineSegmentXZ splitLine = new LineSegmentXZ(
					outline.getCentroid().add(0, -outline.getDiameter()),
					outline.getCentroid().add(0, +outline.getDiameter()));

			Collection<PolygonWithHolesXZ> outlineParts = splitPolygonIntoFaces(asSimplePolygon(outline), List.of(), List.of(splitLine));

			double hingeSpace = 0.03;

			for (PolygonWithHolesXZ outlinePart : outlineParts) {

				boolean isInRightHalf = isRightOf(outlinePart.getOuter().getCentroid(), splitLine.p1, splitLine.p2);

				double axisX;

				if (params.shutterSide == LeftRightBoth.RIGHT
						|| (params.shutterSide == LeftRightBoth.BOTH && isInRightHalf)) {
					axisX = outline.boundingBox().maxX + hingeSpace / 2;
				} else {
					axisX = outline.boundingBox().minX - hingeSpace / 2;
				}

				PolygonShapeXZ mirroredOutlinePart = outlinePart.mirrorX(axisX);

				VectorXZ extrusionStart = outline.getCentroid();
				VectorXYZ extrusionStartXYZ = surface.convertTo3D(extrusionStart);
				SimpleClosedShapeXZ extrusionShape = mirroredOutlinePart.getOuter().shift(extrusionStart.invert());

				target.drawExtrudedShape(params.shutterMaterial, extrusionShape,
						asList(extrusionStartXYZ, extrusionStartXYZ.add(windowNormal.mult(SHUTTER_THICKNESS))),
						nCopies(2, VectorXYZ.Y_UNIT), null, EnumSet.of(END_CAP));

			}

			/* draw shutter hinges */

			// TODO: place hinges based on shapes (placement is not suitable for some, e.g. triangles)
			// TODO: place 2 hinges for certain shapes

			AxisAlignedRectangleXZ bbox = outline.boundingBox().pad(hingeSpace / 2);

			List<VectorXZ> hingeLocations = new ArrayList<>();

			if (params.shutterSide.isLeftOrBoth()) {
				hingeLocations.add(new VectorXZ(bbox.minX, bbox.center().z));
			}
			if (params.shutterSide.isRightOrBoth()) {
				hingeLocations.add(new VectorXZ(bbox.maxX, bbox.center().z));
			}

			for (VectorXZ hingeLocation : hingeLocations) {
				double hingeHeight = 0.1;
				VectorXYZ base = surface.convertTo3D(hingeLocation).addY(-hingeHeight / 2);
				target.drawColumn(STEEL.get(config), null, base, hingeHeight, hingeSpace / 2, hingeSpace / 2, true, true);
			}

		}

	}

}
