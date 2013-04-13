package org.osm2world.core.terrain.data;

import static org.osm2world.core.target.common.material.Materials.TERRAIN_DEFAULT;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.globalTexCoordLists;

import java.util.Collection;

import org.osm2world.TerrainInterpolator;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;

public abstract class TerrainPatch implements RenderableToAllTargets {
	
	/**
	 * Required preparation for rendering.
	 * Usually deals with triangulating the patch
	 * and adding the third dimension afterwards.
	 * 
	 * Call this method exactly once for each patch!
	 */
	abstract public void build(TerrainInterpolator strategy);
	
	/**
	 * counterclockwise triangles
	 */
	protected Collection<TriangleXYZ> triangulation;
		
	@Override
	public void renderTo(Target<?> target) {

		if (triangulation == null) {
			throw new IllegalStateException(
					"build must be called before renderTo");
		}
		
		target.drawTriangles(TERRAIN_DEFAULT, triangulation,
				globalTexCoordLists(triangulation, TERRAIN_DEFAULT, false));
				
	}

	public Collection<TriangleXYZ> getTriangulation() {
		
		if (triangulation == null) {
			throw new IllegalStateException(
					"build must be called before getTriangulation");
		}
		
		return triangulation;
		
	}
		
}
