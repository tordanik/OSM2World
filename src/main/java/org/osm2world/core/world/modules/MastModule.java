package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.ExtrudeOption.END_CAP;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.EnumSet;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.model.LegacyModel;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

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

	public static class MobilePhoneMast extends NoOutlineNodeWorldObject {

		public MobilePhoneMast(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target target) {

			double height = parseHeight(node.getTags(), 7);
			double radiusBottom = height / 40;
			double radiusTop = radiusBottom * 0.8;

			target.drawExtrudedShape(CONCRETE, new CircleXZ(NULL_VECTOR, 1), asList(getBase(), getBase().addY(height)),
					null, asList(radiusBottom, radiusTop), null, EnumSet.of(END_CAP));

			//TODO: proper ModelTarget/instancing support

			for (double angleRad : asList(PI, 0.3 * PI, 1.5 * PI)) {
				VectorXYZ pos = getBase().addY(height - 0.6);
				pos = pos.add(VectorXZ.fromAngle(angleRad).mult(radiusTop));
				MOBILE_PHONE_ANTENNA_MODEL.render(target, pos, angleRad, null, null, null);
			}

		}

	}

	private static final Model MOBILE_PHONE_ANTENNA_MODEL = new LegacyModel() {

		@Override
		public void render(Target target, VectorXYZ position, double direction, Double height, Double width,
				Double length) {

			VectorXZ faceDirection = VectorXZ.fromAngle(direction);
			double antennaHeight = 1.0;

			VectorXYZ antennaCenter = position.add(faceDirection.mult(0.2));

			/* draw a connections between the antenna box and the pole */

			for (double relativeHeight : asList(-0.7, +0.7)) {
				target.drawExtrudedShape(STEEL, new CircleXZ(NULL_VECTOR, 0.025),
						asList(position.addY(relativeHeight * antennaHeight / 2),
								antennaCenter.addY(relativeHeight * antennaHeight / 2)),
						nCopies(2, Y_UNIT), null, null, null);
			}

			/* draw the antenna box itself */

			VectorXYZ antennaBottom = antennaCenter.addY(-antennaHeight / 2);
			target.drawBox(PLASTIC, antennaBottom, faceDirection, antennaHeight, 0.2, 0.08);

		}

	};

}
