package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MAX;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createLineBetween;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.*;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleParseUtil;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.NetworkAreaWorldObject;

/**
 * adds water bodies, streams, rivers and fountains to the world
 */
public class WaterModule extends ConfigurableWorldModule {

	private static final Tag WATER_TAG = new Tag("natural", "water");
	private static final Tag RIVERBANK_TAG = new Tag("waterway", "riverbank");

	private static final Map<String, Double> WATERWAY_WIDTHS;

	static {
		WATERWAY_WIDTHS = new HashMap<>();
		WATERWAY_WIDTHS.put("river", 3.0);
		WATERWAY_WIDTHS.put("stream", 0.5);
		WATERWAY_WIDTHS.put("canal", 2.0);
		WATERWAY_WIDTHS.put("ditch", 1.0);
		WATERWAY_WIDTHS.put("drain", 1.0);
	}

	@Override
	public void applyTo(MapData mapData) {

		for (MapWaySegment line : mapData.getMapWaySegments()) {
			for (String value : WATERWAY_WIDTHS.keySet()) {
				if (line.getTags().contains("waterway", value)) {

					boolean lineInsideWaterArea = false;
					boolean lineStartInsideWaterArea = false;
					boolean lineEndInsideWaterArea = false;

					for (MapOverlap<?, ?> overlap : line.getOverlaps()) {
						MapElement other = overlap.getOther(line);
						if (other instanceof MapArea) {
							if (overlap.type == MapOverlapType.CONTAIN) {
								lineInsideWaterArea = true;
							} else if (overlap.type == MapOverlapType.INTERSECT) {
								lineStartInsideWaterArea |= ((MapArea)other).getPolygon().contains(line.getStartNode().getPos());
								lineEndInsideWaterArea |= ((MapArea)other).getPolygon().contains(line.getEndNode().getPos());
							}
						}
					}

					if (!lineInsideWaterArea && (!lineStartInsideWaterArea || !lineEndInsideWaterArea)) {
						line.addRepresentation(new Waterway(line));
					}
				}
			}
		}

		for (MapNode node : mapData.getMapNodes()) {
			if (getConnectedNetworkSegments(node, Waterway.class, null).size() > 2) {
				node.addRepresentation(new RiverJunction(node));
			}
		}

		for (MapArea area : mapData.getMapAreas()) {
			if (area.getTags().contains(WATER_TAG)
					|| area.getTags().contains(RIVERBANK_TAG)) {
				area.addRepresentation(new Water(area));
			}
			if (area.getTags().contains("amenity", "fountain")) {
				area.addRepresentation(new AreaFountain(area));
			}
		}

	}

