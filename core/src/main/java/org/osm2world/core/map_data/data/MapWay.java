package org.osm2world.core.map_data.data;

import static org.osm2world.core.map_data.creation.MapDataCreationUtil.withoutConsecutiveDuplicates;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.core.math.shapes.PolylineXZ;

/**
 * A way from an OSM dataset.
 *
 * @see MapData
 */
public class MapWay extends MapRelationElement implements BoundedObject {

	private final long id;
	private final TagSet tags;
	private final List<MapNode> nodes;

	private final List<MapWaySegment> waySegments;

	public MapWay(long id, TagSet tags, List<MapNode> nodes) {

		if (nodes.size() < 2) {
			throw new IllegalArgumentException("a way needs at least two nodes, but "
					+ "w" + id + " was created with only " + nodes.size());
		}

		nodes = withoutConsecutiveDuplicates(nodes);

		this.id = id;
		this.tags = tags;
		this.nodes = nodes;

		waySegments = new ArrayList<>(nodes.size() - 1);

		for (int v = 0; v + 1 < nodes.size(); v++) {
			waySegments.add(new MapWaySegment(this, nodes.get(v), nodes.get(v + 1)));
		}

		for (int i = 0; i < nodes.size(); i++) {

			MapNode node = nodes.get(i);

			if (i > 0) {
				node.addInboundLine(waySegments.get(i - 1));
			}

			if (i + 1 < nodes.size()) {
				node.addOutboundLine(waySegments.get(i));
			}

		}

	}

	@Override
	public long getId() {
		return id;
	}

	public List<MapNode> getNodes() {
		return nodes;
	}

	public PolylineXZ getPolylineXZ() {
		List<VectorXZ> points = new ArrayList<>(nodes.size());
		for (MapNode node : nodes) {
			points.add(node.getPos());
		}
		return new PolylineXZ(points);
	}

	public List<MapWaySegment> getWaySegments() {
		return waySegments;
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return getPolylineXZ().boundingBox();
	}

	@Override
	public TagSet getTags() {
		return tags;
	}

	@Override
	public String toString() {
		return "w" + id;
	}

}
