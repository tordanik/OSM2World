package org.osm2world.map_elevation.creation;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.osm2world.util.MathUtil.clamp;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;

/**
 * An interpolator which exploits the grid structure of many {@link TerrainEleDataSource}s
 * by determining elevation locally based on the relative position in the cell of 4 surrounding sites.
 */
public class LocalCellInterpolator extends AbstractGridLocalInterpolator {

	@Override
	protected VectorXYZ interpolateEleFromSurroundingSites(VectorXZ pos, List<VectorXYZ> surroundingSites) {

		VectorXYZ bottomLeft = surroundingSites.get(0);
		VectorXYZ bottomRight = surroundingSites.get(1);
		VectorXYZ topLeft = surroundingSites.get(2);
		VectorXYZ topRight = surroundingSites.get(3);

		double leftX = max(bottomLeft.x, topLeft.x);
		double rightX = min(bottomRight.x, topRight.x);
		double bottomZ = max(bottomLeft.z, bottomRight.z);
		double topZ = min(topLeft.z, topRight.z);

		double impactRight = (pos.x - leftX) / (rightX - leftX);
		double impactTop = (pos.z - bottomZ) / (topZ - bottomZ);

		impactRight = clamp(impactRight, 0, 1);
		impactTop = clamp(impactTop, 0, 1);

		double y = bottomLeft.y * (1 - impactRight) * (1 - impactTop)
				+ bottomRight.y * impactRight * (1 - impactTop)
				+ topLeft.y * (1 - impactRight) * impactTop
				+ topRight.y * impactRight * impactTop;

		return pos.xyz(y);

	}

}
