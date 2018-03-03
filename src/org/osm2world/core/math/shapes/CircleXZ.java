package org.osm2world.core.math.shapes;

import static java.lang.Math.PI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

public class CircleXZ implements SimpleClosedShapeXZ {
	
	private static final int NUM_POINTS = 36;
	
	private final VectorXZ center;
	private final double radius;
	
	public CircleXZ(VectorXZ center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	public List<VectorXZ> getVertexList() {

		List<VectorXZ> result = new ArrayList<VectorXZ>(NUM_POINTS + 1);
		
		double angleInterval = 2 * PI / NUM_POINTS;
		
		for (int i = 0; i < NUM_POINTS; i++) {
			
			double angle = i * angleInterval; //TODO * -1? Winding might be important to consider for shapes!
			double sin = Math.sin(angle);
			double cos = Math.cos(angle);
			
			result.add(center.add(new VectorXZ(radius * sin, radius * cos)));
			
		}
		
		result.add(result.get(0));
		
		return result;
		
	}

	@Override
	public Collection<TriangleXZ> getTriangulation() {
		
		List<VectorXZ> vertices = getVertexList();

		List<TriangleXZ> result = new ArrayList<TriangleXZ>(vertices.size() - 1);
		
		for (int i = 0; i + 1 < vertices.size(); i++) {
			result.add(new TriangleXZ(center, vertices.get(i), vertices.get(i+1)));
		}
		
		return result;
		
	}
	
}
