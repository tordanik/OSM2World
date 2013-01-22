package org.osm2world;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * 2d Delaunay triangulation class.
 * Built to be used as a Voronoi Diagram dual for natural neighbor
 * interpolation of the y elevation values carried by each point.
 * The triangulation is constructed by incremental insertion.
 */
public class DelaunayTriangulation {
	
	/**
	 * a triangle which is the dual of a site in the Voronoi Diagram.
	 * Must be counter-clockwise.
	 */
	public static class DelaunayTriangle {
		
		//TODO: use Site class with VectorXZ and other value - avoids all the .xz() calls
		
		public final VectorXYZ p0, p1, p2;
		
		private DelaunayTriangle neighbor0 = null;
		private DelaunayTriangle neighbor1 = null;
		private DelaunayTriangle neighbor2 = null;
		
		public DelaunayTriangle(VectorXYZ p0, VectorXYZ p1, VectorXYZ p2) {
			
			this.p0 = p0;
			this.p1 = p1;
			this.p2 = p2;
			
			assert !asTriangleXZ().isClockwise() : "must be counter-clockwise";
			
		}
				
		public VectorXYZ getPoint(int i) {
			switch (i) {
				case 0: return p0;
				case 1: return p1;
				case 2: return p2;
				default: throw new Error("invalid index " + i);
			}
		}
		
		
		public DelaunayTriangle getNeighbor(int i) {
			switch (i) {
				case 0: return neighbor0;
				case 1: return neighbor1;
				case 2: return neighbor2;
				default: throw new Error("invalid index " + i);
			}
		}
		
		public void setNeighbor(int i, DelaunayTriangle neighbor) {
			switch (i) {
				case 0: neighbor0 = neighbor; break;
				case 1: neighbor1 = neighbor; break;
				case 2: neighbor2 = neighbor; break;
				default: throw new Error("invalid index " + i);
			}
		}
		
		public int indexOfNeighbor(DelaunayTriangle neighbor) {
			if (neighbor == neighbor0) {
				return 0;
			} else if (neighbor == neighbor1) {
				return 1;
			} else if (neighbor == neighbor2) {
				return 2;
			} else {
				throw new IllegalArgumentException("not a neighbor");
			}
		}
		
		public void replaceNeighbor(DelaunayTriangle oldNeighbor,
				DelaunayTriangle newNeighbor) {
			this.setNeighbor(indexOfNeighbor(oldNeighbor), newNeighbor);
		}
		
		public double angleAt(int pointIndex) {
			
			VectorXZ vecToNext = getPoint(pointIndex).xz().subtract(
					getPoint((pointIndex + 2) % 3).xz());
			VectorXZ vecToPrev = getPoint((pointIndex + 1) % 3).xz().subtract(
					getPoint(pointIndex).xz()).invert();
			
			return VectorXZ.angleBetween(vecToNext, vecToPrev);
			
		}

		public double angleOppositeOf(DelaunayTriangle neighbor) {
			
			return angleAt(((indexOfNeighbor(neighbor)) + 2) % 3);
			
		}
		
		public VectorXZ getCircumcircleCenter() {
			
			VectorXZ b = p1.subtract(p0).xz();
			VectorXZ c = p2.subtract(p0).xz();

			double d = 2 * (b.x * c.z - b.z * c.x);

			double rX = (c.z * (b.x * b.x + b.z * b.z) - b.z * (c.x * c.x + c.z * c.z)) / d;
			double rZ = (b.x * (c.x * c.x + c.z * c.z) - c.x * (b.x * b.x + b.z * b.z)) / d;
			
			return new VectorXZ(rX, rZ).add(p0.xz());
			
		}
		
		public TriangleXZ asTriangleXZ() {
			return new TriangleXZ(p0.xz(), p1.xz(), p2.xz());
		}
		
		@Override
		public String toString() {
			return asTriangleXZ().toString();
		}
		
	}
	
	/**
	 * operation where the triangulation is modified during an insertion
	 */
	public interface Flip {
		public void perform();
		public void undo();
	}
	
	public class Flip13 implements Flip {
		
		DelaunayTriangle originalTriangle;
		VectorXYZ point;
		
		DelaunayTriangle[] createdTriangles;
		
