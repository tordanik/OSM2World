package org.osm2world.viewer.view.debug;

import java.awt.*;

import org.osm2world.math.VectorXYZ;
import org.osm2world.output.common.Primitive;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.PrimitiveBuffer;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;

public class NormalsDebugView extends StaticDebugView {

	private static final Color FLAT_NORMALS_COLOR = Color.YELLOW;
	private static final Color SMOOTH_NORMALS_COLOR = Color.ORANGE;

	public NormalsDebugView() {
		super("Normals", "draws world object normals as arrows");
	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		final PrimitiveBuffer primitiveBuffer = new PrimitiveBuffer();

		primitiveBuffer.outputScene(scene);

		for (Material material : primitiveBuffer .getMaterials()) {

			Color color = material.getInterpolation() == Interpolation.FLAT ?
					FLAT_NORMALS_COLOR : SMOOTH_NORMALS_COLOR;

			for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {

				for (int i = 0; i < primitive.vertices.size(); i++) {
					VectorXYZ v = primitive.vertices.get(i);
					VectorXYZ n = primitive.normals.get(i);
					if (n != null) {
						drawArrow(output, color, 0.3f, v, v.add(n));
					}
				}

			}

		}

	}

}
