package org.osm2world.map_data.creation;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.map_data.data.MapNode;

public class MapDataCreationUtil {

	public static List<MapNode> withoutConsecutiveDuplicates(List<MapNode> nodes) {
		List<MapNode> result = new ArrayList<>();
		for (MapNode node : nodes) {
		    if (result.isEmpty() || node.getPos().distanceTo(result.get(result.size() - 1).getPos()) > 0) {
		        result.add(node);
			}
		}
		return result;
	}

}
