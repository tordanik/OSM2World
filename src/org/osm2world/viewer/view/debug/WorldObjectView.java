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
	private Cubemap envMap;

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
					envMap = Sky.getSky();
				}
				((JOGLTargetShader)target).setEnvMap(envMap);
				// World object view never displays environment maps
				((JOGLTargetShader)target).setShowEnvMap(false);
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
		boolean showSkyReflections = config.getBoolean("showSkyReflections", false);
		boolean showGroundReflections = config.getBoolean("showGroundReflections", false);

		boolean useEnvLighting = config.getBoolean("useEnvLighting", false);

		int geomReflType = 0;
		switch(config.getString("geomReflectionType", "none")) {
			case "cubemap":
				geomReflType = 1;
				break;
			case "plane":
				geomReflType = 0;
				System.err.println("Planar reflections are not yet supported");
				break;
			default:
				geomReflType = 0;
				break;
		}

		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true, drawBoundingBox,
				shadowVolumes, shadowMaps, shadowMapWidth, shadowMapHeight, shadowMapCameraFrustumPadding, 
				useSSAO, SSAOkernelSize, SSAOradius, overwriteProjectionClippingPlanes,
				showSkyReflections, showGroundReflections, geomReflType, useEnvLighting
				));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
	}

}
