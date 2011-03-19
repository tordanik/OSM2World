package org.osm2world.core.map_data.data;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * OSM2World's abstraction of {@link OSMData}, consists of {@link MapElement}s.
 * Initially contains only a slightly altered representation of OSM
 * map data. During later conversion steps, additional information is
 * added to the {@link MapElement}s.
 */
public class MapData {

	final List<MapNode> mapNodes;
	final List<MapWaySegment> mapWaySegments;
	final List<MapArea> mapAreas;

	AxisAlignedBoundingBoxXZ boundary;
	
	public MapData(List<MapNode> mapNodes, List<MapWaySegment> mapWaySegments,
			List<MapArea> mapAreas) {

		this.mapNodes = mapNodes;
		this.mapWaySegments = mapWaySegments;
		this.mapAreas = mapAreas;

		calculateBoundary();
		
	}
	
	private void calculateBoundary() {

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;			
		
		for (MapNode node : mapNodes) {
			final double nodeX = node.getPos().x;
			final double nodeZ = node.getPos().z;
			if (nodeX < minX) { minX = nodeX; }
			if (nodeX > maxX) { maxX = nodeX; }
			if (nodeZ < minZ) { minZ = nodeZ; }
			if (nodeZ > maxZ) { maxZ = nodeZ; }
		}
		
		boundary = new AxisAlignedBoundingBoxXZ(minX, minZ, maxX, maxZ);
		
	}

	public Iterable<MapElement> getMapElements() {
		return Iterables.concat(mapNodes, mapWaySegments, mapAreas);
	}

	public Collection<MapArea> getMapAreas() {
		return mapAreas;
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
	public AxisAlignedBoundingBoxXZ getBoundary() {
		return boundary;
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
	 * Can be used, for example, to access all
	 * {@link TerrainBoundaryWorldObject}s in the grid.
	 */
	public <T> Iterable<T> getWorldObjects(Class<T> type) {
		return Iterables.filter(getWorldObjects(), type);
	}
		
}
