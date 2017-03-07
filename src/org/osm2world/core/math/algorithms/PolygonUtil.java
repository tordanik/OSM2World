package org.osm2world.core.math.algorithms;

import static com.google.common.base.Preconditions.checkArgument;
import static org.osm2world.core.math.GeometryUtil.isRightOf;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.util.MinMaxUtil;

import com.google.common.base.Function;

/**
 * utility class for basic polygon-related algorithms
 */
public class PolygonUtil {

	/** prevents instantiation */
	private PolygonUtil() { }
	
	public static final SimplePolygonXZ convexHull(SimplePolygonXZ polygon) {
		
		List<VectorXZ> vertices = polygon.getVertices();
		
		/* determine points with min/max x value (guaranteed to be in convex hull) */
		
		Function<VectorXZ, Double> getX = new Function<VectorXZ, Double>() {
			public Double apply(VectorXZ v) {
				return v.x;
			}
		};
		
		VectorXZ minV = MinMaxUtil.min(vertices, getX);
		VectorXZ maxV = MinMaxUtil.max(vertices, getX);
		
		int minI = vertices.indexOf(minV);
		int maxI = vertices.indexOf(maxV);
		
		/* split the polygon into an upper and lower "half" at the two points */
		
		List<VectorXZ> upperHalf = new ArrayList<VectorXZ>();
		List<VectorXZ> lowerHalf = new ArrayList<VectorXZ>();
		
		upperHalf.add(minV);
		
		for (int i = (minI + 1) % vertices.size(); i != maxI; i = (i+1) % vertices.size()) {
			upperHalf.add(vertices.get(i));
		}
		
		upperHalf.add(maxV);
		
		lowerHalf.add(maxV);
		
		for (int i = (maxI + 1) % vertices.size(); i != minI; i = (i+1) % vertices.size()) {
			lowerHalf.add(vertices.get(i));
		}
		
		lowerHalf.add(minV);
		
		/* perform the calculation for each of the two parts */
		
		List<VectorXZ> upperResult = convexHullPart(upperHalf);
		List<VectorXZ> lowerResult = convexHullPart(lowerHalf);
		
		/* combine the results */
		
		upperResult.addAll(lowerResult.subList(1, lowerResult.size()));
		
		return new SimplePolygonXZ(upperResult);
		
	}

	/**
	 * calculates the convex hull partially for the upper or lower "half"
	 * of a polygon. Used in {@link #convexHull(SimplePolygonXZ)}.
	 */
	private static List<VectorXZ> convexHullPart(List<VectorXZ> vertices) {
		
		checkArgument(vertices.size() >= 2);
		
		if (vertices.size() <= 3) {
			return vertices;
		}
		
		// preliminary result, vertices can be removed from its end at a later point
		List<VectorXZ> result = new ArrayList<VectorXZ>();
		
		result.add(vertices.get(0));
		result.add(vertices.get(1));
		
		for (int i = 2; i < vertices.size(); i++) {
			
			VectorXZ v = vertices.get(i);
			
			while (result.size() > 1) {
			
				if (isRightOf(result.get(result.size() - 2), v,
						result.get(result.size() - 1))) {
					
					result.remove(result.size() - 1);
					
				} else {
					break;
				}
				
			}
			
			result.add(v);
			
		}
		
		return result;
		
	}
	
}
