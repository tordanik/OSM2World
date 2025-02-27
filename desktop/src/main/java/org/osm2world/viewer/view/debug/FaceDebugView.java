package org.osm2world.viewer.view.debug;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.FaceOutput;
import org.osm2world.scene.material.Material;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.world.data.WorldObject;

/**
 * shows decomposition of {@link WorldObject}s into faces
 * as they would be written to any {@link FaceOutput}
 */
public class FaceDebugView extends DebugView {

	private static final Color BORDER_COLOR = new Color(0, 0, 1.0f);

	@Override
	public String getDescription() {
		return "shows decomposition of WorldObjects into faces";
	}

	@Override
	public boolean canBeUsed() {
		return scene != null;
	}

	private static class FaceSink extends FaceOutput {

		public final List<List<VectorXYZ>> faces =
				new ArrayList<List<VectorXYZ>>();

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
	protected void fillTarget(JOGLOutput target) {

		FaceSink faceSink = new FaceSink();
		faceSink.outputScene(scene);

		for (List<VectorXYZ> face : faceSink.faces) {
			target.drawLineLoop(BORDER_COLOR, 2, face);
		}

	}

}
