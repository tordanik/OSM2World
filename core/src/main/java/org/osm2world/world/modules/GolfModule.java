package org.osm2world.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulationXZtoXYZ;
import static org.osm2world.scene.color.Color.YELLOW;
import static org.osm2world.scene.material.DefaultMaterials.*;
import static org.osm2world.scene.mesh.LevelOfDetail.*;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.triangulateAreaBetween;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.JTSBufferUtil;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.StreetFurnitureModule.Flagpole.StripedFlag;
import org.osm2world.world.modules.SurfaceAreaModule.SurfaceArea;
import org.osm2world.world.modules.common.AbstractModule;

import com.google.common.collect.Streams;

/**
 * adds golf courses to the map
 */
public class GolfModule extends AbstractModule {

	private static final int HOLE_CIRCLE_VERTICES = 8;
	private static final double HOLE_RADIUS = 0.108 / 2;
	private static final double HOLE_DEPTH = 0.102;

	@Override
	public void applyToArea(MapArea area) {

		if (!area.getTags().containsKey("golf")) return;

		if (area.getTags().contains("golf", "tee")) {
			area.addRepresentation(new Tee(area));
		} else if (area.getTags().contains("golf", "fairway")) {
			area.addRepresentation(new Fairway(area));
		} else if (area.getTags().contains("golf", "bunker")) {
			area.addRepresentation(new Bunker(area));
		} else if (area.getTags().contains("golf", "green")) {
			area.addRepresentation(new Green(area));
		}

	}

	private class Tee extends SurfaceArea {

		private Tee(MapArea area) {

			super(area, area.getTags().containsKey("surface")
					? area.getTags().getValue("surface")
					: "grass", config);

		}

	}

	private class Fairway extends SurfaceArea {

		private Fairway(MapArea area) {

			super(area, area.getTags().containsKey("surface")
					? area.getTags().getValue("surface")
					: "grass", config);

		}

	}

	private class Bunker extends AbstractAreaWorldObject implements ProceduralWorldObject {

		public Bunker(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			/* triangulate the bunker's area normally at low LOD */

			target.setCurrentLodRange(LOD0, LOD1);

			List<TriangleXYZ> basicTriangulation = getTriangulation();
			target.drawTriangles(SAND.get(config), basicTriangulation,
					triangleTexCoordLists(basicTriangulation, SAND.get(config), GLOBAL_X_Z));

			/* draw the bunker as a depression by shrinking the outline polygon and lowering it at each step.
			 *
			 * The first step gets special handling and is primarily intended for bunkers in uneven terrain.
			 * It involves an almost vertical drop towards the lowest point of the bunker outline
			 * that is textured with ground, not sand. */

			target.setCurrentLodRange(LOD2, LOD4);

			List<TriangleXYZ> resultingTriangulation = new ArrayList<>();

			double[] dropSteps = {-0.03, -0.07, -0.05, -0.02};

			List<PolygonWithHolesXZ> currentPolys = asList(area.getPolygon());
			double currentEle = Streams.stream(getEleConnectors()).mapToDouble(c -> c.getPosXYZ().y).min().getAsDouble();

			for (int i = 0; i < dropSteps.length; i++) {

				List<PolygonWithHolesXZ> newPolys = new ArrayList<>();

				double oldEle = currentEle;
				double newEle = currentEle + dropSteps[i];

				for (PolygonWithHolesXZ large : currentPolys) {

					List<PolygonWithHolesXZ> small = JTSBufferUtil.bufferPolygon(large, -0.5);

					Function<VectorXZ, VectorXYZ> xyzFunction = v -> {

						if (getEleConnectors().getConnector(v) != null) {
							return getEleConnectors().getPosXYZ(v);
						} else if (large.vertices().contains(v)
								|| large.getHoles().stream().anyMatch(h -> h.vertices().contains(v))) {
							return v.xyz(oldEle);
						} else {
							return v.xyz(newEle);
						}

					};

					Collection<TriangleXZ> triangulationXZ = triangulateAreaBetween(large, small);

					triangulationXZ.stream()
							.map(t -> t.xyz(xyzFunction))
							.forEach(resultingTriangulation::add);

					newPolys.addAll(small);

				}

				currentPolys = newPolys;
				currentEle = newEle;

			}

			/* fill in the rest of the area (if any) with a flat surface at the lowest elevation */

			List<TriangleXZ> triangulationXZ = currentPolys.stream().flatMap(p -> triangulate(p).stream()).collect(toList());

			if (!triangulationXZ.isEmpty()) {
				double ele = currentEle;
				triangulationXZ.stream().map(t -> t.xyz(ele)).forEach(resultingTriangulation::add);
			}

			/* render everything with a sand texture */

			target.drawTriangles(SAND.get(config), resultingTriangulation,
					triangleTexCoordLists(resultingTriangulation, SAND.get(config), GLOBAL_X_Z));

		}

	}

