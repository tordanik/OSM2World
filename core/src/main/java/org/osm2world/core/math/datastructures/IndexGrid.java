package org.osm2world.core.math.datastructures;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;

/**
 * a data structure that can be used to quickly find candidates for intersection tests and similar geometric operations.
 *
 * An {@link BoundedObject} is added to all grid cells that are at least
 * partially covered by the object's axis-aligned bounding box.
 * When testing for intersections or inclusions, only elements in the same cell need to be compared.
 */
public class IndexGrid<T extends BoundedObject> implements SpatialIndex<T> {

	private final AxisAlignedRectangleXZ gridBounds;
	private Collection<T>[][] cells;

	private final int cellCountX, cellCountZ;

	private final double cellSizeX, cellSizeZ;

	public IndexGrid(AxisAlignedRectangleXZ gridBounds,
			int cellCountX, int cellCountZ) {

		this.gridBounds = gridBounds;

		@SuppressWarnings("unchecked") //cannot create generic array
		Collection<T>[][] newCells
			= new Collection[cellCountX][cellCountZ];

		this.cells = newCells;

		this.cellCountX = cellCountX;
		this.cellCountZ = cellCountZ;

		this.cellSizeX = gridBounds.sizeX() / cellCountX;
		this.cellSizeZ = gridBounds.sizeZ() / cellCountZ;

	}

	/**
	 * alternative constructor that uses a target cell size to calculate
	 * the number of cells
	 */
	public IndexGrid(AxisAlignedRectangleXZ gridBounds,
			double approxCellSizeX, double approxCellSizeZ) {
		this(gridBounds,
				((int) (gridBounds.sizeX() / approxCellSizeX)) + 1,
				((int) (gridBounds.sizeZ() / approxCellSizeZ)) + 1);
	}

	public Collection<T>[][] getCellArray() {
		return cells;
	}

	/**
	 * returns the content object collections for all non-empty cells
	 */
	@Override
	public Iterable<Collection<T>> getLeaves() {
		return CellIterator::new;
	}

	/**
	 * read-only iterator for non-null cells
	 */
	private class CellIterator implements Iterator<Collection<T>> {

		int x =  0;
		int z = -1;

		public CellIterator() {
			toNext();
		}

		@Override
		public boolean hasNext() {
			return x < cellCountX;
		}

		@Override
		public Collection<T> next() {
			Collection<T> result = cells[x][z];
			toNext();
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void toNext() {
			do {
				z ++;
				if (z >= cellCountZ) {
					z = 0;
					x ++;
				}
			} while (x < cellCountX && cells[x][z] == null);
		}

	}

	/**
	 * returns all non-empty cells that would contain the object.
	 * Will not modify the intersection grid, and it doesn't matter
	 * whether the object has been inserted or not.
	 */
	@Override
	public Collection<Collection<T>> probeLeaves(BoundedObject object) {

		assert(gridBounds.contains(object.boundingBox()));

		AxisAlignedRectangleXZ objectAABB = object.boundingBox();

		int minCellX = cellXForCoord(objectAABB.minX);
		int minCellZ = cellZForCoord(objectAABB.minZ);
		int maxCellX = cellXForCoord(objectAABB.maxX);
		int maxCellZ = cellZForCoord(objectAABB.maxZ);

		if (minCellX == maxCellX && minCellZ == maxCellZ) {
			// special case for returning a single cell (very common for small objects or points)
			Collection<T> cell = cells[minCellX][minCellZ];
			return cell == null ? emptySet() : singleton(cell);
		} else {

			Collection<Collection<T>> result = new ArrayList<>((maxCellX - minCellX) * (maxCellZ - minCellZ));

			for (int cellX = minCellX; cellX <= maxCellX; cellX ++) {
				for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ ++) {
					if (cells[cellX][cellZ] != null) {
						result.add(cells[cellX][cellZ]);
					}
				}
			}

			return result;

		}

	}

	@Override
	public void insert(T object) {

		assert(gridBounds.contains(object.boundingBox()));

		AxisAlignedRectangleXZ objectAABB = object.boundingBox();

		int minCellX = cellXForCoord(objectAABB.minX);
		int minCellZ = cellZForCoord(objectAABB.minZ);
		int maxCellX = cellXForCoord(objectAABB.maxX);
		int maxCellZ = cellZForCoord(objectAABB.maxZ);

		for (int cellX = minCellX; cellX <= maxCellX; cellX ++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ ++) {
				addToCell(cellX, cellZ, object);
			}
		}

	}

	private void addToCell(int cellX, int cellZ, T object) {
		if (cells[cellX][cellZ] == null) {
			cells[cellX][cellZ] = new ArrayList<>();
		}
		cells[cellX][cellZ].add(object);
	}

	public void remove(T object) {

		assert(gridBounds.contains(object.boundingBox()));

		AxisAlignedRectangleXZ objectAABB = object.boundingBox();

		int minCellX = cellXForCoord(objectAABB.minX);
		int minCellZ = cellZForCoord(objectAABB.minZ);
		int maxCellX = cellXForCoord(objectAABB.maxX);
		int maxCellZ = cellZForCoord(objectAABB.maxZ);

		for (int cellX = minCellX; cellX <= maxCellX; cellX ++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ ++) {
				if (cells[cellX][cellZ] != null) {
					cells[cellX][cellZ].remove(object);
				}
			}
		}

	}

	/**
	 * returns the x index of the cell that contains the coordinate
	 */
	public final int cellXForCoord(double x) {
		return max(0, min(cellCountX - 1, (int) ((x - gridBounds.minX) / cellSizeX)));
	}

	/**
	 * returns the z index of the cell that contains the coordinate
	 */
	public final int cellZForCoord(double z) {
		return max(0, min(cellCountZ - 1, (int) ((z - gridBounds.minZ) / cellSizeZ)));
	}

}
