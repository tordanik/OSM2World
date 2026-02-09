package org.osm2world.osm.creation;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.osm.data.OSMData;
import org.osm2world.osm.ruleset.HardcodedRuleset;

import com.clarisma.common.store.StoreException;
import com.geodesk.feature.*;
import com.geodesk.geom.Box;
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import com.slimjars.dist.gnu.trove.map.TLongObjectMap;
import com.slimjars.dist.gnu.trove.map.hash.TLongObjectHashMap;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.impl.Bounds;
import de.topobyte.osm4j.core.model.impl.RelationMember;
import de.topobyte.osm4j.core.model.impl.Tag;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;

/**
 * {@link OSMDataReader} fetching data from a GeoDesk database (.gol file)
 *
 * @param file  the gol file this reader is obtaining data from
 */
public record GeodeskReader(File file) implements OSMDataReader {

	private static final long ANONYMOUS_NODE_ID_OFFSET = 100_000_000_000L;

	/**
	 * object used to prevent concurrent access to a Geodesk database.
	 * This simple solution also prevents concurrent access to different databases, but that's likely a rare situation.
	 */
	private static final Object synchronizationObject = new Object();

	@Override
	public OSMData getData(LatLonBounds bounds) throws IOException {

		if (!file.exists()) {
			throw new FileNotFoundException("Geodesk file does not exist: " + file);
		}

		synchronized (synchronizationObject) {

			try (FeatureLibrary library = new FeatureLibrary(file.getPath())) {

				Box bbox = Box.ofWSEN(bounds.minlon, bounds.minlat, bounds.maxlon, bounds.maxlat);
				Features features = library.in(bbox);

				InMemoryMapDataSet data = geodeskToOsm4j(features, bounds);

				return new OSMData(data);

			} catch (StoreException e) {
				throw new IOException(e);
			}

		}

	}

