package org.osm2world.core.world.modules;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.target.common.material.Materials.WATER;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createShapeExtrusionAlong;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.globalTexCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.wallTexCoordLists;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds swimming pools and water parks to the world
 */
public class PoolModule extends AbstractModule {

	private final boolean isPool(TagGroup tags) {
		boolean pool = tags.contains("amenity", "swimming_pool");
		pool |= tags.contains("leisure", "swimming_pool");
		
		return pool;
	}
	
	private final boolean isPool(MapArea area) {
		return isPool(area.getTags());
	}
	
	
	@Override
	protected void applyToArea(MapArea area) {
		if (isPool(area))
			area.addRepresentation(new Pool(area));
	}

	
	private static class Pool extends AbstractAreaWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
	
		public Pool(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0.2;
		}

		@Override
		public double getClearingBelow(VectorXZ pos) {
			// 1.5m is a good depth for a pool
			return 1.5;
		}

		@Override
		public void renderTo(Target<?> target) {

			/* render water */
			
			Collection<TriangleXYZ> triangles = getTriangulation();
			
			target.drawTriangles(WATER, triangles, 
					globalTexCoordLists(triangles, WATER, false));

			/* draw a small area around the pool */

			double width=1;
			double height=0.1;
			
			List<VectorXYZ> wallShape = asList(
					new VectorXYZ(-width/2, 0, 0),
					new VectorXYZ(-width/2, height, 0),
					new VectorXYZ(+width/2, height, 0),
					new VectorXYZ(+width/2, 0, 0)
			);

			List<VectorXZ> outer = area.getOuterPolygon().getVertexLoop();
			List<VectorXYZ> path = area.getElevationProfile().getWithEle(outer);
			
			List<List<VectorXYZ>> strips = createShapeExtrusionAlong(
					wallShape, path,
					nCopies(outer.size(), VectorXYZ.Y_UNIT));
			
			for (List<VectorXYZ> strip : strips) {
				target.drawTriangleStrip(Materials.CONCRETE, strip,
						wallTexCoordLists(strip, Materials.CONCRETE));
			}			
		}
	}
}
