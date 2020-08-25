package org.osm2world.core.world.modules;

import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * places bicycle parking facilities in the world
 */
public class BicycleParkingModule extends AbstractModule {

	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("amenity", "bicycle_parking")) {

			if (area.getTags().contains("bicycle_parking", "stands")) {
				area.addRepresentation(new BicycleStands(area));
			}

		}

	}

	private static final class BicycleStands extends AbstractAreaWorldObject {

		private static final ShapeXZ STAND_SHAPE = new CircleXZ(NULL_VECTOR, 0.02f);

		private static final double STAND_DEFAULT_LENGTH = 1.0;
		private static final float STAND_DEFAULT_HEIGHT = 0.7f;

		private EleConnectorGroup standConnectors;

		private final double height;

		protected BicycleStands(MapArea area) {
			super(area);
			height = parseHeight(area.getTags(), STAND_DEFAULT_HEIGHT);
		}

//		@Override
//		public GroundState getGroundState() {
//			return GroundState.ON; //TODO better ground state calculations
//		}

		protected Material getStandMaterial() {
			return STEEL;
		}

		@Override
		public EleConnectorGroup getEleConnectors() {

			//replaces the default implementation (connectors at start and end of the segment)
			//with one that sets up an ele connector for each stand

			if (standConnectors == null) {

				SimplePolygonXZ bbox = area.getOuterPolygon().minimumRotatedBoundingBox();
				List<VectorXZ> standLocations;
				VectorXZ midpoint1, midpoint2;

				// place the stand an eighth of the distance from either end of the bounded box
				// ensures the stand is within the box and passing through the centre
				if (bbox.getVertex(2).distanceTo(bbox.getVertex(1)) > bbox.getVertex(1)
					.distanceTo(bbox.getVertex(0))) {
					midpoint1 = bbox.getVertex(0).add(bbox.getVertex(1)).mult(0.5);
					midpoint2 = bbox.getVertex(2).add(bbox.getVertex(3)).mult(0.5);
				} else {
					midpoint1 = bbox.getVertex(1).add(bbox.getVertex(2)).mult(0.5);
					midpoint2 = bbox.getVertex(3).add(bbox.getVertex(0)).mult(0.5);
				}
				standLocations = equallyDistributePointsAlong(1.0, true,
					midpoint1.add(midpoint2.subtract(midpoint1).mult(0.125)),
					midpoint2.add(midpoint1.subtract(midpoint2).mult(0.125)));

				//TODO: take capacity into account

				standConnectors = new EleConnectorGroup();

				for (VectorXZ standLocation : standLocations) {
					if (area.getPolygon().contains(standLocation)) {
						standConnectors
							.add(new EleConnector(standLocation, null, getGroundState()));
					}
				}
			}

			return standConnectors;

		}

		@Override
		public void renderTo(Target target) {

			SimplePolygonXZ bbox = area.getOuterPolygon().minimumRotatedBoundingBox();

			VectorXZ direction = bbox.getVertex(1).subtract(bbox.getVertex(0));

			//Determining which side of the bounded box is longer; main direction of the stand
			if (bbox.getVertex(2).distanceTo(bbox.getVertex(1)) > direction.length()) {
				direction = bbox.getVertex(2).subtract(bbox.getVertex(1));
			}

			direction = direction.rightNormal();
			VectorXYZ toFront = direction.mult(STAND_DEFAULT_LENGTH / 2).xyz(0);

			for (EleConnector standConnector : standConnectors) {

				List<VectorXYZ> path = new ArrayList<VectorXYZ>();

				path.add(standConnector.getPosXYZ().add(toFront));
				path.add(standConnector.getPosXYZ().add(toFront).addY(height * 0.95));
				path.add(standConnector.getPosXYZ().add(toFront.mult(0.95)).addY(height));
				path.add(standConnector.getPosXYZ().add(toFront.invert().mult(0.95)).addY(height));
				path.add(standConnector.getPosXYZ().add(toFront.invert()).addY(height * 0.95));
				path.add(standConnector.getPosXYZ().add(toFront.invert()));

				List<VectorXYZ> upVectors = new ArrayList<VectorXYZ>();

				upVectors.add(toFront.normalize());
				upVectors.add(upVectors.get(0));
				upVectors.add(Y_UNIT);
				upVectors.add(upVectors.get(2));
				upVectors.add(toFront.invert().normalize());
				upVectors.add(upVectors.get(4));

				target.drawExtrudedShape(getStandMaterial(), STAND_SHAPE, path, upVectors,
					null, null, null);

			}
		}
	}
}
