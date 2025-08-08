package org.osm2world.map_data.creation;

import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;

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

	/**
	 * creates a multipolygon with a single outer ring.
	 * Use {@link #createMultipolygon(List, List, TagSet)} if you have more than one outer ring.
	 */
	public MapArea createMultipolygonArea(List<MapNode> outerRing, List<List<MapNode>> innerRings, TagSet tags) {

		tags = checkAndAddMultipolygonTag(tags);

		MapArea result = new MapArea(createdRelations ++, true, tags,
				closeLoop(outerRing), innerRings.stream().map(GeometryUtil::closeLoop).toList());
		handleOverlaps(result);
		areas.add(result);
		return result;

	}

	/**
	 * creates a multipolygon with one or more outer rings.
	 * If you know you have just a single outer ring, you can use {@link #createMultipolygonArea(List, List, TagSet)}.
	 *
	 * @return either a {@link MapArea} or a {@link MapMultipolygonRelation}
	 */
	public MapRelationElement createMultipolygon(List<List<MapNode>> outerRings, List<List<MapNode>> innerRings, TagSet tags) {

		tags = checkAndAddMultipolygonTag(tags);

		switch (outerRings.size()) {
			case 0: throw new IllegalArgumentException("at least one outer ring is required");
			case 1: return createMultipolygonArea(outerRings.get(0), innerRings, tags);
			default: {

				record Ring(List<MapNode> nodes, SimplePolygonXZ polygon) {
					Ring(List<MapNode> nodes) {
						this(nodes, MapArea.polygonFromMapNodeLoop(nodes));
					}
					boolean contains(Ring other) {
						return polygon.contains(other.polygon.getPointInside())
								&& polygon.getArea() > other.polygon.getArea();
					}
				}

				List<Ring> outers = outerRings.stream().map(GeometryUtil::closeLoop).map(Ring::new).toList();
				List<Ring> inners = innerRings.stream().map(GeometryUtil::closeLoop).map(Ring::new).toList();

				List<MapArea> areas = new ArrayList<>(outers.size());

				for (Ring outer : outers) {

					List<List<MapNode>> containedInnerRings = new ArrayList<>(inners.size());
					for (Ring inner : inners) {
						if (outer.contains(inner)) {

							boolean smallerOuterAlsoContainsInner = false;

							for (Ring otherOuter : outers) {
								if (otherOuter != outer && otherOuter.polygon.getArea() < outer.polygon.getArea()
								   && otherOuter.contains(inner)) {
									smallerOuterAlsoContainsInner = true;
									break;
								}
							}

							if (!smallerOuterAlsoContainsInner) {
								containedInnerRings.add(inner.nodes);
							}

						}
					}

					areas.add(createMultipolygonArea(outer.nodes, containedInnerRings, tags));

				}

				if (areas.stream().mapToInt(a -> a.getHoles().size()).sum() != innerRings.size()) {
					throw new IllegalArgumentException("not all inner rings are unambiguously contained in outer rings");
				}

				var result = new MapMultipolygonRelation(createdRelations ++, tags, areas);

				relations.add(result);
				return result;

			}
		}

	}

	public MapRelation createRelation(List<Map.Entry<String, MapRelationElement>> members, TagSet tags) {

		if (tags.contains("type", "multipolygon")) {
			throw new IllegalArgumentException("type=multipolygon should use createMultipolygon");
		}

		MapRelation result = new MapRelation(createdRelations ++, tags);
		members.forEach(m -> result.addMember(m.getKey(), m.getValue()));
		relations.add(result);
		return result;

	}

	/** @see #build(AxisAlignedRectangleXZ) */
	public MapData build() {
		return new MapData(nodes, ways, areas, relations, null);
	}

	/**
	 * returns a {@link MapData} object containing all the elements created so far
	 * @param boundary  a data boundary, equivalent to the &lt;bounds&gt; element of an .osm file
	 */
	public MapData build(@Nullable AxisAlignedRectangleXZ boundary) {
		return new MapData(nodes, ways, areas, relations, boundary);
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

	private static TagSet checkAndAddMultipolygonTag(TagSet tags) {
		if (!tags.contains("type", "multipolygon")) {
			if (tags.containsKey("type")) {
				throw new IllegalArgumentException("Invalid type for multipolygon relation");
			} else {
				List<Tag> tagList = new ArrayList<>(tags.size() + 1);
				tagList.add(new Tag("type", "multipolygon"));
				tags.forEach(tagList::add);
				return TagSet.of(tagList);
			}
		} else {
			return tags;
		}
	}

}
