package org.osm2world.core.map_data.creation.index;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;

/**
 * a 2D tree (two-dimensional k-d tree) managing {@link MapElement}s of a
 * data set according on their coordinates in the XZ plane.
 * 
 * Data is contained within the leafs.
 * The inner nodes split the XZ plane along parallels to the Z and X axes,
 * alternatingly.
 */
public class Map2dTree implements MapDataIndex {
	
	protected static final int LEAF_SPLIT_SIZE = 11;
	
	protected final Node root;
	
	protected static interface Node {
		
		void add(MapElement element, boolean suppressSplits);

		/** adds all leaves in the subtree starting at this node to a list */
		void collectLeaves(List<Leaf> leaves);
		
	}
	
	protected static class InnerNode implements Node {
		
		public final boolean splitAlongX;
		public final double splitValue;
		
		public Node lowerChild;
		public Node upperChild;
		
		protected InnerNode(boolean splitAlongX, double splitValue) {
			
			this.splitAlongX = splitAlongX;
			this.splitValue = splitValue;
			
			this.lowerChild = new Leaf(this);
			this.upperChild = new Leaf(this);
			
		}

		@Override
		public void add(MapElement element, boolean suppressSplits) {
			
			boolean addToLowerChild = false;
			boolean addToUpperChild = false;
			
			for (MapNode node : getMapNodes(element)) {
				
				VectorXZ pos = node.getPos();
				
				if (splitAlongX) {
				
					addToLowerChild |= pos.x <= splitValue;
					addToUpperChild |= pos.x >= splitValue;
					
				} else {

					addToLowerChild |= pos.z <= splitValue;
					addToUpperChild |= pos.z >= splitValue;
					
				}
				
			}
			
			if (addToLowerChild) {
				lowerChild.add(element, suppressSplits);
			}
			if (addToUpperChild) {
				upperChild.add(element, suppressSplits);
			}
			
		}

		private void trySplitLeaf(Leaf leaf) {

			boolean splitChildAlongX = !splitAlongX;
			double splitChildValue = 0;
			
			/* determine split value as average of all values */
			
			int numNodes = 0;
			
			for (MapElement element : leaf) {
				for (MapNode node : getMapNodes(element)) {
					
					if (splitChildAlongX) {
						splitChildValue += node.getPos().x;
					} else {
						splitChildValue += node.getPos().z;
					}
					
					numNodes += 1;
					
				}
			}
			
			splitChildValue /= numNodes;
			
			/* create the new inner node */
			
			InnerNode newChild = new InnerNode(splitChildAlongX, splitChildValue);
			
			/* check whether splitting will reduce the maximum node size */
			
			for (MapElement element : leaf.elements) {
				newChild.add(element, true);
			}
			
			if (((Leaf)(newChild.lowerChild)).elements.size() < leaf.elements.size() - 5
					&& ((Leaf)(newChild.upperChild)).elements.size() < leaf.elements.size() - 5) {
				
				/* replace the leaf with the new child node */
				
				if (lowerChild == leaf) {
					lowerChild = newChild;
				} else if (upperChild == leaf) {
					upperChild = newChild;
				} else {
					throw new AssertionError("leaf is not a child of this node");
				}
				
			}
					
		}

		public void collectLeaves(List<Leaf> leaves) {
			lowerChild.collectLeaves(leaves);
			upperChild.collectLeaves(leaves);
		}
		
	}
	
	protected static class Leaf implements Node, Iterable<MapElement> {

		protected final InnerNode parent;
		protected final ArrayList<MapElement> elements;
		
		protected int numberWaysAndAreas = 0;
		
		protected Leaf(InnerNode parent) {
			this.parent = parent;
			elements = new ArrayList<MapElement>(LEAF_SPLIT_SIZE);
		}
		
		@Override
		public void add(MapElement element, boolean suppressSplits) {
			
			elements.add(element);

			if (!(element instanceof MapNode)) {
				numberWaysAndAreas += 1;
			}
			
			if (!suppressSplits && numberWaysAndAreas >= LEAF_SPLIT_SIZE) {
				parent.trySplitLeaf(this);
			}
			
		}
		
		@Override
		public Iterator<MapElement> iterator() {
			return elements.iterator();
		}
		
		@Override
		public void collectLeaves(List<Leaf> leaves) {
			leaves.add(this);
		}
		
	}
	
	public Map2dTree(MapData mapData) {
		
		AxisAlignedBoundingBoxXZ dataBoundary = mapData.getDataBoundary();
		root = new InnerNode(true, (dataBoundary.minX + dataBoundary.maxX) / 2);
		
		FaultTolerantIterationUtil.iterate(mapData.getMapElements(), new Operation<MapElement>() {
			@Override public void perform(MapElement element) {
				root.add(element, false);
			}
		});
		
	}
	
	protected static Iterable<MapNode> getMapNodes(MapElement element) {
		
		if (element instanceof MapNode) {
			
			return singleton((MapNode)element);
			
		} else if (element instanceof MapWaySegment) {
			
			return ((MapWaySegment)element).getStartEndNodes();
			
		} else { // element instanceof MapArea
			
			return ((MapArea)element).getBoundaryNodes();
			
		}
		
	}
	
	@Override
	public Iterable<Leaf> getLeaves() {
		
		List<Leaf> leaves = new ArrayList<Leaf>();
		
		root.collectLeaves(leaves);
		
		return leaves;
		
	}
	
}