		public Flip13(DelaunayTriangle triangle, VectorXYZ point) {
			this.originalTriangle = triangle;
			this.point = point;
		}

		@Override
		public void perform() {
			
			createdTriangles = new DelaunayTriangle[3];
			
			createdTriangles[0] = new DelaunayTriangle(
					originalTriangle.p0,
					originalTriangle.p1,
					point);
			createdTriangles[1] = new DelaunayTriangle(
					originalTriangle.p1,
					originalTriangle.p2,
					point);
			createdTriangles[2] = new DelaunayTriangle(
					originalTriangle.p2,
					originalTriangle.p0,
					point);
						
			DelaunayTriangle neighbor0 = originalTriangle.getNeighbor(0);
			DelaunayTriangle neighbor1 = originalTriangle.getNeighbor(1);
			DelaunayTriangle neighbor2 = originalTriangle.getNeighbor(2);
			
			createdTriangles[0].setNeighbor(0, neighbor0);
			createdTriangles[0].setNeighbor(1, createdTriangles[1]);
			createdTriangles[0].setNeighbor(2, createdTriangles[2]);
			if (neighbor0 != null) {
				neighbor0.replaceNeighbor(originalTriangle, createdTriangles[0]);
			}
			
			createdTriangles[1].setNeighbor(0, neighbor1);
			createdTriangles[1].setNeighbor(1, createdTriangles[2]);
			createdTriangles[1].setNeighbor(2, createdTriangles[0]);
			if (neighbor1 != null) {
				neighbor1.replaceNeighbor(originalTriangle, createdTriangles[1]);
			}
			
			createdTriangles[2].setNeighbor(0, neighbor2);
			createdTriangles[2].setNeighbor(1, createdTriangles[0]);
			createdTriangles[2].setNeighbor(2, createdTriangles[1]);
			if (neighbor2 != null) {
				neighbor2.replaceNeighbor(originalTriangle, createdTriangles[2]);
			}
			
			triangles.remove(originalTriangle);
			triangles.add(createdTriangles[0]);
			triangles.add(createdTriangles[1]);
			triangles.add(createdTriangles[2]);
			
		}
		
		@Override
		public void undo() {
			
			triangles.add(originalTriangle);
			triangles.remove(createdTriangles[0]);
			triangles.remove(createdTriangles[1]);
			triangles.remove(createdTriangles[2]);
			
			DelaunayTriangle neighbor0 = originalTriangle.getNeighbor(0);
			DelaunayTriangle neighbor1 = originalTriangle.getNeighbor(1);
			DelaunayTriangle neighbor2 = originalTriangle.getNeighbor(2);
			
			if (neighbor0 != null) {
				neighbor0.replaceNeighbor(createdTriangles[0], originalTriangle);
			}
			
			if (neighbor1 != null) {
				neighbor1.replaceNeighbor(createdTriangles[1], originalTriangle);
			}
			
			if (neighbor2 != null) {
				neighbor2.replaceNeighbor(createdTriangles[2], originalTriangle);
			}
			
		}
		
	}
	
	public class Flip22 implements Flip {
		
		DelaunayTriangle[] originalTriangles;
		DelaunayTriangle[] createdTriangles;
		
		/**
		 * neighbors of the quadrangle formed by both
		 * {@link #originalTriangles} and {@link #createdTriangles}.
		 */
		final DelaunayTriangle[] neighbors = new DelaunayTriangle[4];
		
		public Flip22(DelaunayTriangle triangle) {
			originalTriangles = new DelaunayTriangle[]{
					triangle,
					triangle.getNeighbor(0)};
		}
		
