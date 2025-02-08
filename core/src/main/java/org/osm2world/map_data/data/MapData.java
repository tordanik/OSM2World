package org.osm2world.map_data.data;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.world.data.WorldObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * OSM2World's abstraction of OSM data, consists of {@link MapElement}s.
 *
 * Unlike raw osm data, which uses long/lat, this is projected to a local coordinate system.
 * It's also slightly more convenient to work with because it uses object references
 * to represent relationships between elements (such as nodes of ways and members of relations,
 * but also inverse lookup – e.g. of the ways a node is contained in –,
 * and implicit relationships like intersections).
 */
public class MapData {

	final List<MapNode> mapNodes;
	final List<MapWay> mapWays;
	final List<MapWaySegment> mapWaySegments;
	final List<MapArea> mapAreas;
	final List<MapRelation> mapRelations;

	AxisAlignedRectangleXZ fileBoundary;
	AxisAlignedRectangleXZ dataBoundary;

	final MapMetadata metadata;

	public MapData(List<MapNode> mapNodes, List<MapWay> mapWays, List<MapArea> mapAreas,
			List<MapRelation> mapRelations, AxisAlignedRectangleXZ fileBoundary, MapMetadata metadata) {

		this.mapNodes = mapNodes;
		this.mapWays = mapWays;
		this.mapAreas = mapAreas;
		this.mapRelations = mapRelations;
		this.fileBoundary = fileBoundary;

		mapWaySegments = mapWays.stream().flatMap(w -> w.getWaySegments().stream()).collect(toList());

		calculateDataBoundary();

		this.metadata = metadata;

	}

	private void calculateDataBoundary() {

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		if (fileBoundary != null) {
			// use the file boundary as the minimum extent of the data boundary
			minX = fileBoundary.minX;
			maxX = fileBoundary.maxX;
			minZ = fileBoundary.minZ;
			maxZ = fileBoundary.maxZ;
		}

		for (MapNode node : mapNodes) {
			final double nodeX = node.getPos().x;
			final double nodeZ = node.getPos().z;
			if (nodeX < minX) { minX = nodeX; }
			if (nodeX > maxX) { maxX = nodeX; }
			if (nodeZ < minZ) { minZ = nodeZ; }
			if (nodeZ > maxZ) { maxZ = nodeZ; }
		}

		dataBoundary = new AxisAlignedRectangleXZ(minX, minZ, maxX, maxZ);

	}

	public Iterable<MapElement> getMapElements() {
		return Iterables.concat(mapNodes, mapWaySegments, mapAreas);
	}

	public List<MapRelation> getMapRelations() {
		return mapRelations;
	}

	public Collection<MapArea> getMapAreas() {
		return mapAreas;
	}

	public Collection<MapWay> getMapWays() {
		return mapWays;
	}

	public Collection<MapWaySegment> getMapWaySegments() {
		return mapWaySegments;
	}

	public Collection<MapNode> getMapNodes() {
		return mapNodes;
	}

	/**
	 * returns a rectangular boundary polygon from the minimum/maximum of
	 * coordinates in the map data
	 */
	public AxisAlignedRectangleXZ getDataBoundary() {
		return dataBoundary;
	}

	/**
	 * returns a boundary based on the bounds in the input file if available,
	 * otherwise returns the same as {@link #getDataBoundary()}
	 */
	public AxisAlignedRectangleXZ getBoundary() {
		if (fileBoundary != null) {
			return fileBoundary;
		} else {
			return dataBoundary;
		}
	}

	/**
	 * calculates the center from the {@link MapNode}s' positions
	 */
	public VectorXZ getCenter() {

		int nodeCount = getMapNodes().size();

		double avgX = 0, avgZ = 0;
		for (MapNode node : getMapNodes()) {
			avgX += node.getPos().x / nodeCount; // need to divide before
													// numbers get too large
			avgZ += node.getPos().z / nodeCount;
		}

		return new VectorXZ(avgX, avgZ);

	}

	/**
	 * returns all {@link WorldObject}s from elements in this data set.
	 */
	public Iterable<WorldObject> getWorldObjects() {

		return Iterables.concat(
				Iterables.transform(getMapElements(),
						new Function<MapElement, Iterable<? extends WorldObject>>() {
					@Override public Iterable<? extends WorldObject> apply(MapElement e) {
						return e.getRepresentations();
					}
				}));

	}

	/**
	 * returns all {@link WorldObject}s from elements in this data set
	 * that are instances of a certain type.
	 */
	public <T> Iterable<T> getWorldObjects(Class<T> type) {
		return Iterables.filter(getWorldObjects(), type);
	}

}
