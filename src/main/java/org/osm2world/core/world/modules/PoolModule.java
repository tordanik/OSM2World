package org.osm2world.core.world.modules;

import static java.awt.Color.ORANGE;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.parseColor;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds swimming pools and water parks to the world
 */
public class PoolModule extends AbstractModule {

	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("amenity", "swimming_pool")
				|| area.getTags().contains("leisure", "swimming_pool")) {
			area.addRepresentation(new Pool(area));
		}
	}

	@Override
	protected void applyToWaySegment(MapWaySegment segment) {

		if (segment.getTags().contains("attraction", "water_slide")) {

			List<MapWaySegment> segments = segment.getWay().getWaySegments();

			// WaterSlide renders the entire way at once, so only add it to the first way segment
			if (segment.equals(segments.get(0))) {

				segment.addRepresentation(new WaterSlide(segment, segments));

			}

		}

	}

	public static class Pool extends AbstractAreaWorldObject implements TerrainBoundaryWorldObject {

		public Pool(MapArea area) {
			super(area);
		}

		@Override
		public void renderTo(Target target) {

			/* render water */

			Collection<TriangleXYZ> triangles = getTriangulation();

			target.drawTriangles(PURIFIED_WATER, triangles,
					triangleTexCoordLists(triangles, PURIFIED_WATER, GLOBAL_X_Z));

			/* draw a small area around the pool */

			double width=1;
			double height=0.1;

			ShapeXZ wallShape = new PolylineXZ(
					new VectorXZ(+width/2, 0),
					new VectorXZ(+width/2, height),
					new VectorXZ(-width/2, height),
					new VectorXZ(-width/2, 0)
			);

			List<VectorXYZ> path = getOutlinePolygon().getVertexLoop();

			target.drawExtrudedShape(CONCRETE, wallShape, path,
					nCopies(path.size(), Y_UNIT), null, null, null);

		}
	}

	private static class WaterSlide implements WaySegmentWorldObject {

		private static final Color DEFAULT_COLOR = ORANGE;

		/** cross-section of the pipe */
		private static final ShapeXZ CROSS_SECTION_PIPE;

		/** cross-section of the water running down the pipe */
		private static final ShapeXZ CROSS_SECTION_WATER;

		/** preferred distance between supporting pillars in meters */
		private static final double PILLAR_DISTANCE = 5.5;

		static {

			double height = 0.8;
			double waterHeight = 0.05;

			CircleXZ pipeCircle = new CircleXZ(new VectorXZ(0, height), height);

			List<VectorXZ> crossSection = new ArrayList<VectorXZ>();

			for (VectorXZ v : pipeCircle.getVertexList()) {
				crossSection.add(new VectorXZ(v.x, v.z <= height ? v.z : height - (v.z - height)));
			}

			CROSS_SECTION_PIPE = new PolylineXZ(crossSection);

			//find the lowest pipe vertices above the intended water line

			VectorXZ lowestRightVertexAboveWater = null;

			for (VectorXZ v : pipeCircle.getVertexList()) {
				if (v.x > 0 && v.z >= waterHeight) {
					if (lowestRightVertexAboveWater == null || v.z < lowestRightVertexAboveWater.z) {
						lowestRightVertexAboveWater = v;
					}
				}
			}

			CROSS_SECTION_WATER = new LineSegmentXZ(lowestRightVertexAboveWater,
					new VectorXZ(-lowestRightVertexAboveWater.x, lowestRightVertexAboveWater.z));

		}

		private final MapWaySegment primarySegment;
		private final List<MapWaySegment> segments;

		private final List<EleConnector> eleConnectors;

		public WaterSlide(MapWaySegment primarySegment, List<MapWaySegment> segments) {

			this.primarySegment = primarySegment;
			this.segments = segments;

			eleConnectors = new ArrayList<EleConnector>();

			eleConnectors.add(new EleConnector(segments.get(0).getStartNode().getPos(),
					segments.get(0).getStartNode(), getGroundState()));

			for (MapWaySegment segment : segments) {
				eleConnectors.add(new EleConnector(segment.getEndNode().getPos(),
						segment.getEndNode(), getGroundState()));
			}

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
		}

		@Override
		public void renderTo(Target target) {

			//TODO parse material (e.g. for steel slides) and apply color to it

			Color color = parseColor(primarySegment.getTags().getValue("color"), CSS_COLORS);

			if (color == null) {
				color = DEFAULT_COLOR;
			}

			Material material = new ImmutableMaterial(Interpolation.SMOOTH, color);

			/* construct the baseline */

			List<VectorXYZ> baseline = new ArrayList<VectorXYZ>(eleConnectors.size());

			for (EleConnector eleConnector : eleConnectors) {
				baseline.add(eleConnector.getPosXYZ());
			}

			/* calculate plausible elevations by assuming constant incline */

			double totalLength = 0;

			for (MapWaySegment segment : segments) {
				totalLength += segment.getLineSegment().getLength();
			}

			double height = parseHeight(primarySegment.getTags(), (float)totalLength * 0.1f);

			List<VectorXYZ> path = new ArrayList<VectorXYZ>(baseline.size());

			VectorXYZ previousVector = null;

			for (VectorXYZ v : baseline) {

				VectorXYZ newPathVector;

				if (previousVector == null) {

					newPathVector = v.y(height);

				} else {

					newPathVector = v.y(previousVector.y
							- v.distanceToXZ(previousVector) / totalLength * height);

				}

				path.add(newPathVector);
				previousVector = newPathVector;

			}

			/* draw the pipe and water using extrusion */

			List<VectorXYZ> up = nCopies(path.size(), Y_UNIT);

			target.drawExtrudedShape(material, CROSS_SECTION_PIPE, path, up, null, null, null);
			target.drawExtrudedShape(WATER, CROSS_SECTION_WATER, path, up, null, null, null);

			/* draw supporting pillars */

			for (VectorXYZ v : equallyDistributePointsAlong(PILLAR_DISTANCE, false, path)) {

				double bottomHeight = -100;

				target.drawColumn(STEEL, null, v.y(bottomHeight),
						v.y - bottomHeight, 0.15, 0.15, false, false);

			}

		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return eleConnectors;
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			//no constraints (the waterslide elevation does not currently use the standard mechanism)
		}

		@Override
		public MapWaySegment getPrimaryMapElement() {
			return primarySegment;
		}

		@Override
		public VectorXZ getStartPosition() {
			return primarySegment.getStartNode().getPos();
		}

		@Override
		public VectorXZ getEndPosition() {
			return primarySegment.getEndNode().getPos();
		}

	}

}
