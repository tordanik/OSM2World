package org.osm2world.viewer.view.debug;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.FaceOutput;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.world.data.WorldObject;

/**
 * shows decomposition of {@link WorldObject}s into faces
 * as they would be written to any {@link FaceOutput}
 */
public class FaceDebugView extends StaticDebugView {

	private static final Color BORDER_COLOR = new Color(0, 0, 1.0f);

	public FaceDebugView() {
		super("Faces", "shows decomposition of WorldObjects into faces");
	}

	private static class FaceSink extends FaceOutput {

		public final List<List<VectorXYZ>> faces = new ArrayList<>();

		@Override
		public boolean reconstructFaces() {
			return true;
		}

		@Override
		public void drawFace(MaterialOrRef material, List<VectorXYZ> vs,
				List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists) {
			faces.add(vs);
		}

	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		FaceSink faceSink = new FaceSink();
		faceSink.outputScene(scene);

		for (List<VectorXYZ> face : faceSink.faces) {
			output.drawLineLoop(BORDER_COLOR, 2, face);
		}

	}

}
