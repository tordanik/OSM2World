package org.osm2world.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.output.common.ExtrudeOption.END_CAP;
import static org.osm2world.scene.material.Materials.*;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.EnumSet;
import java.util.List;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.CircleXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.scene.model.ProceduralModel;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/** places various towers and masts (currently only freestanding mobile phone communication masts) */
public class MastModule extends AbstractModule {

	@Override
	protected void applyToNode(MapNode node) {

		if (node.getTags().contains("man_made", "mast")
				&& node.getTags().contains("tower:type", "communication")
				&& node.getTags().contains("communication:mobile_phone", "yes")) {
			node.addRepresentation(new MobilePhoneMast(node));
		}

	}

	public static class MobilePhoneMast extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public MobilePhoneMast(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			double height = parseHeight(node.getTags(), 7);
			double radiusBottom = height / 40;
			double radiusTop = radiusBottom * 0.8;

			target.drawExtrudedShape(CONCRETE, new CircleXZ(NULL_VECTOR, 1), asList(getBase(), getBase().addY(height)),
					null, asList(radiusBottom, radiusTop), EnumSet.of(END_CAP));

			//TODO: proper ModelTarget/instancing support

			for (double angleRad : asList(PI, 0.3 * PI, 1.5 * PI)) {
				VectorXYZ pos = getBase().addY(height - 0.6);
				pos = pos.add(VectorXZ.fromAngle(angleRad).mult(radiusTop));
				target.addSubModel(new ModelInstance(MOBILE_PHONE_ANTENNA_MODEL, new InstanceParameters(pos, angleRad)));
			}

		}

	}

	private static final Model MOBILE_PHONE_ANTENNA_MODEL = new ProceduralModel() {

		@Override
		public void render(CommonTarget target, InstanceParameters params) {

			VectorXZ faceDirection = VectorXZ.fromAngle(params.direction());
			double antennaHeight = 1.0;

			VectorXYZ antennaCenter = params.position().add(faceDirection.mult(0.2));

			/* draw a connections between the antenna box and the pole */

			for (double relativeHeight : asList(-0.7, +0.7)) {
				target.drawExtrudedShape(STEEL, new CircleXZ(NULL_VECTOR, 0.025),
						List.of(params.position().addY(relativeHeight * antennaHeight / 2),
								antennaCenter.addY(relativeHeight * antennaHeight / 2)),
						nCopies(2, Y_UNIT), null, null);
			}

			/* draw the antenna box itself */

			VectorXYZ antennaBottom = antennaCenter.addY(-antennaHeight / 2);
			target.drawBox(PLASTIC, antennaBottom, faceDirection, antennaHeight, 0.2, 0.08);

		}

	};

}
