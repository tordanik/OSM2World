package org.osm2world.viewer.view.debug;

import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.jogl.Cubemap;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTargetShader;

public class SkyView extends DebugView {
	private Cubemap skybox;

	public SkyView(String filename) {
		skybox = new Cubemap(filename);
	}
	
	@Override
	public String getDescription() {
		return "shows the sky";
	};
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		if(target instanceof JOGLTargetShader) {
			((JOGLTargetShader)target).setEnvMap(skybox);
			((JOGLTargetShader)target).setShowEnvMap(true);

		} else {
			System.err.println("Environment maps only supported with shaders rendering");
		}
	}

	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		if (viewChanged) {
			target.reset();
			fillTarget(target);
		}
	}
	
}

