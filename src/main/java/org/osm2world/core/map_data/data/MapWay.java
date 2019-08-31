package org.osm2world.core.map_data.data;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.world.data.WorldObject;

import de.topobyte.osm4j.core.model.iface.OsmWay;

/**
 * A way from an OSM dataset.
 *
 * @See {@link MapData} for context
 */
public class MapWay extends MapRelation.Element {

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

	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(getPolylineXZ().getVertexList());
	}

	public OsmWay getOsmElement() {
		return osmWay;
	}

	public TagGroup getTags() {
		return new MapBasedTagGroup(getTagsAsMap(osmWay));
	}

	@Override
	public String toString() {
		return "w" + osmWay.getId();
	}

	@Override
	public List<? extends WorldObject> getRepresentations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WorldObject getPrimaryRepresentation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<MapOverlap<? extends MapElement, ? extends MapElement>> getOverlaps() {
		// TODO Auto-generated method stub
		return null;
	}

}
