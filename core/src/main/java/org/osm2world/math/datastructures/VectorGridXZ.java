package org.osm2world.math.datastructures;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

import java.util.*;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.LineSegmentXZ;

/**
 * Regular grid of {@link VectorXZ}.
 * <p>
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

		startX = (int)ceil(box.minX / sampleDistance);
		startZ = (int)ceil(box.minZ / sampleDistance);

		int endX = (int)floor(box.maxX / sampleDistance);
		int endZ = (int)floor(box.maxZ / sampleDistance);

		int numSamplesX = endX - startX + 1;
		int numSamplesZ = endZ - startZ + 1;

		if (numSamplesX <= 0 || numSamplesZ <= 0) {

			grid = EMPTY_GRID;

		} else {

			grid = new VectorXZ[numSamplesX][numSamplesZ];

		}

	}

	private VectorGridXZ() {
		this.grid = EMPTY_GRID;
		this.sampleDistance = 0;
		this.startX = 0;
		this.startZ = 0;
	}

	public static VectorGridXZ emptyGrid() {
		return new VectorGridXZ();
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
	public @Nonnull Iterator<VectorXZ> iterator() {

		if (isEmpty()) {
		    return Collections.emptyIterator();
		} else {

			List<Iterator<? extends VectorXZ>> columnIterators = new ArrayList<>(sizeX());

			for (int x = 0; x < sizeX(); x++) {

				for (int z = 0; z < sizeZ(); z++) {
					createIfNecessary(x, z);
				}

				columnIterators.add(new ArrayIterator<>(grid[x]));

			}

			return new IteratorChain<>(columnIterators);

		}

	}

	private void createIfNecessary(int indexX, int indexZ) {

		if (grid[indexX][indexZ] == null) {

			grid[indexX][indexZ] = new VectorXZ(
					(startX + indexX) * sampleDistance,
					(startZ + indexZ) * sampleDistance);

		}

	}

	public Collection<AxisAlignedRectangleXZ> gridCells() {
		List<AxisAlignedRectangleXZ> cells = new ArrayList<>((sizeX() - 1) * (sizeZ() - 1));
		for (int x = 0; x + 1 < sizeX(); x++) {
			for (int z = 0; z + 1 < sizeZ(); z++) {
				cells.add(new AxisAlignedRectangleXZ(
						startX + x * sampleDistance,
						startZ + z * sampleDistance,
						startX + (x + 1) * sampleDistance,
						startZ + (z + 1) * sampleDistance));
			}
		}
		return cells;
	}

	/** returns all (inner) lines connecting points horizontally or vertically */
	public Collection<LineSegmentXZ> gridLines() {

		List<LineSegmentXZ> result = new ArrayList<>();

		for (int x = 0; x + 1 < sizeX(); x++) {
			for (int z = 0; z + 1 < sizeZ(); z++) {
				if (z > 0) {
					result.add(new LineSegmentXZ(get(x, z), get(x + 1, z)));
				}
				if (x > 0) {
					result.add(new LineSegmentXZ(get(x, z), get(x, z + 1)));
				}
			}
		}

		return result;

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
