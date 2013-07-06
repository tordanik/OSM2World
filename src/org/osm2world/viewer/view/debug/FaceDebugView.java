package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.FaceTarget;
import org.osm2world.core.target.common.RenderableToFaceTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.data.WorldObject;

/**
 * shows decomposition of {@link WorldObject}s into faces
 * as they would be written to any {@link FaceTarget}
 */
public class FaceDebugView extends DebugView {
	
	private static final Color BORDER_COLOR = new Color(0, 0, 1.0f);
	
	@Override
	public String getDescription() {
		return "shows decomposition of WorldObjects into faces";
	}
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	private static class FaceSink
		extends FaceTarget<RenderableToFaceTarget> {

		public final List<List<VectorXYZ>> faces =
				new ArrayList<List<VectorXYZ>>();
		
		@Override
		public Class<RenderableToFaceTarget> getRenderableType() {
			return RenderableToFaceTarget.class;
		}

		@Override
		public void render(RenderableToFaceTarget renderable) {
			renderable.renderTo(this);
		}

		@Override
		public boolean reconstructFaces() {
			return true;
		}
		
		@Override
		public void drawFace(Material material, List<VectorXYZ> vs,
				List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists) {
			faces.add(vs);
		}
		
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		
		FaceSink faceSink = new FaceSink();
		
		TargetUtil.renderWorldObjects(faceSink, map, true);
		
		faceSink.finish();
		
		for (List<VectorXYZ> face : faceSink.faces) {
			target.drawLineLoop(BORDER_COLOR, 2, face);
		}
		
	}
	
}
