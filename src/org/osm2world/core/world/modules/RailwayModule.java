package org.osm2world.core.world.modules;

import static com.google.common.collect.Iterables.any;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.util.Predicates.hasType;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseInt;

import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleGeometryUtil;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;

/**
 * adds rails to the world
 */
public class RailwayModule extends ConfigurableWorldModule {

	/** accepted values of the railway key */
	private static final List<String> RAILWAY_VALUES = asList(
			"rail", "light_rail", "tram", "subway", "disused");
	
	@Override
	public void applyTo(MapData grid) {
		
		for (MapWaySegment segment : grid.getMapWaySegments()) {
			if (segment.getTags().containsAny("railway", RAILWAY_VALUES)) {
				segment.addRepresentation(new Rail(segment));
			}
		}
		
		//TODO: the following for loop is copied from water module and should be in a common superclass
		for (MapNode node : grid.getMapNodes()) {
			
			int connectedRails = 0;
			
			for (MapWaySegment line : node.getConnectedWaySegments()) {
				if (any(line.getRepresentations(), hasType(Rail.class))) {
					connectedRails += 1;
				}
			}
			
			if (connectedRails > 2) {
				// node.addRepresentation(new RailJunction(node));
				// TODO: reactivate after implementing proper rendering for rail junctions
			}
			
		}
		
	}
	
	private static class Rail extends AbstractNetworkWaySegmentWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
		
		private static final int DEFAULT_GAUGE_MM = 1435;
	
		/** by how much the ballast goes beyond the ends of the sleeper (on each side) */
		private static final float GROUND_EXTRA_WIDTH = 0.2f;
		
		/** by how much the sleeper goes beyond the rail (on each side) */
		private static final float SLEEPER_EXTRA_WIDTH = 0.5f;
		
		private static final float SLEEPER_LENGTH = 0.26f;
		private static final float SLEEPER_HEIGHT = 0.16f * 0.4f; //extra factor to model sinking into the ballast
		
		private static final float SLEEPER_DISTANCE = 0.6f + SLEEPER_LENGTH;
		
		private static final float RAIL_HEAD_WIDTH = 0.067f; //must match RAIL_SHAPE
		private static final List<VectorXYZ> RAIL_SHAPE = asList(
			new VectorXYZ(-0.45f, 0, 0), new VectorXYZ(-0.1f, 0.1f, 0),
			new VectorXYZ(-0.1f, 0.5f, 0), new VectorXYZ(-0.25f, 0.55f, 0),
			new VectorXYZ(-0.25f, 0.75f, 0), new VectorXYZ(+0.25f, 0.75f, 0),
			new VectorXYZ(+0.25f, 0.55f, 0), new VectorXYZ(+0.1f, 0.5f, 0),
			new VectorXYZ(+0.1f, 0.1f, 0), new VectorXYZ(+0.45f, 0, 0)
		);
		
		static {
			for (int i=0; i < RAIL_SHAPE.size(); i++) {
				VectorXYZ v = RAIL_SHAPE.get(i);
				v = v.mult(0.1117f);
				v = v.y(v.y + SLEEPER_HEIGHT);
				RAIL_SHAPE.set(i, v);
			}
		}
		
		final float gaugeMeters;
		final float railDist;
		
		final float sleeperWidth;
		final float groundWidth;
		
		public Rail(MapWaySegment segment) {
			
			super(segment);
			
			gaugeMeters = parseInt(segment.getTags(), DEFAULT_GAUGE_MM, "gauge") / 1000.0f;
			railDist = gaugeMeters + 2 * (0.5f * RAIL_HEAD_WIDTH);
			
			sleeperWidth = gaugeMeters + 2 * RAIL_HEAD_WIDTH + 2 * SLEEPER_EXTRA_WIDTH;
			groundWidth = sleeperWidth + 2 * GROUND_EXTRA_WIDTH;
			
		}

		@Override
		public GroundState getGroundState() {
			
			if (segment.getTags().contains("railway", "subway")
					&& !segment.getTags().contains("tunnel", "no")){
				return GroundState.BELOW;
			}
			else if ( segment.getTags().contains("tunnel", "yes"))
			{
				return GroundState.BELOW;
			}
			
			return super.getGroundState();
			
		}
		
		@Override
		public void renderTo(Target<?> target) {

			/* draw ground */
			
			List<VectorXYZ> groundVs = WorldModuleGeometryUtil.createTriangleStripBetween(
					getOutline(false), getOutline(true));
			
			target.drawTriangleStrip(Materials.RAIL_BALLAST_DEFAULT, groundVs,
					texCoordLists(groundVs, Materials.RAIL_BALLAST_DEFAULT, GLOBAL_X_Z));
			
			
			/* draw rails */

			@SuppressWarnings("unchecked")
			List<VectorXYZ>[] railLines = new List[2];
			
			railLines[0] = WorldModuleGeometryUtil.createLineBetween(
					getOutline(false), getOutline(true),
					((groundWidth - railDist) / groundWidth) / 2);

			railLines[1] = WorldModuleGeometryUtil.createLineBetween(
					getOutline(false), getOutline(true),
					1 - ((groundWidth - railDist) / groundWidth) / 2);

			for (List<VectorXYZ> railLine : railLines) {
				
				List<List<VectorXYZ>> stripVectors =
					WorldModuleGeometryUtil.createShapeExtrusionAlong(
					RAIL_SHAPE, railLine,
					Collections.nCopies(railLine.size(), VectorXYZ.Y_UNIT));
					
				for (List<VectorXYZ> stripVector : stripVectors) {
					target.drawTriangleStrip(Materials.RAIL_DEFAULT, stripVector, null);
				}
			
			}
			
			
			/* draw railway ties/sleepers */
						
			List<VectorXYZ> sleeperPositions = equallyDistributePointsAlong(
					SLEEPER_DISTANCE, false, getCenterline());
			
			for (VectorXYZ sleeperPosition : sleeperPositions) {
				
				target.drawBox(Materials.RAIL_SLEEPER_DEFAULT,
						sleeperPosition, segment.getDirection(),
						SLEEPER_HEIGHT, sleeperWidth, SLEEPER_LENGTH);
				
			}
			
		}

		@Override
		public float getWidth() {
			return groundWidth;
		}
		
	}
	
	public static class RailJunction
		extends JunctionNodeWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
		
		public RailJunction(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			//TODO (code duplication): copied from RoadModule
			GroundState currentGroundState = null;
			checkEachLine: {
				for (MapWaySegment line : this.node.getConnectedWaySegments()) {
					if (line.getPrimaryRepresentation() == null) continue;
					GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
					if (currentGroundState == null) {
						currentGroundState = lineGroundState;
					} else if (currentGroundState != lineGroundState) {
						currentGroundState = GroundState.ON;
						break checkEachLine;
					}
				}
			}
			return currentGroundState;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			if (getOutlinePolygon() == null) return;
			
			/* draw ground */

			List<VectorXYZ> vectors = getOutlinePolygon().getVertexLoop();

			Material material = Materials.RAIL_BALLAST_DEFAULT;
			
			target.drawConvexPolygon(material, vectors,
					texCoordLists(vectors, material, GLOBAL_X_Z));

			/* draw connection between each pair of rails */

			/* TODO: use node.getConnectedLines() instead?
			 * (allows access to information from there,
			 *  such as getOutline!)
			 */

			for (int i=0; i<cutCenters.size(); i++) {
				for (int j=0; j<i; j++) {

					/* connect those rails with an obtuse angle between them */


				}
			}
			
		}

	}
	
}
