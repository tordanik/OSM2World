package org.osm2world.core.map_data.data;

import java.util.List;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;

import com.google.common.collect.ImmutableList;

/**
 * connection between two {@link MapNode}s that's part of a polyline or polygon.
 *
 * @see MapData
 */
public abstract class MapSegment {

	protected final MapNode startNode;
	protected final MapNode endNode;

	protected MapSegment(MapNode startNode, MapNode endNode) {
		if (startNode == null || endNode == null) {
			throw new IllegalArgumentException();
		}
		this.startNode = startNode;
		this.endNode = endNode;
	}

	public MapNode getStartNode() {
		return startNode;
	}

	public MapNode getEndNode() {
		return endNode;
	}

	public MapNode getOtherNode(MapNode node) {
		if (node == startNode) {
			return endNode;
		} else if (node == endNode) {
			return startNode;
		} else {
			throw new IllegalArgumentException("not a node of this segment");
		}
	}

	public List<MapNode> getStartEndNodes() {
		return ImmutableList.of(startNode, endNode);
	}

	public LineSegmentXZ getLineSegment() {
		return new LineSegmentXZ(startNode.getPos(), endNode.getPos());
	}

	/** caches the result for {@link #getDirection()} */
	private VectorXZ direction = null;

	/** caches the result for {@link #getRightNormal()} */
	private VectorXZ rightNormal = null;

	/**
	 * returns a normalized vector indicating the line's horizontal direction
	 */
	public VectorXZ getDirection() {
		if (direction == null) {
			direction = endNode.getPos().subtract(startNode.getPos());
			direction = direction.normalize();
			//TODO: (performance) getDirection method in VectorXZ?
		}
		return direction;
	}

	/**
	 * returns a the result of applying {@link VectorXZ#rightNormal()}
	 * to vector returned by {@link #getDirection()}
	 */
	public VectorXZ getRightNormal() {
		if (rightNormal == null) {
			rightNormal = getDirection().rightNormal();
		}
		return rightNormal;
	}

	/**
	 * returns the center of the line
	 */
	public VectorXZ getCenter() {
		return (startNode.getPos().add(endNode.getPos())).mult(0.5);
	}

	/**
	 * returns true if this MapSegment shares a node with another MapSegment
	 */
	public boolean isConnectedTo(MapSegment other) {
		return endNode == other.getStartNode()
			|| endNode == other.getEndNode()
			|| startNode == other.getStartNode()
			|| startNode == other.getEndNode();
	}

	/**
	 * returns true if this MapSegment shares a node with a MapArea
	 */
	public boolean isConnectedTo(MapArea other) {
		return other.getBoundaryNodes().contains(startNode)
			|| other.getBoundaryNodes().contains(endNode);
	}

	/**
	 * returns true if this MapSegment shares both nodes with another MapSegment
	 */
	public boolean sharesBothNodes(MapSegment other) {
		return (endNode == other.getStartNode()
				&& startNode == other.getEndNode())
			|| (endNode == other.getEndNode()
				&& startNode == other.getStartNode());
	}

	/** returns the {@link MapElement} a segment is part of */
	public static MapElement getElement(MapSegment s) {
		if (s instanceof MapWaySegment w) {
			return w;
		} else if (s instanceof MapAreaSegment a) {
			return a.getArea();
		} else {
			throw new Error("segment must be part of a way or area");
		}
	}

}
