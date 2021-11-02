package org.osm2world.core.world.modules.building;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.interpolateBetween;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.*;

import java.util.List;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;

public class Door implements WallElement {

	/** position on a wall surface */
	private final VectorXZ position;

	private final DoorParameters parameters;

	public Door(VectorXZ position, DoorParameters params) {
		this.position = position;
		this.parameters = params;
	}

	@Override
	public SimplePolygonXZ outline() {

		return new SimplePolygonXZ(asList(
				position.add(new VectorXZ(-parameters.width/2, 0)),
				position.add(new VectorXZ(+parameters.width/2, 0)),
				position.add(new VectorXZ(+parameters.width/2, +parameters.height)),
				position.add(new VectorXZ(-parameters.width/2, +parameters.height)),
				position.add(new VectorXZ(-parameters.width/2, 0))));

	}

	@Override
	public Double insetDistance() {
		return 0.10;
	}

	@Override
	public void renderTo(Target target, WallSurface surface) {

		Material doorMaterial = ENTRANCE_DEFAULT;

		switch (parameters.type) {
		case "no":
			doorMaterial = VOID;
			break;
		case "overhead":
			doorMaterial = GARAGE_DOOR;
			break;
		}

		doorMaterial = doorMaterial.withColor(parameters.color);

		PolygonXYZ frontOutline = surface.convertTo3D(outline());

		VectorXYZ doorNormal = surface.normalAt(outline().getCentroid());
		VectorXYZ toBack = doorNormal.mult(-insetDistance());
		PolygonXYZ backOutline = frontOutline.add(toBack);

		/* draw the door itself */

		VectorXYZ bottomLeft = backOutline.verticesNoDup().get(0);
		VectorXYZ bottomRight = backOutline.verticesNoDup().get(1);
		VectorXYZ topLeft = backOutline.verticesNoDup().get(3);
		VectorXYZ topRight = backOutline.verticesNoDup().get(2);

		if (parameters.numberOfWings == 1 || !"hinged".equals(parameters.type)) {

			List<VectorXYZ> vsDoor = asList(topLeft, bottomLeft, topRight, bottomRight);
			target.drawTriangleStrip(doorMaterial, vsDoor,
					texCoordLists(vsDoor, doorMaterial, STRIP_FIT));

		} else {

			if (parameters.numberOfWings > 2) {
				System.err.println("Warning: Unsupported number of door wings: " + parameters.numberOfWings);
			}

			VectorXYZ bottomCenter = interpolateBetween(bottomLeft, bottomRight, 0.5);
			VectorXYZ topCenter = interpolateBetween(topLeft, topRight, 0.5);

			List<VectorXYZ> vsDoorLeft = asList(topLeft, bottomLeft, topCenter, bottomCenter);
			target.drawTriangleStrip(doorMaterial, vsDoorLeft,
					texCoordLists(vsDoorLeft, doorMaterial, STRIP_FIT));

			List<VectorXYZ> vsDoorRight = asList(topCenter, bottomCenter, topRight, bottomRight);
			target.drawTriangleStrip(doorMaterial, vsDoorRight,
					texCoordLists(vsDoorRight, doorMaterial, mirroredHorizontally(STRIP_FIT)));

		}

	}

}
