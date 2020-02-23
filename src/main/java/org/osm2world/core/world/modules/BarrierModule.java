package org.osm2world.core.world.modules;

import static java.lang.Math.ceil;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseColor;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.ExtrudeOption.*;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

import com.google.common.collect.Lists;

/**
 * adds barriers to the world
 */
public class BarrierModule extends AbstractModule {

	@Override
	protected void applyToWaySegment(MapWaySegment line) {

		TagGroup tags = line.getTags();
		if (!tags.containsKey("barrier")) return; //fast exit for common case

		if (Wall.fits(tags)) {
			line.addRepresentation(new Wall(line));
		} else if (CityWall.fits(tags)) {
			line.addRepresentation(new CityWall(line));
		} else if (Hedge.fits(tags)) {
			line.addRepresentation(new Hedge(line));
		} else if (Railing.fits(tags)) {
			line.addRepresentation(new Railing(line));
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
		}

	}

	@Override
	protected void applyToNode(MapNode node) {

		TagGroup tags = node.getTags();
		if (!tags.containsKey("barrier")) return; //fast exit for common case

		if (Bollard.fits(tags)) {
			node.addRepresentation(new Bollard(node, tags));
		}


	}

	private static abstract class LinearBarrier extends AbstractNetworkWaySegmentWorldObject {

		protected final float height;
		protected final float width;

		public LinearBarrier(MapWaySegment waySegment,
				float defaultHeight, float defaultWidth) {

			super(waySegment);

			height = parseHeight(waySegment.getTags(), defaultHeight);
			width = parseWidth(waySegment.getTags(), defaultWidth);

		}

		@Override
		public float getWidth() {
			return width;
		}

	}

	/**
	 * superclass for linear barriers that are a simple colored or textured "wall"
	 * (use width and height to create vertical sides and a flat top)
	 */
	private static abstract class ColoredWall extends LinearBarrier {

		private final Material material;

		public ColoredWall(Material material, MapWaySegment segment,
				float defaultHeight, float defaultWidth) {
			super(segment, defaultHeight, defaultWidth);
			this.material = material;
		}

		@Override
		public void renderTo(Target target) {

			List<VectorXYZ> leftBottomOutline = getOutline(false);
			List<VectorXYZ> leftTopOutline = addYList(leftBottomOutline, height);

			List<VectorXYZ> rightBottomOutline = getOutline(true);
			List<VectorXYZ> rightTopOutline = addYList(rightBottomOutline, height);

			/* close the wall at the end if necessary */

			if (getConnectedNetworkSegments(segment.getStartNode(), this.getClass(), s -> s != this).isEmpty()) {
				List<VectorXYZ> startCapVs = asList(
						leftTopOutline.get(0),
						leftBottomOutline.get(0),
						rightTopOutline.get(0),
						rightBottomOutline.get(0));
				target.drawTriangleStrip(material, startCapVs, texCoordLists(startCapVs, material, STRIP_WALL));
			}

			if (getConnectedNetworkSegments(segment.getEndNode(), this.getClass(), s -> s != this).isEmpty()) {
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

			reverse(leftTopOutline);
			reverse(leftBottomOutline);

			List<VectorXYZ> leftVs = createTriangleStripBetween(leftTopOutline, leftBottomOutline);
			target.drawTriangleStrip(material, leftVs, texCoordLists(leftVs, material, STRIP_WALL));

			List<VectorXYZ> rightVs = createTriangleStripBetween(rightTopOutline, rightBottomOutline);
			target.drawTriangleStrip(material, rightVs, texCoordLists(rightVs, material, STRIP_WALL));

		}

	}

	private static class Wall extends ColoredWall {

		private final static Material DEFAULT_MATERIAL = Materials.CONCRETE;

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "wall");
		}

		private static Material getMaterial(TagGroup tags) {

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
					|| material.getTextureDataList().get(0).colorable;

			if (colorString != null && colorable) {
				material = material.withColor(parseColor(colorString, CSS_COLORS));
			}

			return material;

		}

