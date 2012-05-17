package org.osm2world.viewer.view.debug;

import java.util.ArrayList;
import java.util.Collection;

import javax.media.opengl.GL;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
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
	 * {@link #renderToImpl(GL, Camera, Projection)}.
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
	protected void renderToImpl(GL gl, Camera camera, Projection projection) {

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
		
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION,
				new float[] {1.0f, 1.5f, -(-1.0f), 0.0f}, 0);
		
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_LIGHTING);
		
		// render
		
		renderer.render(camera, projection);
		
		// switch lighting off
		
		gl.glDisable(GL.GL_LIGHT0);
		gl.glDisable(GL.GL_LIGHTING);
		
	}

}
