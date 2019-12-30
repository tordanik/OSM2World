package org.osm2world.core.target.povray;

import org.osm2world.core.target.Renderable;

public interface RenderableToPOVRay extends Renderable {

	/**
	 * adds any global declarations that may be necessary.
	 * This is called before the renderTo calls.
	 */
	public void addDeclarationsTo(POVRayTarget target);

}
