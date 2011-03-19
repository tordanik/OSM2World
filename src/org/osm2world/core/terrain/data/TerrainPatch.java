package org.osm2world.core.terrain.data;

import java.awt.Color;
import java.util.Collection;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.Material.Lighting;
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
	public void renderTo(Target target) {		

		if (triangulation == null) {
			// TODO (error handling): reactivate exception later
			// throw new IllegalStateException("build must be called before renderTo");
			return;
		}
		
		target.drawTriangles(new Material(Lighting.SMOOTH, Color.GREEN), triangulation);
				
	}
	
	@Override
	public void renderTo(POVRayTarget target) {		
		renderTo(target);		
	}
		
}
