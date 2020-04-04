package org.osm2world.core.map_data.data;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.math.shapes.PolylineXZ;

import de.topobyte.osm4j.core.model.iface.OsmWay;

/**
 * A way from an OSM dataset.
 *
 * @See {@link MapData} for context
 */
public class MapWay extends MapRelation.Element implements IntersectionTestObject {

	private final OsmWay osmWay;
	private final List<MapNode> nodes;

	private final List<MapWaySegment> waySegments;

	public MapWay(OsmWay osmWay, List<MapNode> nodes) {

		if (nodes.size() < 2) {
			throw new IllegalArgumentException("a way needs at least two nodes, but "
					+ osmWay + " was created with only " + nodes.size());
		}

		this.osmWay = osmWay;
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

	public OsmWay getOsmElement() {
		return osmWay;
	}

	public TagGroup getTags() {
		return TagGroup.of(getTagsAsMap(osmWay));
	}

	@Override
	public String toString() {
		return "w" + osmWay.getId();
	}

}
