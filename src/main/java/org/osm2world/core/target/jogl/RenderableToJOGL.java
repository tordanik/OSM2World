package org.osm2world.core.target.jogl;

import org.osm2world.core.target.Renderable;

public interface RenderableToJOGL extends Renderable {

	public void renderTo(JOGLTarget target);
	
}
