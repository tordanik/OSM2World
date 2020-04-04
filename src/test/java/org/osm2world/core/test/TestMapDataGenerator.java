package org.osm2world.core.test;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.map_data.data.EmptyTagGroup.EMPTY_TAG_GROUP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.OriginMapProjection;
import org.osm2world.core.map_data.creation.OrthographicAzimuthalMapProjection;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagGroup;
import org.osm2world.core.math.VectorXZ;

import com.google.common.collect.Streams;
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;

import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmRelationMember;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Relation;
import de.topobyte.osm4j.core.model.impl.RelationMember;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;

/**
 * creates {@link MapElement}s for tests.
 * Internally uses a projection centered on lon=0.0, lat=0.0.
 *
 * TODO: this should be made obsolete in the long term by decoupling MapElements from the osm4j representations,
 * making them easier to construct
 */
public class TestMapDataGenerator {

	private final OriginMapProjection projection;

	private final List<MapNode> nodes = new ArrayList<>();
	private final List<MapWay> ways = new ArrayList<>();
	private final List<MapArea> areas = new ArrayList<>();
	private final List<MapRelation> relations = new ArrayList<>();

	private int createdNodes = 0, createdWays = 0, createdRelations = 0;

	public TestMapDataGenerator() {
		this.projection = new OrthographicAzimuthalMapProjection();
		projection.setOrigin(new LatLon(0, 0));
	}

	public MapNode createNode(double x, double z, TagGroup tags) {
		VectorXZ pos = new VectorXZ(x, z);
		OsmNode osmNode = createOsm4jNode(projection.calcLon(pos), projection.calcLat(pos), tags);
		MapNode result = new MapNode(pos, osmNode);
		nodes.add(result);
		return result;
	}

	public MapNode createNode(double x, double z) {
		return createNode(x, z, EMPTY_TAG_GROUP);
	}

	public MapWay createWay(List<MapNode> wayNodes, TagGroup tags) {
		wayNodes = closeLoop(wayNodes);
		OsmWay osmWay = createOsm4jWay(wayNodes, tags);
		MapWay result = new MapWay(osmWay, wayNodes);
		ways.add(result);
		return result;
	}

	public MapArea createWayArea(List<MapNode> wayNodes, TagGroup tags) {
		wayNodes = closeLoop(wayNodes);
		OsmWay osmWay = createOsm4jWay(wayNodes, tags);
		MapArea result = new MapArea(osmWay, wayNodes);
		areas.add(result);
		return result;
	}

	//TODO implement a createMultipolygonArea method

	public MapRelation createRelation(Map<String, MapRelation.Element> members, TagGroup tags) {
		OsmRelation osmRelation = createOsm4jRelation(members, tags);
		MapRelation result = new MapRelation(osmRelation);
		relations.add(result);
		return result;
	}

	/** returns a {@link MapData} object containing all the elements created so far */
	public MapData createMapData() {
		return new MapData(nodes, ways, areas, relations, null);
	}

	private OsmNode createOsm4jNode(double lon, double lat, TagGroup tags) {
		return new Node(createdNodes ++, lon, lat, toOsm4jTags(tags));
	}

	private OsmWay createOsm4jWay(List<MapNode> wayNodes, TagGroup tags) {
		TLongArrayList nodeIds = new TLongArrayList();
		wayNodes.forEach(n -> nodeIds.add(n.getOsmElement().getId()));
		return new Way(createdWays ++, nodeIds, toOsm4jTags(tags));
	}

	private OsmRelation createOsm4jRelation(Map<String, MapRelation.Element> members, TagGroup tags) {
		List<OsmRelationMember> osm4jMembers = members.entrySet().stream()
				.map(m -> toOsm4jMember(m.getKey(), m.getValue().getOsmElement()))
				.collect(toList());
		return new Relation(createdRelations ++, osm4jMembers, toOsm4jTags(tags));
	}

	private static List<OsmTag> toOsm4jTags(TagGroup tags) {
		return Streams.stream(tags)
			.map(t -> new Tag(t.key, t.value))
			.collect(toList());
	}

	private OsmRelationMember toOsm4jMember(String role, OsmEntity entity) {

		EntityType type;

		if (entity instanceof OsmNode) {
			type = EntityType.Node;
		} else if (entity instanceof OsmWay) {
			type = EntityType.Way;
		} else if (entity instanceof OsmRelation) {
			type = EntityType.Relation;
		} else {
			throw new Error("Unexpected implementation of OsmEntity");
		}

		return new RelationMember(entity.getId(), type, role);

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
