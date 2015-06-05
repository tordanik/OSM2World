package org.osm2world.core.target.jogl;

import javax.media.opengl.GL3;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

public class JOGLRendererShader extends JOGLRenderer {

	protected JOGLRendererShader(GL3 gl, JOGLTextureManager textureManager, PrimitiveBuffer primitiveBuffer) {
		super(textureManager);
	}

	@Override
	public void render(Camera camera, Projection projection) {
		// TODO Auto-generated method stub

	}

}
