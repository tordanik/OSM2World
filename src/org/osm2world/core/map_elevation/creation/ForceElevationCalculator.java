package org.osm2world.core.map_elevation.creation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionGrid;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.util.MinUtil;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class ForceElevationCalculator extends AdvancedAbstractElevationCalculator {

	public static final int CALCULATION_STEPS = 100;
	
	@Override
	protected ElevationDeterminationScenario createScenario(
			CellularTerrainElevation eleData) {
		return new ForceElevationDeterminationScenario(eleData);
	}
	
	protected static class ForceElevationDeterminationScenario
			extends AbstractElevationDeterminationScenario {

		protected final CellularTerrainElevation eleData;
		
		protected final Collection<Force> forces = new ArrayList<Force>();
		
		public ForceElevationDeterminationScenario(
				CellularTerrainElevation eleData) {
			this.eleData = eleData;
		}
		
		@Override
		protected void handleConstantElevation(
				ForceNode node, Float ele, boolean tagged) {
			forces.add(new NodeElevationForce(node, ele, tagged));
		}
		
		@Override
		protected void handleVerticalMinDistance(
				ForceNode lowerNode, ForceNode upperNode, double minDistance) {
			forces.add(new VerticalDistanceForce(lowerNode, upperNode, minDistance));
		}
		
		@Override
		protected void handleSameElevation(ForceNode node1, ForceNode node2) {
			forces.add(new SameEleForce(node1, node2));
		}
		
		/**
		 * simulates several abstract "forces" affecting the elevation of
		 * grid elements. Each force represents available information (such
		 * as the required clearing above roads - a force will push bridges
		 * away from the road if they are too low) or desirable characteristics
		 * of the grid's elevation (e.g. smooth curves - a straightening force
		 * will quickly increase for extreme angles).
		 * 
		 * Uses {@link #calculateStep(float)} for the individual calculation steps.
		 */
		public void calculate() {
			
			addConnectionForces();
			addNeighborshipForces();
			
			//FIXME: will be added each time if called multiple times!
			
			initializeElevations();
			
			/* perform calculations */
			
			for (int step=0; step < CALCULATION_STEPS; step++) {
				float forceScale = forceScaleForStep(step);
				calculateStep(forceScale);
			}

			writeResult();

		}

		/**
		 * create forces between all subsequent nodes on each line.
		 * This requires that all nodes on the line are present,
		 * so it cannot be done in addLine (missing intersection nodes).
		 */
		private void addConnectionForces() {
			
			for (final MapWaySegment line : lineMap.keySet()) {
				
				List<ForceNode> lineFNodes =
					new ArrayList<ForceNode>(lineMap.get(line));
				
				/* sort nodes by distance from line start node */
				
				//TODO: partial duplicate to LineElevationProfile method
				Collections.sort(lineFNodes, new Comparator<ForceNode>() {
					final VectorXZ start = line.getStartNode().getPos();
					@Override
					public int compare(ForceNode n1, ForceNode n2) {
						return Double.compare(
								VectorXZ.distanceSquared(n1.getPos(), start),
								VectorXZ.distanceSquared(n2.getPos(), start));
					}
				});
				
				/* extract incline information */
				
				Boolean directionUp = null;
				Float incline = null;
				
				TagGroup tags = line.getOsmWay().tags;
				if (tags.containsKey("incline")) {
					String inclineVal = tags.getValue("incline");
					if ("up".equals(inclineVal)) {
						directionUp = true;
					} else if ("down".equals(inclineVal)) {
						directionUp = false;
					} else {
						try {
							Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
							Matcher m = p.matcher(inclineVal);
							if (m.matches()) {
								incline = Float.parseFloat(m.group(1));
							}
						} catch (NumberFormatException e) {  }
					}
				}
				
				/* create connections for subsequent (as per ordering) node */
				
				for (int i = 0; i+1 < lineFNodes.size(); i++) {
					
					if (directionUp != null) {
						forces.add(new DirectionConnectionForce(
								lineFNodes.get(i),
								lineFNodes.get(i+1),
								directionUp
						));
						
					} else if (incline != null) {
						
						forces.add(new InclineConnectionForce(
								lineFNodes.get(i),
								lineFNodes.get(i+1),
								incline
						));
						
					} else {
						
						forces.add(new UnknownInclineConnectionForce(
								lineFNodes.get(i),
								lineFNodes.get(i+1)
						));
						
					}
				}
	
			}
		
		}
		

		/**
		 * create forces between all nodes depending on their position
		 * in the base terrain grid:
		 * <ul>
		 * <li> connect each TerrainPoint to its up to 4 grid neighbors
		 * <li> distribute other nodes to grid cells (quadrangle created by 4 TerrainPoints)
		 *      and connect them with other nodes from the same cell as well as the corner TerrainPoints
		 * </ul>
		 */
		private void addNeighborshipForces() {
			
			/* create IntersectionGrid for answering
			 * "nodes in this cell" queries faster */
			
			final IntersectionGrid speedupGrid = new IntersectionGrid(
					new AxisAlignedBoundingBoxXZ(
							eleData.getBoundaryPolygonXZ().getVertices()).pad(20),
							50, 50); //TODO (performance): choose appropriate cell size params
			
			for (TerrainElevationCell cell : eleData.getCells()) {
				speedupGrid.insert(cell);
			}
			
			//add nodes based on ways and areas
			
			for (MapWaySegment segment : lineMap.keySet()) {
				
				if (segment.getPrimaryRepresentation() != null &&
						segment.getPrimaryRepresentation().getGroundState() == GroundState.ON) {
					
					for (ForceNode fNode : lineMap.get(segment)) {
						speedupGrid.insert(fNode);
					}
					
				}
				
			}

			for (MapArea area : areaMap.keySet()) {
				
				if (area.getPrimaryRepresentation() != null &&
						area.getPrimaryRepresentation().getGroundState() == GroundState.ON) {
					
					for (ForceNode fNode : areaMap.get(area)) {
						speedupGrid.insert(fNode);
					}
					
				}
				
			}
			
			//add nodes that have their own representations
			for (MapNode node : nodeMap.keySet()) {
				
				if (node.getPrimaryRepresentation() != null
						&& node.getPrimaryRepresentation().getGroundState() == GroundState.ON) {
					
					ForceNode fNode = nodeMap.get(node);
					
					speedupGrid.insert(fNode);
					
				}
				
			}
			
			/* end of IntersectionGrid creation;
			 * begin creating the forces */
			
			for (TerrainElevationCell cell : eleData.getCells()) {
				
				/* forces between TerrainPoints:
				 * add two forces (for the left and upper cell border) */
				
				forces.add(new NeighborshipForce(
						terrainPointMap.get(cell.getTopLeft()),
						terrainPointMap.get(cell.getBottomLeft())));
				
				forces.add(new NeighborshipForce(
						terrainPointMap.get(cell.getTopLeft()),
						terrainPointMap.get(cell.getTopRight())));
				
				//TODO: right column and bottom row of cells need special treatment
				
				
				/* forces within grid cells */
				
				Set<ForceNode> forceNodesInCell = new HashSet<ForceNode>();
				
				Iterable<IntersectionTestObject> potentialNodes =
					Iterables.concat(speedupGrid.cellsFor(cell));
				
				for (IntersectionTestObject potentialNode : potentialNodes) {
					if (potentialNode instanceof ForceNode) {
						ForceNode node = (ForceNode) potentialNode;
						if (cell.getPolygonXZ().contains(node.getPos())) {
							forceNodesInCell.add(node);
						}
					}
				}
				
				/* add forces between the nodes within the cell and the cell corners */
				
				for (final ForceNode fNode : forceNodesInCell) {
					
					TerrainPoint closestTerrainPoint = MinUtil.min(
							cell.getTerrainPoints(),
							new Function<TerrainPoint, Double>() {
								@Override public Double apply(TerrainPoint p) {
									return VectorXZ.distance(fNode.getPos(), p.getPos());
								}
							});
					
					forces.add(new NeighborshipForce(fNode,
							terrainPointMap.get(closestTerrainPoint)));
					
				}
				
				/* add forces between nodes within the cell */
				
				//				for (int i = 0; i < forceNodesInCell.size(); i++) {
				//					for (int j = i+1; j < forceNodesInCell.size(); j++) {
				//
				//						forces.add(new NeighborshipForce(
				//								forceNodesInCell.get(i),
				//								forceNodesInCell.get(j)));
				//
				//					}
				//				}
				
			}
			
		}
		
		/**
		 * sets elevations for nodes with unknown (null) elevations.
		 * To do this, elevations are derived from nodes with known elevations
		 * that are connected via connection forces or neighborship forces.
		 */
		private void initializeElevations() {
			
			/* identify connections as a peparation */
			
			Multimap<ForceNode, ForceNode> connectedNodes =
				HashMultimap.create(forceNodes.size(), 6);
			
			for (Force force : forces) {
				if (force instanceof ConnectionForce) {
					
					ConnectionForce c = (ConnectionForce)force;
					
					connectedNodes.put(c.node1, c.node2);
					connectedNodes.put(c.node2, c.node1);
					
				}
			}
			
			/* initially, gather all nodes with known elevations */
			
			Set<ForceNode> nodesWithRecentEle = new HashSet<ForceNode>();
			
			for (ForceNode fNode : forceNodes) {
				if (fNode.getCurrentEle() != null) {
					nodesWithRecentEle.add(fNode);
				}
			}
			
			/* propagate elevations along connections to immediate neighbors
			 * and from/to terrain cell boundaries.
			 * Average is used if a node with unknown ele has multiple
			 * immediate neighbors with known ele. */
			
			while (!nodesWithRecentEle.isEmpty()) {
				
				Multimap<ForceNode, Float> suggestedEles =
					ArrayListMultimap.create();
				
				for (ForceNode fNode : nodesWithRecentEle) {
					for (ForceNode connectedNode : connectedNodes.get(fNode)) {
						
						if (connectedNode.getCurrentEle() == null) {
							
							suggestedEles.put(
									connectedNode, fNode.getCurrentEle());
							
						}
						
					}
				}
				
				nodesWithRecentEle = new HashSet<ForceNode>();
				
				for (ForceNode fNode : suggestedEles.keys()) {
					
					float eleSum = 0;
					
					for (float suggestedEle : suggestedEles.get(fNode)) {
						eleSum += suggestedEle;
					}
					
					fNode.setCurrentEle(eleSum /
							suggestedEles.get(fNode).size());
					
					nodesWithRecentEle.add(fNode);
					
				}
				
			}
			
			/* use 0 for everything that is still unknown */
			
			for (ForceNode fNode : forceNodes) {
				if (fNode.getCurrentEle() == null) {
					fNode.setCurrentEle(0);
				}
			}
			
		}
		
		private float forceScaleForStep(int step) {
			float reductionPerStep = 1f / (CALCULATION_STEPS + 1);
			return 1f - step * reductionPerStep;
		}
		
		/**
		 * performs a single step in the calculation of {@link #calculate()}
		 * by applying each force in {@link #forces}.
		 * @param forceScale  factor for force effects, will be reduced for later steps
		 *                    in order to achieve resonable convergence to a final state
		 */
		private void calculateStep(float forceScale) {

			//TODO (performance): preparation can be parallelized, as force nodes aren't modified yet
			for (Force force : forces) {
				force.prepare(forceScale);
			}
			
			for (Force force : forces) {
				force.apply();
			}

		}
		
		protected static interface Force {
			
			/**
			 * Calculates the effect of the next application,
			 * but doesn't modify the {@link ForceNode}s yet.
			 * Must be called once before each {@link #apply()} call.
			 * This method has been introduced to remove effects of the
			 * force order (otherwise, later forces could "react"
			 * to the previous ones).
			 * 
			 * @param forceScale  scale factor for the force's strength
			 */
			public void prepare(float forceScale);
			
			/**
			 * applies this force to the nodes affected by it.
			 */
			public void apply();
			
		}

		/**
		 * force that influences the elevation of two connected nodes
		 * in a way that fits incline information.
		 * There are different behaviors depending on available information,
		 * represented the concrete subclasses.
		 */
		protected static abstract class ConnectionForce implements Force {

			protected final ForceNode node1, node2;
			
			/** effect used in {@link #apply()} */
			protected Float effect = null;
			
			public ConnectionForce(ForceNode node1, ForceNode node2) {
				this.node1 = node1;
				this.node2 = node2;
			}

			@Override
			public void apply() {
				if (effect != null) {
					node1.changeCurrentEle(+effect/2);
					node2.changeCurrentEle(-effect/2);
					effect = null;
				}
			}

		}

		/**
		 * Connection to be used if there is no incline information.
		 * A weak force will try to bring the nodes to the same elevation.
		 */
		protected static final class UnknownInclineConnectionForce extends ConnectionForce {
			
			private static final float baseStrengthFactor = 0.1f;
			
			public UnknownInclineConnectionForce(ForceNode node1, ForceNode node2) {
				super(node1, node2);
			}

			@Override
			public void prepare(float forceScale) {
				effect = Math.abs(node1.getCurrentEle() - node2.getCurrentEle());
				effect *= baseStrengthFactor;
				effect *= forceScale;
				if (node1.getCurrentEle() > node2.getCurrentEle()) {
					effect = -effect;
				}
			}
			
		}

		/**
		 * Connection to be used if only incline=up/down is given.
		 * A strong force will try to create at least a little incline,
		 * but will not try to increase the incline beyond that.
		 */
		protected static final class DirectionConnectionForce extends ConnectionForce {
			
			private static final float DESIRED_INCLINE = 0.05f;

			private final boolean up;
			private float eleDiffLimit;
			
			public DirectionConnectionForce(ForceNode node1,
					ForceNode node2, boolean up) {

				super(node1, node2);
				
				this.up = up;
				
				double nodeDist = node2.getPos().subtract(node1.getPos()).length();
				eleDiffLimit = (float)nodeDist * DESIRED_INCLINE;
				
			}

			@Override
			public void prepare(float forceScale) {
								
				float eleDiff = node2.getCurrentEle() - node1.getCurrentEle();
				
				effect = 0f;
				if (up && eleDiff < eleDiffLimit) {
					effect = eleDiff - eleDiffLimit;
				} else if (!up && eleDiff > -eleDiffLimit) {
					effect = eleDiff + eleDiffLimit;
				}
				effect *= forceScale;
				
			}
			
		}

		/**
		 * Connection to be used if there is explicit incline information.
		 * A strong force will try to bring the nodes to appropriate elevations.
		 * (As the tagged incline is "maximum practical incline",
		 * a somewhat lower incline is accepted.)
		 */
		protected static final class InclineConnectionForce extends ConnectionForce {
			
			private final float incline;
			
			public InclineConnectionForce(ForceNode node1,
					ForceNode node2, float incline) {
				super(node1, node2);
				this.incline = incline;
			}

			@Override
			public void prepare(float forceScale) {
				effect = 0f;
				//TODO: implement
			}
			
		}
		
		/**
		 * force that tries to keep two nodes at the same elevation
		 */
		protected static final class SameEleForce extends ConnectionForce {
			
			public SameEleForce(ForceNode node1, ForceNode node2) {
				super(node1, node2);
			}
			
			@Override
			public void prepare(float forceScale) {
				effect = Math.abs(node1.getCurrentEle() - node2.getCurrentEle());
				effect *= forceScale;
				if (node1.getCurrentEle() > node2.getCurrentEle()) {
					effect = -effect;
				}
			}
			
		}
		
		/**
		 * force that tries to reduce extreme angles.
		 * If the center node is not on the interpolation between
		 * the two adjacent lines, a force will try to move it there.
		 */
		protected static final class AngleForce implements Force {

			protected final ForceNode node, neighbor1, neighbor2;
						
			public AngleForce(ForceNode node, ForceNode neighbor1,
					ForceNode neighbor2) {
				this.node = node;
				this.neighbor1 = neighbor1;
				this.neighbor2 = neighbor2;
			}
			
			/** effect used in {@link #apply()} */
			protected Double effect = null;
			
			@Override
			public void prepare(float forceScale) {
				double interpolatedEle = GeometryUtil.interpolateElevation(
					node.getPos(), neighbor1.getCurrentXYZ(), neighbor2.getCurrentXYZ()).y;
				effect = interpolatedEle  - node.getCurrentEle();
				effect *= forceScale;
				//TODO finish implementation, then use this class
			}
			
			@Override
			public void apply() {
				node.changeCurrentEle(effect);
				effect = null;
			}
			
		}

		/**
		 * force trying to keep nodes with tagged elevation (or, with a
		 * weaker force, TerrainNodes with elevation data) close to that value
		 */
		protected static final class NodeElevationForce implements Force {

			private final ForceNode node;
			private final float ele;
			private final boolean tagged;

			private Float effect = null;
			
			public NodeElevationForce(ForceNode node, float ele, boolean tagged) {
				this.node = node;
				this.ele = ele;
				this.tagged = tagged;
			}
			
			@Override
			public void prepare(float forceScale) {
				effect = ele - node.getCurrentEle();
				effect *= forceScale;
				if (!tagged) {
					effect *= 0.3f;
				}
			}

			@Override
			public void apply() {
				node.changeCurrentEle(effect);
				effect = null;
			}
			
		}

		/**
		 * force that tries to enforce a minimum vertical distance between two nodes,
		 * used at intersections and overlaps
		 */
		protected static final class VerticalDistanceForce implements Force {
			
			private final ForceNode lowerNode;
			private final ForceNode upperNode;
			
			private final double minDistance;

			private Double effect = null;
			
			public VerticalDistanceForce(ForceNode lowerNode,
					ForceNode upperNode, double minDistance) {
				this.lowerNode = lowerNode;
				this.upperNode = upperNode;
				this.minDistance = minDistance;
			}
			
			@Override
			public void prepare(float forceScale) {
				float currentDistance = upperNode.getCurrentEle() - lowerNode.getCurrentEle();
				if (currentDistance < minDistance) {
					effect = minDistance - currentDistance;
					effect *= forceScale;
				} else {
					effect = 0.0;
				}
			}
			
			@Override
			public void apply() {
				lowerNode.changeCurrentEle(-effect/2);
				upperNode.changeCurrentEle(+effect/2);
				effect = null;
			}
			
		}
		

		/**
		 * Force for linking two nodes that are at a small distance from each other
		 * in the terrain.
		 * A weak force will try to bring the nodes to the same elevation.
		 */
		protected static final class NeighborshipForce extends ConnectionForce {
			
			final float distanceFactor;
			
			public NeighborshipForce(ForceNode node1, ForceNode node2) {
				super(node1, node2);
				float xzDistance = (float) node1.getPos().subtract(node2.getPos()).length();
				if (xzDistance <= 1) {
					distanceFactor = 1;
				} else {
					distanceFactor = 1 / xzDistance;
				}
			}

			@Override
			public void prepare(float forceScale) {
							
				float heightDiff = Math.abs(node1.getCurrentEle() - node2.getCurrentEle());
				effect = heightDiff * distanceFactor * forceScale;
				effect *= 0.4f;
				
				if (node1.getCurrentEle() > node2.getCurrentEle()) {
					effect = -effect;
				}
				
			}
			
		}
		
	}

}