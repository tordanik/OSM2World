package org.osm2world.core.world.modules.building;

import static java.util.Arrays.asList;
import static org.osm2world.core.target.common.material.Materials.SINGLE_WINDOW;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;

import java.util.List;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;

class TexturedWindow implements Window {

	/** position on a wall surface */
	private final VectorXZ position;

	private final WindowParameters params;

	public TexturedWindow(VectorXZ position, WindowParameters params) {
		this.position = position;
		this.params = params;
	}

	@Override
	public SimplePolygonXZ outline() {

		return new SimplePolygonXZ(asList(
				position.add(new VectorXZ(-params.overallProperties.width/2, 0)),
				position.add(new VectorXZ(+params.overallProperties.width/2, 0)),
				position.add(new VectorXZ(+params.overallProperties.width/2, +params.overallProperties.height)),
				position.add(new VectorXZ(-params.overallProperties.width/2, +params.overallProperties.height)),
				position.add(new VectorXZ(-params.overallProperties.width/2, 0))));

	}

	@Override
	public Double insetDistance() {
		return 0.10;
	}

	@Override
	public void renderTo(Target target, WallSurface surface) {

		PolygonXYZ frontOutline = surface.convertTo3D(outline());

		VectorXYZ windowNormal = surface.normalAt(outline().getCentroid());
		VectorXYZ toBack = windowNormal.mult(insetDistance());
		PolygonXYZ backOutline = frontOutline.add(toBack);

		/* draw the window itself */

		VectorXYZ bottomLeft = backOutline.verticesNoDup().get(0);
		VectorXYZ bottomRight = backOutline.verticesNoDup().get(1);
		VectorXYZ topLeft = backOutline.verticesNoDup().get(3);
		VectorXYZ topRight = backOutline.verticesNoDup().get(2);

		List<VectorXYZ> vsWindow = asList(topLeft, bottomLeft, topRight, bottomRight);

		target.drawTriangleStrip(SINGLE_WINDOW, vsWindow,
				texCoordLists(vsWindow, SINGLE_WINDOW, STRIP_FIT));

	}

}