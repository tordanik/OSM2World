package org.osm2world.core.target.common;

import static org.osm2world.core.math.algorithms.NormalCalculationUtil.*;
import static org.osm2world.core.target.common.Primitive.Type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.Material.Lighting;

/**
 * superclass for targets that are based on OpenGL primitives.
 * These targets will treat different primitives similarly:
 * They convert them all to a list of vertices
 * and represent the primitive type using an enum or flags.
 */
public abstract class PrimitiveTarget extends AbstractTarget {

	/**
	 * @param vs       vertices that form the primitive
	 * @param normals  normal vector for each vertex; same size as vs
	 */
	abstract protected void drawPrimitive(Primitive.Type type, Material material,
			List<? extends VectorXYZ> vs, VectorXYZ[] normals);

	@Override
	public void drawTriangleStrip(Material material, VectorXYZ... vs) {
		drawTriangleStrip(material, Arrays.asList(vs));        
	}
	
	@Override
	public void drawTriangleStrip(Material material, List<? extends VectorXYZ> vs) {
		boolean smooth = (material.lighting == Lighting.SMOOTH);
		drawPrimitive(TRIANGLE_STRIP, material, vs,
				calculateTriangleStripNormals(vs, smooth));
	}

	@Override
	public void drawTriangleFan(Material material, List<? extends VectorXYZ> vs) {
		boolean smooth = (material.lighting == Lighting.SMOOTH);
		drawPrimitive(TRIANGLE_FAN, material, vs,
				calculateTriangleFanNormals(vs, smooth));
	}
	
	@Override
	public void drawPolygon(Material material, VectorXYZ... vs) {
		drawPolygon(material, Arrays.asList(vs));        
	}

	public void drawPolygon(Material material, List<? extends VectorXYZ> vs) {
		boolean smooth = (material.lighting == Lighting.SMOOTH);
		drawPrimitive(CONVEX_POLYGON, material, vs,
				calculateTriangleFanNormals(vs, smooth));        
	}
	
	@Override
	public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles) {
		
		List<VectorXYZ> vectors = new ArrayList<VectorXYZ>(triangles.size()*3);
		
		for (TriangleXYZ triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
		}
		
		drawPrimitive(TRIANGLES, material, vectors, 
				calculateTriangleNormals(vectors, material.lighting == Lighting.SMOOTH));
		
	}
	
	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles) {

		List<VectorXYZ> vectors = new ArrayList<VectorXYZ>(triangles.size()*3);
		List<VectorXYZ> normals = new ArrayList<VectorXYZ>(triangles.size()*3);
				
		for (TriangleXYZWithNormals triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
			normals.add(triangle.n1);
			normals.add(triangle.n2);
			normals.add(triangle.n3);
		}
		
		drawPrimitive(TRIANGLES, material, vectors, normals.toArray(new VectorXYZ[normals.size()]));
		
	}
	
}
