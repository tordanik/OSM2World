package org.osm2world.core.map_elevation.creation;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapAA;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.data.AreaElevationProfile;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionGrid;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.util.MinUtil;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class ForceElevationCalculator implements ElevationCalculator {

	public static final int CALCULATION_STEPS = 100;
	private static final int DIST_INVISIBLE_NODES = 50;
	
	@Override
	public void calculateElevations(MapData mapData,
			CellularTerrainElevation eleData) {
				
		ElevationDeterminationScenario scenario = createScenario(eleData);
		
		/* create empty elevation profiles for the elements
		 * and add them to the scenario */
		
		for (MapElement e : mapData.getMapElements()) {
			
			if (e.getPrimaryRepresentation() == null) continue;
			
			scenario.addElement(e);

			for (MapOverlap<?,?> o : e.getOverlaps()) {
				if (o.e1.getPrimaryRepresentation() != null
						&& o.e2.getPrimaryRepresentation() != null) {
						scenario.addOverlap(o);
				}
			}
			
		}
		
		/* add the terrain points to the scenario */
		
		for (TerrainPoint p : eleData.getTerrainPoints()) {
			scenario.addTerrainPoint(p);
		}
		
		/* run the scenario */
		
		scenario.calculate();
		
	}

	protected ElevationDeterminationScenario createScenario(CellularTerrainElevation eleData) {
		return new ElevationDeterminationScenario(eleData);
	}
	
	/**
	 * Group of connected GridElements that are set up for elevation determination.
	 * A {@link #calculate()} call will determine and store the final elevation
	 * profile for each element.
	 */
	protected static class ElevationDeterminationScenario {

		protected final CellularTerrainElevation eleData;
		
		protected final Map<MapNode,ForceNodeOnNode> nodeMap = new HashMap<MapNode, ForceNodeOnNode>();
		protected final Map<MapWaySegment,List<ForceNode>> lineMap = new HashMap<MapWaySegment, List<ForceNode>>();
		protected final Map<MapArea,List<ForceNode>> areaMap = new HashMap<MapArea, List<ForceNode>>();
		protected final Map<TerrainPoint,ForceNodeOnTerrainPoint> terrainPointMap = new HashMap<TerrainPoint,ForceNodeOnTerrainPoint>();

		
		protected final Set<MapOverlap<?, ?>> knownOverlaps = new HashSet<MapOverlap<?,?>>();
		
		protected final Collection<Force> forces = new ArrayList<Force>();
		protected final Collection<ForceNode> forceNodes = new ArrayList<ForceNode>();
		
		public ElevationDeterminationScenario(CellularTerrainElevation eleData) {
			this.eleData = eleData;
		}
		
		public final void addElement(MapElement e) {
			if (e instanceof MapNode) {
				addNode((MapNode)e);
			} else if (e instanceof MapWaySegment) {
				addWaySegment((MapWaySegment)e);
			} else if (e instanceof MapArea) {
				addArea((MapArea)e);
			}
		}
		
		public final void addOverlap(MapOverlap<?,?> o) {
			if (o instanceof MapIntersectionWW) {
				addIntersectionWW((MapIntersectionWW)o);
			} else if (o instanceof MapOverlapWA) {
				addOverlapWA((MapOverlapWA)o);
			} else if (o instanceof MapOverlapAA) {
				addOverlapAA((MapOverlapAA)o);
			}
		}

		public final void addNode(MapNode node) {
			
			if (nodeMap.containsKey(node)) return;
				
			ForceNodeOnNode fNode = new ForceNodeOnNode(node);
			nodeMap.put(node, fNode);
			forceNodes.add(fNode);
			
			if (node.getTags().containsKey("ele")) {
				Float ele = ValueStringParser.parseOsmDecimal(
						node.getTags().getValue("ele"), true);
				forces.add(new NodeElevationForce(fNode, ele, true));
			}
			
			node.setElevationProfile(new NodeElevationProfile(node));

			//TODO: terrain nodes below/above elements above/below the ground
			
		}

		public final void addWaySegment(MapWaySegment line) {

			if (lineMap.containsKey(line)) return;
			
			List<ForceNode> lineForceNodes = new ArrayList<ForceNode>();
			
			/* add start/end nodes if necessary */
			
			addNode(line.getStartNode());
			addNode(line.getEndNode());
			
			lineForceNodes.add(nodeMap.get(line.getStartNode()));
			lineForceNodes.add(nodeMap.get(line.getEndNode()));
			
			/* insert additional nodes to make the resulting curve smoother.
			 * This also makes it less likely that a way passes through a
			 * terrain cell without having a single node within that cell. */
			
			if (line.getLineSegment().getLength() > DIST_INVISIBLE_NODES) {
			
			List<VectorXZ> positions = equallyDistributePointsAlong(
					DIST_INVISIBLE_NODES, false,
					line.getStartNode().getPos(),
					line.getEndNode().getPos());
			
				for (VectorXZ pos : positions) {
					ForceNodeOnLine forceNode = new ForceNodeOnLine(line, pos);
					forceNodes.add(forceNode);
					lineForceNodes.add(forceNode);
				}
				
			}
			
			//TODO: terrain nodes below/above elements above/below the ground

			/* add to map */
			
			lineMap.put(line, lineForceNodes);
		
			line.setElevationProfile(new WaySegmentElevationProfile(line));
			
		}

		public final void addArea(MapArea area) {

			if (areaMap.containsKey(area)) return;
			
			List<ForceNode> areaForceNodes = new ArrayList<ForceNode>();
			
			for (MapNode node : area.getBoundaryNodes()) {
				addNode(node);
				areaForceNodes.add(nodeMap.get(node));
			}
			
			for (List<MapNode> hole : area.getHoles()) {
				for (MapNode node : hole) {
					addNode(node);
					areaForceNodes.add(nodeMap.get(node));
				}
			}
			
			areaMap.put(area, areaForceNodes);
			
			area.setElevationProfile(new AreaElevationProfile(area));
			
		}
				
		public void addIntersectionWW(MapIntersectionWW intersection) {
			
			if (knownOverlaps.contains(intersection)) return;

			knownOverlaps.add(intersection);
			
			final MapWaySegment line1 = intersection.e1;
			final MapWaySegment line2 = intersection.e2;
			final VectorXZ pos = intersection.pos;
			
			addWaySegment(line1);
			addWaySegment(line2);
			
			final ForceNode fNode1 = new ForceNodeOnLine(line1, pos);
			final ForceNode fNode2 = new ForceNodeOnLine(line2, pos);
						
			addVerticalDistanceForce(line1, line2, pos, fNode1, fNode2);

			forceNodes.add(fNode1);
			forceNodes.add(fNode2);
			
			lineMap.get(line1).add(fNode1);
			lineMap.get(line2).add(fNode2);
			
		}
		
		//TODO: add ForceNodeOnArea (and FNOnNode) where a node is *inside* an area
		
		public void addOverlapWA(MapOverlapWA overlap) {
			
			if (knownOverlaps.contains(overlap)) return;
			
			knownOverlaps.add(overlap);
			
			MapWaySegment waySegment = overlap.e1;
			MapArea area = overlap.e2;
			
			addWaySegment(waySegment);
			addArea(area);
			
			if (overlap.type == MapOverlapType.INTERSECT) {
				
				for (VectorXZ pos : overlap.getIntersectionPositions()) {
					
					final ForceNode fNodeW = new ForceNodeOnLine(waySegment, pos);
					final ForceNode fNodeA = new ForceNodeOnArea(area, pos);

					addVerticalDistanceForce(waySegment, area, pos, fNodeW, fNodeA);

					forceNodes.add(fNodeW);
					forceNodes.add(fNodeA);
					
					lineMap.get(waySegment).add(fNodeW);
					
				}
				
			} else if (overlap.type == MapOverlapType.CONTAIN) {
				
				for (MapNode node : waySegment.getStartEndNodes()) {
				
					final ForceNode fNodeW = nodeMap.get(node);
					final ForceNode fNodeA = new ForceNodeOnArea(area, node.getPos());

					addVerticalDistanceForce(waySegment, area, node.getPos(), fNodeW, fNodeA);

					forceNodes.add(fNodeW);
					forceNodes.add(fNodeA);
					
					lineMap.get(waySegment).add(fNodeW);
					
				}
				
			}
			
		}
		
		public void addOverlapAA(MapOverlapAA overlap) {

			//TODO implement addOverlapAA
			
		}
		
		public final void addTerrainPoint(TerrainPoint point) {
			
			//TODO: should affect areas containing it
			
			if (terrainPointMap.containsKey(point)) return;
			
			ForceNodeOnTerrainPoint fNode = new ForceNodeOnTerrainPoint(point);
			terrainPointMap.put(point, fNode);
			forceNodes.add(fNode);
			
			if (point.getEle() != null) {
				forces.add(new NodeElevationForce(fNode, point.getEle(), false));
			}
			
		}

		private void addVerticalDistanceForce(MapElement e1,
				MapElement e2, VectorXZ pos,
				ForceNode fNode1, ForceNode fNode2) {
			
			if (higherThan(e1, e2)) {
				
				double minDistance = getMinVerticalDistance(e1, e2, pos);
				forces.add(new VerticalDistanceForce(fNode2, fNode1, minDistance));
				
			} else if (higherThan(e2, e1)) {
				
				double minDistance = getMinVerticalDistance(e2, e1, pos);
				forces.add(new VerticalDistanceForce(fNode1, fNode2, minDistance));
				
			} else {
				forces.add(new SameEleForce(fNode1, fNode2));
			}
			
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
			
			for (int step=0; step < getCalculationSteps(); step++) {
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
				
				List<ForceNode> lineFNodes = lineMap.get(line);
				
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
			for (Map<? extends MapElement, List<ForceNode>> map
					: asList(lineMap, areaMap)) {
				for (MapElement segment : map.keySet()) {
					
					if (segment.getPrimaryRepresentation() != null &&
							segment.getPrimaryRepresentation().getGroundState() == GroundState.ON) {
						
						for (ForceNode fNode : map.get(segment)) {
							speedupGrid.insert(fNode);
						}
						
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
		
		protected final float forceScaleForStep(int step) {
			float reductionPerStep = 1f / (getCalculationSteps() + 1);
			return 1f - step * reductionPerStep;
		}
		
		/**
		 * determines the number of calculation steps.
		 * The default implementation returns
		 * {@link ElevationCalculator#CALCULATION_STEPS}.
		 */
		protected int getCalculationSteps() {
			return CALCULATION_STEPS;
		}
		
		/**
		 * performs a single step in the calculation of {@link #calculate()}
		 * by applying each force in {@link #forces}.
		 * @param forceScale  factor for force effects, will be reduced for later steps
		 *                    in order to achieve resonable convergence to a final state
		 */
		private final void calculateStep(float forceScale) {

			//TODO (performance): preparation can be parallelized, as force nodes aren't modified yet
			for (Force force : forces) {
				force.prepare(forceScale);
			}
			
			for (Force force : forces) {
				force.apply();
			}

		}

		private final void writeResult() {
			for (ForceNode fNode : forceNodes) {
				fNode.writeResult();
			}
		}

		/**
		 * returns true if the first of the two parameter features
		 * is higher than the second - this is relevant for elevation
		 * differences at intersections.
		 * Note that this will return false if the two features cannot
		 * be compared.
		 */
		private boolean higherThan(
				MapElement e1, MapElement e2) {
			
			GroundState groundState1 = e1.getPrimaryRepresentation().getGroundState();
			GroundState groundState2 = e2.getPrimaryRepresentation().getGroundState();
			
			if (groundState1 == groundState2) {
				return e1.getLayer() > e2.getLayer();
			} else if (groundState1.isHigherThan(groundState2)) {
				return true;
			} else {
				return false;
			}
			
		}

		private double getMinVerticalDistance(MapElement upper,
				MapElement lower, VectorXZ pos) {
			
			double minClearingBelowUpper = 0;
			for (WorldObject rep : upper.getRepresentations()) {
				if (rep.getClearingBelow(pos) > minClearingBelowUpper) {
					minClearingBelowUpper = rep.getClearingBelow(pos);
				}
			}
			
			double minClearingAboveLower = 0;
			for (WorldObject rep : lower.getRepresentations()) {
				if (rep.getClearingAbove(pos) > minClearingAboveLower) {
					minClearingAboveLower = rep.getClearingAbove(pos);
				}
			}
									
			return minClearingBelowUpper + minClearingAboveLower;
			
		}
		
		/**
		 * a node that is affected by the forces during elevation determination.
		 * Can be based on a {@link MapNode}, but also on points within a
		 * {@link MapWaySegment} or {@link MapArea}.
		 */
		protected static abstract class ForceNode implements IntersectionTestObject {

			protected Float currentEle = null;

			/**
			 * returns the x-z-position of this node
			 * (this position is fixed during the simulation)
			 */
			public abstract VectorXZ getPos();
			
			/**
			 * returns the current elevation that has resulted from
			 * force application so far.
			 */
			public Float getCurrentEle() {
				return currentEle;
			}

			/**
			 * returns the current 3d position
			 */
			public VectorXYZ getCurrentXYZ() {
				return getPos().xyz(getCurrentEle());
			}
			
			/**
			 * changes the current elevation (in this object, not the underlying grid).
			 * 
			 * @param up  amount of upwards movement (negative values move downwards)
			 */
			public void changeCurrentEle(double up) {
				currentEle += (float)up;
			}
			
			/**
			 * sets the current elevation to an absolute value.
			 * Usually only used during initialization.
			 */
			protected void setCurrentEle(double currentEle) {
				this.currentEle = (float)currentEle;
			}

			@Override
			public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
				return new AxisAlignedBoundingBoxXZ(
						getPos().x, getPos().z,
						getPos().x, getPos().z);
			}
			
			/** writes the current elevation information back to the grid */
			public abstract void writeResult();

		}

		/** {@link ForceNode} based on a {@link MapNode} */
		public final class ForceNodeOnNode extends ForceNode {

			private final MapNode node;
			
//			/**
//			 * stores all lines that this node needs to write its results to.
//			 * Created only when needed (many nodes won't affect lines)
//			 */
//			private Collection<GridLine> affectedLines;
//
//			/**
//			 * stores all lines that this node needs to write its results to.
//			 * Created only when needed (many nodes won't affect lines)
//			 */
//			private Collection<MapArea> affectedAreas;
			
			public ForceNodeOnNode(MapNode node) {
				
				this.node = node;
				
				Float ele = null;
				
				if (node.getTags().containsKey("ele")) {
					
					ele = ValueStringParser.parseOsmDecimal(
							node.getTags().getValue("ele"), true);
					
				}
				
				if (ele == null) {
					
					/* use elevation information from nodes or areas containing
					 * this node. If they have contradicting information, the
					 * results will be unpredictable.
					 */
					
					for (MapSegment segment : node.getConnectedSegments()) {
						
						TagGroup tags;
						if (segment instanceof MapWaySegment) {
							tags = ((MapWaySegment) segment).getTags();
						} else {
							tags = ((MapAreaSegment) segment).getArea().getTags();
						}
						
						if (tags.containsKey("ele")) {
							ele = ValueStringParser.parseOsmDecimal(
									tags.getValue("ele"), true);
						}
						
					}
					
				}
				
				if (ele != null) {
					setCurrentEle(ele);
				}
				
			}

			@Override
			public VectorXZ getPos() {
				return node.getPos();
			}

			@Override
			public void writeResult() {

				/* set elevation of the node itself */
				
				node.getElevationProfile().setEle(currentEle);
				
				/* set elevation info of the connected way segments */
				
				for (MapWaySegment inboundLine : node.getInboundLines()) {
					if (inboundLine.getPrimaryRepresentation() != null) {
						WaySegmentElevationProfile profile = inboundLine.getElevationProfile();
						VectorXZ endPos = inboundLine.getPrimaryRepresentation().getEndPosition(); //note that this *does* take offsets into account
						profile.addPointWithEle(endPos.xyz(currentEle));
					}
				}
				
				for (MapWaySegment outboundLine : node.getOutboundLines()) {
					if (outboundLine.getPrimaryRepresentation() != null) {
						WaySegmentElevationProfile profile = outboundLine.getElevationProfile();
						VectorXZ startPos = outboundLine.getPrimaryRepresentation().getStartPosition(); //note that this *does* take offsets into account
						profile.addPointWithEle(startPos.xyz(currentEle));
					}
				}
				
				/* set elevation info of the connected areas */
				
				for (MapArea area : node.getAdjacentAreas()) {
					if (area.getPrimaryRepresentation() != null) {
						AreaElevationProfile profile = area.getElevationProfile();
						profile.addPointWithEle(node.getPos().xyz(currentEle));
						//TODO: does this need to take offsets into accounts (compare way segments above)?
					}
				}
			
			}

//			public void addAffectedLine(GridLine line) {
//				if (affectedLines == null) {
//					affectedLines = new ArrayList<GridLine>();
//				}
//				affectedLines.add(line);
//			}
//
//			public void addAffectedArea(MapArea area) {
//				if (affectedAreas == null) {
//					affectedAreas = new ArrayList<MapArea>();
//				}
//				affectedAreas.add(area);
//			}
			
		}

		/** {@link ForceNode} representing a point on a {@link MapWaySegment} */
		public class ForceNodeOnLine extends ForceNode {

			private final MapWaySegment line;
			private final VectorXZ pos;

			public ForceNodeOnLine(MapWaySegment line, VectorXZ pos) {
				
				this.line = line;
				this.pos = pos;
				
				ForceNodeOnNode startEleNode = nodeMap.get(line.getStartNode());
				ForceNodeOnNode endEleNode = nodeMap.get(line.getEndNode());
				
				if (startEleNode.getCurrentEle() != null
						&& endEleNode.getCurrentEle() != null) {
					
					setCurrentEle(interpolateElevation(pos,
							startEleNode.getCurrentXYZ(),
							endEleNode.getCurrentXYZ()).y);
					
				}
				
			}

			@Override
			public VectorXZ getPos() {
				return pos;
			}

			@Override
			public void writeResult() {
				line.getElevationProfile().addPointWithEle(pos.xyz(currentEle));
			}

		}

		/** {@link ForceNode} representing a point on a {@link MapArea} */
		public class ForceNodeOnArea extends ForceNode {

			private final MapArea area;
			private final VectorXZ pos;

			public ForceNodeOnArea(MapArea area, VectorXZ pos) {
				
				this.area = area;
				this.pos = pos;
								
			}

			@Override
			public VectorXZ getPos() {
				return pos;
			}

			@Override
			public void writeResult() {
				area.getElevationProfile().addPointWithEle(pos.xyz(currentEle));
			}
			
		}
		
		public class ForceNodeOnTerrainPoint extends ForceNode {
			
			private final TerrainPoint point;
			
			public ForceNodeOnTerrainPoint(TerrainPoint point) {
				this.point = point;
				if (point.getEle() != null) {
					this.currentEle = point.getEle();
				}
			}
			
			@Override
			public VectorXZ getPos() {
				return point.getPos();
			}
			
			@Override
			public void writeResult() {
				point.setEle(currentEle);
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