package org.osm2world.core.terrain.data;

import java.util.Collection;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.povray.POVRayTarget;
import org.osm2world.core.target.povray.RenderableToPOVRay;

public abstract class TerrainPatch implements RenderableToAllTargets, RenderableToPOVRay {
	
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
	public Collection<TriangleXYZ> triangulation;
		
	@Override
	public void renderTo(Target<?> target) {

		if (triangulation == null) {
			// TODO (error handling): reactivate exception later
			// throw new IllegalStateException("build must be called before renderTo");
			return;
		}
		
		target.drawTriangles(Materials.TERRAIN_DEFAULT, triangulation);
				
	}
	
	@Override
	public void renderTo(POVRayTarget target) {
		renderTo(target);
	}
		
}
