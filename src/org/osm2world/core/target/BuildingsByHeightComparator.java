package org.osm2world.core.target;

import java.util.Comparator;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.BuildingModule.Building;
import org.osm2world.core.world.modules.BuildingModule.BuildingPart;
import org.osm2world.core.world.modules.BuildingModule.BuildingPart.Roof;

final class BuildingsByHeightComparator implements Comparator<MapArea> {
	
	public double getMaxEle(Building b) {
		double maxEle = 0.0;
		if (b != null) {
			for (BuildingPart part : b.getParts()) {
				Roof roof = part.getRoof();
				if (roof != null) {
					maxEle = Math.max(maxEle, roof.getMaxRoofEle()); 
				}
			}
			double areaAsEle = b.getArea().getOuterPolygon().getArea() / 200.0 * 4.0;
			maxEle += areaAsEle;
		}
		return maxEle;
	}

	@Override
	public int compare(MapArea o1, MapArea o2) {
		Building b1 = null;
		Building b2 = null;
		
		for(WorldObject wo1 : o1.getRepresentations()) {
			if (wo1 instanceof Building) {
				b1 = (Building) wo1;
			}
		}
		
		for(WorldObject wo2 : o2.getRepresentations()) {
			if (wo2 instanceof Building) {
				b2 = (Building) wo2;
			}
		}
		
		// From max to min
		return Double.compare(getMaxEle(b2), getMaxEle(b1));
	}
}