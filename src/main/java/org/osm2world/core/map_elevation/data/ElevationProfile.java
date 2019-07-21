package org.osm2world.core.map_elevation.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * abstract superclass for all {@link MapElement}s' elevation profiles
 */
abstract public class ElevationProfile {

	public abstract double getEleAt(VectorXZ pos);

	public VectorXYZ getWithEle(VectorXZ pos) {
		return pos.xyz(getEleAt(pos));
	}

	public List<VectorXYZ> getWithEle(List<VectorXZ> posList) {
		ArrayList<VectorXYZ> result = new ArrayList<VectorXYZ>(posList.size());
		for (VectorXZ pos : posList) {
			result.add(getWithEle(pos));
		}
		return result;
	}

	public abstract Collection<VectorXYZ> getPointsWithEle();

	public abstract double getMinEle();
	public abstract double getMaxEle();

	abstract protected MapElement getElement();

}
