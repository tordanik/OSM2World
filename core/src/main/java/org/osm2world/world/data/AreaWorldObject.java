package org.osm2world.world.data;

import javax.annotation.Nonnull;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;

public interface AreaWorldObject extends WorldObject {

	@Override
	MapArea getPrimaryMapElement();

	/**
	 * @see WorldObject#getOutlinePolygonXZ()
	 * @return the {@link PolygonWithHolesXZ} covered by {@link #getPrimaryMapElement()}
	 */
	@Override
	default	@Nonnull PolygonShapeXZ getOutlinePolygonXZ() {
		MapArea area = getPrimaryMapElement();
		if (!area.getPolygon().getOuter().isClockwise()) {
			return area.getPolygon();
		} else {
			return new PolygonWithHolesXZ(
					area.getPolygon().getOuter().makeCounterclockwise(),
					area.getPolygon().getHoles());
		}
	}

}
