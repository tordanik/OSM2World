package org.osm2world.core.world.modules;

import static java.lang.Math.max;
import static java.lang.Math.toRadians;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.math.VectorXZ.listXYZ;
import static org.osm2world.core.math.algorithms.TriangulationUtil.triangulationXZtoXYZ;
import static org.osm2world.core.target.common.material.Material.Interpolation.SMOOTH;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD3;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD4;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordFunctions;
import static org.osm2world.core.util.ValueParseUtil.parseAngle;
import static org.osm2world.core.util.ValueParseUtil.parseUInt;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolylineShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.Geometry;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.ModelInstance;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * places bicycle parking facilities in the world
 */
public class BicycleParkingModule extends AbstractModule {

	@Override
	protected void applyToElement(MapElement element) {
		if (element.getTags().contains("amenity", "bicycle_parking")) {

			if (element.getTags().contains("bicycle_parking", "stands")) {
				if (element instanceof MapArea) {
					((MapArea) element).addRepresentation(new BicycleStandsArea((MapArea) element));
				} else if (element instanceof MapWaySegment) {
					if (element.equals(((MapWaySegment) element).getWay().getWaySegments().get(0))) {
						// renders the entire way at once, so only add it to the first way segment
						((MapWaySegment) element).addRepresentation(new BicycleStandsWay((MapWaySegment) element));

					}
				} else if (element instanceof MapNode) {
					if (element.getTags().containsKey("direction")) {
						((MapNode) element).addRepresentation(new BicycleStandsNode((MapNode) element));
					}
				}
			}

		}
	}

	public static abstract class BicycleStands implements TerrainBoundaryWorldObject {

		protected static final double DEFAULT_DISTANCE_BETWEEN_STANDS = 1.0;
		private static final double STAND_DEFAULT_LENGTH = 1.0;
		private static final double STAND_DEFAULT_HEIGHT = 0.7;

		private EleConnectorGroup eleConnectors;
		private List<EleConnector> standEleConnectors;

		/** the line along which stand centers should be placed */
		protected abstract PolylineShapeXZ lineThroughStandCenters();

		/**
		 * the area of the bicycle parking facility (if any).
		 * Used to avoid placing stands in holes of multipolygons and to draw the surface material below the stands.
		 */
		protected @Nullable PolygonShapeXZ area() {
			return null;
		}

		/** returns the number of stands, or null if it isn't known */
		protected @Nullable Integer numberOfStands() {
			Integer capacity = parseUInt(getPrimaryMapElement().getTags().getValue("capacity"));
			if (capacity != null && capacity > 0) {
				return (capacity + 1) / 2;
			} else {
				return null;
			}
		}

		@Override
		public EleConnectorGroup getEleConnectors() {

			if (eleConnectors == null) {

				eleConnectors = new EleConnectorGroup();
				standEleConnectors = new ArrayList<>();

				/* create connectors for the area */

				if (area() != null) {
					eleConnectors.addConnectorsForTriangulation(area().getTriangulation(), null, getGroundState());
				}

				/* create a connector for each stand */

				PolylineShapeXZ centerline = lineThroughStandCenters();

				double distanceBetweenStands = DEFAULT_DISTANCE_BETWEEN_STANDS;
				if (numberOfStands() != null) {
					distanceBetweenStands = centerline.getLength() / (numberOfStands() - 1);
				}

				List<VectorXYZ> standLocations = Integer.valueOf(1).equals(numberOfStands())
						? singletonList(centerline.pointAtOffset(centerline.getLength() / 2.0).xyz(0))
						: equallyDistributePointsAlong(distanceBetweenStands, true,listXYZ(centerline.vertices(), 0));

				for (VectorXYZ standLocation : standLocations) {
					if (area() == null || area().contains(standLocation.xz())) {
						EleConnector connector = new EleConnector(standLocation.xz(), null, getGroundState());
						standEleConnectors.add(connector);
						eleConnectors.add(connector);
					}
				}
			}

			return eleConnectors;

		}

		@Override
		public List<Mesh> buildMeshes() {

			@Nullable Material surfaceMaterial = getSurfaceMaterial();
			@Nullable PolygonShapeXZ area = area();

			if (area != null && surfaceMaterial != null) {

				/* render surface area */
				var builder = new TriangleGeometry.Builder(texCoordFunctions(surfaceMaterial, GLOBAL_X_Z), null, SMOOTH);
				builder.addTriangles(triangulationXZtoXYZ(area.getTriangulation(), eleConnectors::getPosXYZ));
				return List.of(new Mesh(builder.build(), surfaceMaterial));

			} else {
				return emptyList();
			}

		}

