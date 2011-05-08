package org.osm2world.core.terrain.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.terrain.creation.TemporaryElevationStorage;

/**
 * the "normal" {@link TerrainPatch} implementation.
 * Has 2D outer polygons and holes, is triangulated,
 * and elevation data is added to the resulting triangles.
 */
public class GenericTerrainPatch extends TerrainPatch {
	
	private final PolygonWithHolesXZ polygon;
	private final Collection<VectorXZ> points;
	private final TemporaryElevationStorage eleStorage;
	
	public GenericTerrainPatch(PolygonWithHolesXZ polygon,
			Collection<VectorXZ> points,
			TemporaryElevationStorage eleStorage) {
		
		this.polygon = polygon;
		this.points = points;
		this.eleStorage = eleStorage;
		
	}
	
	public PolygonWithHolesXZ getPolygon() {
		return polygon;
	}
	
	public Collection<VectorXZ> getPoints() {
		return points;
	}
	
	@Override
	public void build() {
		
		if (triangulation != null) {
			throw new IllegalStateException(
					"this patch has already been triangulated");
		}
		
		/* perform triangulation */
		
		List<TriangleXZ> trianglesXZ =
			TriangulationUtil.triangulate(polygon, points);
		
		/* write triangulation, restoring the third (y) dimension */
		
		triangulation = new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			TriangleXZ ccwTriangle = triangleXZ.makeCounterclockwise();
			triangulation.add(eleStorage.restoreElevationForTriangle(ccwTriangle));
		}
		
	}
	
}
