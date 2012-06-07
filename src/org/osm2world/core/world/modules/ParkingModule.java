package org.osm2world.core.world.modules;

import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.globalTexCoordLists;

import java.util.Collection;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.modules.common.WorldModuleParseUtil;

/**
 * adds parking spaces to the world
 */
public class ParkingModule extends AbstractModule {
	
	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("amenity","parking")) {
		
			String parkingValue = area.getTags().getValue("parking");
			
			if ("surface".equals(parkingValue) || parkingValue == null) {
				area.addRepresentation(new SurfaceParking(area));
			}
						
		}
	}
	
	private static class SurfaceParking extends AbstractAreaWorldObject
	implements TerrainBoundaryWorldObject, RenderableToAllTargets {
	
		public SurfaceParking(MapArea area) {
			super(area);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return WorldModuleParseUtil.parseClearing(area.getTags(), 3);
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			String surface = area.getTags().getValue("surface");
			Material material = getSurfaceMaterial(surface, ASPHALT);
			
			Collection<TriangleXYZ> triangles = getTriangulation();
			
			target.drawTriangles(material, triangles,
					globalTexCoordLists(triangles, material, false));
			
		}
		
	}
	
}
