package org.osm2world.viewer.view.debug;

import static javax.media.opengl.GL2ES1.GL_LIGHT_MODEL_AMBIENT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.*;

import java.util.ArrayList;
import java.util.Collection;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.primitivebuffer.JOGLPrimitiveBufferRenderer;
import org.osm2world.core.target.primitivebuffer.PrimitiveBuffer;

public class WorldObjectView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows the world objects";
	};
	
	JOGLPrimitiveBufferRenderer renderer = null;
	
	/**
	 * old renderers that are no longer needed but may still have OpenGL
	 * resources associated with them. They cannot be deleted in
	 * {@link #setPrimitiveBuffers(PrimitiveBuffer, PrimitiveBuffer)}
	 * because that thread doesn't have the GL context associated with it.
	 * Instead, they will be stored here until the next call to
	 * {@link #renderToImpl(GL, Camera)}.
	 */
	Collection<JOGLPrimitiveBufferRenderer> rendererTrashBin =
			new ArrayList<JOGLPrimitiveBufferRenderer>();
	
	@Override
	public void setPrimitiveBuffers(PrimitiveBuffer gridPrimitiveBuffer,
			PrimitiveBuffer terrainPrimitiveBuffer) {
		
		super.setPrimitiveBuffers(gridPrimitiveBuffer, terrainPrimitiveBuffer);
		
		if (renderer != null) {
			rendererTrashBin.add(renderer);
			renderer = null;
		}
		
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void renderToImpl(GL2 gl, Camera camera) {

		// clean up old renderers
		
		if (!rendererTrashBin.isEmpty()) {
			
			for (JOGLPrimitiveBufferRenderer oldRenderer : rendererTrashBin) {
				oldRenderer.freeResources();
			}
		
			rendererTrashBin.clear();
			
		}
		
		// create new renderer
		
		if (renderer == null) {
			renderer = new JOGLPrimitiveBufferRenderer(gl, mapDataPrimitiveBuffer);
		}
		
		// define light source
		
		gl.glLightfv(GL_LIGHT0, GL_AMBIENT,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL_LIGHT0, GL_DIFFUSE,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL_LIGHT0, GL_SPECULAR,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL_LIGHT0, GL_POSITION,
				new float[] {1.0f, 1.5f, -(-1.0f), 0.0f}, 0);
		
		gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT,
				new float[] { 1.0f , 1.0f , 1.0f , 1.0f } , 0);
		
		gl.glEnable(GL_LIGHT0);
		gl.glEnable(GL_LIGHTING);
		
		// render
		
		renderer.render();
		
		// switch lighting off
		
		gl.glDisable(GL_LIGHT0);
		gl.glDisable(GL_LIGHTING);
		
	}

}
