package org.osm2world.core.map_data.creation;

import static org.osm2world.core.math.algorithms.GeometryUtil.closeLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.GeometryUtil;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;

/**
 * creates {@link MapData} consisting of one or more {@link MapElement}s.
 * This is an alternative to using {@link OSMToMapDataConverter},
 * and is intended for code using OSM2World as a library.
 * It is also used in OSM2World's unit tests.
 */
public class MapDataBuilder {

	private final List<MapNode> nodes = new ArrayList<>();
	private final List<MapWay> ways = new ArrayList<>();
	private final List<MapWaySegment> waySegments = new ArrayList<>();
	private final List<MapArea> areas = new ArrayList<>();
	private final List<MapRelation> relations = new ArrayList<>();

	private int createdNodes = 0, createdWays = 0, createdRelations = 0;

	public MapNode createNode(double x, double z, TagSet tags) {
		VectorXZ pos = new VectorXZ(x, z);
		MapNode result = new MapNode(createdNodes ++, tags, pos);
		handleOverlaps(result);
		nodes.add(result);
		return result;
	}

	public MapNode createNode(double x, double z) {
		return createNode(x, z, TagSet.of());
	}

	public MapWay createWay(List<MapNode> wayNodes, TagSet tags) {
		MapWay result = new MapWay(createdWays ++, tags, wayNodes);
		result.getWaySegments().forEach(this::handleOverlaps);
		waySegments.addAll(result.getWaySegments());
		ways.add(result);
		return result;
	}

	public MapArea createWayArea(List<MapNode> wayNodes, TagSet tags) {
		wayNodes = closeLoop(wayNodes);
		MapArea result = new MapArea(createdWays ++, false, tags, wayNodes);
		handleOverlaps(result);
		areas.add(result);
		return result;
	}

	public MapArea createMultipolygonArea(List<MapNode> outerRing, List<List<MapNode>> innerRings, TagSet tags) {

		if (!tags.contains("type", "multipolygon")) {
			if (tags.containsKey("type")) {
				throw new IllegalArgumentException("Invalid type for multipolygon relation");
			} else {
				List<Tag> tagList = new ArrayList<>(tags.size() + 1);
				tagList.add(new Tag("type", "multipolygon"));
				tags.forEach(tagList::add);
				tags = TagSet.of(tagList);
			}
		}

		MapArea result = new MapArea(createdRelations ++, true, tags,
				closeLoop(outerRing), innerRings.stream().map(GeometryUtil::closeLoop).toList());
		handleOverlaps(result);
		areas.add(result);
		return result;

	}

	public MapRelation createRelation(List<Map.Entry<String, MapRelationElement>> members, TagSet tags) {

		if (tags.contains("type", "multipolygon")) {
			throw new IllegalArgumentException("type=multipolygon should use createMultipolygonArea");
		}

		MapRelation result = new MapRelation(createdRelations ++, tags);
		members.forEach(m -> result.addMember(m.getKey(), m.getValue()));
		relations.add(result);
		return result;

	}

	/** @see #build(AxisAlignedRectangleXZ, MapMetadata) */
	public MapData build() {
		return new MapData(nodes, ways, areas, relations, null, new MapMetadata(null, null));
	}

	/**
	 * returns a {@link MapData} object containing all the elements created so far
	 * @param boundary  a data boundary, equivalent to the &lt;bounds&gt; element of an .osm file
	 * @param metadata  metadata that would usually be derived from information outside the data boundary
	 */
	public MapData build(@Nullable AxisAlignedRectangleXZ boundary, @Nullable MapMetadata metadata) {
		metadata = (metadata == null) ? new MapMetadata(null, null) : metadata;
		return new MapData(nodes, ways, areas, relations, boundary, metadata);
	}

	/**
	 * tests for intersections and other types of overlaps between a new element and existing elements.
	 * If any are detected, {@link MapOverlap} instances are created and assigned.
	 */
	private void handleOverlaps(MapElement newElement) {
		for (var elements : List.of(nodes, waySegments, areas)) {
			for (var element : elements) {
				OSMToMapDataConverter.addOverlapBetween(element, newElement);
			}
		}
	}

}
