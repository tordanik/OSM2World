package org.osm2world.viewer.view.debug;

import org.osm2world.math.shapes.LineSegmentXYZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.mesh.Mesh;

/**
 * shows face edges in {@link org.osm2world.scene.mesh.Geometry}s
 */
public class EdgeDebugView extends StaticDebugView {

	public EdgeDebugView() {
		super("Edges", "shows edges of WorldObject geometries");
	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		for (Mesh mesh : scene.getMeshes(config)) {
			for (LineSegmentXYZ edge : mesh.geometry.asTriangles().edges()) {
				output.drawLineLoop(mesh.material.color(), 2, edge.vertices());
			}
		}

	}

}
