package org.osm2world.core.map_elevation.creation;

import static org.osm2world.core.math.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
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
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * superclass for all {@link ElevationCalculator}s that work by setting up
 * a "scenario" for a simulation/calculation first, then running it
 */
public abstract class AdvancedAbstractElevationCalculator
		implements ElevationCalculator {
	
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
		
		/* write the results */
		
		scenario.writeResult();
		
	}

	protected abstract ElevationDeterminationScenario createScenario(
			CellularTerrainElevation eleData);
	
	/**
	 * Group of connected GridElements that are set up for elevation determination.
	 * A {@link #calculate()} call will determine and store the final elevation
	 * profile for each element.
	 */
	protected static interface ElevationDeterminationScenario {

		void addElement(MapElement e);

		void addOverlap(MapOverlap<?, ?> o);

		void addTerrainPoint(TerrainPoint p);
		
		void calculate();
		
		void writeResult();
		
	}
	
	protected static abstract class AbstractElevationDeterminationScenario
		implements ElevationDeterminationScenario {
		
		private static final int DIST_INVISIBLE_NODES = 50;
		
		protected final Map<MapNode,ForceNodeOnNode> nodeMap =
			new HashMap<MapNode, ForceNodeOnNode>();
		protected final Multimap<MapWaySegment,ForceNode> lineMap =
			HashMultimap.create();
		protected final Multimap<MapArea,ForceNode> areaMap =
			HashMultimap.create();
		protected final Map<TerrainPoint,ForceNodeOnTerrainPoint> terrainPointMap =
			new HashMap<TerrainPoint,ForceNodeOnTerrainPoint>();
				
		protected final Set<MapOverlap<?, ?>> knownOverlaps = new HashSet<MapOverlap<?,?>>();

		protected final Collection<ForceNode> forceNodes = new ArrayList<ForceNode>();
		
		@Override
		public final void addElement(MapElement e) {
			if (e instanceof MapNode) {
				addNode((MapNode)e);
			} else if (e instanceof MapWaySegment) {
				addWaySegment((MapWaySegment)e);
			} else if (e instanceof MapArea) {
				addArea((MapArea)e);
			}
		}
		
		@Override
		public final void addOverlap(MapOverlap<?,?> o) {
			if (o instanceof MapIntersectionWW) {
				addIntersectionWW((MapIntersectionWW)o);
			} else if (o instanceof MapOverlapWA) {
				addOverlapWA((MapOverlapWA)o);
			} else if (o instanceof MapOverlapAA) {
				addOverlapAA((MapOverlapAA)o);
			}
		}
		
		public final void addTerrainPoint(TerrainPoint point) {
			
			//TODO: should affect areas containing it
			
			if (terrainPointMap.containsKey(point)) return;
			
			ForceNodeOnTerrainPoint fNode = new ForceNodeOnTerrainPoint(point);
			terrainPointMap.put(point, fNode);
			forceNodes.add(fNode);
			
			if (point.getEle() != null) {
				handleConstantElevation(fNode, point.getEle(), false);
			}
			
		}

		private final void addNode(MapNode node) {
			
			if (nodeMap.containsKey(node)) return;
				
			ForceNodeOnNode fNode = new ForceNodeOnNode(node);
			nodeMap.put(node, fNode);
			forceNodes.add(fNode);
			
			if (node.getTags().containsKey("ele")) {
				Float ele = ValueStringParser.parseOsmDecimal(
						node.getTags().getValue("ele"), true);
				handleConstantElevation(fNode, ele, true);
			}
			
			node.setElevationProfile(new NodeElevationProfile(node));

			//TODO: terrain nodes below/above elements above/below the ground
			
		}

		private final void addWaySegment(MapWaySegment line) {

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
			
			lineMap.putAll(line, lineForceNodes);
			
			line.setElevationProfile(new WaySegmentElevationProfile(line));
			
		}

		private final void addArea(MapArea area) {

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
			
			areaMap.putAll(area, areaForceNodes);
			
			area.setElevationProfile(new AreaElevationProfile(area));
			
		}
				
		private final void addIntersectionWW(MapIntersectionWW intersection) {
			
			if (knownOverlaps.contains(intersection)) return;

			knownOverlaps.add(intersection);
			
			final MapWaySegment line1 = intersection.e1;
			final MapWaySegment line2 = intersection.e2;
			final VectorXZ pos = intersection.pos;
			
			addWaySegment(line1);
			addWaySegment(line2);
			
			final ForceNode fNode1 = new ForceNodeOnLine(line1, pos);
			final ForceNode fNode2 = new ForceNodeOnLine(line2, pos);
						
			handleVerticalDistance(line1, line2, pos, fNode1, fNode2);

			forceNodes.add(fNode1);
			forceNodes.add(fNode2);
			
			lineMap.get(line1).add(fNode1);
			lineMap.get(line2).add(fNode2);
			
		}
		
		//TODO: add ForceNodeOnArea (and FNOnNode) where a node is *inside* an area
		
		private final void addOverlapWA(MapOverlapWA overlap) {
			
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

					handleVerticalDistance(waySegment, area, pos, fNodeW, fNodeA);

					forceNodes.add(fNodeW);
					forceNodes.add(fNodeA);
					
					lineMap.get(waySegment).add(fNodeW);
					
				}
				
			} else if (overlap.type == MapOverlapType.CONTAIN) {
				
				for (MapNode node : waySegment.getStartEndNodes()) {
				
					final ForceNode fNodeW = nodeMap.get(node);
					final ForceNode fNodeA = new ForceNodeOnArea(area, node.getPos());

					handleVerticalDistance(waySegment, area, node.getPos(), fNodeW, fNodeA);

					forceNodes.add(fNodeW);
					forceNodes.add(fNodeA);
					
					lineMap.get(waySegment).add(fNodeW);
					
				}
				
			}
			
		}
		
		private final void addOverlapAA(MapOverlapAA overlap) {

			//TODO implement addOverlapAA
			
		}

		protected abstract void handleConstantElevation(
				ForceNode node, Float ele, boolean tagged);
		
		protected abstract void handleSameElevation(
				ForceNode node1, ForceNode node2);
		
		protected abstract void handleVerticalMinDistance(
				ForceNode lowerNode, ForceNode upperNode,
				double minDistance);
		
		private void handleVerticalDistance(MapElement e1,
				MapElement e2, VectorXZ pos,
				ForceNode fNode1, ForceNode fNode2) {
			
			if (higherThan(e1, e2)) {
				
				double minDistance = getMinVerticalDistance(e1, e2, pos);
				handleVerticalMinDistance(fNode2, fNode1, minDistance);
				
			} else if (higherThan(e2, e1)) {
				
				double minDistance = getMinVerticalDistance(e2, e1, pos);
				handleVerticalMinDistance(fNode1, fNode2, minDistance);
				
			} else {
				handleSameElevation(fNode1, fNode2);
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

		public final void writeResult() {
			for (ForceNode fNode : forceNodes) {
				fNode.writeResult();
			}
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
		public final class ForceNodeOnLine extends ForceNode {

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
		public final class ForceNodeOnArea extends ForceNode {

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
		
		public final class ForceNodeOnTerrainPoint extends ForceNode {
			
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
		
	}
	
}
