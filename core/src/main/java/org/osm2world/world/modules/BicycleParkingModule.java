package org.osm2world.world.modules;

import static java.lang.Math.max;
import static java.lang.Math.toRadians;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.VectorXZ.listXYZ;
import static org.osm2world.math.algorithms.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulationXZtoXYZ;
import static org.osm2world.scene.color.Color.*;
import static org.osm2world.scene.material.DefaultMaterials.STEEL;
import static org.osm2world.scene.material.Material.Interpolation.SMOOTH;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD3;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD4;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordFunctions;
import static org.osm2world.util.ValueParseUtil.parseAngle;
import static org.osm2world.util.ValueParseUtil.parseUInt;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.DefaultMaterials;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.mesh.*;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.scene.model.Models;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.NodeWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * places bicycle parking facilities in the world
 */
public class BicycleParkingModule extends AbstractModule {

	private static final List<Color> BIKE_COLORS = List.of(
			BLACK, BLACK, BLUE, RED, LIGHT_GRAY, WHITE, GREEN, YELLOW, ORANGE, PINK);

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

	public abstract class BicycleStands implements WorldObject {

		protected static final double DEFAULT_DISTANCE_BETWEEN_STANDS = 1.0;
		private static final double STAND_DEFAULT_LENGTH = 1.0;
		private static final double STAND_DEFAULT_HEIGHT = 0.7;

		private EleConnectorGroup eleConnectors;
		private List<EleConnector> standEleConnectors;

		/** the line along which stand centers should be placed */
		protected abstract PolylineShapeXZ lineThroughStandCenters();

		/**
		 * the footprint area of the bicycle parking facility (if any).
		 * Used to avoid placing stands in holes of multipolygons and to draw the surface material below the stands.
		 */
		protected @Nullable Collection<PolygonShapeXZ> area() {
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

				if (area() != null && !area().isEmpty()) {
					eleConnectors.addConnectorsForTriangulation(triangulate(area()), null, getGroundState());
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
					// no longer check for overlaps: would prevent stands without surface=* even on empty terrain, and any stands on sidewalks etc.
					// if (area() == null || area().stream().anyMatch(it -> it.contains(standLocation.xz()))) {
					EleConnector connector = new EleConnector(standLocation.xz(), null, getGroundState());
					standEleConnectors.add(connector);
					eleConnectors.add(connector);
				}
			}

			return eleConnectors;

		}

		@Override
		public List<Mesh> buildMeshes() {

			@Nullable Material surfaceMaterial = getSurfaceMaterial();
			@Nullable Collection<PolygonShapeXZ> area = area();

			if (area != null && !area.isEmpty() && surfaceMaterial != null) {

				/* render surface area */
				var builder = new TriangleGeometry.Builder(texCoordFunctions(surfaceMaterial, GLOBAL_X_Z), null, SMOOTH);
				builder.addTriangles(triangulationXZtoXYZ(triangulate(area), eleConnectors::getPosXYZ));
				return List.of(new Mesh(builder.build(), surfaceMaterial));

			} else {
				return emptyList();
			}

		}

		@Override
		public List<ModelInstance> getSubModels() {

			double bikeDensity = config.getDouble("parkedVehicleDensity", 1.0); // 0.3);
			var random = new Random(getPrimaryMapElement().getElementWithId().getId());

			/* place models for the stands */

			TagSet tags = getPrimaryMapElement().getTags();
			PolylineShapeXZ centerline = lineThroughStandCenters();

			double height = parseHeight(tags, STAND_DEFAULT_HEIGHT);
			double length = parseLength(tags, STAND_DEFAULT_LENGTH);
			BicycleStandModel model = new BicycleStandModel(height, length, STEEL.get(config));

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

				// maybe add a bike model next to it

				if (random.nextDouble() > bikeDensity) continue;

				Model bikeModel = Models.getModel("BIKE", random);
				Color bikeColor = BIKE_COLORS.get(random.nextInt(BIKE_COLORS.size()));

				double bikeDirection = random.nextBoolean()
						? localDirection
						: Angle.ofRadians(localDirection).plus(Angle.ofDegrees(180)).radians;

				VectorXYZ bikePos = standConnector.getPosXYZ()
						.add(VectorXZ.fromAngle(bikeDirection).mult(-0.1 + random.nextDouble(0.2)))
						.add(VectorXZ.fromAngle(localDirection).rightNormal().mult(random.nextBoolean() ? 0.2 : -0.2));

				if (bikeModel != null) {
					result.add(new ModelInstance(bikeModel,
							new InstanceParameters(bikePos, bikeDirection, bikeColor, new LODRange(LOD3, LOD4))));
				}

			}

			return result;

		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			if (getSurfaceMaterial() != null && getOutlinePolygonXZ() != null) {
				return List.of(getOutlinePolygonXZ());
			} else {
				return emptyList();
			}
		}

		private @Nullable Material getSurfaceMaterial() {
			return config.mapStyle().resolveMaterial(
					DefaultMaterials.getSurfaceMaterial(getPrimaryMapElement().getTags().getValue("surface"), config));
		}

		@Override
		public int getOverlapPriority() {
			return 30;
		}
	}

	public final class BicycleStandsArea extends BicycleStands implements AreaWorldObject {

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
		protected Collection<PolygonShapeXZ> area() {
			return getGroundFootprint();
		}

	}

	public final class BicycleStandsWay extends BicycleStands implements WaySegmentWorldObject {

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

	public final class BicycleStandsNode extends BicycleStands implements NodeWorldObject {

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

	private record BicycleStandModel(double height, double length, Material material) implements Model {

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
					material.textureDimensions());

			return List.of(new Mesh(geom, material, LOD3, LOD4));

		}

	}

}
