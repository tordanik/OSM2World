package org.osm2world.core.world.data;

import java.util.Collection;
import java.util.Collections;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.algorithms.Poly2TriTriangulationUtil;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.util.exception.TriangulationException;

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
	
	private EleConnectorGroup connectors;
	
	protected AbstractAreaWorldObject(MapArea area) {
		
		this.area = area;
		
		outlinePolygonXZ = area.getPolygon().getOuter().makeCounterclockwise();
		
	}
	
	@Override
	public EleConnectorGroup getEleConnectors() {
		
		if (connectors == null) {
			
			connectors = new EleConnectorGroup();
			
			connectors.addConnectorsForTriangulation(
					getTriangulationXZ(), null, getGroundState());
			
		}
		
		return connectors;
		
	}
	
	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}
	
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
	 * decompose this area into counterclockwise triangles.
	 */
	protected Collection<TriangleXZ> getTriangulationXZ() {
		
		try {
		
			return Poly2TriTriangulationUtil.triangulate(
				area.getPolygon().getOuter(),
				area.getPolygon().getHoles(),
				Collections.<LineSegmentXZ>emptyList(),
				Collections.<VectorXZ>emptyList());
			
		} catch (TriangulationException e) {
			
			System.err.println("Poly2Tri exception for " + this + ":");
			e.printStackTrace();
			System.err.println("... falling back to JTS triangulation.");
			
			return JTSTriangulationUtil.triangulate(
					area.getPolygon().getOuter(),
					area.getPolygon().getHoles(),
					Collections.<LineSegmentXZ>emptyList(),
					Collections.<VectorXZ>emptyList());
			
		}
		
	}
	
	/**
	 * decompose this area into counterclockwise 3d triangles.
	 * Only available after elevation calculation.
	 */
	protected Collection<TriangleXYZ> getTriangulation() {
		return connectors.getTriangulationXYZ(getTriangulationXZ());
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + area + ")";
	}
	
}
