package org.osm2world.core.heightmap.data;

import java.util.Collection;

import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;

public interface TerrainElevationCell extends BoundedObject {

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
