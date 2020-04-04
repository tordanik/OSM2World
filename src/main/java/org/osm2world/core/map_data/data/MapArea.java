package org.osm2world.core.map_data.data;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;
import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.AreaWorldObject;

import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;


/**
 * An area (closed way or multipolygon relation) from an OSM dataset.
 *
 * @See {@link MapData} for context
 */
public class MapArea extends MapRelation.Element implements MapElement {

	private final OsmEntity objectWithTags;

	private final List<MapNode> nodes;
	private final List<List<MapNode>> holes;

	private final PolygonWithHolesXZ polygon;

	private Collection<MapAreaSegment> areaSegments;

	@SuppressWarnings("unchecked") //is later checked for EMPTY_LIST using ==
	private Collection<MapOverlap<?,?>> overlaps = Collections.EMPTY_LIST;

	private List<AreaWorldObject> representations =
		new ArrayList<AreaWorldObject>(1);

	public MapArea(OsmEntity objectWithTags, List<MapNode> nodes) {
		this(objectWithTags, nodes, emptyList());
	}

	public MapArea(OsmEntity objectWithTags, List<MapNode> nodes,
			List<List<MapNode>> holes) {

		this.objectWithTags = objectWithTags;
		this.nodes = nodes;
		this.holes = holes;

		try {

			this.polygon = convertToPolygon(nodes, holes);

			finishConstruction();

		} catch (InvalidGeometryException e) {
			throw new InvalidGeometryException(
					"outer polygon is not simple for this object: "
					+ objectWithTags, e);
		}

	}

	public MapArea(OsmEntity objectWithTags, List<MapNode> nodes,
			List<List<MapNode>> holes, PolygonWithHolesXZ polygon) {

		this.objectWithTags = objectWithTags;
		this.nodes = nodes;
		this.holes = holes;
		this.polygon = polygon;

		finishConstruction();

	}

	private static final PolygonWithHolesXZ convertToPolygon(
			List<MapNode> nodes, List<List<MapNode>> holes) {

		SimplePolygonXZ outerPolygon =
				polygonFromMapNodeLoop(nodes);

		List<SimplePolygonXZ> holePolygons =
			new ArrayList<SimplePolygonXZ>(holes.size());
		for (List<MapNode> hole : holes) {
			holePolygons.add(polygonFromMapNodeLoop(hole));
		}

		return new PolygonWithHolesXZ(outerPolygon, holePolygons);

	}

	/** shared functionality used by multiple constructors */
	private void finishConstruction() {

		areaSegments = new ArrayList<MapAreaSegment>();

		for (List<MapNode> ring : getRings()) {

			boolean isOuter = ring == nodes;

			SimplePolygonXZ ringPolygon = isOuter
					? polygon.getOuter()
					: polygon.getHoles().get(holes.indexOf(ring));

			for (int v = 0; v+1 < ring.size(); v++) {
				//relies on duplication of first/last node

				MapAreaSegment segment = new MapAreaSegment(this,
						isOuter ? ringPolygon.isClockwise() : !ringPolygon.isClockwise(),
						ring.get(v), ring.get(v+1));

				segment.getStartNode().addAdjacentArea(this, segment);
				segment.getEndNode().addAdjacentArea(this, segment);

				areaSegments.add(segment);

			}

		}

	}

	public static final SimplePolygonXZ polygonFromMapNodeLoop(
			List<MapNode> nodes) {

		List<VectorXZ> vertices = new ArrayList<VectorXZ>(nodes.size());

		for (MapNode node : nodes) {
			vertices.add(node.getPos());
		}

		return new SimplePolygonXZ(vertices);

	}

	public List<MapNode> getBoundaryNodes() {
		return nodes;
	}

	public Collection<List<MapNode>> getHoles() {
		return holes;
	}

	/** returns all outer and inner rings as lists of nodes: {@link #getHoles()} plus {@link #getBoundaryNodes()}. */
	public Collection<List<MapNode>> getRings() {
		if (holes.isEmpty()) {
			return singletonList(nodes);
		} else {
			List<List<MapNode>> result = new ArrayList<>(holes);
			result.add(0, nodes);
			return result;
		}
	}

	/** returns the {@link OsmWay} or {@link OsmRelation} with the tags for this area */
	@Override
	public OsmEntity getOsmElement() {
		return objectWithTags;
	}

	@Override
	public TagGroup getTags() {
		return new MapBasedTagGroup(getTagsAsMap(objectWithTags));
	}

	/**
	 * returns the area as a polygon.
	 */
	public PolygonWithHolesXZ getPolygon() {
		return polygon;
	}

	public SimplePolygonXZ getOuterPolygon() {
		return getPolygon().getOuter();
	}

	/**
	 * returns the segments making up this area's outer and inner boundaries
	 */
	public Collection<MapAreaSegment> getAreaSegments() {
		return areaSegments;
	}

	@Override
	public List<AreaWorldObject> getRepresentations() {
		return representations;
	}

	@Override
	public AreaWorldObject getPrimaryRepresentation() {
		if (representations.isEmpty()) {
			return null;
		} else {
			return representations.get(0);
		}
	}

	/**
	 * adds a visual representation for this area
	 */
	public void addRepresentation(AreaWorldObject representation) {
		this.representations.add(representation);
	}

	public void addOverlap(MapOverlap<?, ?> overlap) {
		assert overlap.e1 == this || overlap.e2 == this;
		if (overlaps == Collections.EMPTY_LIST) {
			overlaps = new ArrayList<MapOverlap<?,?>>();
		}
		overlaps.add(overlap);
	}

	@Override
	public Collection<MapOverlap<?,?>> getOverlaps() {
		return overlaps;
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return getOuterPolygon().boundingBox();
	}

	@Override
	public String toString() {
		if (objectWithTags instanceof OsmWay) {
			return "w" + objectWithTags.getId();
		} else {
			assert objectWithTags instanceof OsmRelation;
			return "r" + objectWithTags.getId();
		}
	}

}
