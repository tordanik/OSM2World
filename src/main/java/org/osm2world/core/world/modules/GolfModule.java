package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.VectorXZ.fromAngle;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.SurfaceAreaModule.SurfaceArea;
import org.osm2world.core.world.modules.common.AbstractModule;

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

	private static class Green extends AbstractAreaWorldObject
			implements RenderableToAllTargets, TerrainBoundaryWorldObject {

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

			List<VectorXZ> holeRing = new ArrayList<VectorXZ>(HOLE_CIRCLE_VERTICES);

			for (int i = 0; i < HOLE_CIRCLE_VERTICES; i++) {
				VectorXZ direction = fromAngle(2 * PI * ((double)i / HOLE_CIRCLE_VERTICES));
				VectorXZ vertex = pinPosition.add(direction.mult(HOLE_RADIUS));
				holeRing.add(vertex);
			}

			holeRing.add(holeRing.get(0));

			pinHoleLoop = new SimplePolygonXZ(holeRing);

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
		public void renderTo(Target<?> target) {

			/* render green surface */

			String surfaceValue = area.getTags().getValue("surface");

			Material material = Materials.GRASS;

			if (surfaceValue != null && !"grass".equals(surfaceValue)) {
				material = Materials.getSurfaceMaterial(surfaceValue, material);
			}

			Collection<TriangleXZ> trianglesXZ = getGreenTriangulation();
			Collection<TriangleXYZ> triangles = getEleConnectors().getTriangulationXYZ(trianglesXZ);

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles , material, GLOBAL_X_Z));

			/* render pin */

			PolygonXYZ upperHoleRing = pinConnectors.getPosXYZ(pinHoleLoop);

			drawPin(target, pinPosition, upperHoleRing.getVertexLoop());

		}

		private List<TriangleXZ> getGreenTriangulation() {

			List<SimplePolygonXZ> holes = area.getPolygon().getHoles();

			holes.add(pinHoleLoop);

			return TriangulationUtil.triangulate(
				area.getPolygon().getOuter(),
				holes,
				Collections.<VectorXZ>emptyList());

		}

		private static void drawPin(Target<?> target,
				VectorXZ pos, List<VectorXYZ> upperHoleRing) {

			double minHoleEle = min(upperHoleRing, comparingDouble(v -> v.y)).y;

			double holeBottomEle = minHoleEle - HOLE_DEPTH;

			/* draw hole */

			List<VectorXYZ> lowerHoleRing = new ArrayList<VectorXYZ>();
			for (VectorXYZ v : upperHoleRing) {
				lowerHoleRing.add(v.y(holeBottomEle));
			}

			List<VectorXYZ> vs = createTriangleStripBetween(
					upperHoleRing, lowerHoleRing);

			Material groundMaterial = Materials.EARTH.makeSmooth();

			target.drawTriangleStrip(groundMaterial, vs,
					texCoordLists(vs, groundMaterial, STRIP_WALL));

			target.drawConvexPolygon(groundMaterial, lowerHoleRing,
					texCoordLists(vs, groundMaterial, GLOBAL_X_Z));

			/* draw flag */

			target.drawColumn(Materials.PLASTIC_GREY.makeSmooth(), null,
					pos.xyz(holeBottomEle), 1.5, 0.007, 0.007, false, true);

			ImmutableMaterial flagcloth = new ImmutableMaterial(Interpolation.SMOOTH, Color.YELLOW);

			List<VectorXYZ> flagVertices = asList(
					new VectorXYZ(pos.x, 1.5, pos.z),
					new VectorXYZ(pos.x, 1.2, pos.z),
					new VectorXYZ(pos.x + 0.4, 1.5, pos.z),
					new VectorXYZ(pos.x + 0.4, 1.2, pos.z));

			target.drawTriangleStrip(flagcloth, flagVertices,
					texCoordLists(flagVertices, flagcloth, STRIP_WALL));

		}

	}

}
