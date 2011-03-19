package org.osm2world.core.terrain.creation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.LineSegmentXYZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * a data store for elevation information during terrain creation.
 * 
 * This is necessary because 2D computations (such as polygon differences
 * and Conforming Delaunay Triangulation) will strip the third dimension from
 * elevation vertices and may also add additional vertices.
 */
public class TemporaryElevationStorage {
	
	private static final double INTERPOL_DIST_TOLERANCE = 0.01;
	
	private Map<VectorXZ, VectorXYZ> nodeMap = new HashMap<VectorXZ, VectorXYZ>();
	private List<LineSegmentXYZ> lineSegments = new ArrayList<LineSegmentXYZ>();
	
	/**
	 * adds the 3d version of a flattened vector
	 * so that the 3d version can later be retrieved.
	 */
	public void addVector(VectorXZ vXZ, VectorXYZ vXYZ) {
		nodeMap.put(vXZ, vXYZ);
	}
	
	/**
	 * adds a line segment for interpolating along.
	 * Note that line segments which have been added earlier
	 * will take priority over those which have been added later,
	 * so insertion order matters.
	 */
	public void addInterpolationLineSegment(LineSegmentXYZ segment) {
		lineSegments.add(segment);
	}
	
	/**
	 * adds all the elevation info for a polygon that needs to be flattened
	 * (vertex vectors and line segments along the edges)
	 */
	public void addPolygon(PolygonXZ polyXZ, PolygonXYZ polyXYZ) {
		
		if (polyXZ.size() != polyXYZ.size()) {
			throw new IllegalArgumentException("the two polys need to have the same size");
		}
		
		for (int i = 0; i < polyXZ.size(); i++) {
			addVector(polyXZ.getVertices().get(i),
					polyXYZ.getVertices().get(i));
		}
		
		for (LineSegmentXYZ segment : polyXYZ.getSegments()) {
			addInterpolationLineSegment(segment);
		}
		
	}
	
	/**
	 * returns the 3d vector at a position;
	 * either by retrieving a previously existing vector
	 * or by interpolating along interpolation lines
	 * 
	 * @return  3d vector; null if the vector didn't exist previously
	 *          and isn't close to any of the interpolation lines 
	 */
	public VectorXYZ restoreElevationForVector(VectorXZ vector) {
		VectorXYZ existingVectorXYZ = nodeMap.get(vector);
		if (existingVectorXYZ != null) {
			return existingVectorXYZ;
		} else {
			for (LineSegmentXYZ segment : lineSegments) {
				if (GeometryUtil.distanceFromLineSegment(vector, segment.getSegmentXZ()) < INTERPOL_DIST_TOLERANCE) {
					return GeometryUtil.interpolateElevation(vector, segment.p1, segment.p2);
				}
			}
			return null; 
		}
	}
	
	public TriangleXYZ restoreElevationForTriangle(TriangleXZ triangle) {
		return new TriangleXYZ(
				restoreElevationForVector(triangle.v1),
				restoreElevationForVector(triangle.v2),
				restoreElevationForVector(triangle.v3)
				);
	}

	public PolygonXYZ restoreElevationForPolygon(SimplePolygonXZ polygon) {
		List<VectorXYZ> vertexLoop = new ArrayList<VectorXYZ>(polygon.size()+1);
		for (VectorXZ vXZ : polygon.getVertices()) {
			vertexLoop.add(restoreElevationForVector(vXZ));
		}
		vertexLoop.add(vertexLoop.get(0));
		return new PolygonXYZ(vertexLoop);
	}
	
}
