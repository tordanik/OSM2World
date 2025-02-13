package org.osm2world.world.modules;

import static org.osm2world.map_data.creation.EmptyTerrainBuilder.EMPTY_SURFACE_VALUE;
import static org.osm2world.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MIN;
import static org.osm2world.map_elevation.data.GroundState.ABOVE;
import static org.osm2world.map_elevation.data.GroundState.BELOW;
import static org.osm2world.output.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.output.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.*;

import org.osm2world.map_data.creation.EmptyTerrainBuilder;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.Tag;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.datastructures.VectorGridXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.output.common.material.Material;
import org.osm2world.output.common.material.Materials;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * adds generic areas with surface information to the world.
 * Is based on surface information on otherwise unknown/unspecified areas.
 */
public class SurfaceAreaModule extends AbstractModule {

	/** assumptions about default surfaces for certain tags */
	public static final Map<Tag, String> defaultSurfaceMap = new HashMap<>();

	static {
		defaultSurfaceMap.put(new Tag("landcover", "grass"), "grass");
		defaultSurfaceMap.put(new Tag("landcover", "gravel"), "gravel");
		defaultSurfaceMap.put(new Tag("landcover", "ground"), "ground");
		defaultSurfaceMap.put(new Tag("landuse", "construction"), "ground");
		defaultSurfaceMap.put(new Tag("landuse", "grass"), "grass");
		defaultSurfaceMap.put(new Tag("landuse", "meadow"), "grass");
		defaultSurfaceMap.put(new Tag("leisure", "pitch"), "ground");
		defaultSurfaceMap.put(new Tag("natural", "bare_rock"), "rock");
		defaultSurfaceMap.put(new Tag("natural", "beach"), "sand");
		defaultSurfaceMap.put(new Tag("natural", "fell"), "grass");
		defaultSurfaceMap.put(new Tag("natural", "glacier"), "snow");
		defaultSurfaceMap.put(new Tag("natural", "grassland"), "grass");
		defaultSurfaceMap.put(new Tag("natural", "mud"), "ground");
		defaultSurfaceMap.put(new Tag("natural", "sand"), "sand");
		defaultSurfaceMap.put(new Tag("natural", "shingle"), "pebblestone");
		defaultSurfaceMap.put(new Tag("natural", "scree"), "scree");
		defaultSurfaceMap.put(new Tag("natural", "scrub"), "scrub");
	}

	@Override
	protected void applyToArea(MapArea area) {

		if (!area.getRepresentations().isEmpty()) return;

		TagSet tags = area.getTags();

		if (tags.containsKey("surface")) {
			if (!tags.contains("surface", EMPTY_SURFACE_VALUE)
					|| config.getBoolean("createTerrain", true)) {
				area.addRepresentation(new SurfaceArea(area, tags.getValue("surface")));
			}
		} else {

			for (Tag tagWithDefault : defaultSurfaceMap.keySet()) {
				if (tags.contains(tagWithDefault)) {
					area.addRepresentation(new SurfaceArea(
							area, defaultSurfaceMap.get(tagWithDefault)));
				}
			}

		}

	}

	public static class SurfaceArea extends AbstractAreaWorldObject
			implements ProceduralWorldObject {

		private final String surface;

		private List<TriangleXZ> triangulationXZ;

		public SurfaceArea(MapArea area, String surface) {
			super(area);
			this.surface = surface;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			Material material;

			if (surface.equals(EMPTY_SURFACE_VALUE)) {
				material = Materials.TERRAIN_DEFAULT;
			} else {
				material = Materials.getSurfaceMaterial(surface);
			}

			if (material != null) {

				List<TriangleXYZ> triangles = getTriangulation();

				if (!triangles.isEmpty()) {
					target.drawTriangles(material, triangles,
							triangleTexCoordLists(triangles, material, GLOBAL_X_Z));
				}

			}

		}

		/**
		 * calculates the true ground footprint of this area by removing
		 * area covered by other overlapping features, then triangulates it
		 * into counterclockwise triangles.
		 */
		@Override
		protected List<TriangleXZ> getTriangulationXZ() {

			if (triangulationXZ != null) {
				return triangulationXZ;
			}

			Collection<PolygonShapeXZ> footprint = getGroundFootprint();

			/* add a grid of points within the area for smoother surface shapes */

			List<VectorXZ> eleConnectorPoints = new ArrayList<>();

			VectorGridXZ pointGrid = new VectorGridXZ(
					area.boundingBox(),
					EmptyTerrainBuilder.POINT_GRID_DIST);

			for (VectorXZ point : pointGrid) {
				eleConnectorPoints.add(point);
			}

			/* triangulate, using elevation information from all participants */

			triangulationXZ = TriangulationUtil.triangulate(footprint, eleConnectorPoints);

			return triangulationXZ;

		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {

			super.defineEleConstraints(enforcer);

			/* add vertical distance to connectors above and below */

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
			FaultTolerantIterationUtil.forEach(overlap.getOther(area).getRepresentations(),  otherWO -> {

				for (EleConnector eleConnector : otherWO.getEleConnectors()) {

					EleConnector ownConnector = getEleConnectors().getConnector(eleConnector.pos);

					if (ownConnector == null) continue;

					if (eleConnector.groundState == ABOVE) {

						enforcer.requireVerticalDistance(
								MIN, 1,
								eleConnector, ownConnector); //TODO actual clearing

					} else if (eleConnector.groundState == BELOW) {

						enforcer.requireVerticalDistance(
								MIN, 10,
								ownConnector, eleConnector); //TODO actual clearing

					}

				}

			});
			}

		}

		@Override
		public GroundState getGroundState() {
			if (BridgeModule.isBridge(area.getTags())) {
				return GroundState.ABOVE;
			} else if (TunnelModule.isTunnel(area.getTags())) {
				return GroundState.BELOW;
			} else {
				return super.getGroundState();
			}
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public int getOverlapPriority() {
			return surface.equals(EMPTY_SURFACE_VALUE) ? Integer.MIN_VALUE : 20;
		}

	}

}
