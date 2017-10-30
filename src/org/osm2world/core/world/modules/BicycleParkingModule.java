package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createShapeExtrusionAlong;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.data.NoOutlineWaySegmentWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * places bicycle parking facilities in the world
 */
public class BicycleParkingModule extends AbstractModule {
	
	@Override
	protected void applyToWaySegment(MapWaySegment segment) {
		
		if (segment.getTags().contains("amenity", "bicycle_parking")) {
			
			if (segment.getTags().contains("bicycle_parking", "stands")) {
				segment.addRepresentation(new BicycleStands(segment));
			}
			
		}
		
	}
	
	private static final class BicycleStands extends NoOutlineWaySegmentWorldObject
			implements RenderableToAllTargets {
		
		private static final List<VectorXYZ> STAND_SHAPE = asList(
				new VectorXYZ(-0.02f, -0.05f, 0), new VectorXYZ(-0.02f,     0f, 0),
				new VectorXYZ(+0.02f,     0f, 0), new VectorXYZ(+0.02f, -0.05f, 0));
		
		private static final double STAND_DEFAULT_LENGTH = 1.0;
		private static final float STAND_DEFAULT_HEIGHT = 0.7f;
		
		private List<EleConnector> standConnectors;
		
		private final double height;
		
		private BicycleStands(MapWaySegment segment) {
			
			super(segment);
			
			height = parseHeight(segment.getTags(), STAND_DEFAULT_HEIGHT);
			
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON; //TODO better ground state calculations
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
		
			//replaces the default implementation (connectors at start and end of the segment)
			//with one that sets up an ele connector for each stand
			
			if (standConnectors == null) {
				
				List<VectorXZ> standLocations = equallyDistributePointsAlong(1.0, true,
						segment.getStartNode().getPos(), segment.getEndNode().getPos());
				
				//TODO: take capacity into account, but that requires having access to the entire way
				
				standConnectors = new ArrayList<EleConnector>();
				
				for (VectorXZ standLocation : standLocations) {
					standConnectors.add(new EleConnector(standLocation, null, getGroundState()));
				}
				
			}
			
			return standConnectors;
			
		}
		
		@Override
		public void renderTo(Target<?> target) {

			VectorXZ start = segment.getStartNode().getPos();
			VectorXZ end = segment.getEndNode().getPos();
			
			VectorXZ direction = end.subtract(start).rightNormal();
			VectorXYZ toFront = direction.mult(STAND_DEFAULT_LENGTH / 2).xyz(0);
			
			for (EleConnector standConnector : standConnectors) {
				
				List<VectorXYZ> path = new ArrayList<VectorXYZ>();
				
				path.add(standConnector.getPosXYZ().add(toFront));
				path.add(standConnector.getPosXYZ().add(toFront).addY(height*0.95));
				path.add(standConnector.getPosXYZ().add(toFront.mult(0.95)).addY(height));
				path.add(standConnector.getPosXYZ().add(toFront.invert().mult(0.95)).addY(height));
				path.add(standConnector.getPosXYZ().add(toFront.invert()).addY(height*0.95));
				path.add(standConnector.getPosXYZ().add(toFront.invert()));
				
				List<VectorXYZ> upVectors = new ArrayList<VectorXYZ>();

				upVectors.add(toFront.normalize());
				upVectors.add(upVectors.get(0));
				upVectors.add(Y_UNIT);
				upVectors.add(upVectors.get(2));
				upVectors.add(toFront.invert().normalize());
				upVectors.add(upVectors.get(4));
				
				List<List<VectorXYZ>> strips = createShapeExtrusionAlong(
						STAND_SHAPE, path, upVectors);
				
				for (List<VectorXYZ> strip : strips) {
					target.drawTriangleStrip(STEEL, strip,
							texCoordLists(strip, STEEL, STRIP_WALL));
				}
				
			}
			
		}
		
	}
	
}
