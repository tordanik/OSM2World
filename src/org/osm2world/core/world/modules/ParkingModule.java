package org.osm2world.core.world.modules;

import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

import java.util.Collection;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

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
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			String surface = area.getTags().getValue("surface");
			Material material = getSurfaceMaterial(surface, ASPHALT);
			
			Collection<TriangleXYZ> triangles = getTriangulation();
			
			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));
			
		}
		
	}
	
}
