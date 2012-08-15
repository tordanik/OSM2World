package org.osm2world.viewer.view.debug;

import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.WorldObject;

public class WorldObjectView extends DebugView {
	
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
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
		iterate(map.getWorldObjects(), new Operation<WorldObject>() {
			@Override public void perform(WorldObject w) {
				TargetUtil.renderObject(target, w);
			}
		});
		
	}

}
