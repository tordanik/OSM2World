package org.osm2world.world.modules;

import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXYZ.addYList;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.math.algorithms.GeometryUtil.interpolateBetween;
import static org.osm2world.output.common.ExtrudeOption.END_CAP;
import static org.osm2world.output.common.ExtrudeOption.START_CAP;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.material.Materials.*;
import static org.osm2world.scene.mesh.LevelOfDetail.*;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createVerticalTriangleStrip;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseHeight;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseWidth;
import static org.osm2world.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.*;
import java.util.function.Function;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
import org.osm2world.world.data.NodeModelInstance;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;
import org.osm2world.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * adds barriers to the world
 */
public class BarrierModule extends AbstractModule {

	@Override
	protected void applyToWaySegment(MapWaySegment line) {

		TagSet tags = line.getTags();
		if (!tags.containsKey("barrier")) return; //fast exit for common case

		if (Wall.fits(tags)) {
			line.addRepresentation(new Wall(line));
		} else if (CityWall.fits(tags)) {
			line.addRepresentation(new CityWall(line));
		} else if (Hedge.fits(tags)) {
			line.addRepresentation(new Hedge(line));
		} else if (Railing.fits(tags)) {
			line.addRepresentation(new Railing(line));
		} else if (Balustrade.fits(tags)) {
			line.addRepresentation(new Balustrade(line));
		} else if (ChainLinkFence.fits(tags)) {
			line.addRepresentation(new ChainLinkFence(line));
		} else if (CableBarrier.fits(tags)) {
			line.addRepresentation(new CableBarrier(line));
		} else if (HandRail.fits(tags)) {
			line.addRepresentation(new HandRail(line));
		} else if (Guardrail.fits(tags)) {
			line.addRepresentation(new Guardrail(line));
		} else if (JerseyBarrier.fits(tags)) {
			line.addRepresentation(new JerseyBarrier(line));
		} else if (TrellisWorkFence.fits(tags)) {
			line.addRepresentation(new TrellisWorkFence(line));
		} else if (PoleFence.fits(tags)) {
			line.addRepresentation(new PoleFence(line));
		} else if (BollardRow.fits(tags)) {
			line.addRepresentation(new BollardRow(line));
		} else if (ChainRow.fits(tags)) {
			line.addRepresentation(new ChainRow(line));
		}

	}

	@Override
	protected void applyToNode(MapNode node) {

		TagSet tags = node.getTags();
		if (!tags.containsKey("barrier")) return; //fast exit for common case

		if (tags.contains("barrier", "bollard")) {
			node.addRepresentation(new NodeModelInstance(node, createBollardModel(tags)));
		} else if (Chain.fits(tags)){
			node.addRepresentation(new Chain(node, tags));
		}

	}

	@Override
	protected void applyToArea(MapArea area) {
		TagSet tags = area.getTags();
		if (tags.contains("natural", "shrubbery")
				|| tags.contains("barrier", "hedge")) {
			area.addRepresentation(new DenseShrubbery(area));
		}
	}

	private static Model createBollardModel(TagSet tags) {
		double height = parseHeight(tags, 1.0);
		double width = parseWidth(tags, 0.3);
		// TODO: support and document other bollard shapes, or support bollard models from 3dmr
		if (tags.contains("bollard:shape", "roundtop")) {
			return new RoundtopBollard(height, width);
		} else {
			return new CylinderBollard(height, width);
		}
	}

	private static abstract class LinearBarrier extends AbstractNetworkWaySegmentWorldObject
			implements ProceduralWorldObject {

		protected final double height;
		protected final double width;

		public LinearBarrier(MapWaySegment waySegment, double defaultHeight, double defaultWidth) {

			super(waySegment);

			height = parseHeight(waySegment.getTags(), defaultHeight);
			width = parseWidth(waySegment.getTags(), defaultWidth);

			createAttachmentConnectors();

		}

		@Override
		public double getWidth() {
			return width;
		}

	}

	/**
	 * superclass for linear barriers that are a simple colored or textured "wall"
	 * (use width and height to create vertical sides and a flat top)
	 */
	private static abstract class ColoredWall extends LinearBarrier implements ProceduralWorldObject {

		private final Material material;

