package org.osm2world.core.math;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.collections.iterators.IteratorChain;

/**
 * regular grid of {@link VectorXZ}.
 *
 * Individual points are created only when they are first accessed,
 * so no memory is wasted on unused points.
 */
public class VectorGridXZ implements Iterable<VectorXZ> {

	private static final VectorXZ[][] EMPTY_GRID = new VectorXZ[0][0];

	private final VectorXZ[][] grid;

	private final double sampleDistance;

	private final int startX;
	private final int startZ;

	/**
	 * returns a regular grid of points within a bounding box.
	 */
	public VectorGridXZ(AxisAlignedRectangleXZ box, double sampleDistance) {

		this.sampleDistance = sampleDistance;

		startX = (int)ceil((box.minX + 0.01) / sampleDistance);
		startZ = (int)ceil((box.minZ + 0.01) / sampleDistance);

		int endX = (int)floor((box.maxX - 0.01) / sampleDistance);
		int endZ = (int)floor((box.maxZ - 0.01) / sampleDistance);

		int numSamplesX = endX - startX + 1;
		int numSamplesZ = endZ - startZ + 1;

		if (numSamplesX <= 0 || numSamplesZ <= 0) {

			grid = EMPTY_GRID;

		} else {

			grid = new VectorXZ[numSamplesX][numSamplesZ];

		}

	}

	public int size() {
		return sizeX() * sizeZ();
	}

	public int sizeX() {
		return grid.length;
	}

	public int sizeZ() {
		return grid == EMPTY_GRID ? 0 : grid[0].length;
	}

	public boolean isEmpty() {
		return grid == EMPTY_GRID;
	}

	public VectorXZ get(int indexX, int indexZ) {

		createIfNecessary(indexX, indexZ);

		return grid[indexX][indexZ];

	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<VectorXZ> iterator() {

		if (isEmpty()) {
		    return Collections.EMPTY_LIST.iterator();
		} else {

			List<Iterator<VectorXZ>> columnIterators =
					new ArrayList<Iterator<VectorXZ>>(sizeX());

			for (int x = 0; x < sizeX(); x++) {

				for (int z = 0; z < sizeZ(); z++) {
					createIfNecessary(x, z);
				}

				columnIterators.add(new ArrayIterator(grid[x]));

			}

			return new IteratorChain(columnIterators);

		}

	}

	private void createIfNecessary(int indexX, int indexZ) {

		if (grid[indexX][indexZ] == null) {

			grid[indexX][indexZ] = new VectorXZ(
					(startX + indexX) * sampleDistance,
					(startZ + indexZ) * sampleDistance);

		}

	}

	@Override
	public String toString() {

		if (isEmpty()) {

			return "{0*0}";

		} else {

			return "{" + sizeX() + "*" + sizeZ() + ", start " + get(0,0) + "}";

		}

	}

}
