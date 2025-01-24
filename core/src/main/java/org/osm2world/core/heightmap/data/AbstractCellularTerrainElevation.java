package org.osm2world.core.heightmap.data;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

public abstract class AbstractCellularTerrainElevation implements
		CellularTerrainElevation {

	final int numPointsX;
	final int numPointsZ;

	final Collection<TerrainPoint> terrainPoints;
	final TerrainPoint[][] terrainPointGrid;

	@Override
	public Collection<TerrainPoint> getTerrainPoints() {
		return terrainPoints;
	}

	@Override
	public TerrainPoint[][] getTerrainPointGrid() {
		return terrainPointGrid;
	}

	@Override
	public PolygonXYZ getBoundaryPolygon() {

		List<VectorXYZ> vertices = new ArrayList<VectorXYZ>();

		// first row
		for (int x = 0; x < numPointsX; x++) {
			vertices.add(vectorXYZForPointAt(x, 0));
		}

		// last column
		for (int z = 1; z < numPointsZ - 1; z++) {
			vertices.add(vectorXYZForPointAt(numPointsX - 1, z));
		}

		// last row
		for (int x = numPointsX - 1; x >= 0; x--) {
			vertices.add(vectorXYZForPointAt(x, numPointsZ - 1));
		}

		// first column
		for (int z = numPointsZ - 2; z >= 0 /* [0][0] will be added again*/; z--) {
			vertices.add(vectorXYZForPointAt(0, z));
		}

		return new PolygonXYZ(vertices);

	}

	@Override
	public SimplePolygonXZ getBoundaryPolygonXZ() {

		List<VectorXZ> vertices = new ArrayList<VectorXZ>();

		// first row
		for (int x = 0; x < numPointsX; x++) {
			vertices.add(vectorXZForPointAt(x, 0));
		}

		// last column
		for (int z = 1; z < numPointsZ - 1; z++) {
			vertices.add(vectorXZForPointAt(numPointsX - 1, z));
		}

		// last row
		for (int x = numPointsX - 1; x >= 0; x--) {
			vertices.add(vectorXZForPointAt(x, numPointsZ - 1));
		}

		// first column
		for (int z = numPointsZ - 2; z >= 0 /* [0][0] will be added again*/; z--) {
			vertices.add(vectorXZForPointAt(0, z));
		}

		return new SimplePolygonXZ(vertices);

	}

	private VectorXYZ vectorXYZForPointAt(int x, int z) {
		TerrainPoint point = terrainPointGrid[x][z];
		return point.getPos().xyz(point.getEle());
	}

	private VectorXZ vectorXZForPointAt(int x, int z) {
		TerrainPoint point = terrainPointGrid[x][z];
		return point.getPos();
	}

	@Override
	public Iterable<? extends TerrainElevationCell> getCells() {

		return new Iterable<CellImpl>() {
			@Override public Iterator<CellImpl> iterator() {
				return new CellIterator();
			}
		};

	}

	//TODO (duplicated code): merge with independently written version from IntersectionGrid
	private final class CellIterator implements Iterator<CellImpl> {

		int currX = -1, currZ = 0;

		@Override
		public boolean hasNext() {
			return currX + 1 < numPointsX-1 || currZ + 2 < numPointsZ-1;
		}

		@Override
		public CellImpl next() {

			currX += 1;
			if (currX == numPointsX-1) {
				currZ += 1;
				currX = 0;
				if (currZ == numPointsZ-1) {
					throw new NoSuchElementException();
				}
			}

			return new CellImpl(currX, currZ);

		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private final class CellImpl implements TerrainElevationCell {

		private int leftXIndex;
		private int bottomZIndex;

		public CellImpl(int leftXIndex, int bottomZIndex) {

			assert leftXIndex + 1 < numPointsX
				&& bottomZIndex + 1 < numPointsZ;

			this.leftXIndex = leftXIndex;
			this.bottomZIndex = bottomZIndex;

		}

		@Override public final TerrainPoint getBottomLeft() {
			return terrainPointGrid[leftXIndex][bottomZIndex];
		}
		@Override public final TerrainPoint getTopLeft() {
			return terrainPointGrid[leftXIndex][bottomZIndex+1];
		}
		@Override public final TerrainPoint getBottomRight() {
			return terrainPointGrid[leftXIndex+1][bottomZIndex];
		}
		@Override public final TerrainPoint getTopRight() {
			return terrainPointGrid[leftXIndex+1][bottomZIndex+1];
		}

		@Override
		public Collection<TerrainPoint> getTerrainPoints() {
			List<TerrainPoint> terrainPoints = new ArrayList<TerrainPoint>(4);
			terrainPoints.add(terrainPointGrid[leftXIndex][bottomZIndex]);
			terrainPoints.add(terrainPointGrid[leftXIndex+1][bottomZIndex]);
			terrainPoints.add(terrainPointGrid[leftXIndex+1][bottomZIndex+1]);
			terrainPoints.add(terrainPointGrid[leftXIndex][bottomZIndex+1]);
			return terrainPoints;
		}

		@Override
		public final PolygonXYZ getPolygonXYZ() {
			List<VectorXYZ> vertices = new ArrayList<VectorXYZ>(5);
			vertices.add(vectorXYZForPointAt(leftXIndex, bottomZIndex));
			vertices.add(vectorXYZForPointAt(leftXIndex+1, bottomZIndex));
			vertices.add(vectorXYZForPointAt(leftXIndex+1, bottomZIndex+1));
			vertices.add(vectorXYZForPointAt(leftXIndex, bottomZIndex+1));
			vertices.add(vertices.get(0));
			return new PolygonXYZ(vertices);
		}

		@Override
		public final SimplePolygonXZ getPolygonXZ() {
			List<VectorXZ> vertices = new ArrayList<VectorXZ>(5);
			vertices.add(vectorXZForPointAt(leftXIndex, bottomZIndex));
			vertices.add(vectorXZForPointAt(leftXIndex+1, bottomZIndex));
			vertices.add(vectorXZForPointAt(leftXIndex+1, bottomZIndex+1));
			vertices.add(vectorXZForPointAt(leftXIndex, bottomZIndex+1));
			vertices.add(vertices.get(0));
			return new SimplePolygonXZ(vertices);
		}

		@Override
		public AxisAlignedRectangleXZ boundingBox() {
			return new AxisAlignedRectangleXZ(
					min(getTopLeft().getPos().x, getBottomLeft().getPos().x),
					min(getBottomLeft().getPos().z, getBottomRight().getPos().z),
					max(getTopRight().getPos().x, getBottomRight().getPos().x),
					max(getTopLeft().getPos().z, getTopRight().getPos().z));
		}

		@Override
		public String toString() {
			return "cell at x: " + leftXIndex + ", z: " + bottomZIndex;
		}

		/* auto-generated hashCode and equals follow */

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + bottomZIndex;
			result = prime * result + leftXIndex;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CellImpl other = (CellImpl) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (bottomZIndex != other.bottomZIndex)
				return false;
			if (leftXIndex != other.leftXIndex)
				return false;
			return true;
		}

		private AbstractCellularTerrainElevation getOuterType() {
			return AbstractCellularTerrainElevation.this;
		}

	}

	public AbstractCellularTerrainElevation(AxisAlignedRectangleXZ boundary,
			int numPointsX, int numPointsZ) {

		if (numPointsX < 2 || numPointsZ < 2) {
			throw new IllegalArgumentException("need at least 2x2 points for cell grid");
		}

		this.numPointsX = numPointsX;
		this.numPointsZ = numPointsZ;

		terrainPoints = new ArrayList<TerrainPoint>(numPointsX * numPointsZ);
		terrainPointGrid = new TerrainPoint[numPointsX][numPointsZ];

		for (int x = 0; x < numPointsX; x++) {
			for (int z = 0; z < numPointsZ; z++) {

				VectorXZ pos = new VectorXZ(boundary.minX + x * boundary.sizeX()
						/ (numPointsX - 1), boundary.minZ + z * boundary.sizeZ()
						/ (numPointsZ - 1));

				TerrainPoint terrainPoint = new TerrainPoint(pos,
						getElevation(pos));

				terrainPointGrid[x][z] = terrainPoint;
				terrainPoints.add(terrainPoint);

			}
		}

	}

	protected abstract Float getElevation(VectorXZ pos);

}
