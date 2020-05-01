package org.osm2world.core.world.modules.building;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;

/** the roof of a {@link BuildingPart} */
interface Roof extends Renderable {

	/**
	 * returns the outline (with holes) of the roof.
	 * The shape will be generally identical to that of the
	 * building itself, but additional vertices might have
	 * been inserted into segments.
	 */
	PolygonWithHolesXZ getPolygon();

	/**
	 * returns roof elevation at a position.
	 */
	double getRoofEleAt(VectorXZ coord);

	/**
	 * returns maximum roof height
	 */
	double getRoofHeight();

	/**
	 * returns maximum roof elevation
	 */
	double getMaxRoofEle();

}