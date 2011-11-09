package org.osm2world.core.terrain.data;

import java.util.Collection;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;

public abstract class TerrainPatch implements RenderableToAllTargets {
	
	/**
	 * Required preparation for rendering.
	 * Usually deals with triangulating the patch
	 * and restoring the polygons' third dimension afterwards.
	 * 
	 * Call this method exactly once for each patch!
	 */
	abstract public void build();
	
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
		
		target.drawTriangles(Materials.TERRAIN_DEFAULT, triangulation);
				
	}

	public Collection<TriangleXYZ> getTriangulation() {
		
		if (triangulation == null) {
			throw new IllegalStateException(
					"build must be called before getTriangulation");
		}
		
		return triangulation;
		
	}
		
}
