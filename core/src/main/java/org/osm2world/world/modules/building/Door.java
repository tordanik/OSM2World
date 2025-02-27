package org.osm2world.world.modules.building;

import static java.util.Arrays.asList;
import static org.osm2world.math.algorithms.GeometryUtil.interpolateBetween;
import static org.osm2world.scene.material.Materials.*;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.scene.texcoord.TexCoordUtil.mirroredHorizontally;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;

import java.util.List;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonXYZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.scene.material.Material;

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
	public double insetDistance() {
		return parameters.inset;
	}

	@Override
	public void renderTo(CommonTarget target, WallSurface surface) {

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
				ConversionLog.warn("Unsupported number of door wings: " + parameters.numberOfWings);
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

	public static boolean isDoorNode(MapNode n) {
		return n.getTags().containsKey("entrance") || n.getTags().containsKey("door");
	}

}