	private class Green extends AbstractAreaWorldObject implements ProceduralWorldObject {

		private final VectorXZ pinPosition;
		private final SimplePolygonXZ pinHoleLoop;
		private final EleConnectorGroup pinConnectors;

		private Green(MapArea area) {

			super(area);

			/* check whether a pin has been explicitly mapped */

			VectorXZ explicitPinPosition = null;

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {

				MapElement other = overlap.getOther(area);

				if (other.getTags().contains("golf","pin")
						&& other instanceof MapNode) {

					explicitPinPosition = ((MapNode)other).getPos();

					break;

				}

			}

			/* place an implicit pin if none has been mapped */

			if (explicitPinPosition != null) {

				pinPosition = explicitPinPosition;

			} else {

				pinPosition = area.getOuterPolygon().getCenter();

			}

			/* create circle around the hole */

			pinHoleLoop = new SimplePolygonXZ(new CircleXZ(pinPosition, HOLE_RADIUS)
					.vertices(HOLE_CIRCLE_VERTICES))
					.makeCounterclockwise();

			pinConnectors = new EleConnectorGroup();
			pinConnectors.addConnectorsFor(pinHoleLoop.getVertexCollection(), area, GroundState.ON);

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public EleConnectorGroup getEleConnectors() {

			EleConnectorGroup eleConnectors = super.getEleConnectors();

			if (pinConnectors != null) {
				eleConnectors.addAll(pinConnectors);
			}

			return eleConnectors;

		}

		@Override
		public void buildMeshesAndModels(Target target) {

			/* render green surface */

			String surfaceValue = area.getTags().getValue("surface");

			Material material = GRASS.get(config);

			if (surfaceValue != null && !"grass".equals(surfaceValue)) {
				material = getSurfaceMaterial(surfaceValue, material, config);
			}

			List<TriangleXZ> trianglesXZ = getGreenTriangulation();
			List<TriangleXYZ> triangles = triangulationXZtoXYZ(trianglesXZ, getEleConnectors()::getPosXYZ);

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles , material, GLOBAL_X_Z));

			/* render pin */

			target.setCurrentLodRange(LOD3, LOD4);

			PolygonXYZ upperHoleRing = pinConnectors.getPosXYZ(pinHoleLoop);

			drawPin(target, pinPosition, upperHoleRing.vertices());

		}

		private List<TriangleXZ> getGreenTriangulation() {
			List<SimplePolygonXZ> holes = new ArrayList<>(area.getPolygon().getHoles());
			holes.add(pinHoleLoop);
			return TriangulationUtil.triangulate(area.getPolygon().getOuter(), holes, emptyList());
		}

		private void drawPin(Target target, VectorXZ pos, List<VectorXYZ> upperHoleRing) {

			double minHoleEle = min(upperHoleRing, comparingDouble(v -> v.y)).y;

			double holeBottomEle = minHoleEle - HOLE_DEPTH;

			/* draw hole */

			List<VectorXYZ> lowerHoleRing = upperHoleRing.stream().map(v -> v.y(holeBottomEle)).collect(toList());

			List<VectorXYZ> vs = createTriangleStripBetween(lowerHoleRing, upperHoleRing);

			Material groundMaterial = EARTH.get(config).makeSmooth();

			target.drawTriangleStrip(groundMaterial, vs,
					texCoordLists(vs, groundMaterial, STRIP_WALL));

			target.drawConvexPolygon(groundMaterial, lowerHoleRing,
					texCoordLists(lowerHoleRing, groundMaterial, GLOBAL_X_Z));

			/* draw pole and flag */

			Material flagPoleMaterial = PLASTIC.get(config).withColor(new Color(184, 184, 184));
			target.drawColumn(flagPoleMaterial, null,
					pos.xyz(holeBottomEle), 1.5, 0.007, 0.007, false, true);

			var flag = new StripedFlag(3.0 / 4, List.of(YELLOW), true);
			flag.renderFlag(target, pos.xyz(holeBottomEle + 1.5), 0.3, 0.4, config);

		}

	}

}