	/** converts OSM data in GeoDesk's representation to osm4j's */
	private InMemoryMapDataSet geodeskToOsm4j(Features features, LatLonBounds bounds) {

		NodeIdProvider nodeIdProvider = new NodeIdProvider();

		/* collect all features to convert (including way nodes and relation members) */

		TLongObjectMap<Feature> golNodeMap = new TLongObjectHashMap<>();
		TLongObjectMap<Feature> golWayMap = new TLongObjectHashMap<>();
		TLongObjectMap<Feature> golRelationMap = new TLongObjectHashMap<>();

		features.nodes().forEach(it -> golNodeMap.put(it.id(), it));
		features.ways().forEach(it -> golWayMap.put(it.id(), it));
		features.relations().forEach(it -> golRelationMap.put(it.id(), it));

		int maxRelationNestingDepth = 3;
		for (int i = 0; i < maxRelationNestingDepth; i++) {
			Set<Feature> newRelations = new HashSet<>();
			for (Feature relation : golRelationMap.valueCollection()) {
				if (!membersShouldBeIncluded(relation)) continue;
				for (Feature memberRelation : relation.members().relations()) {
					if (!golRelationMap.containsKey(memberRelation.id())) {
						newRelations.add(memberRelation);
					}
				}
			}
			newRelations.forEach(it -> golRelationMap.put(it.id(), it));
		}

		for (Feature relation : golRelationMap.valueCollection()) {
			if (membersShouldBeIncluded(relation)) {
				relation.members().nodes().forEach(it -> golNodeMap.put(it.id(), it));
				relation.members().ways().forEach(it -> golWayMap.put(it.id(), it));
			}
		}

		for (Feature way : golWayMap.valueCollection()) {
			for (Feature wayNode : way.nodes()) {
				golNodeMap.put(nodeIdProvider.nodeId(wayNode), wayNode);
			}
		}

		/* perform the actual conversion */

		TLongObjectMap<OsmNode> osm4jNodeMap = new TLongObjectHashMap<>();
		TLongObjectMap<OsmWay> osm4jWayMap = new TLongObjectHashMap<>();
		TLongObjectMap<OsmRelation> osm4jRelationMap = new TLongObjectHashMap<>();

		for (long nodeId : golNodeMap.keys()) {
			Feature node = golNodeMap.get(nodeId);
			var golNode = new de.topobyte.osm4j.core.model.impl.Node(nodeId, node.lon(), node.lat());
			golNode.setTags(geodeskTagsToOsm4j(node.tags()));
			osm4jNodeMap.put(nodeId, golNode);
		}

		for (Feature way : golWayMap.valueCollection()) {
			TLongArrayList nodeIds = new TLongArrayList();
			way.nodes().forEach(n -> nodeIds.add(nodeIdProvider.nodeId(n)));
			assert(stream(nodeIds.toArray()).allMatch(osm4jNodeMap::containsKey));
			var golWay = new de.topobyte.osm4j.core.model.impl.Way(way.id(), nodeIds);
			golWay.setTags(geodeskTagsToOsm4j(way.tags()));
			osm4jWayMap.put(way.id(), golWay);
		}

		for (Feature relation : golRelationMap.valueCollection()) {
			List<OsmRelationMember> members = new ArrayList<>();
			for (Feature m : relation.members()) {
				EntityType mType;
				if (m instanceof Node) {
					mType = EntityType.Node;
				} else if (m instanceof Way) {
					mType = EntityType.Way;
				} else {
					mType = EntityType.Relation;
				}
				members.add(new RelationMember(m.id(), mType, m.role()));
			}
			var golRelation = new de.topobyte.osm4j.core.model.impl.Relation(relation.id(), members);
			golRelation.setTags(geodeskTagsToOsm4j(relation.tags()));
			osm4jRelationMap.put(relation.id(), golRelation);
		}

		InMemoryMapDataSet data = new InMemoryMapDataSet();
		data.setNodes(osm4jNodeMap);
		data.setWays(osm4jWayMap);
		data.setRelations(osm4jRelationMap);
		data.setBounds(new Bounds(bounds.minlon, bounds.maxlon, bounds.maxlat, bounds.minlat));
		return data;

	}

	private boolean membersShouldBeIncluded(Feature relation) {
		return relation.hasTag("type", "multipolygon")
				&& new HardcodedRuleset().isRelevantRelation(geodeskTagsToTagSet(relation.tags()));
	}

	private List<? extends OsmTag> geodeskTagsToOsm4j(Tags geodeskTags) {
		Map<String, Object> tagMap = geodeskTags.toMap();
		if (tagMap.isEmpty()) {
			return List.of();
		} else {
			return tagMap.entrySet().stream()
					.map(t -> new Tag(t.getKey(), t.getValue().toString()))
					.collect(toList());
		}
	}

	private static TagSet geodeskTagsToTagSet(Tags geodeskTags) {
		return TagSet.of(geodeskTags.toMap().entrySet().stream()
				.map(it -> new org.osm2world.map_data.data.Tag(it.getKey(), it.getValue().toString()))
				.collect(toList()));
	}

	private static class NodeIdProvider {

		private final TObjectLongMap<LatLon> anonymousNodeIdMap = new TObjectLongHashMap<>();

		/** returns the id non-anonymous nodes, or else a fake id based on the coords */
		private long nodeId(Feature node) {
			if (node.id() >= ANONYMOUS_NODE_ID_OFFSET) {
				throw new Error("Node id too large: " + node.id());
			} else if (node.id() != 0) {
				return node.id();
			} else {
				LatLon pos = new LatLon(node.lat(), node.lon());
				if (!anonymousNodeIdMap.containsKey(pos)) {
					anonymousNodeIdMap.put(pos, ANONYMOUS_NODE_ID_OFFSET + anonymousNodeIdMap.size());
				}
				return anonymousNodeIdMap.get(pos);
			}
		}

	}

}
