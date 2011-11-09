package org.osm2world.core.target.povray;

import org.osm2world.core.target.Renderable;

public interface RenderableToPOVRay extends Renderable {

	/**
	 * lets the Renderable add global declarations.
	 * This is called before the renderTo calls.
	 */
	public void addDeclarationsTo(POVRayTarget target);
	
	public void renderTo(POVRayTarget target);
		
}
