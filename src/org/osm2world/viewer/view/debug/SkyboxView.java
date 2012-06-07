package org.osm2world.viewer.view.debug;

import static java.lang.Math.sqrt;
import static javax.media.opengl.GL.GL_CULL_FACE;

import javax.media.opengl.GL2;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;

public class SkyboxView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows a skybox in the background";
	};
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	private JOGLTarget target = null;
	
	@Override
	protected void renderToImpl(GL2 gl, Camera camera, Projection projection) {
		
		if (camera == null) return;
		
		if (target == null) { // keep the target around to avoid reloading textures
			target = new JOGLTarget(gl, camera);
		}
				
		boolean cullFaceEnabled = gl.glIsEnabled(GL_CULL_FACE);

		gl.glDisable(GL_CULL_FACE);
		
		double skyboxSize = 1.95 * projection.getFarClippingDistance() / sqrt(3);
		
		target.drawBox(Materials.SKYBOX,
				camera.getPos().add(0, -skyboxSize / 2, 0),
				VectorXZ.Z_UNIT, skyboxSize, skyboxSize, skyboxSize);
		
		if (cullFaceEnabled) gl.glEnable(GL_CULL_FACE);
				
	}

}
