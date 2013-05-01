package org.osm2world.core.world.modules;

import static java.util.Collections.*;
import static org.osm2world.core.map_data.creation.EmptyTerrainBuilder.EMPTY_SURFACE_TAG;
import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MIN;
import static org.osm2world.core.map_elevation.data.GroundState.*;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.globalTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.creation.EmptyTerrainBuilder;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorGridXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.algorithms.Poly2TriTriangulationUtil;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds generic areas with surface information to the world.
 * Is based on surface information on otherwise unknown/unspecified areas.
 */
public class SurfaceAreaModule extends AbstractModule {
	
	/** assumptions about default surfaces for certain tags */
	private static final Map<Tag, String> defaultSurfaceMap
		= new HashMap<Tag, String>();
	
	static {
		defaultSurfaceMap.put(new Tag("leisure", "pitch"), "ground");
		defaultSurfaceMap.put(new Tag("landuse", "construction"), "ground");
		defaultSurfaceMap.put(new Tag("golf", "bunker"), "sand");
		defaultSurfaceMap.put(new Tag("golf", "green"), "grass");
		defaultSurfaceMap.put(new Tag("natural", "sand"), "sand");
		defaultSurfaceMap.put(new Tag("natural", "beach"), "sand");
	}
	
	@Override
	protected void applyToArea(MapArea area) {
		
		if (!area.getRepresentations().isEmpty()) return;
		
		TagGroup tags = area.getTags();
		
		if (tags.containsKey("surface")) {
			area.addRepresentation(new SurfaceArea(area, tags.getValue("surface")));
		} else {
		
			for (Tag tagWithDefault : defaultSurfaceMap.keySet()) {
				if (tags.contains(tagWithDefault)) {
					area.addRepresentation(new SurfaceArea(
							area, defaultSurfaceMap.get(tagWithDefault)));
				}
			}
			
		}
		
	}
	
	private static class SurfaceArea extends AbstractAreaWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
		
		private final String surface;
		
		private Collection<TriangleXZ> triangulationXZ;
		
		public SurfaceArea(MapArea area, String surface) {
			super(area);
			this.surface = surface;
		}

		@Override
		public void renderTo(Target<?> target) {
			
			Material material = null;
			
			if (surface.equals(EMPTY_SURFACE_TAG.value)) {
				material = Materials.TERRAIN_DEFAULT;
			} else {
				material = Materials.getSurfaceMaterial(surface);
			}
			
			if (material != null) {
				
				Collection<TriangleXYZ> triangles = getTriangulation();
				target.drawTriangles(material, triangles,
						globalTexCoordLists(triangles, material, false));
				
			}
			
		}
		
		/**
		 * calculates the true ground footprint of this area by removing
		 * area covered by other overlapping features, then triangulates it
		 * into counterclockwise triangles.
		 */
		@Override
		protected Collection<TriangleXZ> getTriangulationXZ() {
			
			if (triangulationXZ != null) {
				return triangulationXZ;
			}
			
			boolean isEmptyTerrain = surface.equals(EMPTY_SURFACE_TAG.value);
			
			/* collect the outlines of overlapping ground polygons and other polygons,
			 * and EleConnectors within the area */
			
			List<SimplePolygonXZ> subtractPolys = new ArrayList<SimplePolygonXZ>();
			List<SimplePolygonXZ> allPolys = new ArrayList<SimplePolygonXZ>();
			
			List<VectorXZ> eleConnectorPoints = new ArrayList<VectorXZ>();
			
			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
			for (WorldObject otherWO : overlap.getOther(area).getRepresentations()) {

				if (otherWO instanceof TerrainBoundaryWorldObject
						&& otherWO.getGroundState() == GroundState.ON) {
					
					if (otherWO instanceof SurfaceArea && !isEmptyTerrain) {
						// empty terrain has lowest priority
						continue;
					}
					
					if (overlap.type == MapOverlapType.CONTAIN
							&& overlap.e1 == this.area) {
						// completely within other element, no ground area left
						return emptyList();
					}
					
					TerrainBoundaryWorldObject terrainBoundary =
						(TerrainBoundaryWorldObject)otherWO;
					
					SimplePolygonXZ outlinePolygon = terrainBoundary.getOutlinePolygonXZ();
					
					if (outlinePolygon != null) {
						
						subtractPolys.add(outlinePolygon);
						allPolys.add(outlinePolygon);
						
						for (EleConnector eleConnector : otherWO.getEleConnectors()) {
							
							if (!outlinePolygon.getVertexCollection().contains(eleConnector.pos)) {
								eleConnectorPoints.add(eleConnector.pos);
							}
							
						}
						
					}
					
				} else {
					
					for (EleConnector eleConnector : otherWO.getEleConnectors()) {
						
						if (eleConnector.reference == null) {
							/* workaround to avoid using connectors at intersections,
							 * which might fall on area segments
							 * //TODO cleaner solution
							 */
							continue;
						}
						
						eleConnectorPoints.add(eleConnector.pos);
					}
					
					if (otherWO instanceof WorldObjectWithOutline) {
						
						SimplePolygonXZ outlinePolygon =
								((WorldObjectWithOutline)otherWO).getOutlinePolygonXZ();
						
						if (outlinePolygon != null) {
							
							allPolys.add(outlinePolygon);
							
						}
						
					}
					
				}
				
			}
			}
			