		public Wall(MapWaySegment segment) {
			super(getMaterial(segment.getTags()), segment, 1f, 0.25f);
		}

	}

	private static class CityWall extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "city_wall");
		}
		public CityWall(MapWaySegment segment) {
			super(Wall.DEFAULT_MATERIAL, segment, 10, 2);
		}
	}

	private static class Hedge extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "hedge");
		}
		public Hedge(MapWaySegment segment) {
			super(Materials.HEDGE, segment, 1f, 0.5f);
		}
	}

	private static class Railing extends LinearBarrier {

		private static final SimpleClosedShapeXZ BAR_SHAPE =
				new AxisAlignedRectangleXZ(-0.5, -0.3, 0.5, 0);

		private static final SimpleClosedShapeXZ SQUARE =
				new AxisAlignedRectangleXZ(-0.5, -0.5, 0.5, 0.5);

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "fence")
					&& tags.contains("fence_type", "railing");
		}

		public Railing(MapWaySegment segment) {
			super(segment, 1f, 0.1f);
		}

		@Override
		public void renderTo(Target target) {

			Material material = METAL_FENCE;

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(2.4, true, getCenterline());

			/* draw top and bottom bars */

			target.drawExtrudedShape(material, BAR_SHAPE, addYList(polePositions, height),
					nCopies(polePositions.size(), Y_UNIT),
					nCopies(polePositions.size(), (double)width), null, null);

			target.drawExtrudedShape(material, BAR_SHAPE, addYList(polePositions, 0.15 * height),
					nCopies(polePositions.size(), Y_UNIT),
					nCopies(polePositions.size(), (double)width), null, null);

			/* draw poles and the smaller vertical bars */

			List<VectorXYZ> poleFacingDirections = nCopies(2, segment.getDirection().xyz(0));
			List<Double> poleScale = nCopies(2, (double)width);
			List<Double> smallPoleScale = nCopies(2, width * 0.2);

			for (VectorXYZ v : polePositions) {

				//draw pole
				target.drawExtrudedShape(material, SQUARE,
						asList(v, v.addY(0.99 * height)),
						poleFacingDirections, poleScale, null, null);

			}

			for (int i = 0; i + 1 < polePositions.size(); i++) {

				List<VectorXYZ> smallPolePositions = equallyDistributePointsAlong(
						0.12, true, polePositions.subList(i, i+2));

				for (int j = 1; j < smallPolePositions.size() - 1; j++) {

					VectorXYZ v = smallPolePositions.get(j);

					//draw small vertical bar
					target.drawExtrudedShape(material, SQUARE,
							asList(v.addY(0.14 * height), v.addY(0.99 * height)),
							poleFacingDirections, smallPoleScale, null, null);

				}

			}

		}

	}

	private static class ChainLinkFence extends LinearBarrier {

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "fence")
					&& (tags.contains("fence_type", "chain_link") || (tags.contains("fence_type", "metal")));
		}

		public ChainLinkFence(MapWaySegment segment) {
			super(segment, 1f, 0.02f);
		}

		@Override
		public void renderTo(Target target) {

			/* render fence */

			List<VectorXYZ> pointsWithEle = getCenterline();

			List<VectorXYZ> vsFence = createVerticalTriangleStrip(
					pointsWithEle, 0, height);
			List<List<VectorXZ>> texCoordListsFence = texCoordLists(
					vsFence, CHAIN_LINK_FENCE, STRIP_WALL);

			target.drawTriangleStrip(CHAIN_LINK_FENCE, vsFence, texCoordListsFence);

			List<VectorXYZ> pointsWithEleBack =
					new ArrayList<VectorXYZ>(pointsWithEle);
			Collections.reverse(pointsWithEleBack);

			List<VectorXYZ> vsFenceBack = createVerticalTriangleStrip(
					pointsWithEleBack, 0, height);
			List<List<VectorXZ>> texCoordListsFenceBack = texCoordLists(
					vsFenceBack, CHAIN_LINK_FENCE, STRIP_WALL);

			target.drawTriangleStrip(CHAIN_LINK_FENCE, vsFenceBack,
					texCoordListsFenceBack);

			/* render poles */

			//TODO connect the poles to the ground independently

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(
					2f, true, getCenterline());

			for (VectorXYZ base : polePositions) {

				target.drawColumn(Materials.METAL_FENCE_POST, null, base,
						height, width, width, false, true);

			}

		}
	}

	private static class PoleFence extends LinearBarrier {

		private Material material;
		protected float barWidth;
		protected float barGap;
		protected float barOffset;
		protected int bars;
		protected Material defaultFenceMaterial = Materials.WOOD;
		protected Material defaultPoleMaterial = Materials.WOOD;
		protected Material poleMaterial;

		public static boolean fits(TagGroup tags) {
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
		public void renderTo(Target target) {

			if (material == null) {
				material = defaultFenceMaterial;
				poleMaterial = defaultPoleMaterial;
			}

			List<VectorXYZ> baseline = getCenterline();

			/* render fence */
			for (int i = 0; i < bars; i++) {
				float barEndHeight = height - (i * barGap) - barOffset;
				float barStartHeight = barEndHeight - barWidth;

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
					2f, false, getCenterline());

			for (VectorXYZ base : polePositions) {

				target.drawColumn(poleMaterial, null, base,
						height, width, width, false, true);

			}

		}
	}

	private static class TrellisWorkFence extends LinearBarrier {

		private static final SimpleClosedShapeXZ PLANK_SHAPE;

		static {

			// create a semicircle shape

			List<VectorXZ> vertexLoop = new ArrayList<VectorXZ>();

			for (VectorXZ v : new CircleXZ(NULL_VECTOR, 0.05).getVertexList()) {
				if (v.x <= 0) {
					vertexLoop.add(v);
				}
			}

			if (!vertexLoop.get(0).equals(vertexLoop.get(vertexLoop.size() - 1))) {
				vertexLoop.add(vertexLoop.get(0));
			}

			PLANK_SHAPE = new SimplePolygonXZ(vertexLoop);

		}

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "fence")
					&& tags.contains("fence_type", "trellis_work");
		}

		public TrellisWorkFence(MapWaySegment segment) {
			super(segment, 0.7f, 0.1f);
		}

		@Override
		public void renderTo(Target target) {

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
						nCopies(2, Y_UNIT), null, null, EnumSet.of(START_CAP, END_CAP));

				target.drawExtrudedShape(WOOD, PLANK_SHAPE,
						asList(positions.get(leftIndex).addY(height),
								positions.get(rightIndex)),
						nCopies(2, Y_UNIT), null, null, EnumSet.of(START_CAP, END_CAP));

			}

		}

	}

	private static class CableBarrier extends PoleFence {

		public static boolean fits(TagGroup tags) {
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

	private static class HandRail extends PoleFence {

		public static boolean fits(TagGroup tags) {
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

	private static class Guardrail extends LinearBarrier {

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

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "guard_rail");
		}

		public Guardrail(MapWaySegment line) {
			super(line, DEFAULT_HEIGHT, 0.0001f);
		}

		@Override
		public void renderTo(Target target) {

			List<VectorXYZ> centerline = getCenterline();

			Material material = STEEL.makeSmooth();

			/* draw the rail itself */

			List<VectorXYZ> path = addYList(centerline, this.height - SHAPE_GERMAN_B_HEIGHT);

			//front
			target.drawExtrudedShape(material, SHAPE_GERMAN_B,
					path, nCopies(path.size(), Y_UNIT), null, null, null);

			//back
			target.drawExtrudedShape(material, new PolylineXZ(Lists.reverse(SHAPE_GERMAN_B.getVertexList())),
					path, nCopies(path.size(), Y_UNIT), null, null, null);

			/* add posts */

			List<VectorXYZ> polePositions = equallyDistributePointsAlong(
					METERS_BETWEEN_POLES, false, centerline);

			for (VectorXYZ base : polePositions) {

				VectorXZ railNormal = segment.getRightNormal();

				// extrude the pole

				List<VectorXYZ> polePath = asList(base,
						base.addY(height - SHAPE_GERMAN_B_HEIGHT*0.33));

				target.drawExtrudedShape(material, SHAPE_POST_DOUBLE_T, polePath,
						nCopies(polePath.size(), railNormal.xyz(0)), null, null, null);

				// extrude the bolts connecting the pole to the rail

				ShapeXZ boltShape = new CircleXZ(NULL_VECTOR, BOLT_RADIUS);

				VectorXYZ centerBetweenBolts = base.addY(height - SHAPE_GERMAN_B_HEIGHT*0.5);

				List<VectorXYZ> boltPositions = asList(
						centerBetweenBolts.add(segment.getDirection().mult(BOLT_OFFSET)),
						centerBetweenBolts.add(segment.getDirection().mult(-BOLT_OFFSET)));

				for (VectorXYZ boltPosition : boltPositions) {

					List<VectorXYZ> boltPath = asList(boltPosition,
							boltPosition.add(railNormal.mult(BOLT_DEPTH)));

					target.drawExtrudedShape(material, boltShape, boltPath,
							nCopies(boltPath.size(), Y_UNIT), null, null, EnumSet.of(END_CAP));

				}

			}

		}

	}

	private static class JerseyBarrier extends LinearBarrier {

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

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "jersey_barrier");
		}

		public JerseyBarrier(MapWaySegment line) {
			super(line, DEFAULT_HEIGHT, DEFAULT_WIDTH);
		}

		@Override
		public void renderTo(Target target) {

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
						nCopies(2, Y_UNIT), null, null, EnumSet.of(START_CAP, END_CAP));

			}

		}

	}

	private static class Bollard extends NoOutlineNodeWorldObject {

		private static final float DEFAULT_HEIGHT = 1;

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "bollard");
		}

		private final float height;

		public Bollard(MapNode node, TagGroup tags) {

			super(node);

			height = parseHeight(tags, DEFAULT_HEIGHT);

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON; //TODO: flexible ground states
		}

		@Override
		public void renderTo(Target target) {
			target.drawColumn(Materials.CONCRETE,
					null, getBase(), height, 0.15f, 0.15f, false, true);
		}

	}

	//TODO: bollard_count or similar tag exists? create "Bollards" rep.
	//just as lift gates etc, this should use the line.getRightNormal and the road width

}