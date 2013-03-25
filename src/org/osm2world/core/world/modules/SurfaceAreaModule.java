package org.osm2world.core.world.modules;

import static java.util.Collections.*;
import static org.osm2world.core.map_data.creation.EmptyTerrainBuilder.EMPTY_SURFACE_TAG;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.globalTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.terrain.creation.CAGUtil;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WorldObject;
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
	
	private class SurfaceArea extends AbstractAreaWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
		
		private final String surface;
		
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
				
				for (PolygonWithHolesXZ polygon : getGroundPolygons()) {
					
					Collection<TriangleXYZ> triangles = getTriangulation(polygon);
					target.drawTriangles(material, triangles,
							globalTexCoordLists(triangles, material, false));
				
				}
					
			}
			
		}
		
		/**
		 * calculates the true ground footprint of this area by removing
		 * area covered by other overlapping features.
		 */
		private Collection<PolygonWithHolesXZ> getGroundPolygons() {
			
			boolean isEmptyTerrain = surface.equals(EMPTY_SURFACE_TAG.value);
			
			/* collect the outlines of overlapping ground polygons */
			
			List<SimplePolygonXZ> subtractPolys = new ArrayList<SimplePolygonXZ>();
			Collection<VectorXYZ> unconnectedEles = emptyList(); //TODO
			
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
					
					PolygonXYZ outlinePolygon = terrainBoundary.getOutlinePolygon();
					
					if (outlinePolygon != null
							&& outlinePolygon.getXZPolygon().isSimple()) { //TODO is simplicity check necessary?
						
						subtractPolys.add(outlinePolygon.getSimpleXZPolygon());
						
					}
					
				} else {
					
					// no boundary, but ele data can be relevant
					
					//TODO
					
//
//					ElevationProfile eleProfile = worldObject
//							.getPrimaryMapElement().getElevationProfile();
//
//					unconnectedEleMap.putAll(terrainCell,
//							eleProfile.getPointsWithEle());
//
					
				}
				
			}
			}
			
			/* create "leftover" polygons by subtracting the existing ones */
			
			if (subtractPolys.isEmpty() /* && unconnectedEles.isEmpty() TODO */) {
				
				//TODO handle the common "empty terrain cell" special case more efficiently?
				
				return singleton(area.getPolygon());
				
			} else {
				
				return CAGUtil.subtractPolygons(
						area.getOuterPolygon(), subtractPolys);
				
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
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
	}

}
