package org.osm2world.core.target.common;

import static org.osm2world.core.math.algorithms.NormalCalculationUtil.*;
import static org.osm2world.core.target.common.Primitive.Type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;

/**
 * superclass for targets that are based on OpenGL primitives.
 * These targets will treat different primitives similarly:
 * They convert them all to a list of vertices
 * and represent the primitive type using an enum or flags.
 */
public abstract class PrimitiveTarget<R extends Renderable>
		extends AbstractTarget<R> {

	/**
	 * @param vs       vertices that form the primitive
	 * @param normals  normal vector for each vertex; same size as vs
	 * @param textureCoordLists  texture coordinates for each texture layer,
	 *                           each list has the same size as vs
	 */
	abstract protected void drawPrimitive(Primitive.Type type, Material material,
			List<? extends VectorXYZ> vs, VectorXYZ[] normals,
			List<List<VectorXZ>> textureCoordLists);
	
	@Override
	public void drawTriangleStrip(Material material, List<? extends VectorXYZ> vs,
			List<List<VectorXZ>> textureCoordLists) {
		boolean smooth = (material.getLighting() == Lighting.SMOOTH);
		drawPrimitive(TRIANGLE_STRIP, material, vs,
				calculateTriangleStripNormals(vs, smooth),
				textureCoordLists);
	}

	@Override
	public void drawTriangleFan(Material material, List<? extends VectorXYZ> vs,
			List<List<VectorXZ>> textureCoordLists) {
		boolean smooth = (material.getLighting() == Lighting.SMOOTH);
		drawPrimitive(TRIANGLE_FAN, material, vs,
				calculateTriangleFanNormals(vs, smooth),
				textureCoordLists);
	}
	
	@Override
	public void drawPolygon(Material material, VectorXYZ... vs) {
		drawPolygon(material, Arrays.asList(vs));
	}

	public void drawPolygon(Material material, List<? extends VectorXYZ> vs) {
		boolean smooth = (material.getLighting() == Lighting.SMOOTH);
		drawPrimitive(CONVEX_POLYGON, material, vs,
				calculateTriangleFanNormals(vs, smooth),
				Collections.<List<VectorXZ>>emptyList());
	}
	
	@Override
	public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles) {
		drawTriangles(material, triangles, Collections.<List<VectorXZ>>emptyList());
	}
	
	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> textureCoordLists) {
		
		List<VectorXYZ> vectors = new ArrayList<VectorXYZ>(triangles.size()*3);
		
		for (TriangleXYZ triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
		}
		
		drawPrimitive(TRIANGLES, material, vectors,
				calculateTriangleNormals(vectors,
						material.getLighting() == Lighting.SMOOTH),
						textureCoordLists);
		
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
		
		drawPrimitive(TRIANGLES, material, vectors,
				normals.toArray(new VectorXYZ[normals.size()]),
				Collections.<List<VectorXZ>>emptyList());
		
	}
	
}
