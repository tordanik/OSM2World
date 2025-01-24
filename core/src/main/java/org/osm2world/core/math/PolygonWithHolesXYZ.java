package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.List;

/**
 * a three-dimensional polygon that may have inner rings
 */
public record PolygonWithHolesXYZ(PolygonXYZ outer, List<PolygonXYZ> holes) implements BoundedObject {

	public List<PolygonXYZ> rings() {
		List<PolygonXYZ> result = new ArrayList<>(holes().size() + 1);
		result.add(outer());
		result.addAll(holes());
		return result;
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return AxisAlignedRectangleXZ.bboxUnion(rings());
	}

}
