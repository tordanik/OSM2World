package org.osm2world.core.map_data.creation.index;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.math.datastructures.IntersectionGrid;


public class MapIntersectionGrid implements MapDataIndex {
	
	private final IntersectionGrid<MapElement> intersectionGrid;
			
	public MapIntersectionGrid(MapData mapData) {
		
		intersectionGrid = new IntersectionGrid<MapElement>(
				mapData.getDataBoundary().pad(10),
				mapData.getDataBoundary().sizeX() / 50,
				mapData.getDataBoundary().sizeX() / 50);
		
		for (MapElement element : mapData.getMapElements()) {
			intersectionGrid.insert(element);
		}
		
	}
	
	@Override
	public Iterable<? extends Iterable<MapElement>> getLeaves() {
		return intersectionGrid.getCells();
	}
	
}
