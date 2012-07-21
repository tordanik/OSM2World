package org.osm2world.core.target.primitivebuffer;

import javax.media.opengl.GL2;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTextureManager;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 */
public abstract class JOGLPrimitiveBufferRenderer {
	
	protected GL2 gl;
	protected JOGLTextureManager textureManager;
	
	protected JOGLPrimitiveBufferRenderer(GL2 gl) {
		
		this.gl = gl;
		
		this.textureManager = new JOGLTextureManager(gl);
		
	}

	public abstract void render(Camera camera, Projection projection);
	
	/**
	 * frees all OpenGL resources associated with this object.
	 * Rendering will no longer be possible afterwards!
	 */
	public void freeResources() {
		
		textureManager.releaseAll();
		
		gl = null;
		
	}
	
	@Override
	protected void finalize() {
		freeResources();
	}
	
}
