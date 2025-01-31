package org.osm2world.core.math.datastructures;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;

/**
 * a 2D tree (two-dimensional k-d tree) managing {@link MapElement}s of a
 * data set according on their coordinates in the XZ plane.
 *
 * Data is contained within the leafs.
 * The inner nodes split the XZ plane along parallels to the Z and X axes,
 * alternatingly.
 */
public class Map2dTree implements SpatialIndex<MapElement> {

	protected static final int LEAF_SPLIT_SIZE = 11;

	protected final Node root;

	protected static interface Node {

		void add(MapElement element, boolean suppressSplits);

		List<Leaf> probe(BoundedObject element);

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

		@Override
		public List<Leaf> probe(BoundedObject element) {

			boolean addToLowerChild = false;
			boolean addToUpperChild = false;

			if (element instanceof MapElement) {

				for (MapNode node : getMapNodes((MapElement) element)) {

					VectorXZ pos = node.getPos();

					if (splitAlongX) {

						addToLowerChild |= pos.x <= splitValue;
						addToUpperChild |= pos.x >= splitValue;

					} else {

						addToLowerChild |= pos.z <= splitValue;
						addToUpperChild |= pos.z >= splitValue;

					}

				}

			} else {

				if (splitAlongX) {

					addToLowerChild |= element.boundingBox().minX <= splitValue;
					addToUpperChild |= element.boundingBox().maxX >= splitValue;

				} else {

					addToLowerChild |= element.boundingBox().minZ <= splitValue;
					addToUpperChild |= element.boundingBox().maxZ >= splitValue;

				}

			}

			if (addToLowerChild && addToUpperChild) {
				List<Leaf> leaves = new ArrayList<>();
				leaves.addAll(lowerChild.probe(element));
				leaves.addAll(upperChild.probe(element));
				return leaves;
			} else if (addToLowerChild) {
				return lowerChild.probe(element);
			} else if (addToUpperChild) {
				return upperChild.probe(element);
			} else {
				throw new AssertionError ("The element is not in this Node");
			}

		}

		@Override
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
		public List<Leaf> probe(BoundedObject element) {
			return singletonList(this);
		}

		@Override
		public void collectLeaves(List<Leaf> leaves) {
			leaves.add(this);
		}

	}

	public Map2dTree(AxisAlignedRectangleXZ dataBoundary) {

		root = new InnerNode(true, (dataBoundary.minX + dataBoundary.maxX) / 2);

	}

	@Override
	public void insert(MapElement element) {
		root.add(element, false);
	}

	@Override
	public Collection<Leaf> probeLeaves(BoundedObject e) {
		return root.probe(e);
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
