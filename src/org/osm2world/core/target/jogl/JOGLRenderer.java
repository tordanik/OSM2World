package org.osm2world.core.target.jogl;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 */
abstract class JOGLRenderer {
	
	protected JOGLTextureManager textureManager;
	
	protected JOGLRenderer(JOGLTextureManager textureManager) {
		
		this.textureManager = textureManager;
		
	}

	public abstract void render(Camera camera, Projection projection);
	
	/**
	 * frees all OpenGL resources associated with this object.
	 * Rendering will no longer be possible afterwards!
	 */
	public void freeResources() {
		
		textureManager = null;
		
	}
	
	@Override
	protected void finalize() {
		freeResources();
	}
	
}
