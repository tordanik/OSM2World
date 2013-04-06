package org.osm2world.core.world.data;

import java.util.Collection;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
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
	
	private EleConnectorGroup connectors;
	
	protected AbstractAreaWorldObject(MapArea area) {
		
		this.area = area;
		
		outlinePolygonXZ = area.getPolygon().getOuter().makeCounterclockwise();
		
	}
	
	@Override
	public Iterable<EleConnector> getEleConnectors() {
		
		if (connectors == null) {
			
			connectors = new EleConnectorGroup();
			
			connectors.addConnectorsFor(area.getPolygon(),
					getGroundState() == GroundState.ON);
			connectors.addConnectorsForTriangulation(getTriangulationXZ(),
					getGroundState() == GroundState.ON);
			
			//TODO do no longer triangulate before elevation calculation
			//     when constrained triangulation (no new points) works
			
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
		return TriangulationUtil.triangulate(area.getPolygon());
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
