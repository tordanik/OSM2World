package org.osm2world;

import static java.lang.Math.*;
import static org.osm2world.core.math.GeometryUtil.isRightOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

//TODO: test performance effects of:
// * starting point choice of the walk
// * caching circumcircles
// * only calculating area of triangles that are actually changed

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
	public static class DelaunayTriangle implements IntersectionTestObject {
		
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

		public DelaunayTriangle getLeftNeighbor(VectorXYZ atPoint) {
			return getNeighbor((indexOfPoint(atPoint) + 2) % 3);
		}

		public DelaunayTriangle getRightNeighbor(VectorXYZ atPoint) {
			return getNeighbor(indexOfPoint(atPoint));
		}
		
		public void setNeighbor(int i, DelaunayTriangle neighbor) {
			switch (i) {
				case 0: neighbor0 = neighbor; break;
				case 1: neighbor1 = neighbor; break;
				case 2: neighbor2 = neighbor; break;
				default: throw new Error("invalid index " + i);
			}
		}
		
		public int indexOfPoint(VectorXYZ point) {
			if (point == p0) {
				return 0;
			} else if (point == p1) {
				return 1;
			} else if (point == p2) {
				return 2;
			} else {
				throw new IllegalArgumentException("not in this triangle");
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
		
		private TriangleXZ triangleXZ = null;
		
		public TriangleXZ asTriangleXZ() {
			if (triangleXZ == null) {
				triangleXZ = new TriangleXZ(p0.xz(), p1.xz(), p2.xz());
			}
			return triangleXZ;
		}
		
		private TriangleXYZ triangleXYZ = null;
		
		public TriangleXYZ asTriangleXYZ() {
			if (triangleXYZ == null) {
				triangleXYZ = new TriangleXYZ(p0, p1, p2);
			}
			return triangleXYZ;
		}
		
		@Override
		public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
			return new AxisAlignedBoundingBoxXZ(
					min(p0.x, min(p1.x, p2.x)),
					min(p0.z, min(p1.z, p2.z)),
					max(p0.x, max(p1.x, p2.x)),
					max(p0.z, max(p1.z, p2.z)));
		}
		
		@Override
		public String toString() {
			return asTriangleXZ().toString();
		}
		
	}
	
	/**
	 * operation where the triangulation is modified during an insertion
	 */
	private interface Flip {
		
		public void perform();
		public void undo();
		
		public DelaunayTriangle[] getCreatedTriangles();
		public DelaunayTriangle[] getRemovedTriangles();
		
	}
	
	private class Flip13 implements Flip {
		
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
			
		}
		
		@Override
		public void undo() {
			
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
	
		@Override
		public DelaunayTriangle[] getCreatedTriangles() {
			assert createdTriangles != null;
			return createdTriangles;
		}
		
		@Override
		public DelaunayTriangle[] getRemovedTriangles() {
			return new DelaunayTriangle[] {originalTriangle};
			//TODO array creation overhead :(
		}
		
	}
	
	private class Flip22 implements Flip {
		
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
						
		}
		
		@Override
		public void undo() {
			
			if (neighbors[0] != null)
				neighbors[0].replaceNeighbor(createdTriangles[1], originalTriangles[0]);
			if (neighbors[1] != null)
				neighbors[1].replaceNeighbor(createdTriangles[0], originalTriangles[0]);
			if (neighbors[2] != null)
				neighbors[2].replaceNeighbor(createdTriangles[0], originalTriangles[1]);
			if (neighbors[3] != null)
				neighbors[3].replaceNeighbor(createdTriangles[1], originalTriangles[1]);
			
		}
		
		@Override
		public DelaunayTriangle[] getCreatedTriangles() {
			assert createdTriangles != null;
			return createdTriangles;
		}
		
		@Override
		public DelaunayTriangle[] getRemovedTriangles() {
			return originalTriangles;
		}
		
	}
		
	public class NaturalNeighbors {

		public final VectorXYZ[] neighbors;
		public final double[] relativeWeights;
		
		NaturalNeighbors(Collection<VectorXYZ> neighbors) {
			
			this.neighbors = new VectorXYZ[neighbors.size()];
			neighbors.toArray(this.neighbors);
			
			relativeWeights = new double[neighbors.size()];
			
		}
				
	}

	/**
	 * produces iterables for iterating over all the triangles in this
	 * triangulation via depth-first search
	 */
	private final Iterable<DelaunayTriangle> ITERABLE =
			new Iterable<DelaunayTriangle>() {
		
		@Override
		public Iterator<DelaunayTriangle> iterator() {
			
			final DelaunayTriangle start = handleTriangle.getNeighbor(0);
			
			final Stack<DelaunayTriangle> startStack = new Stack<DelaunayTriangle>();
			startStack.push(start);
			
			final Set<DelaunayTriangle> startSet = new HashSet<DelaunayTriangle>();
			startSet.add(handleTriangle);
			startSet.add(start);
			
			return new Iterator<DelaunayTriangle>() {
				
				private final Set<DelaunayTriangle> visitedTriangles = startSet;
				private final Stack<DelaunayTriangle> triangleStack = startStack;
				private DelaunayTriangle nextTriangle = start;
				private int nextIndex = 0;
								
				public boolean hasNext() {
					return nextTriangle != null;
				};
				
				public DelaunayTriangle next() {
					
					DelaunayTriangle currentTriangle = nextTriangle;
					nextTriangle = null;
					
					while (nextTriangle == null && !triangleStack.isEmpty()) {
												
						while (nextIndex <= 2 && nextTriangle == null) {
							
							nextTriangle =
									triangleStack.peek().getNeighbor(nextIndex);
							
							if (nextTriangle != null &&
									!visitedTriangles.contains(nextTriangle)) {
								
								triangleStack.push(nextTriangle);
								visitedTriangles.add(nextTriangle);
								nextIndex = 0;
								
							} else {
								
								nextTriangle = null;
								nextIndex ++;
								
							}
							
						}
						
						if (nextTriangle == null) {
							// go back to previous triangle
							triangleStack.pop();
							nextIndex = 0;
						}
						
					}
										
					return currentTriangle;
					
				};
			
				public void remove() {
					throw new UnsupportedOperationException();
				};
				
			};
			
		}
		
	};

	/**
	 * a fake triangle outside of the bounds that is used as a start
	 * for iterating/walking through the triangulation along neighborships
	 */
	public final DelaunayTriangle handleTriangle;

	public DelaunayTriangulation(AxisAlignedBoundingBoxXZ bounds) {
				
		VectorXYZ boundV0 = bounds.bottomLeft().xyz(0);
		VectorXYZ boundV1 = bounds.bottomRight().xyz(0);
		VectorXYZ boundV2 = bounds.topRight().xyz(0);
		VectorXYZ boundV3 = bounds.topLeft().xyz(0);
		
		DelaunayTriangle t1 = new DelaunayTriangle(boundV0, boundV1, boundV3);
		DelaunayTriangle t2 = new DelaunayTriangle(boundV1, boundV2, boundV3);
				
		t1.setNeighbor(1, t2);
		t2.setNeighbor(2, t1);
		
		handleTriangle = new DelaunayTriangle(boundV1, boundV0,
				new VectorXYZ(bounds.center().x, 0, bounds.minZ - bounds.sizeZ()));
				
		t1.setNeighbor(0, handleTriangle);
		handleTriangle.setNeighbor(0, t1);
		
	}
	
	/**
	 * returns all triangles
	 */
	public Iterable<DelaunayTriangle> getTriangles() {
		return ITERABLE;
	}

	public Stack<Flip> insert(VectorXYZ point) { //TODO: should use <T extends Has(Immutable)Position>
		
		DelaunayTriangle triangleEnclosingPoint = getEnlosingTriangle(point.xz());
		
		if (triangleEnclosingPoint == null) {
			System.out.println("null");
			//TODO check for null
		}
		
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
	
	/**
	 * temporarily inserts a point to calculate its natural neighbors,
	 * then undoes the insertion
	 */
	public NaturalNeighbors probe(VectorXZ point) {

		VectorXYZ probePoint = point.xyz(0);
		
		/* insert the point */
		
		Stack<Flip> flipStack = insert(probePoint);
		
		/* identify neighbors and modified triangles */
		
		Set<VectorXYZ> neighbors = new HashSet<VectorXYZ>();
		Multimap<VectorXYZ, DelaunayTriangle> oldTriangles = HashMultimap.create();
		Multimap<VectorXYZ, DelaunayTriangle> newTriangles = HashMultimap.create();
		
		// first loop, identifies neighbors and newly created triangles
		for (Flip flip : flipStack) {
			for (DelaunayTriangle triangle : flip.getCreatedTriangles()) {
				newTriangles.put(triangle.p0, triangle);
				newTriangles.put(triangle.p1, triangle);
				newTriangles.put(triangle.p2, triangle);
				neighbors.add(triangle.p0);
				neighbors.add(triangle.p1);
				neighbors.add(triangle.p2);
			}
		}
		
		// second loop, removes some newTriangles and identifies oldTriangles
		for (Flip flip : flipStack) {
			for (DelaunayTriangle triangle : flip.getRemovedTriangles()) {
				oldTriangles.put(triangle.p0, triangle);
				oldTriangles.put(triangle.p1, triangle);
				oldTriangles.put(triangle.p2, triangle);
				newTriangles.remove(triangle.p0, triangle);
				newTriangles.remove(triangle.p1, triangle);
				newTriangles.remove(triangle.p2, triangle);
			}
		}
		
		// third loop, removes some oldTriangles
		for (Flip flip : flipStack) {
			for (DelaunayTriangle triangle : flip.getCreatedTriangles()) {
				oldTriangles.remove(triangle.p0, triangle);
				oldTriangles.remove(triangle.p1, triangle);
				oldTriangles.remove(triangle.p2, triangle);
			}
		}
		
		// FIXME: cannot use any oldTriangle as starting point, may have been newly created before
		
		neighbors.remove(probePoint);
		
		NaturalNeighbors result = new NaturalNeighbors(neighbors);
		
		/* calculate size of voronoi cells with the point */
		
		for (int i = 0; i < result.neighbors.length; i++) {
			result.relativeWeights[i] = getVoronoiCellSize(result.neighbors[i],
					newTriangles.get(result.neighbors[i]));
		}
		
		/* undo insertion */
		
		while (!flipStack.isEmpty()) {
			flipStack.pop().undo();
		}

		/* calculate difference of voronoi cell size with and without the point */
		
		double areaDifferenceSum = 0;
		
		for (int i = 0; i < result.neighbors.length; i++) {
			
			double area = getVoronoiCellSize(result.neighbors[i],
					oldTriangles.get(result.neighbors[i]));
			
			result.relativeWeights[i] = area - result.relativeWeights[i];
			
			areaDifferenceSum += result.relativeWeights[i];
		}
		
		/* calculate relative weights of neighbors */
		
		for (int i = 0; i < result.neighbors.length; i++) {
			result.relativeWeights[i] /= areaDifferenceSum;
			
			if (result.relativeWeights[i] > 1) {
				System.out.println(result.relativeWeights[i]);
			}
			
		}
		
		return result;
		
	}
	
	public List<DelaunayTriangle> getIncidentTriangles(final VectorXYZ point) {

		List<DelaunayTriangle> result = new ArrayList<DelaunayTriangle>();
		
		for (DelaunayTriangle triangle : getTriangles()) {
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
	
	public List<TriangleXZ> getVoronoiCellSectors(VectorXYZ point) {
		return getVoronoiCellSectors(point, getIncidentTriangles(point));
	}
			
	/**
	 * TODO describe effect of incident triangles
	 */
	public List<TriangleXZ> getVoronoiCellSectors(VectorXYZ point,
			Collection<DelaunayTriangle> incidentTriangles) {
		
		final VectorXZ pointXZ = point.xz();
		
		/* build sorted list of triangles
		 * by starting at any incidentTriangle and going left and right
		 * around the point, appending neighbors. */
		
		List<DelaunayTriangle> triangles =
				new ArrayList<DelaunayTriangle>(incidentTriangles.size() + 2);
		
		DelaunayTriangle startTriangle = incidentTriangles.iterator().next();
		triangles.add(startTriangle);
		
		DelaunayTriangle currentTriangle = startTriangle;
		
		while (/* incidentTriangles.contains(currentTriangle)  TODO re-enable
				&& */ (currentTriangle != startTriangle || triangles.size() == 1)
				&& currentTriangle.getRightNeighbor(point) != null) {
			
			currentTriangle = currentTriangle.getRightNeighbor(point);
			triangles.add(currentTriangle);
			
		}
		
		Collections.reverse(triangles);
		
		if (currentTriangle != startTriangle) { //check for full circle
		
			List<DelaunayTriangle> leftTriangles =
					new ArrayList<DelaunayTriangle>();
			
			currentTriangle = startTriangle;
		
			while (/* incidentTriangles.contains(currentTriangle) TODO re-enable
					&& */ (currentTriangle != startTriangle || triangles.isEmpty())
					&& currentTriangle.getLeftNeighbor(point) != null) { //TODO: avoid infinite loop
				
				currentTriangle = currentTriangle.getLeftNeighbor(point);
				leftTriangles.add(currentTriangle);
				
			}
			
			triangles.addAll(leftTriangles);
			
		}
				
		/* calculate the circumcircle centers */
		
		List<VectorXZ> centers = new ArrayList<VectorXZ>();
		
		for (DelaunayTriangle t : triangles) {
			centers.add(t.getCircumcircleCenter());
		}

		/* calculate the sectors */
		
		List<TriangleXZ> result = new ArrayList<TriangleXZ>();
				
		for (int i = 0; i+1 < centers.size(); i++) {
			result.add(new TriangleXZ(pointXZ,
					centers.get(i),
					centers.get(i+1)));
		}
		
		return result;
		
	}
	
	/**
	 * returns the size of a voronoi cell or a part of the voronoi cell.
	 * 
	 * @param point  point corresponding to the voronoi cell
	 * @param incidentTriangles  if this contains all triangles incident to
	 *   point, then the size of the entire cell will be calculated.
	 *   Otherwise, only sides that contain at least one circumcircle center
	 *   of a triangle in this collection are taken into account.
	 */
	public double getVoronoiCellSize(VectorXYZ point,
			Collection<DelaunayTriangle> incidentTriangles) {
		
		double size = 0;
		
		for (TriangleXZ t : getVoronoiCellSectors(point, incidentTriangles)) {
			size += t.getArea();
		}
		
		return size;
		
	}
	
	private boolean isDelaunay(DelaunayTriangle triangle) {
		
		DelaunayTriangle neighborTriangle = triangle.getNeighbor(0);
		
		if (neighborTriangle != null && neighborTriangle != handleTriangle) {
			
			double a1 = triangle.angleAt(2);
			double a2 = neighborTriangle.angleOppositeOf(triangle);
			
			return a1 + a2 <= PI;
			
		} else {
			
			return true;
			
		}
		
	}
	
	/**
	 * returns the triangle containing the given point
	 * 
	 * @param point  must lie within the triangulation; != null
	 */
	public DelaunayTriangle getEnlosingTriangle(VectorXZ point) {
		
		/* use a 'visibility walk' through the triangulation,
		 * starting at the handleTriangle */
		
		DelaunayTriangle currentTriangle = handleTriangle;
		
		boolean triangleContainsPoint = false;
		
		while (!triangleContainsPoint) {
			
			triangleContainsPoint = true;
			
			for (int i = 0; i <= 2; i++) {
				
				// check whether the line defined by the i-th edge separates
				// the target point from the current triangle center.
				// (relies on counterclockwise winding)
				
				if (isRightOf(point,
						currentTriangle.getPoint(i).xz(),
						currentTriangle.getPoint((i + 1) % 3).xz())) { //TODO avoid xz()
					
					triangleContainsPoint = false;
					currentTriangle = currentTriangle.getNeighbor(i);
					break;
					
				}
				
			}
			
		}
		
		return currentTriangle;
		
	}
	
}
