package org.osm2world.viewer.view.debug;

import static org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding.CCW;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.viewer.model.RenderOptions;

public class WorldObjectView extends DebugView {
	
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
		
		iterate(map.getWorldObjects(), new Operation<WorldObject>() {
			@Override public void perform(WorldObject w) {
				TargetUtil.renderObject(target, w);
			}
		});
		
	}
	
	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		setParameters(target);
	}
	
	private void setParameters(final JOGLTarget target) {
		
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
	}

}
