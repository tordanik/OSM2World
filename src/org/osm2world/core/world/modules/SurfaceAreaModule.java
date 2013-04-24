package org.osm2world.core.world.modules;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.modules.common.WorldModuleTexturingUtil;

/**
 * adds generic areas with surface information to the world.
 * Is based on surface information on otherwise unknown/unspecified areas.
 */
public class SurfaceAreaModule extends AbstractModule {

	private static final Map<Tag, String> defaultSurfaceMap
		= new HashMap<Tag, String>();
	
	static {
		defaultSurfaceMap.put(new Tag("leisure", "pitch"), "ground");
		defaultSurfaceMap.put(new Tag("landuse", "construction"), "ground");
		defaultSurfaceMap.put(new Tag("golf", "bunker"), "sand");
		defaultSurfaceMap.put(new Tag("golf", "green"), "grass");
		defaultSurfaceMap.put(new Tag("natural", "sand"), "sand");
		defaultSurfaceMap.put(new Tag("natural", "beach"), "sand");
		defaultSurfaceMap.put(new Tag("landuse", "meadow"), "grass");
		defaultSurfaceMap.put(new Tag("landuse", "grass"), "grass");
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
			
			Material material = Materials.getSurfaceMaterial(surface);
			
			if (material != null) {
				Collection<TriangleXYZ> triangles = getTriangulation();
				target.drawTriangles(material, triangles,
						WorldModuleTexturingUtil.globalTexCoordLists(triangles, material, false));
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
