package org.osm2world.core.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.VectorXZ;

/**
 * creates {@link MapElement}s for tests.
 * Internally uses a projection centered on lon=0.0, lat=0.0.
 *
 * TODO: this should be made obsolete in the long term by decoupling MapElements from the osm4j representations,
 * making them easier to construct
 */
public class TestMapDataGenerator {

	private final List<MapNode> nodes = new ArrayList<>();
	private final List<MapWay> ways = new ArrayList<>();
	private final List<MapArea> areas = new ArrayList<>();
	private final List<MapRelation> relations = new ArrayList<>();

	private int createdNodes = 0, createdWays = 0, createdRelations = 0;

	public MapNode createNode(double x, double z, TagSet tags) {
		VectorXZ pos = new VectorXZ(x, z);
		MapNode result = new MapNode(createdNodes ++, tags, pos);
		nodes.add(result);
		return result;
	}

	public MapNode createNode(double x, double z) {
		return createNode(x, z, TagSet.of());
	}

	public MapWay createWay(List<MapNode> wayNodes, TagSet tags) {
		wayNodes = closeLoop(wayNodes);
		MapWay result = new MapWay(createdWays ++, tags, wayNodes);
		ways.add(result);
		return result;
	}

	public MapArea createWayArea(List<MapNode> wayNodes, TagSet tags) {
		wayNodes = closeLoop(wayNodes);
		MapArea result = new MapArea(createdWays ++, false, tags, wayNodes);
		areas.add(result);
		return result;
	}

	//TODO implement a createMultipolygonArea method

	public MapRelation createRelation(Map<String, MapRelation.Element> members, TagSet tags) {
		MapRelation result = new MapRelation(createdRelations ++, tags);
		members.forEach((role, element) -> result.addMembership(role, element));
		relations.add(result);
		return result;
	}

	/** returns a {@link MapData} object containing all the elements created so far */
	public MapData createMapData() {
		return new MapData(nodes, ways, areas, relations, null);
	}

	/**
	 * returns the list itself if the last element of the list equals the first;
	 * otherwise, returns a list that's the same except with the first element appended to the end.
	 */
	private <T> List<T> closeLoop(List<T> list) {
		if (!list.get(0).equals(list.get(list.size() - 1))) {
			list = new ArrayList<>(list);
			list.add(list.get(0));
		}
		return list;
	}

}
