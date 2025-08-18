package org.osm2world.viewer.view.debug;

import org.osm2world.math.shapes.LineSegmentXYZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.mesh.Mesh;

/**
 * shows face edges in {@link org.osm2world.scene.mesh.Geometry}s
 */
public class EdgeDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows edges of WorldObject geometries";
	}

	@Override
	protected void fillTarget(JOGLOutput target) {

		for (Mesh mesh : scene.getMeshes()) {
			for (LineSegmentXYZ edge : mesh.geometry.asTriangles().edges()) {
				target.drawLineLoop(mesh.material.getColor(), 2, edge.vertices());
			}
		}

	}

}
