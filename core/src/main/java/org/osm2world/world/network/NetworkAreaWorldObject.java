package org.osm2world.world.network;

import java.util.Collection;
import java.util.List;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.world.data.AbstractAreaWorldObject;

public abstract class NetworkAreaWorldObject extends AbstractAreaWorldObject {

	public NetworkAreaWorldObject(MapArea area) {
		super(area);
	}

	@Override
	public Collection<PolygonShapeXZ> getRawGroundFootprint() {
		return List.of(getOutlinePolygonXZ());
	}

}
