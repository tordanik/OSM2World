package org.osm2world.core.world.modules;

import static java.awt.Color.YELLOW;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.core.target.common.material.Materials.SAND;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.JTSBufferUtil;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.StreetFurnitureModule.Flagpole.StripedFlag;
import org.osm2world.core.world.modules.SurfaceAreaModule.SurfaceArea;
import org.osm2world.core.world.modules.common.AbstractModule;

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

	private static class Tee extends SurfaceArea {

		private Tee(MapArea area) {

			super(area, area.getTags().containsKey("surface")
					? area.getTags().getValue("surface")
					: "grass");

		}

	}

	private static class Fairway extends SurfaceArea {

		private Fairway(MapArea area) {

			super(area, area.getTags().containsKey("surface")
					? area.getTags().getValue("surface")
					: "grass");

		}

	}

	private static class Bunker extends AbstractAreaWorldObject implements TerrainBoundaryWorldObject {

		public Bunker(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target target) {

			/* draw the bunker as a depression by shrinking the outline polygon and lowering it at each step.
			 *
			 * The first step gets special handling and is primarily intended for bunkers in uneven terrain.
			 * It involves an almost vertical drop towards the lowest point of the bunker outline
			 * that is textured with ground, not sand. */

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

					Collection<TriangleXZ> triangulationXZ = trianguateAreaBetween(large, small);

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

			target.drawTriangles(SAND, resultingTriangulation,
					triangleTexCoordLists(resultingTriangulation, SAND, GLOBAL_X_Z));

		}

	}

	private static class Green extends AbstractAreaWorldObject implements TerrainBoundaryWorldObject {

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

			pinHoleLoop = new SimplePolygonXZ(new CircleXZ(pinPosition, HOLE_RADIUS).vertices(HOLE_CIRCLE_VERTICES));

			pinConnectors = new EleConnectorGroup();
			pinConnectors.addConnectorsFor(pinHoleLoop.getVertexCollection(), area, GroundState.ON);

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
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
		public void renderTo(Target target) {

			/* render green surface */

			String surfaceValue = area.getTags().getValue("surface");

			Material material = Materials.GRASS;

			if (surfaceValue != null && !"grass".equals(surfaceValue)) {
				material = Materials.getSurfaceMaterial(surfaceValue, material);
			}

			List<TriangleXZ> trianglesXZ = getGreenTriangulation();
			List<TriangleXYZ> triangles = getEleConnectors().getTriangulationXYZ(trianglesXZ);

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles , material, GLOBAL_X_Z));

			/* render pin */

			PolygonXYZ upperHoleRing = pinConnectors.getPosXYZ(pinHoleLoop);

			drawPin(target, pinPosition, upperHoleRing.vertices());

		}

		private List<TriangleXZ> getGreenTriangulation() {
			List<SimplePolygonXZ> holes = new ArrayList<>(area.getPolygon().getHoles());
			holes.add(pinHoleLoop);
			return TriangulationUtil.triangulate(area.getPolygon().getOuter(), holes, emptyList());
		}

		private static void drawPin(Target target, VectorXZ pos, List<VectorXYZ> upperHoleRing) {

			double minHoleEle = min(upperHoleRing, comparingDouble(v -> v.y)).y;

			double holeBottomEle = minHoleEle - HOLE_DEPTH;

			/* draw hole */

			List<VectorXYZ> lowerHoleRing = upperHoleRing.stream().map(v -> v.y(holeBottomEle)).collect(toList());

			List<VectorXYZ> vs = createTriangleStripBetween(upperHoleRing, lowerHoleRing);

			Material groundMaterial = Materials.EARTH.makeSmooth();

			target.drawTriangleStrip(groundMaterial, vs,
					texCoordLists(vs, groundMaterial, STRIP_WALL));

			target.drawConvexPolygon(groundMaterial, lowerHoleRing,
					texCoordLists(vs, groundMaterial, GLOBAL_X_Z));

			/* draw pole and flag */

			target.drawColumn(Materials.PLASTIC_GREY.makeSmooth(), null,
					pos.xyz(holeBottomEle), 1.5, 0.007, 0.007, false, true);

			StripedFlag flag = new StripedFlag(3 / 4, asList(YELLOW), true);
			flag.renderFlag(target, pos.xyz(holeBottomEle + 1.5), 0.3, 0.4);

		}

	}

}
