package org.osm2world.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.output.common.ExtrudeOption.END_CAP;
import static org.osm2world.scene.material.DefaultMaterials.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.CircleXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.common.ExtrudeOption;
import org.osm2world.scene.material.Material;
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

	public class MobilePhoneMast extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public MobilePhoneMast(MapNode node) {
			super(node, true);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			VectorXYZ base = getBase();
			double height = getHeight(7);

			double radiusBottom = height / 40;
			double radiusTop = radiusBottom * 0.8;

			List<VectorXYZ> path = asList(base, base.addY(height));
			Set<ExtrudeOption> options = EnumSet.of(END_CAP);
			target.drawExtrudedShape(CONCRETE.get(config), new CircleXZ(VectorXZ.NULL_VECTOR, 1), path, null, asList(radiusBottom, radiusTop), options);

			Model antennaModel = new MobilePhoneAntennaModel(config);

			for (double angleRad : asList(PI, 0.3 * PI, 1.5 * PI)) {
				VectorXYZ pos = base.addY(height - 0.6);
				pos = pos.add(VectorXZ.fromAngle(angleRad).mult(radiusTop));
				target.addSubModel(new ModelInstance(antennaModel, new InstanceParameters(pos, angleRad)));
			}

		}

	}

	private record MobilePhoneAntennaModel(
			Material antennaMaterial,
			Material connectionMaterial
	) implements ProceduralModel {

		public MobilePhoneAntennaModel(O2WConfig config) {
			this(PLASTIC.get(config), STEEL.get(config));
		}

		@Override
		public void render(CommonTarget target, InstanceParameters params) {

			VectorXZ faceDirection = VectorXZ.fromAngle(params.direction());
			double antennaHeight = 1.0;

			VectorXYZ antennaCenter = params.position().add(faceDirection.mult(0.2));

			/* draw connections between the antenna box and the pole */

			for (double relativeHeight : asList(-0.7, +0.7)) {
				List<VectorXYZ> path = List.of(params.position().addY(relativeHeight * antennaHeight / 2),
						antennaCenter.addY(relativeHeight * antennaHeight / 2));
				target.drawExtrudedShape(connectionMaterial, new CircleXZ(VectorXZ.NULL_VECTOR, 0.025), path, nCopies(2, Y_UNIT), null, null);
			}

			/* draw the antenna box itself */

			VectorXYZ antennaBottom = antennaCenter.addY(-antennaHeight / 2);
			target.drawBox(antennaMaterial, antennaBottom, faceDirection, antennaHeight, 0.2, 0.08);

		}

	}

}