		@Override
		public void perform() {
			
			/* determine points and neighbors (4 each) of the quadrangle */
			
			VectorXYZ[] points = new VectorXYZ[4];

			points[0] = originalTriangles[0].getPoint(1);
			neighbors[0] = originalTriangles[0].getNeighbor(1);

			points[1] = originalTriangles[0].getPoint(2);
			neighbors[1] = originalTriangles[0].getNeighbor(2);
			
			int i = originalTriangles[1].indexOfNeighbor(originalTriangles[0]);

			points[2] = originalTriangles[1].getPoint((i + 1) % 3);
			neighbors[2] = originalTriangles[1].getNeighbor((i + 1) % 3);

			points[3] = originalTriangles[1].getPoint((i + 2) % 3);
			neighbors[3] = originalTriangles[1].getNeighbor((i + 2) % 3);
			
			/* build two new triangles for the quadrangle */
			
			createdTriangles = new DelaunayTriangle[2];
			
			createdTriangles[0] = new DelaunayTriangle(
					points[2], points[3], points[1]);
			createdTriangles[1] = new DelaunayTriangle(
					points[3], points[0], points[1]);
			
			createdTriangles[0].setNeighbor(0, neighbors[2]);
			createdTriangles[0].setNeighbor(1, createdTriangles[1]);
			createdTriangles[0].setNeighbor(2, neighbors[1]);
			
			createdTriangles[1].setNeighbor(0, neighbors[3]);
			createdTriangles[1].setNeighbor(1, neighbors[0]);
			createdTriangles[1].setNeighbor(2, createdTriangles[0]);

			if (neighbors[0] != null)
				neighbors[0].replaceNeighbor(originalTriangles[0], createdTriangles[1]);
			if (neighbors[1] != null)
				neighbors[1].replaceNeighbor(originalTriangles[0], createdTriangles[0]);
			if (neighbors[2] != null)
				neighbors[2].replaceNeighbor(originalTriangles[1], createdTriangles[0]);
			if (neighbors[3] != null)
				neighbors[3].replaceNeighbor(originalTriangles[1], createdTriangles[1]);
			
			triangles.remove(originalTriangles[0]);
			triangles.remove(originalTriangles[1]);
			triangles.add(createdTriangles[0]);
			triangles.add(createdTriangles[1]);
			
		}
		
		@Override
		public void undo() {
			
			triangles.add(originalTriangles[0]);
			triangles.add(originalTriangles[1]);
			triangles.remove(createdTriangles[0]);
			triangles.remove(createdTriangles[1]);
			
			if (neighbors[0] != null)
				neighbors[0].replaceNeighbor(createdTriangles[1], originalTriangles[0]);
			if (neighbors[1] != null)
				neighbors[1].replaceNeighbor(createdTriangles[0], originalTriangles[0]);
			if (neighbors[2] != null)
				neighbors[2].replaceNeighbor(createdTriangles[0], originalTriangles[1]);
			if (neighbors[3] != null)
				neighbors[3].replaceNeighbor(createdTriangles[1], originalTriangles[1]);
			
		}
		
	}
	
	public final List<DelaunayTriangle> triangles; //TODO make private
	
	public DelaunayTriangulation(VectorXYZ... bounds) {
		
		assert bounds.length == 4;
		assert !new PolygonXYZ(asList(bounds[0], bounds[1], bounds[2],
				bounds[3], bounds[0])).getSimpleXZPolygon().isClockwise();
		
		triangles = new ArrayList<DelaunayTriangle>();
		triangles.add(new DelaunayTriangle(bounds[0], bounds[1], bounds[3]));
		triangles.add(new DelaunayTriangle(bounds[1], bounds[2], bounds[3]));
		
		triangles.get(0).setNeighbor(1, triangles.get(1));
		triangles.get(1).setNeighbor(2, triangles.get(0));
		
	}
	
	public Stack<Flip> insert(VectorXYZ point) { //TODO: should use <T extends Has(Immutable)Position>
		
		DelaunayTriangle triangleEnclosingPoint = getEnlosingTriangle(point);
		
		//TODO check for null
		
		/* split the enclosing triangle */
		
		Stack<Flip> flipStack = new Stack<Flip>();
		
		Flip13 initialFlip = new Flip13(triangleEnclosingPoint, point);
		initialFlip.perform();
		flipStack.push(initialFlip);
		
		Queue<DelaunayTriangle> uncheckedTriangles = new LinkedList<DelaunayTriangle>();
		
		uncheckedTriangles.offer(initialFlip.createdTriangles[0]);
		uncheckedTriangles.offer(initialFlip.createdTriangles[1]);
		uncheckedTriangles.offer(initialFlip.createdTriangles[2]);
		
		/*  */
		
		while (!uncheckedTriangles.isEmpty()) {
			
			DelaunayTriangle triangle = uncheckedTriangles.poll();
			
			//TODO: only checks with first neighbor! Document this!
			
			if (!isDelaunay(triangle)) {
				
				Flip22 flip = new Flip22(triangle);
				flip.perform();
				flipStack.push(flip);
				
				uncheckedTriangles.offer(flip.createdTriangles[0]);
				uncheckedTriangles.offer(flip.createdTriangles[1]);
				
			}
			
		}
		
		return flipStack;
		
	}
	
