package org.osm2world.core.world.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.AreaElevationProfile;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * implementation of {@link AreaWorldObject} that offers some basic features:
 * < ul><li> providing the object outline based on the {@link MapArea}
 * </li><li> providing bounding geometry for intersection tests
 * </li><li> calculating a triangulation of the surface for rendering
 * </ul>
 */
public abstract class AbstractAreaWorldObject
	implements WorldObjectWithOutline, AreaWorldObject,
		IntersectionTestObject {
	
	protected final MapArea area;

	protected AbstractAreaWorldObject(MapArea area) {
		this.area = area;
	}
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		return area.getPolygon().getOuter().makeCounterclockwise();
	}

	@Override
	public PolygonXYZ getOutlinePolygon() {
		
		List<VectorXZ> vertices = getOutlinePolygonXZ().getVertexLoop();
		List<VectorXYZ> vs = new ArrayList<VectorXYZ>(vertices.size()+1);
		
		for (int i = 0; i < vertices.size(); i++) {
			VectorXZ pos = vertices.get(i);
			vs.add( pos.xyz(area.getElevationProfile().getEleAt(pos)) );
		}
		
		vs.add(vs.get(0));
				
		return new PolygonXYZ(vs);
				
	}

	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(area.getOuterPolygon().getVertices());
	}
	
	@Override
	public MapElement getPrimaryMapElement() {
		return area;
	}

	/**
	 * decompose a given polygon into counterclockwise triangles,
	 * using this area's elevation data.
	 */
	protected Collection<TriangleXYZ> getTriangulation(
			PolygonWithHolesXZ polygon) {
		
		final AreaElevationProfile eleProfile = area.getElevationProfile();
		
		Collection<TriangleXZ> trianglesXZ =
			TriangulationUtil.triangulate(polygon);
		
		Collection<TriangleXYZ> trianglesXYZ =
			new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			VectorXYZ v1 = eleProfile.getWithEle(triangleXZ.v1);
			VectorXYZ v2 = eleProfile.getWithEle(triangleXZ.v2);
			VectorXYZ v3 = eleProfile.getWithEle(triangleXZ.v3);
			if (triangleXZ.isClockwise()) {
				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
			} else  {
				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
			}
		}
		
		return trianglesXYZ;
		
	}
	
	/**
	 * decompose this area into counterclockwise triangles
	 */
	protected Collection<TriangleXYZ> getTriangulation() {
		
		return getTriangulation(area.getPolygon());
				
	}
	
}
