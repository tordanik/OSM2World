package org.osm2world.core.math.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;

/**
 * a data structure that can be used to speed up intersection tests.
 * 
 * An IntersectionTestObject is added to all grid cells that are at least
 * partially covered by the object's axis-aligned bounding box.
 * When testing for intersections or inclusions, only elements in the same
 * cell need to be compared.
 */
public class IntersectionGrid<T extends IntersectionTestObject> {
	
	private final AxisAlignedBoundingBoxXZ gridBounds;
	private Collection<T>[][] cells;
	
	private final int cellCountX, cellCountZ;
	
	private final double cellSizeX, cellSizeZ;
	
	public IntersectionGrid(AxisAlignedBoundingBoxXZ gridBounds,
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
	public IntersectionGrid(AxisAlignedBoundingBoxXZ gridBounds,
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
	public Iterable<Collection<T>> getCells() {
		return new Iterable<Collection<T>>() {
			@Override public Iterator<Collection<T>> iterator() {
				return new CellIterator();
			}
		};
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
	public Collection<Collection<T>> cellsFor(
			IntersectionTestObject object) {

		assert(gridBounds.contains(object));

		AxisAlignedBoundingBoxXZ objectAABB = object.getAxisAlignedBoundingBoxXZ();
		
		int minCellX = cellXForCoord(objectAABB.minX, objectAABB.minZ);
		int minCellZ = cellZForCoord(objectAABB.minX, objectAABB.minZ);
		int maxCellX = cellXForCoord(objectAABB.maxX, objectAABB.maxZ);
		int maxCellZ = cellZForCoord(objectAABB.maxX, objectAABB.maxZ);
		
		Collection<Collection<T>> result =
			new ArrayList<Collection<T>>(
					(maxCellX - minCellX) * (maxCellZ - minCellZ));
		
		for (int cellX = minCellX; cellX <= maxCellX; cellX ++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ ++) {
				if (cells[cellX][cellZ] != null) {
					result.add(cells[cellX][cellZ]);
				}
			}
		}
		
		return result;
		
	}
	
	public void insert(T object) {
		
		assert(gridBounds.contains(object));
		
		AxisAlignedBoundingBoxXZ objectAABB = object.getAxisAlignedBoundingBoxXZ();
		
		int minCellX = cellXForCoord(objectAABB.minX, objectAABB.minZ);
		int minCellZ = cellZForCoord(objectAABB.minX, objectAABB.minZ);
		int maxCellX = cellXForCoord(objectAABB.maxX, objectAABB.maxZ);
		int maxCellZ = cellZForCoord(objectAABB.maxX, objectAABB.maxZ);
		
		for (int cellX = minCellX; cellX <= maxCellX; cellX ++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ ++) {
				addToCell(cellX, cellZ, object);
			}
		}
		
	}

	private void addToCell(int cellX, int cellZ, T object) {
		if (cells[cellX][cellZ] == null) {
			cells[cellX][cellZ] = new ArrayList<T>();
		}
		cells[cellX][cellZ].add(object);
	}

	/**
	 * returns the x index of the cell that contains the coordinate
	 */
	private final int cellXForCoord(double x, double z) {
		return (int) ((x - gridBounds.minX) / cellSizeX);
	}
	
	/**
	 * returns the z index of the cell that contains the coordinate
	 */
	private final int cellZForCoord(double x, double z) {
		return (int) ((z - gridBounds.minZ) / cellSizeZ);
	}
	
}
