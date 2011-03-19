package org.osm2world.core.target.povray;

import org.osm2world.core.target.Renderable;

public interface RenderableToPOVRay extends Renderable {
	
	public void renderTo(POVRayTarget target);
		
}
