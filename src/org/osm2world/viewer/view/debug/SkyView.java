package org.osm2world.viewer.view.debug;

import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.jogl.Cubemap;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTargetShader;

import org.osm2world.core.target.jogl.Sky;

public class SkyView extends DebugView {
	private Cubemap skybox;
	private boolean procedural;

	private long lastTime;

	public SkyView(String filename) {
		procedural = false;
		skybox = new Cubemap(filename);
	}

	public SkyView(boolean procedural) {
		this.procedural = procedural;
		if(!procedural)
			System.err.println("To define a static skybox, pass the filename");
		skybox = Sky.getSky();
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
			if(procedural) {
				((JOGLTargetShader)target).updateSky();
				skybox = Sky.getSky();
			}
			((JOGLTargetShader)target).setEnvMap(skybox);
			((JOGLTargetShader)target).setShowEnvMap(true);
		} else {
			System.err.println("Environment maps only supported with shaders rendering");
		}
	}

	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		if(Sky.getTime() != lastTime) {
			target.reset();
			fillTarget(target);
			lastTime = Sky.getTime();
		}
	}
	
}

