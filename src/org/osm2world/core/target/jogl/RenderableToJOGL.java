package org.osm2world.core.target.jogl;

import javax.media.opengl.GL2;

import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.common.rendering.Camera;


public interface RenderableToJOGL extends Renderable {

	public void renderTo(GL2 gl, Camera camera);
	
}