		public ColoredWall(Material material, MapWaySegment segment,
				float defaultHeight, float defaultWidth) {
			super(segment, defaultHeight, defaultWidth);
			this.material = material;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			LevelOfDetail minLod = LOD2;
			if (height > 5 || width > 3) {
				minLod = LOD0;
			} else if (height > 2 || width > 1) {
				minLod = LOD1;
			}

			target.setCurrentLodRange(minLod, LOD4);


			List<VectorXYZ> leftBottomOutline = getOutline(false);
			List<VectorXYZ> leftTopOutline = addYList(leftBottomOutline, height);

			List<VectorXYZ> rightBottomOutline = getOutline(true);
			List<VectorXYZ> rightTopOutline = addYList(rightBottomOutline, height);

			/* define the base ele function */

			Function<VectorXZ, Double> baseEleFunction = (VectorXZ point) -> {
				PolylineXZ centerlineXZ = new PolylineXZ(getCenterlineXZ());
				double ratio = centerlineXZ.offsetOf(centerlineXZ.closestPoint(point));
				return GeometryUtil.interpolateOn(getCenterline(), ratio).y;
			};

			/* close the wall at the end if necessary */

			if (capNeededAtNode(segment.getStartNode())) {
				List<VectorXYZ> startCapVs = asList(
						leftTopOutline.get(0),
						leftBottomOutline.get(0),
						rightTopOutline.get(0),
						rightBottomOutline.get(0));
				target.drawTriangleStrip(material, startCapVs, texCoordLists(startCapVs, material, STRIP_WALL));
			}

			if (capNeededAtNode(segment.getEndNode())) {
				List<VectorXYZ> startCapVs = asList(
						rightTopOutline.get(rightTopOutline.size() - 1),
						rightBottomOutline.get(rightBottomOutline.size() - 1),
						leftTopOutline.get(leftTopOutline.size() - 1),
						leftBottomOutline.get(leftBottomOutline.size() - 1));
				target.drawTriangleStrip(material, startCapVs, texCoordLists(startCapVs, material, STRIP_WALL));
			}

			/* draw the top of the wall */

			List<VectorXYZ> topVs = asList(
					leftTopOutline.get(0),
					rightTopOutline.get(0),
					leftTopOutline.get(leftTopOutline.size() - 1),
					rightTopOutline.get(rightTopOutline.size() - 1));

			if (leftTopOutline.size() > 2 && leftTopOutline.size() == rightTopOutline.size()) {
				topVs = createTriangleStripBetween(leftTopOutline, rightTopOutline);
			}

			target.drawTriangleStrip(material, topVs, texCoordLists(topVs, material, STRIP_WALL));

			/* draw the sides of the wall */

			target.setCurrentAttachmentTypes(baseEleFunction, "wall");

			reverse(leftTopOutline);
			reverse(leftBottomOutline);

			List<VectorXYZ> leftVs = createTriangleStripBetween(leftTopOutline, leftBottomOutline);
			target.drawTriangleStrip(material, leftVs, texCoordLists(leftVs, material, STRIP_WALL));

			List<VectorXYZ> rightVs = createTriangleStripBetween(rightTopOutline, rightBottomOutline);
			target.drawTriangleStrip(material, rightVs, texCoordLists(rightVs, material, STRIP_WALL));

		}

		private boolean capNeededAtNode(MapNode node) {

			List<? extends ColoredWall> others = getConnectedNetworkSegments(node, this.getClass(), s -> s != this);

			return others.isEmpty()
					|| others.stream().allMatch(it -> it.height < this.height || it.width < this.width);

		}

	}

	public static class Wall extends ColoredWall {

