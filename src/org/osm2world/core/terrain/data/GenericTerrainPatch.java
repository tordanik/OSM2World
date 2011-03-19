package org.osm2world.core.terrain.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.terrain.creation.TemporaryElevationStorage;

/**
 * the "normal" {@link TerrainPatch} implementation.
 * Has 2D outer polygons and holes, is triangulated,
 * and elevation data is added to the resulting triangles.
 */
public class GenericTerrainPatch extends TerrainPatch {
	
	private final SimplePolygonXZ outerPolygon;
	private final Collection<SimplePolygonXZ> holes;
	private final TemporaryElevationStorage eleStorage;
	
	public GenericTerrainPatch(SimplePolygonXZ outerPoly,
			Collection<SimplePolygonXZ> holes,
			TemporaryElevationStorage eleStorage) {
		
		this.outerPolygon = outerPoly;
		this.holes = holes;
		this.eleStorage = eleStorage;
		
	}
	
	public SimplePolygonXZ getOuterPolygon() {
		return outerPolygon;
	}

	public Collection<SimplePolygonXZ> getHoles() {
		return holes;
	}
		
	@Override
	public void build() {

		if (triangulation != null) {
			throw new IllegalStateException("this patch has already been triangulated");
		}
		
		/* perform triangulation */
		
		List<TriangleXZ> trianglesXZ = 
			TriangulationUtil.triangulate(outerPolygon, holes);
					
		/* write triangulation, restoring the third (y) dimension */
		
		triangulation = new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			TriangleXZ ccwTriangle = triangleXZ.makeCounterclockwise();
			triangulation.add(eleStorage.restoreElevationForTriangle(ccwTriangle));
		}
				
	}
		
}
