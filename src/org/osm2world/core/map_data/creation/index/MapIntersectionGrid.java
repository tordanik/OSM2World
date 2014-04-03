package org.osm2world.core.map_data.creation.index;

import java.util.Collection;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.datastructures.IntersectionGrid;


public class MapIntersectionGrid implements MapDataIndex {
	
	private final IntersectionGrid<MapElement> intersectionGrid;
			
	public MapIntersectionGrid(AxisAlignedBoundingBoxXZ dataBoundary) {
		
		AxisAlignedBoundingBoxXZ gridBounds = dataBoundary.pad(10);
		
		intersectionGrid = new IntersectionGrid<MapElement>(
				gridBounds,
				gridBounds.sizeX() / 50,
				gridBounds.sizeZ() / 50);
		
	}
	
	@Override
	public void insert(MapElement e) {
		intersectionGrid.insert(e);
	}
	
	@Override
	public Collection<? extends Iterable<MapElement>> insertAndProbe(MapElement e) {
		insert(e);
		return intersectionGrid.cellsFor(e);
	}
	
	@Override
	public Iterable<? extends Iterable<MapElement>> getLeaves() {
		return intersectionGrid.getCells();
	}
	
}
