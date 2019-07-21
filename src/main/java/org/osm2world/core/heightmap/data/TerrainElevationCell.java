package org.osm2world.core.heightmap.data;

import java.util.Collection;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

public interface TerrainElevationCell extends IntersectionTestObject {

	public TerrainPoint getTopLeft();
	public TerrainPoint getBottomLeft();
	public TerrainPoint getTopRight();
	public TerrainPoint getBottomRight();

	public Collection<TerrainPoint> getTerrainPoints();

	/** returns the counterclockwise polygon surrounding this cell. */
	public SimplePolygonXZ getPolygonXZ();

	/**
	 * returns 3d polygon surrounding this cell.
	 * ordering and XZ coordinates are the same as for {@link #getPolygonXZ()}.
	 */
	public PolygonXYZ getPolygonXYZ();

}