		@Override
		public List<ModelInstance> getSubModels() {

			/* place models for the stands */

			TagSet tags = getPrimaryMapElement().getTags();
			PolylineShapeXZ centerline = lineThroughStandCenters();

			double height = parseHeight(tags, STAND_DEFAULT_HEIGHT);
			double length = parseLength(tags, STAND_DEFAULT_LENGTH);
			BicycleStandModel model = new BicycleStandModel(height, length);

			Double direction = parseAngle(tags.getValue("direction"));
			if (direction != null) { direction = toRadians(direction); }

			List<ModelInstance> result = new ArrayList<>();

			for (EleConnector standConnector : standEleConnectors) {

				double localDirection;
				if (direction != null) {
					localDirection = direction;
				} else {
					LineSegmentXZ segment = centerline.closestSegment(standConnector.pos);
					localDirection = segment.getDirection().rightNormal().angle();
				}

				result.add(new ModelInstance(model, new InstanceParameters(standConnector.getPosXYZ(), localDirection)));

			}

			return result;

		}

		@Override
		public Collection<PolygonShapeXZ> getTerrainBoundariesXZ() {
			if (area() != null && getSurfaceMaterial() != null) {
				return singletonList(area());
			} else {
				return emptyList();
			}
		}

		private @Nullable Material getSurfaceMaterial() {
			return Materials.getSurfaceMaterial(getPrimaryMapElement().getTags().getValue("surface"));
		}

	}

	public static final class BicycleStandsArea extends BicycleStands implements AreaWorldObject {

		private final MapArea area;

		protected BicycleStandsArea(MapArea area) {
			this.area = area;
		}

		@Override
		public MapArea getPrimaryMapElement() {
			return area;
		}

		@Override
		protected PolylineShapeXZ lineThroughStandCenters() {

			SimplePolygonXZ bbox = area.getOuterPolygon().minimumRotatedBoundingBox();
			VectorXZ midpoint1, midpoint2;

			if (bbox.getVertex(2).distanceTo(bbox.getVertex(1)) > bbox.getVertex(1).distanceTo(bbox.getVertex(0))) {
				midpoint1 = bbox.getVertex(0).add(bbox.getVertex(1)).mult(0.5);
				midpoint2 = bbox.getVertex(2).add(bbox.getVertex(3)).mult(0.5);
			} else {
				midpoint1 = bbox.getVertex(1).add(bbox.getVertex(2)).mult(0.5);
				midpoint2 = bbox.getVertex(3).add(bbox.getVertex(0)).mult(0.5);
			}

			return new LineSegmentXZ(
					midpoint1.add(midpoint2.subtract(midpoint1).normalize().mult(DEFAULT_DISTANCE_BETWEEN_STANDS / 2)),
					midpoint2.add(midpoint1.subtract(midpoint2).normalize().mult(DEFAULT_DISTANCE_BETWEEN_STANDS / 2)));

		}

		@Override
		protected PolygonShapeXZ area() {
			return area.getPolygon();
		}

	}

	public static final class BicycleStandsWay extends BicycleStands implements WaySegmentWorldObject {

		private final MapWay way;
		private final MapWaySegment waySegment;

		protected BicycleStandsWay(MapWaySegment segment) {
			this.way = segment.getWay();
			this.waySegment = segment;
		}

		@Override
		public MapWaySegment getPrimaryMapElement() {
			return waySegment;
		}

		@Override
		protected PolylineShapeXZ lineThroughStandCenters() {
			return way.getPolylineXZ();
		}

	}

	public static final class BicycleStandsNode extends BicycleStands implements NodeWorldObject {

		private final MapNode node;
		private final double direction;

		public BicycleStandsNode(MapNode node) {
			this.node = node;
			this.direction = parseDirection(node.getTags(), 0);
		}

		@Override
		public MapNode getPrimaryMapElement() {
			return node;
		}

		@Override
		protected PolylineShapeXZ lineThroughStandCenters() {
			int numberOfStands = numberOfStands() == null ? 2 : numberOfStands();
			double length = max(0.1, (numberOfStands - 1) * DEFAULT_DISTANCE_BETWEEN_STANDS);
			VectorXZ toEnd = VectorXZ.fromAngle(direction).rightNormal().mult(length / 2);
			return new LineSegmentXZ(node.getPos().subtract(toEnd), node.getPos().add(toEnd));
		}

	}

	private record BicycleStandModel(double height, double length) implements Model {

		private static final ShapeXZ STAND_SHAPE = new CircleXZ(NULL_VECTOR, 0.02f);

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			VectorXYZ toFront = VectorXZ.fromAngle(params.direction()).mult(length / 2).xyz(0);

			List<VectorXYZ> path = List.of(
					params.position().add(toFront),
					params.position().add(toFront).addY(height * 0.95),
					params.position().add(toFront.mult(0.95)).addY(height),
					params.position().add(toFront.invert().mult(0.95)).addY(height),
					params.position().add(toFront.invert()).addY(height * 0.95),
					params.position().add(toFront.invert()));

			List<VectorXYZ> upVectors = List.of(
					toFront.normalize(),
					toFront.normalize(),
					Y_UNIT,
					Y_UNIT,
					toFront.invert().normalize(),
					toFront.invert().normalize());

			Geometry geom = new ExtrusionGeometry(STAND_SHAPE, path, upVectors, null, null, null,
					STEEL.getTextureDimensions());

			return List.of(new Mesh(geom, STEEL, LOD3, LOD4));

		}

	}

}
