package org.osm2world.core.world.data;

import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
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
	
	private final SimplePolygonXZ outlinePolygonXZ;
	private final EleConnectorGroup connectors;
	
	protected AbstractAreaWorldObject(MapArea area) {
		
		this.area = area;
		
		outlinePolygonXZ = area.getPolygon().getOuter().makeCounterclockwise();
		
		connectors = new EleConnectorGroup();
		connectors.addConnectorsFor(area.getPolygon());
		
	}
	
	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return connectors;
	}
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		return outlinePolygonXZ;
	}

	@Override
	public PolygonXYZ getOutlinePolygon() {
		return connectors.getPosXYZ(outlinePolygonXZ);
	}

	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(
				area.getOuterPolygon().getVertexCollection());
	}
	
	@Override
	public final MapArea getPrimaryMapElement() {
		return area;
	}

	/**
	 * decompose a given polygon into counterclockwise triangles,
	 * using this area's elevation data.
	 */
	protected Collection<TriangleXYZ> getTriangulation(
			PolygonWithHolesXZ polygon) {
		
		//TODO: triangulation before ele calculation would allow creating connectors for all vertices
		
		Collection<TriangleXZ> trianglesXZ =
			TriangulationUtil.triangulate(polygon);
		
		Collection<TriangleXYZ> trianglesXYZ =
			new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			VectorXYZ v1 = connectors.getPosXYZ(triangleXZ.v1);
			VectorXYZ v2 = connectors.getPosXYZ(triangleXZ.v2);
			VectorXYZ v3 = connectors.getPosXYZ(triangleXZ.v3);
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