	public static class Waterway extends AbstractNetworkWaySegmentWorldObject
			implements TerrainBoundaryWorldObject, LegacyWorldObject {

		public Waterway(MapWaySegment line) {
			super(line);
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {

			super.defineEleConstraints(enforcer);

			/* enforce downhill flow */

			if (!segment.getTags().containsKey("incline")) {
				enforcer.requireIncline(MAX, 0, getCenterlineEleConnectors());
			}

		}

		public double getWidth() {
			return WorldModuleParseUtil.parseWidth(segment.getTags(),
					WATERWAY_WIDTHS.get(segment.getTags().getValue("waterway")));
		}

		@Override
		public PolygonXYZ getOutlinePolygon() {
			if (isContainedWithinRiverbank()) {
				return null;
			} else {
				return super.getOutlinePolygon();
			}
		}

		@Override
		public SimplePolygonXZ getOutlinePolygonXZ() {
			if (isContainedWithinRiverbank()) {
				return null;
			} else {
				return super.getOutlinePolygonXZ();
			}
		}

		@Override
		public void renderTo(Target target) {

			//note: simply "extending" a river cannot work - unlike streets -
			//      because there can be islands within the riverbank polygon.
			//      That's why rivers will be *replaced* with Water areas instead.

			/* only draw the river if it doesn't have a riverbank */

			//TODO: handle case where a river is completely within riverbanks, but not a *single* riverbank

			if (! isContainedWithinRiverbank()) {

				List<VectorXYZ> leftOutline = getOutline(false);
				List<VectorXYZ> rightOutline = getOutline(true);

				List<VectorXYZ> leftWaterBorder = createLineBetween(
						leftOutline, rightOutline, 0.05f);
				List<VectorXYZ> rightWaterBorder = createLineBetween(
						leftOutline, rightOutline, 0.95f);

				modifyLineHeight(leftWaterBorder, -0.2f);
				modifyLineHeight(rightWaterBorder, -0.2f);

				List<VectorXYZ> leftGround = createLineBetween(
						leftOutline, rightOutline, 0.35f);
				List<VectorXYZ> rightGround = createLineBetween(
						leftOutline, rightOutline, 0.65f);

				modifyLineHeight(leftGround, -1);
				modifyLineHeight(rightGround, -1);

				/* render ground */

				List<List<VectorXYZ>> strips = asList(
					createTriangleStripBetween(
							leftOutline, leftWaterBorder),
					createTriangleStripBetween(
							leftWaterBorder, leftGround),
					createTriangleStripBetween(
							leftGround, rightGround),
					createTriangleStripBetween(
							rightGround, rightWaterBorder),
					createTriangleStripBetween(
							rightWaterBorder, rightOutline)
				);

				for (List<VectorXYZ> strip : strips) {
					target.drawTriangleStrip(TERRAIN_DEFAULT, strip,
						texCoordLists(strip, TERRAIN_DEFAULT, GLOBAL_X_Z));
				}

				/* render water */

				List<VectorXYZ> vs = createTriangleStripBetween(
						leftWaterBorder, rightWaterBorder);

				target.drawTriangleStrip(WATER, vs,
						texCoordLists(vs, WATER, GLOBAL_X_Z));

			}

		}

		private boolean isContainedWithinRiverbank() {
			boolean containedWithinRiverbank = false;

			for (MapOverlap<?,?> overlap : segment.getOverlaps()) {
				if (overlap.getOther(segment) instanceof MapArea) {
					MapArea area = (MapArea)overlap.getOther(segment);
					if (area.getPrimaryRepresentation() instanceof Water &&
							area.getPolygon().contains(segment.getLineSegment())) {
						containedWithinRiverbank = true;
						break;
					}
				}
			}
			return containedWithinRiverbank;
		}

		private static void modifyLineHeight(List<VectorXYZ> leftWaterBorder, float yMod) {
			for (int i = 0; i < leftWaterBorder.size(); i++) {
				VectorXYZ v = leftWaterBorder.get(i);
				leftWaterBorder.set(i, v.y(v.y+yMod));
			}
		}

	}

	public static class RiverJunction extends JunctionNodeWorldObject<Waterway>
			implements TerrainBoundaryWorldObject, LegacyWorldObject {

		public RiverJunction(MapNode node) {
			super(node, Waterway.class);
		}

		@Override
		public void renderTo(Target target) {

			//TODO: check whether it's within a riverbank (as with Waterway)

			List<VectorXYZ> vertices = getOutlinePolygon().verticesNoDup();

			target.drawConvexPolygon(WATER, vertices,
					texCoordLists(vertices, WATER, GLOBAL_X_Z));

			//TODO: only cover with water to 0.95 * distance to center; add land below

		}

	}

	public static class Water extends NetworkAreaWorldObject
			implements TerrainBoundaryWorldObject, LegacyWorldObject {

		//TODO: only cover with water to 0.95 * distance to center; add land below.
		// possible algorithm: for each node of the outer polygon, check whether it
		// connects to another water surface. If it doesn't move it inwards,
		// where "inwards" is calculated based on the two adjacent polygon segments.

		public Water(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			enforcer.requireSameEle(getEleConnectors());
		}

		@Override
		public void renderTo(Target target) {
			List<TriangleXYZ> triangles = getTriangulation();
			target.drawTriangles(WATER, triangles,
					triangleTexCoordLists(triangles, WATER, GLOBAL_X_Z));
		}

	}

	public static class AreaFountain extends AbstractAreaWorldObject
			implements TerrainBoundaryWorldObject, LegacyWorldObject {

		public AreaFountain(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target target) {

			/* render water */

			List<TriangleXYZ> triangles = getTriangulation();
			target.drawTriangles(PURIFIED_WATER, triangles,
					triangleTexCoordLists(triangles, PURIFIED_WATER, GLOBAL_X_Z));

			/* render walls */

			double width=0.1;
			double height=0.5;

			for (PolygonXYZ ring : getOutlinePolygon().rings()) {

				ShapeXZ wallShape = new PolylineXZ(
						new VectorXZ(+width / 2, 0),
						new VectorXZ(+width / 2, height),
						new VectorXZ(-width / 2, height),
						new VectorXZ(-width / 2, 0)
				);

				target.drawExtrudedShape(CONCRETE, wallShape, ring.vertices(),
						nCopies(ring.vertices().size(), Y_UNIT), null, null, null);

			}
			
		}

	}
}
