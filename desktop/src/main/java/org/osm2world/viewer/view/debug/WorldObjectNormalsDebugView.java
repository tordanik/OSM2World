package org.osm2world.viewer.view.debug;

import static org.osm2world.util.FaultTolerantIterationUtil.forEach;

import java.awt.*;

import org.osm2world.math.VectorXYZ;
import org.osm2world.output.OutputUtil;
import org.osm2world.output.common.Primitive;
import org.osm2world.output.common.material.Material;
import org.osm2world.output.common.material.Material.Interpolation;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.PrimitiveBuffer;

public class WorldObjectNormalsDebugView extends DebugView {

	private static final Color FLAT_NORMALS_COLOR = Color.YELLOW;
	private static final Color SMOOTH_NORMALS_COLOR = Color.ORANGE;

	@Override
	public String getDescription() {
		return "draws world object normals as arrows";
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	@Override
	protected void fillTarget(JOGLOutput target) {

		final PrimitiveBuffer primitiveBuffer = new PrimitiveBuffer();

		forEach(map.getWorldObjects(), w -> OutputUtil.renderObject(primitiveBuffer, w));

		for (Material material : primitiveBuffer .getMaterials()) {

			Color color = material.getInterpolation() == Interpolation.FLAT ?
					FLAT_NORMALS_COLOR : SMOOTH_NORMALS_COLOR;

			for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {

				for (int i = 0; i < primitive.vertices.size(); i++) {
					VectorXYZ v = primitive.vertices.get(i);
					VectorXYZ n = primitive.normals.get(i);
					if (n != null) {
						drawArrow(target, color, 0.3f, v, v.add(n));
					}
				}

			}

		}

	}

}