			/* add a grid of points within the area for smoother surface shapes */
			
			VectorGridXZ pointGrid = new VectorGridXZ(
					area.getAxisAlignedBoundingBoxXZ(),
					EmptyTerrainBuilder.POINT_GRID_DIST);
			
			for (VectorXZ point : pointGrid) {
				
				//don't insert if it is e.g. on top of a tunnel;
				//otherwise there would be no minimum vertical distance
				
				boolean safe = true;
				
				for (SimplePolygonXZ polygon : allPolys) {
					if (polygon.contains(point)) {
						safe = false;
						break;
					}
				}
				
				if (safe) {
					eleConnectorPoints.add(point);
				}
				
			}
			
			/* create "leftover" polygons by subtracting the existing ones */
			
			Collection<PolygonWithHolesXZ> polygons;
			
			if (subtractPolys.isEmpty()) {
				
				/* SUGGEST (performance) handle the common "empty terrain cell"
				 * special case more efficiently, also regarding point raster? */
				
				polygons = singleton(area.getPolygon());
				
			} else {
				
				polygons = CAGUtil.subtractPolygons(
						area.getOuterPolygon(), subtractPolys);
				
			}
						
			/* triangulate, using elevation information from all participants */
			
			triangulationXZ = new ArrayList<TriangleXZ>();
			
			for (PolygonWithHolesXZ polygon : polygons) {
				
				List<VectorXZ> points = new ArrayList<VectorXZ>();
				
				for (VectorXZ point : eleConnectorPoints) {
					if (polygon.contains(point)) {
						points.add(point);
					}
				}
				
				try {
					
					triangulationXZ.addAll(Poly2TriTriangulationUtil.triangulate(
							polygon.getOuter(),
							polygon.getHoles(),
							Collections.<LineSegmentXZ>emptyList(),
							points));
					
				} catch (NullPointerException e) {
					
					System.err.println("Poly2Tri exception for " + this + ":");
					e.printStackTrace();
					System.err.println("... falling back to JTS triangulation.");
					
					triangulationXZ.addAll(JTSTriangulationUtil.triangulate(
							polygon.getOuter(),
							polygon.getHoles(),
							Collections.<LineSegmentXZ>emptyList(),
							points));
					
				}
				
			}
			
			return triangulationXZ;
			
		}
		
		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			
			super.defineEleConstraints(enforcer);
			
			/** add vertical distance to connectors above and below */
			
			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
			for (WorldObject otherWO : overlap.getOther(area).getRepresentations()) {
				
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
				
			}
			}
			
		}
		
		@Override
		public PolygonXYZ getOutlinePolygon() {
			if (surface.equals(EMPTY_SURFACE_TAG.value)) {
				// avoid interfering with e.g. tree placement
				return null;
			} else {
				return super.getOutlinePolygon();
			}
		}
		
		@Override
		public SimplePolygonXZ getOutlinePolygonXZ() {
			if (surface.equals(EMPTY_SURFACE_TAG.value)) {
				// avoid interfering with e.g. tree placement
				return null;
			} else {
				return super.getOutlinePolygonXZ();
			}
		}
		
		@Override
		public GroundState getGroundState() {
			if (BridgeModule.isBridge(area.getTags())) {
				return GroundState.ABOVE;
			} else if (TunnelModule.isTunnel(area.getTags())) {
				return GroundState.BELOW;
			} else {
				return GroundState.ON;
			}
		}
		
	}

}
