package org.osm2world.core.map_data.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;

/**
 * a Quadtree managing {@link MapElement}s of a data set
 * according on their coordinates in the XZ plane.
 */
public class MapQuadtree {
	
	static final int LEAF_SPLIT_SIZE = 11;
	
	final QuadInnerNode root;
	
	static abstract class QuadNode {
		
		public final double minX, maxX, minZ, maxZ;
		
		private final SimplePolygonXZ boundary;
		
		QuadNode(double minX2, double maxX2, double minZ2, double maxZ2) {
			
			this.minX = minX2;
			this.maxX = maxX2;
			this.minZ = minZ2;
			this.maxZ = maxZ2;
			
			List<VectorXZ> vertices = new ArrayList<VectorXZ>(5);
			vertices.add(new VectorXZ(minX2, minZ2));
			vertices.add(new VectorXZ(maxX2, minZ2));
			vertices.add(new VectorXZ(maxX2, maxZ2));
			vertices.add(new VectorXZ(minX2, maxZ2));
			vertices.add(vertices.get(0));
			boundary = new SimplePolygonXZ(vertices);
			
		}
		
		/** returns true if this node's bounds contain at least a part of the element */
		boolean contains(MapElement element) {
			
			if (element instanceof MapNode) {
				
				return contains(((MapNode)element).getPos());
				
			} else if (element instanceof MapWaySegment) {				
				MapWaySegment line = (MapWaySegment)element;
								
				VectorXZ lineStart = line.getStartNode().getPos();
				VectorXZ lineEnd = line.getEndNode().getPos();
				
				if (contains(lineStart) || contains(lineEnd)) {
					return true;
				} else if (boundary.intersects(lineStart, lineEnd)) {
					//SUGGEST (performance): use that the box is axis-aligned?
					return true;
				}				
				return false;
				
			} else { // element instanceof MapArea				
				MapArea area = ((MapArea)element);
								
				for (MapNode node : area.getBoundaryNodes()) {
					if (contains(node.getPos())) {
						return true;
					}
				}
				
				if (boundary.intersects(area.getPolygon().getOuter())
						|| area.getPolygon().contains(boundary)) {
					//SUGGEST (performance): use that the box is axis-aligned?
					return true;
				}
				
				return false;
			}
		}
		
		boolean contains(VectorXZ pos) {
			return pos.x >= minX && pos.x <= maxX
				&& pos.z >= minZ && pos.z <= maxZ;
		}
		
		abstract void add(MapElement element);

		abstract void addAll(Collection<MapElement> elements);

		/** adds all leaves in the subtree starting at this node to a list */		
		abstract void collectLeaves(List<QuadLeaf> leaves);
		
	}
	
	class QuadInnerNode extends QuadNode {
		
		/** array with four elements */
		final QuadNode childNodes[];

		QuadInnerNode(double minX, double maxX, double minZ, double maxZ) {
			super(minX, maxX, minZ, maxZ);
			
			childNodes = new QuadNode[4];
			
			double halfX = (minX+maxX)/2;
			double halfZ = (minZ+maxZ)/2;
			
			childNodes[0] = new QuadLeaf(this, minX, halfX, minZ, halfZ);
			childNodes[1] = new QuadLeaf(this, halfX, maxX, minZ, halfZ);
			childNodes[2] = new QuadLeaf(this, minX, halfX, halfZ, maxZ);
			childNodes[3] = new QuadLeaf(this, halfX, maxX, halfZ, maxZ);
			
		}
		
		@Override
		void add(MapElement element) {
			for (int i=0; i<4; i++) {
				if (childNodes[i].contains(element)) {
					childNodes[i].add(element);
					//continue loop, the element can cross leaf borders
				}
			}
		}
		
		@Override
		void addAll(Collection<MapElement> elements) {
			for (MapElement element : elements) {
				add(element);
			}
		}

		void trySplitLeaf(QuadLeaf leaf) {
			
			QuadInnerNode newChild =
				new QuadInnerNode(leaf.minX, leaf.maxX, leaf.minZ, leaf.maxZ);
			
			/* check whether splitting will reduce the maximum node size */
			
			boolean nodeSizeReduced = true;
						
			for (int i=0; i<4; i++) {
				boolean newLeafcontainsAllElements = true;
				for (MapElement element : leaf.getElements()) {
					if (!newChild.childNodes[i].contains(element)) {
						newLeafcontainsAllElements = false;
						break;
					}
				}
				if (newLeafcontainsAllElements) {
					nodeSizeReduced = false;
					break;
				}
			}
			
			if (nodeSizeReduced) {
				
				/* replace the leaf with the new child node */
				
				for (int i=0; i<4; i++) {
					if (childNodes[i] == leaf) {
						childNodes[i] = newChild;
						childNodes[i].addAll(leaf.elements);
						return;
					}					
				}
				
				throw new AssertionError("leaf is not a child of this node");
				
			}
					
		}

		void collectLeaves(List<QuadLeaf> leaves) {
			for (int i=0; i<4; i++) {
				childNodes[i].collectLeaves(leaves);
			}
		}
		
	}
	
	public class QuadLeaf extends QuadNode {

		final QuadInnerNode parent;
		final ArrayList<MapElement> elements;
		
		QuadLeaf(QuadInnerNode parent, double minX, double maxX, double minZ, double maxZ) {
			super(minX, maxX, minZ, maxZ);
			
			this.parent = parent;
			
			elements = new ArrayList<MapElement>(LEAF_SPLIT_SIZE);
			
		}
		
		public List<MapElement> getElements() {
			return elements;
		};
		
		@Override
		void add(MapElement element) {
			
			elements.add(element);

			if (elements.size() >= LEAF_SPLIT_SIZE) {
				parent.trySplitLeaf(this);
			}
			
		}
		
		@Override
		void addAll(Collection<MapElement> element) {
			/* addAll cannot be implemented by iterating over add:
			 * if the leaf would be "split"(replaced with an inner node)
			 * during the iteration, the remaining elements would still
			 * be added to the now-useless leaf object */
			
			elements.addAll(element);

			if (elements.size() >= LEAF_SPLIT_SIZE) {
				parent.trySplitLeaf(this);
			}
			
		}
		
		@Override
		void collectLeaves(List<QuadLeaf> leaves) {
			leaves.add(this);
		}
		
	}
	
	public MapQuadtree(MapData grid) {
		
		root = new QuadInnerNode(
				grid.getBoundary().minX, grid.getBoundary().maxX,
				grid.getBoundary().minZ, grid.getBoundary().maxZ);
		
		FaultTolerantIterationUtil.iterate(grid.getMapElements(), new Operation<MapElement>() {
			@Override public void perform(MapElement element) {				
				root.add(element);
			}
		});
		
	}
	
	public Iterable<QuadLeaf> getLeaves() {
		
		List<QuadLeaf> leaves = new ArrayList<QuadLeaf>();
		
		root.collectLeaves(leaves);
		
		return leaves;
		
	}
	
}