	public void insertAndUndo(VectorXYZ point) {
		
		Stack<Flip> flipStack = insert(point);
		
		while (!flipStack.isEmpty()) {
			flipStack.pop().undo();
		}
		
	}
	
	private static class DelaunayEdge {
		
		public final VectorXYZ v1;
		public final VectorXYZ v2;
		
		public DelaunayEdge(VectorXYZ v1, VectorXYZ v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
		
		public VectorXYZ getCenter() {
			return v1.add(v2).mult(0.5);
		}
		
		public double getDirectionAngle() {
			return v2.xz().subtract(v1.xz()).angle();
		}
		
	}
	
	//TODO only really needed for debugging
	public List<TriangleXZ> getVoronoiParts(VectorXYZ point) {
		
		List<TriangleXZ> result = new ArrayList<TriangleXZ>();
		
		final VectorXZ pointXZ = point.xz();
		
		List<VectorXZ> centers = new ArrayList<VectorXZ>();
		
		for (DelaunayTriangle t : getIncidentTriangles(point)) {
			centers.add(t.getCircumcircleCenter());
		}
		
		Collections.sort(centers, new Comparator<VectorXZ>() {

			@Override
			public int compare(VectorXZ v1, VectorXZ v2) {
				return Double.compare(
						v2.subtract(pointXZ).angle(),
						v1.subtract(pointXZ).angle());
			}
			
		});
		
		for (int i = 0; i < centers.size(); i++) {
			result.add(new TriangleXZ(point.xz(),
					centers.get(i),
					centers.get((i+1) % centers.size())));
		}
		
		return result;
		
	}
	
	private List<DelaunayEdge> getIncidentEdges(final VectorXYZ point) {

		List<DelaunayEdge> result = new ArrayList<DelaunayEdge>();
		
		for (DelaunayTriangle triangle : triangles) {
			if (triangle.p0.equals(point)) {
				result.add(new DelaunayEdge(point, triangle.p1));
			} else if (triangle.p1.equals(point)) {
				result.add(new DelaunayEdge(point, triangle.p2));
			} else if (triangle.p2.equals(point)) {
				result.add(new DelaunayEdge(point, triangle.p0));
			}
		}
		
		Collections.sort(result, new Comparator<DelaunayEdge>() {

			@Override
			public int compare(DelaunayEdge e1, DelaunayEdge e2) {
				return Double.compare(
						e1.getDirectionAngle(),
						e2.getDirectionAngle());
			}
			
		});
		
		return result;
		
	}
	
	public List<DelaunayTriangle> getIncidentTriangles(final VectorXYZ point) {

		List<DelaunayTriangle> result = new ArrayList<DelaunayTriangle>();
		
		for (DelaunayTriangle triangle : triangles) {
			if (triangle.p0.equals(point)) {
				result.add(triangle);
			} else if (triangle.p1.equals(point)) {
				result.add(triangle);
			} else if (triangle.p2.equals(point)) {
				result.add(triangle);
			}
		}
		
		return result;
		
	}
	
	private boolean isDelaunay(DelaunayTriangle triangle) {

		DelaunayTriangle neighborTriangle = triangle.getNeighbor(0);
		
		if (neighborTriangle != null) {
			
			double a1 = triangle.angleAt(2);
			double a2 = neighborTriangle.angleOppositeOf(triangle);
			
			return a1 + a2 <= PI;
			
		} else {
			
			return true;
			
		}
		
	}

	private DelaunayTriangle getEnlosingTriangle(VectorXYZ point) {
		
		for (DelaunayTriangle triangle : triangles) {
			if (triangle.asTriangleXZ().contains(point.xz())) {
				return triangle;
			}
		}
		
		return null;
		
	}
	
}
