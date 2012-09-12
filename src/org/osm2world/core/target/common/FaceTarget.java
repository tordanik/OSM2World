package org.osm2world.core.target.common;

import static java.lang.Math.abs;
import static java.util.Collections.nCopies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * a target that relies on faces to represent geometry.
 * The faces used by this target are polygons
 * with three or more coplanar vertices.
 * 
 * TODO: this currently produces faces that are not convex
 */
public abstract class FaceTarget<R extends Renderable>
	extends AbstractTarget<R> {
	
	abstract public void drawFace(Material material, List<VectorXYZ> vs,
			List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists);
	
	/**
	 * decides whether faces should be reconstructed from triangulations
	 * and other primitives.
	 */
	abstract public boolean reconstructFaces();
	
	/**
	 * prevents triangles from before the call to be connected with triangles
	 * after this call when faces are reconstructed.
	 * 
	 * This is automatically done at the beginning of each new object.
	 * It only has any effect if {@link #reconstructFaces()} is enabled.
	 * 
	 * Calling this method at appropriate times can also help to speed up
	 * performance by lowering the number of candidates for merging.
	 */
	public void flushReconstructedFaces() {
		drawAndClearCurrentFaces();
	}
	
	/**
	 * mutable representation of a face
	 */
	protected final static class Face {
		
		public final List<VectorXYZ> vs;
		public final List<List<VectorXZ>> texCoordLists;
		public final VectorXYZ normal;
		
		public Face(List<VectorXYZ> vs,
				List<List<VectorXZ>> texCoordLists, VectorXYZ normal) {
			
			this.vs = vs;
			this.texCoordLists = texCoordLists;
			this.normal = normal;
			
		}
		
		/**
		 * @return  true if the triangle has been successfully inserted
		 */
		public boolean tryInsert(IsolatedTriangle t) {
			
			for (int i = 0; i < vs.size(); i++) {
				int j = (i+1) % vs.size();
				int k = (i+2) % vs.size();
				
				if (vs.get(i).equals(t.triangle.v3)
						&& vs.get(j).equals(t.triangle.v2)) { /* TODO tex coords equal */
					
					if (vs.get(k).equals(t.triangle.v1)) {
						
						removeVertex(j);
						
					} else {
						
						insertVertex(j, t.triangle.v1,
								texCoordLists, t.texCoordOffset + 0);
						
					}
					
					return true;
					
				}
				
				if (vs.get(i).equals(t.triangle.v1)
						&& vs.get(j).equals(t.triangle.v3)) { /* TODO tex coords equal */
					
					if (vs.get(k).equals(t.triangle.v2)) {
						
						removeVertex(j);
						
					} else {
						
						insertVertex(j, t.triangle.v2,
								texCoordLists, t.texCoordOffset + 1);
						
					}
					
					return true;
					
				}
				
				if (vs.get(i).equals(t.triangle.v2)
						&& vs.get(j).equals(t.triangle.v1)) { /* TODO tex coords equal */
					
					if (vs.get(k).equals(t.triangle.v3)) {
						
						removeVertex(j);
						
					} else {
						
						insertVertex(j, t.triangle.v3,
								texCoordLists, t.texCoordOffset + 2);
						
					}
					
					return true;
					
				}
				
			}
			
			return false;
			
		}
		
		public void removeDuplicateEdges() {
			
			boolean repeat = true;
			
			while (repeat) {
				
				repeat = false;
				
				assert vs.size() >= 3;
				
				for (int i = 0; i < vs.size(); i++) {
					int j = (i+1) % vs.size();
					int k = (i+2) % vs.size();
					
					//TODO: what about tex coords?
					if (vs.get(i).equals(vs.get(k))) {
						
						if (k > j) {
							removeVertex(k);
							removeVertex(j);
						} else {
							removeVertex(j);
							removeVertex(k);
						}
						
						repeat = true;
						break;
						
					}
					
				}
				
			}
			
		}
		
		private void insertVertex(int i, VectorXYZ vertex,
				List<List<VectorXZ>> insTexCoordLists, int texCoordPos) {
			
			this.vs.add(i, vertex);
			
			for (int list = 0; list < texCoordLists.size(); list++) {
				
				this.texCoordLists.get(list).add(i,
						insTexCoordLists.get(list).get(texCoordPos));
			}
			
		}
		
		private void removeVertex(int i) {
			
			this.vs.remove(i);
			
			for (int list = 0; list < texCoordLists.size(); list++) {
				this.texCoordLists.get(list).remove(i);
			}
			
		}
		
		@Override
		public String toString() {
			return vs.toString();
		}
		
	}
	
	protected final static class IsolatedTriangle {
		
		public final TriangleXYZ triangle;
		public final VectorXYZ normal;
		public final int texCoordOffset;
		public final List<List<VectorXZ>> texCoordLists;
		
		public IsolatedTriangle(TriangleXYZ triangle, VectorXYZ normal,
				int texCoordOffset, List<List<VectorXZ>> texCoordLists) {
			
			this.triangle = triangle;
			this.normal = normal;
			this.texCoordOffset = texCoordOffset;
			this.texCoordLists = texCoordLists;
			
		}
		
		@Override
		public String toString() {
			return triangle.toString();
		}
		
	}
	
	private final Multimap<Material, IsolatedTriangle> isolatedTriangles =
			HashMultimap.create();
	
	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {
		
		int i = 0;
		
		for (TriangleXYZ triangle : triangles) {
			
			VectorXYZ n = triangle.getNormal();
			
			if (Double.isNaN(n.x) || Double.isNaN(n.y) || Double.isNaN(n.z)) {
				continue; //TODO log
			}
			
			isolatedTriangles.put(material,
					new IsolatedTriangle(triangle, n, i*3, texCoordLists));
			
		}
		
	}
	
	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {
		
		drawTriangles(material, triangles, texCoordLists);
		
		//TODO keep normals information
		
	}
	
	@Override
	public void beginObject(WorldObject object) {
		
		drawAndClearCurrentFaces();
		
	}
	
	@Override
	public void finish() {
		
		drawAndClearCurrentFaces();
		
	}
	
	/**
	 * integrates all {@link #isolatedTriangles} into faces,
	 * then draws all faces and clears the collection.
	 */
	private void drawAndClearCurrentFaces() {
		
		for (Material material : isolatedTriangles.keySet()) {
			
			Collection<Face> faces = combineTrianglesToFaces(isolatedTriangles.get(material));
			
			/* draw faces */
			
			for (Face face : faces) {
				drawFace(material, face.vs,
						nCopies(face.vs.size(), face.normal),
						face.texCoordLists);
			}
			
		}
		
		isolatedTriangles.clear();
		
	}
	
	/**
	 * @param isolatedTriangles  non-empty collection of triangles
	 */
	protected static Collection<Face> combineTrianglesToFaces(
			Collection<IsolatedTriangle> isolatedTriangles) {
		
		List<IsolatedTriangle> triangles =
				new LinkedList<IsolatedTriangle>(isolatedTriangles);
		
		Collection<Face> faces = new ArrayList<Face>();
		
		/* turn one triangle into a face */
		
		faces.add(createFaceFromTriangle(triangles.remove(0)));
		
		/* turn remaining triangles into faces or insert them into existing ones */
		
		trianglesToFacesLoop: while (!triangles.isEmpty()) {
			
			/* try to insert triangles into existing faces */
			
			for (IsolatedTriangle triangle : triangles) {
				for (Face face : faces) {
					
					if (normalAlmostEquals(face.normal, triangle.normal)) {
						
						boolean inserted = face.tryInsert(triangle);
						
						if (inserted) {
							triangles.remove(triangle);
							continue trianglesToFacesLoop;
						}
						
					}
					
				}
			}
			
			/* could not extend existing faces, start a new face instead */
			
			faces.add(createFaceFromTriangle(triangles.remove(0)));
			
		}
		
		/* eliminate duplicate edges */
		
		for (Face face : faces) {
			face.removeDuplicateEdges();
		}
		
		return faces;
		
	}
	
	protected static boolean normalAlmostEquals(VectorXYZ n1, VectorXYZ n2) {
		
		return abs(n1.x - n2.x) <= 0.01
				&& abs(n1.y - n2.y) <= 0.01
				&& abs(n1.z - n2.z) <= 0.01;
		
	}
	
	protected static Face createFaceFromTriangle(IsolatedTriangle t) {
		
		List<VectorXYZ> newFaceVs =
				new ArrayList<VectorXYZ>(t.triangle.getVertices());
		
		List<List<VectorXZ>> newFaceTCLists = new ArrayList<List<VectorXZ>>();
		
		for (int list = 0; list < t.texCoordLists.size(); list++) {
			newFaceTCLists.add(new ArrayList<VectorXZ>(
					t.texCoordLists.get(list).subList(
							t.texCoordOffset, t.texCoordOffset + 3)));
		}
		
		return new Face(newFaceVs,	newFaceTCLists, t.normal);
		
	}
	
}
