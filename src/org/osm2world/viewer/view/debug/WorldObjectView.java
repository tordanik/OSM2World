package org.osm2world.viewer.view.debug;

import static org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.Cubemap;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTargetShader;
import org.osm2world.core.target.jogl.Sky;
import org.osm2world.viewer.model.RenderOptions;

public class WorldObjectView extends DebugView {
	private int change = 0;
	private Cubemap skybox;
	private long time;
	
	private final RenderOptions renderOptions;
	
	public WorldObjectView(RenderOptions renderOptions) {
		this.renderOptions = renderOptions;
	}
	
	@Override
	public String getDescription() {
		return "shows the world objects";
	};
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void fillTarget(final JOGLTarget target) {
		
		setParameters(target);
		target.setXZBoundary(map.getBoundary());
		
		boolean underground = config.getBoolean("renderUnderground", true);
		
		TargetUtil.renderWorldObjects(target, map, underground);
		
	}
	
	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		// TODO Refine this so that cubemaps are updated only when the elevation of the camera changes
		// Only update the cubemaps once the view has remained the same for 25 frames
		if(viewChanged)
			change = 25;
		
		setParameters(target);

		// TODO add showEnvRefl to renderParamaters
		if(target instanceof JOGLTargetShader) {
			if(change == 0)
				((JOGLTargetShader)target).updateReflections();

			if(Sky.getTime() != time) {
				if(true) {
					skybox = Sky.getSky();
				}
				((JOGLTargetShader)target).setEnvMap(skybox);
				((JOGLTargetShader)target).setShowEnvMap(false);
				((JOGLTargetShader)target).setShowEnvRefl(true);
				time = Sky.getTime();
			}
		} else {
			System.err.println("Environment maps only supported with shaders rendering");
		}
		change--;
		change = Math.max(change, -1);
	}
	
	private void setParameters(final JOGLTarget target) {
		
		boolean drawBoundingBox = config.getBoolean("drawBoundingBox", false);
		boolean shadowVolumes = "shadowVolumes".equals(config.getString("shadowImplementation"))
				|| "both".equals(config.getString("shadowImplementation"));
		boolean shadowMaps = "shadowMap".equals(config.getString("shadowImplementation"))
				|| "both".equals(config.getString("shadowImplementation"));
		int shadowMapWidth = config.getInt("shadowMapWidth", 4096);
		int shadowMapHeight = config.getInt("shadowMapHeight", 4096);
		int shadowMapCameraFrustumPadding = config.getInt("shadowMapCameraFrustumPadding", 8);
		boolean useSSAO = "true".equals(config.getString("useSSAO"));
		int SSAOkernelSize = config.getInt("SSAOkernelSize", 16);
		float SSAOradius = config.getFloat("SSAOradius", 1);
		boolean overwriteProjectionClippingPlanes = "true".equals(config.getString("overwriteProjectionClippingPlanes"));
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true, drawBoundingBox, shadowVolumes, shadowMaps, shadowMapWidth, shadowMapHeight, 
    			shadowMapCameraFrustumPadding, useSSAO, SSAOkernelSize, SSAOradius, overwriteProjectionClippingPlanes));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
	}

}