		private final static Material DEFAULT_MATERIAL = Materials.STONE;

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "wall");
		}

		private static Material getMaterial(TagSet tags) {

			Material material = null;

			if ("gabion".equals(tags.getValue("wall"))) {
				material = Materials.WALL_GABION;
			} else if ("brick".equals(tags.getValue("wall"))) {
				material = BRICK;
			} else if ( tags.containsKey("material") ) {
				material = Materials.getMaterial(tags.getValue("material").toUpperCase());
			}

			if (material == null) {
				material = DEFAULT_MATERIAL;
			}

			String colorString = tags.getValue("colour");
			boolean colorable = material.getNumTextureLayers() == 0
					|| material.getTextureLayers().get(0).colorable;

			if (colorString != null && colorable) {
				material = material.withColor(parseColor(colorString, CSS_COLORS));
			}

			return material;

		}

		public Wall(MapWaySegment segment) {
			super(getMaterial(segment.getTags()), segment, 1f, 0.25f);
		}

	}

	public static class CityWall extends ColoredWall {
		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "city_wall");
		}
		public CityWall(MapWaySegment segment) {
			super(Wall.DEFAULT_MATERIAL, segment, 10, 2);
		}
	}

	public static class Hedge extends ColoredWall {
		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "hedge");
		}
		public Hedge(MapWaySegment segment) {
			super(Materials.HEDGE, segment, 1f, 0.5f);
		}
		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {
			return emptyList();
		}
	}

	public static class Railing extends LinearBarrier {

		private static final SimpleClosedShapeXZ BAR_SHAPE =
				new AxisAlignedRectangleXZ(-0.5, -0.3, 0.5, 0);

		private static final SimpleClosedShapeXZ SQUARE =
				new AxisAlignedRectangleXZ(-0.5, -0.5, 0.5, 0.5);

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "fence")
					&& tags.contains("fence_type", "railing");
		}

		public Railing(MapWaySegment segment) {
			super(segment, 1f, 0.1f);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			Material material = METAL_FENCE;

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(2.4, true, getCenterline());

			/* draw top and bottom bars */

			target.drawExtrudedShape(material, BAR_SHAPE, addYList(polePositions, height),
					nCopies(polePositions.size(), Y_UNIT),
					nCopies(polePositions.size(), width), null);

			target.drawExtrudedShape(material, BAR_SHAPE, addYList(polePositions, 0.15 * height),
					nCopies(polePositions.size(), Y_UNIT),
					nCopies(polePositions.size(), width), null);

			/* draw poles and the smaller vertical bars */

			List<VectorXYZ> poleFacingDirections = nCopies(2, segment.getDirection().xyz(0));
			List<Double> poleScale = nCopies(2, width);
			List<Double> smallPoleScale = nCopies(2, width * 0.2);

			for (VectorXYZ v : polePositions) {

				//draw pole
				target.drawExtrudedShape(material, SQUARE,
						asList(v, v.addY(0.99 * height)),
						poleFacingDirections, poleScale, null);

			}

			for (int i = 0; i + 1 < polePositions.size(); i++) {

				List<VectorXYZ> smallPolePositions = equallyDistributePointsAlong(
						0.12, true, polePositions.subList(i, i+2));

				for (int j = 1; j < smallPolePositions.size() - 1; j++) {

					VectorXYZ v = smallPolePositions.get(j);

					//draw small vertical bar
					target.drawExtrudedShape(material, SQUARE,
							asList(v.addY(0.14 * height), v.addY(0.99 * height)),
							poleFacingDirections, smallPoleScale, null);

				}

			}

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class Balustrade extends LinearBarrier {

		private static final SimpleClosedShapeXZ BAR_SHAPE =
				new AxisAlignedRectangleXZ(-0.5, -0.3, 0.5, 0);

		private static final SimpleClosedShapeXZ COLUMN_SHAPE =
				new CircleXZ(NULL_VECTOR, 0.5);

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "fence")
					&& tags.contains("fence_type", "balustrade");
		}

		public Balustrade(MapWaySegment segment) {
			super(segment, 1f, 0.25f);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(1.75 * width, false, getCenterline());

			/* parse material and color */

			Material material = null;

			if (segment.getTags().containsKey("material")) {
				material = Materials.getMaterial(segment.getTags().getValue("material").toUpperCase());
				//TODO also look at fence:material
			}

			if (material == null) {
				material = CONCRETE;
			}

			material = material.withColor(parseColor(segment.getTags().getValue("colour"), CSS_COLORS));
			material = material.makeSmooth();

			/* draw top and bottom bars */

			target.drawExtrudedShape(material, BAR_SHAPE, addYList(getCenterline(), height),
					nCopies(getCenterline().size(), Y_UNIT),
					nCopies(getCenterline().size(), width), EnumSet.of(START_CAP, END_CAP));

			target.drawExtrudedShape(material, BAR_SHAPE, addYList(getCenterline(), 0.07),
					nCopies(getCenterline().size(), Y_UNIT),
					nCopies(getCenterline().size(), width), EnumSet.of(START_CAP, END_CAP));

			/* draw columns */

			double poleHeight = 0.99 * height;
			List<Double> poleScale = asList(0.6, 0.47, 0.45, 0.65, 0.67, 0.65, 0.45, 0.47, 0.6)
					.stream().map(v -> v * width * 1.5).collect(toList());
			List<Double> poleRelativeHeights = asList(0.0, 0.2, 0.25, 0.45, 0.5, 0.55, 0.75, 0.8, 1.0);
			assert poleScale.size() == poleRelativeHeights.size();

			for (VectorXYZ v : polePositions) {

				List<VectorXYZ> extrudePath = poleRelativeHeights.stream()
						.map(h -> interpolateBetween(v, v.addY(poleHeight), h))
						.collect(toList());

				target.drawExtrudedShape(material, COLUMN_SHAPE, extrudePath,
						nCopies(extrudePath.size(), segment.getDirection().xyz(0)), poleScale, null);

			}

		}

	}

	public static class ChainLinkFence extends LinearBarrier {

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "fence")
					&& (tags.contains("fence_type", "chain_link") || (tags.contains("fence_type", "metal")));
		}

		public ChainLinkFence(MapWaySegment segment) {
			super(segment, 1f, 0.02f);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			/* render fence */

			List<VectorXYZ> pointsWithEle = getCenterline();

			List<VectorXYZ> vsFence = createVerticalTriangleStrip(pointsWithEle, 0, height);
			target.drawTriangleStrip(CHAIN_LINK_FENCE, vsFence,
					texCoordLists(vsFence, CHAIN_LINK_FENCE, STRIP_WALL));

			if (!CHAIN_LINK_FENCE.isDoubleSided()) {

				List<VectorXYZ> pointsWithEleBack = new ArrayList<>(pointsWithEle);
				Collections.reverse(pointsWithEleBack);

				List<VectorXYZ> vsFenceBack = createVerticalTriangleStrip(pointsWithEleBack, 0, height);
				target.drawTriangleStrip(CHAIN_LINK_FENCE, vsFenceBack,
						texCoordLists(vsFenceBack, CHAIN_LINK_FENCE, STRIP_WALL));

			}

			/* render poles */

			//TODO connect the poles to the ground independently

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(
					2f, true, getCenterline());

			for (VectorXYZ base : polePositions) {

				target.drawColumn(Materials.METAL_FENCE_POST, null, base,
						height, width, width, false, true);

			}

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class PoleFence extends LinearBarrier {

		private Material material;
		protected float barWidth;
		protected float barGap;
		protected float barOffset;
		protected int bars;
		protected Material defaultFenceMaterial = Materials.WOOD;
		protected Material defaultPoleMaterial = Materials.WOOD;
		protected Material poleMaterial;

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "fence");
		}

		public PoleFence(MapWaySegment segment) {
			super(segment, 1f, 0.02f);
			if (segment.getTags().containsKey("material")){
				material = getMaterial(segment.getTags().getValue("material").toUpperCase());
				poleMaterial = material;
			}

			this.barWidth = 0.1f;
			this.barGap = 0.2f;
			this.bars = 10;
			this.barOffset = barGap/2;

		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			if (material == null) {
				material = defaultFenceMaterial;
				poleMaterial = defaultPoleMaterial;
			}

			// prevents issues with barriers that cross themselves

			List<VectorXYZ> centerline = getCenterline();
			List<VectorXYZ> baseline = new ArrayList<>(asList(centerline.get(0), centerline.get(centerline.size() - 1)));


			/* render fence */
			for (int i = 0; i < bars; i++) {
				double barEndHeight = height - (i * barGap) - barOffset;
				double barStartHeight = barEndHeight - barWidth;

				if (barStartHeight > 0) {
					List<VectorXYZ> vsLowFront = createVerticalTriangleStrip(baseline, barStartHeight, barEndHeight);
					List<VectorXYZ> vsLowBack = createVerticalTriangleStrip(baseline, barEndHeight, barStartHeight);

					List<List<VectorXZ>> texCoordListsFenceFront = texCoordLists(
							vsLowFront, material, STRIP_WALL);
					List<List<VectorXZ>> texCoordListsFenceBack = texCoordLists(
							vsLowBack, material, STRIP_WALL);

					target.drawTriangleStrip(material, vsLowFront, texCoordListsFenceFront);
					target.drawTriangleStrip(material, vsLowBack, texCoordListsFenceBack);
				}
			}


			/* render poles */

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(
					2f, false, baseline);

			for (VectorXYZ base : polePositions) {

				target.drawColumn(poleMaterial, null, base,
						height, width, width, false, true);

			}

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class TrellisWorkFence extends LinearBarrier {

		private static final SimpleClosedShapeXZ PLANK_SHAPE;

		static {

			// create a semicircle shape

			List<VectorXZ> vertexLoop = new ArrayList<VectorXZ>();

			for (VectorXZ v : new CircleXZ(NULL_VECTOR, 0.05).vertices()) {
				if (v.x <= 0) {
					vertexLoop.add(v);
				}
			}

			if (!vertexLoop.get(0).equals(vertexLoop.get(vertexLoop.size() - 1))) {
				vertexLoop.add(vertexLoop.get(0));
			}

			PLANK_SHAPE = new SimplePolygonXZ(vertexLoop);

		}

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "fence")
					&& tags.contains("fence_type", "trellis_work");
		}

		public TrellisWorkFence(MapWaySegment segment) {
			super(segment, 0.7f, 0.1f);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			double distanceBetweenRepetitions = 0.3;

			List<VectorXYZ> positions = equallyDistributePointsAlong(
					distanceBetweenRepetitions / 2, true, getCenterline());

			int numIntersections = (int)ceil(height / distanceBetweenRepetitions);

			VectorXZ offsetBackPlank = segment.getRightNormal().mult(-0.05);

			for (int i = 0; i + 2 * numIntersections - 1 < positions.size(); i += 2) {

				int leftIndex = i;
				int rightIndex = i + 2 * numIntersections - 1;

				target.drawExtrudedShape(WOOD, PLANK_SHAPE,
						asList(positions.get(leftIndex).add(offsetBackPlank),
								positions.get(rightIndex).addY(height).add(offsetBackPlank)),
						nCopies(2, Y_UNIT), null, EnumSet.of(START_CAP, END_CAP));

				target.drawExtrudedShape(WOOD, PLANK_SHAPE,
						asList(positions.get(leftIndex).addY(height),
								positions.get(rightIndex)),
						nCopies(2, Y_UNIT), null, EnumSet.of(START_CAP, END_CAP));

			}

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class CableBarrier extends PoleFence {

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "cable_barrier");
		}

		public CableBarrier(MapWaySegment segment) {
			super(segment);

			this.barWidth = 0.03f;
			this.barGap = 0.1f;
			this.bars = 4;
			this.barOffset = barGap/2;

			this.defaultFenceMaterial = Materials.METAL_FENCE;
			this.defaultPoleMaterial = Materials.METAL_FENCE_POST;
		}
	}

	public static class HandRail extends PoleFence {

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "handrail");
		}

		public HandRail(MapWaySegment segment) {
			super(segment);

			this.barWidth = 0.05f;
			this.barGap = 0f;
			this.bars = 1;
			this.barOffset = 0;

			this.defaultFenceMaterial = Materials.HANDRAIL_DEFAULT;
			this.defaultPoleMaterial = Materials.HANDRAIL_DEFAULT;
		}

	}

	public static class Guardrail extends LinearBarrier {

		private static final float DEFAULT_HEIGHT = 0.75f;

		private static final float METERS_BETWEEN_POLES = 4;

		private static final float SHAPE_GERMAN_B_HEIGHT = 0.303f;
		private static ShapeXZ SHAPE_GERMAN_B = new PolylineXZ(
				new VectorXZ(-0.055, 0),
				new VectorXZ(-0.075, 0.007),
				new VectorXZ(-0.075, 0.1095),
				new VectorXZ(     0, 0.127),
				new VectorXZ(     0, 0.183),
				new VectorXZ(-0.075, 0.2005),
				new VectorXZ(-0.075, 0.303),
				new VectorXZ(-0.055, 0.310)
				);

		private static ShapeXZ SHAPE_POST_DOUBLE_T = new PolylineXZ(
				new VectorXZ(0, 0),
				new VectorXZ(-0.075, 0),
				new VectorXZ(0, 0),
				new VectorXZ(+0.075, 0),
				new VectorXZ(0, 0),
				new VectorXZ(0, -0.28),
				new VectorXZ(-0.075, -0.28),
				new VectorXZ(0, -0.28),
				new VectorXZ(+0.075, -0.28),
				new VectorXZ(0, -0.28),
				new VectorXZ(0, 0)
				);

		/** half the distance between the centres of a pair of bolts */
		private static final double BOLT_OFFSET = 0.1;

		private static final double BOLT_RADIUS = 0.015;
		private static final double BOLT_DEPTH = 0.02;

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "guard_rail");
		}

		public Guardrail(MapWaySegment line) {
			super(line, DEFAULT_HEIGHT, 0.0001f);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			List<VectorXYZ> centerline = getCenterline();

			Material material = STEEL.makeSmooth();

			/* draw the rail itself */

			target.setCurrentLodRange(LOD3, LOD4);

			List<VectorXYZ> path = addYList(centerline, this.height - SHAPE_GERMAN_B_HEIGHT);

			target.drawExtrudedShape(material.makeDoubleSided(), SHAPE_GERMAN_B,
					path, nCopies(path.size(), Y_UNIT), null, null);

			/* add posts */

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(
					METERS_BETWEEN_POLES, false, centerline);

			for (VectorXYZ base : polePositions) {

				VectorXZ railNormal = segment.getRightNormal();

				// extrude the pole

				List<VectorXYZ> polePath = asList(base,
						base.addY(height - SHAPE_GERMAN_B_HEIGHT*0.33));

				target.setCurrentLodRange(LOD3, LOD4);
				target.drawExtrudedShape(material, SHAPE_POST_DOUBLE_T, polePath,
						nCopies(polePath.size(), railNormal.xyz(0)), null, null);

				// extrude the bolts connecting the pole to the rail

				ShapeXZ boltShape = new CircleXZ(NULL_VECTOR, BOLT_RADIUS);

				VectorXYZ centerBetweenBolts = base.addY(height - SHAPE_GERMAN_B_HEIGHT*0.5);

				List<VectorXYZ> boltPositions = asList(
						centerBetweenBolts.add(segment.getDirection().mult(BOLT_OFFSET)),
						centerBetweenBolts.add(segment.getDirection().mult(-BOLT_OFFSET)));

				for (VectorXYZ boltPosition : boltPositions) {

					List<VectorXYZ> boltPath = asList(boltPosition,
							boltPosition.add(railNormal.mult(BOLT_DEPTH)));

					target.setCurrentLodRange(LOD4, LOD4);
					target.drawExtrudedShape(material, boltShape, boltPath,
							nCopies(boltPath.size(), Y_UNIT), null, EnumSet.of(END_CAP));

				}

			}

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class JerseyBarrier extends LinearBarrier {

		private static final float DEFAULT_HEIGHT = 1.145f;
		private static final float DEFAULT_WIDTH = 0.82f;
		private static final double ELEMENT_LENGTH = 3.0;
		private static final double GAP_LENGTH = 0.3;

		private static ShapeXZ DEFAULT_SHAPE = new SimplePolygonXZ(asList(
				new VectorXZ(+0.41, 0    ),
				new VectorXZ(+0.41, 0.075),
				new VectorXZ(+0.20, 0.330),
				new VectorXZ(+0.15, 1.145),
				new VectorXZ(-0.15, 1.145),
				new VectorXZ(-0.20, 0.330),
				new VectorXZ(-0.41, 0.075),
				new VectorXZ(-0.41, 0    ),
				new VectorXZ(+0.41, 0    )
				));

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "jersey_barrier");
		}

		public JerseyBarrier(MapWaySegment line) {
			super(line, DEFAULT_HEIGHT, DEFAULT_WIDTH);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			/* subdivide the centerline;
			 * there'll be a jersey barrier element between each pair of successive points */

			List<VectorXYZ> points = equallyDistributePointsAlong(
					ELEMENT_LENGTH + GAP_LENGTH, true, getCenterline());

			/* draw jersey barrier elements with small gaps in between */

			for (int i = 0; i + 1 < points.size(); i++) {

				double relativeOffset = 0.5 * GAP_LENGTH / (ELEMENT_LENGTH + GAP_LENGTH);

				List<VectorXYZ> path = asList(
						interpolateBetween(points.get(i), points.get(i+1), relativeOffset),
						interpolateBetween(points.get(i), points.get(i+1), 1.0 - relativeOffset));

				target.drawExtrudedShape(CONCRETE, DEFAULT_SHAPE, path,
						nCopies(2, Y_UNIT), null, EnumSet.of(START_CAP, END_CAP));

			}

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class BollardRow extends AbstractNetworkWaySegmentWorldObject {

		private final Model bollardModel;

		public BollardRow(MapWaySegment segment) {
			super(segment);
			this.bollardModel = createBollardModel(segment.getTags());
		}

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "bollard");
		}

		@Override
		public List<Mesh> buildMeshes() {
			return emptyList(); // all meshes are in sub-models
		}

		@Override
		public List<ModelInstance> getSubModels() {

			//TODO connect the bollards to the ground independently

			//TODO: bollard_count or similar tag exists? create "Bollards" rep.
			//just as lift gates etc, this should use the line.getRightNormal and the road width

			List<VectorXYZ> bollardPositions = equallyDistributePointsAlong(2, false, getCenterline());

			return bollardPositions.stream()
					.map(base -> new ModelInstance(bollardModel, new InstanceParameters(base, 0)))
					.collect(toList());

		}

		@Override
		public double getWidth() {
			return 0.15;
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return emptyList();
		}

	}

	public static class ChainRow extends PoleFence{

		private static final Integer DEFAULT_NO_CHAIN_SEGMENTS = 8;

		private static final SimpleClosedShapeXZ BAR_SHAPE = new AxisAlignedRectangleXZ(-0.03, -0.03, 0.03, 0);

		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "chain");
		}

		public ChainRow(MapWaySegment line) {
			super(line);

			this.barWidth = 0.05f;
			this.defaultPoleMaterial = STEEL;
			this.defaultFenceMaterial = STEEL;
			this.barGap = 1.5f;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			if (this.poleMaterial == null) {
				poleMaterial = defaultPoleMaterial;
			}

			/* render poles */

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(
					this.barGap, true, getCenterline());

			for (VectorXYZ base : polePositions) {
				target.setCurrentLodRange(LOD2, LOD4);
				target.drawColumn(this.poleMaterial, null, base,
						this.height, this.barWidth, this.barWidth, false, true);
			}

			/* render chain */

			for(int k = 0; k < polePositions.size() - 1; k++){

				List<VectorXYZ> chainPath = new ArrayList<VectorXYZ>();
				VectorXYZ offset = polePositions.get(k+1).subtract(polePositions.get(k));
				float actualBarGap = (float)offset.length();

				// +0.1 allows for floating point errors
				for(float i = 0f; i <= actualBarGap + 0.1; i += actualBarGap/DEFAULT_NO_CHAIN_SEGMENTS){
					//approximation of chain sag
					double a = 5f / 2f;
					double sag = a * cosh((i-(actualBarGap/2))/a)  -  a * cosh(actualBarGap/(2 * a));
					double pointDrop = height + sag;
					chainPath.add(polePositions.get(k).add(offset.normalize().mult(i)).addY(pointDrop));
				}

				target.setCurrentLodRange(LOD3, LOD4);
				target.drawExtrudedShape(defaultFenceMaterial, BAR_SHAPE, chainPath, nCopies(DEFAULT_NO_CHAIN_SEGMENTS + 1,
						Y_UNIT), null, null);
			}
		}
	}

	public static class CylinderBollard implements Model {

		private final double height;
		private final double width;

		public CylinderBollard(double height, double width) {
			this.height = height;
			this.width = width;
		}

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {
			return singletonList(new Mesh(ExtrusionGeometry.createColumn(null, params.position(), height,
					width/2, width/2, false, true, null, CONCRETE.getTextureDimensions()), CONCRETE, LOD2, LOD4));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(height);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(width);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CylinderBollard other = (CylinderBollard) obj;
			if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
				return false;
			if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
				return false;
			return true;
		}

	}

	public static class RoundtopBollard implements Model {

		private final double height;
		private final double width;

		public RoundtopBollard(double height, double width) {
			this.height = height;
			this.width = width;
		}

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			double radius = width / 2;
			double splitHeight = max(0, height - radius);

			double radius2 = radius * radius;

			List<Double> heights = new ArrayList<>(asList(
					splitHeight + 0d,
					splitHeight + 1*radius/6,
					splitHeight + 2*radius/6,
					splitHeight + 3*radius/6,
					splitHeight + 4*radius/6,
					splitHeight + 5*radius/6,
					splitHeight + 5.5*radius/6,
					splitHeight + radius));
			List<Double> scaleFactors = new ArrayList<>(asList(
					1d,
					sqrt(radius2 - 1*radius/6 * 1*radius/6) / radius,
					sqrt(radius2 - 2*radius/6 * 2*radius/6) / radius,
					sqrt(radius2 - 3*radius/6 * 3*radius/6) / radius,
					sqrt(radius2 - 4*radius/6 * 4*radius/6) / radius,
					sqrt(radius2 - 5*radius/6 * 5*radius/6) / radius,
					sqrt(radius2 - 5.5*radius/6 * 5.5*radius/6) / radius,
					0d));

			if (splitHeight > 0) {
				heights.add(0, 0.0);
				scaleFactors.add(0, 1.0);
			}

			List<VectorXYZ> path = new ArrayList<>();
			heights.forEach(it -> path.add(params.position().addY(it)));

			return singletonList(new Mesh(new ExtrusionGeometry(new CircleXZ(NULL_VECTOR, radius),
					path, null, scaleFactors, null, null, CONCRETE.getTextureDimensions()), CONCRETE, LOD2, LOD4));

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(height);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(width);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RoundtopBollard other = (RoundtopBollard) obj;
			if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
				return false;
			if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
				return false;
			return true;
		}

	}

	public static class Chain extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private static final double DEFAULT_HEIGHT = 1;
		private final double height;
		private static final Integer DEFAULT_NO_CHAIN_SEGMENTS = 8;
		private static final double DEFAULT_BAR_WIDTH = 0.05f;

		private static final SimpleClosedShapeXZ BAR_SHAPE = new AxisAlignedRectangleXZ(-0.03, -0.03, 0.03, 0);


		public static boolean fits(TagSet tags) {
			return tags.contains("barrier", "chain");
		}


		public Chain(MapNode node, TagSet tags) {

			super(node);

			height = parseHeight(tags, DEFAULT_HEIGHT);

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON; //TODO: flexible ground states
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			/* render posts */

			VectorXYZ offset;

			if (!node.getConnectedWaySegments().isEmpty()) {
				offset = node.getConnectedWaySegments().get(0).getDirection().xyz(0).rotateY(PI / 2);
			} else {
				//Face north if no connecting ways segements
				offset = new VectorXYZ(1,0,0);
			}
			VectorXYZ post1Pos = getBase().add(offset);
			VectorXYZ post2Pos = getBase().add(offset.rotateY(PI));
			double distanceBetweenPosts = offset.length() * 2;

			target.drawColumn(STEEL, null, post1Pos,
					height, DEFAULT_BAR_WIDTH, DEFAULT_BAR_WIDTH, false, true);

			target.drawColumn(STEEL, null, post2Pos,
					height, DEFAULT_BAR_WIDTH, DEFAULT_BAR_WIDTH, false, true);


			/* render chain */

			List<VectorXYZ> chainPath = new ArrayList<VectorXYZ>();

			for(float i = 0f; i <= distanceBetweenPosts + 0.1; i += distanceBetweenPosts/DEFAULT_NO_CHAIN_SEGMENTS){
				//approximation of chain sag
				double a = 5f / 2f;
				double sag = a * cosh((i-(distanceBetweenPosts/2))/a)  -  a * cosh(distanceBetweenPosts/(2 * a));
				double pointDrop = height + sag;
				chainPath.add(post2Pos.add(offset.normalize().mult(i)).addY(pointDrop));
			}

			target.drawExtrudedShape(STEEL, BAR_SHAPE, chainPath, nCopies(DEFAULT_NO_CHAIN_SEGMENTS + 1,
					Y_UNIT), null, EnumSet.of(START_CAP, END_CAP));

		}

	}

	public static class DenseShrubbery extends AbstractAreaWorldObject implements ProceduralWorldObject {

		/** the shrubbery:shape=* value. Only "box" is supported at the moment. */
		private static enum ShrubberyShape { BOX }

		private final ShrubberyShape shape = ShrubberyShape.BOX;
		private final double height;

		public DenseShrubbery(MapArea area) {
			super(area);
			this.height = parseHeight(area.getTags(), 0.7);
		}

		@Override
		public void buildMeshesAndModels(Target target) {
			switch (shape) {
			case BOX:
				target.setCurrentLodRange(LOD2, LOD4);
				List<TriangleXYZ> topTriangles = getTriangulation().stream()
						.map(t -> t.shift(new VectorXYZ(0, height, 0)))
						.toList();
				target.drawTriangles(HEDGE, topTriangles, triangleTexCoordLists(topTriangles, HEDGE, GLOBAL_X_Z));
				for (PolygonXYZ ring : getOutlinePolygon().rings()) {
					List<VectorXYZ> outerStrip = createTriangleStripBetween(
							addYList(ring.vertices(), height), ring.vertices());
					target.drawTriangleStrip(HEDGE, outerStrip, texCoordLists(outerStrip, HEDGE, STRIP_WALL));
				}
				break;
			}
		}

	}

}
