package org.osm2world.scene.texcoord;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXZ;

/**
 * describes positions within a strip in terms of distance along and fraction across the strip.
 * The strip is defined by its left and right outline, which have the same number of vertices.
 * Used by {@link StripPositionTexCoordFunction}.S
 */
public class StripCoordinates {

	private final List<VectorXZ> left;
	private final List<VectorXZ> right;

	/** distance along the left outline at each of its vertices */
	private final double[] distanceAlongLeft;

	public StripCoordinates(List<VectorXZ> left, List<VectorXZ> right) {

		if (left.size() != right.size() || left.size() < 2) {
			throw new IllegalArgumentException("outlines need the same number of vertices, and at least 2");
		}

		this.left = left;
		this.right = right;

		distanceAlongLeft = new double[left.size()];
		for (int i = 1; i < left.size(); i++) {
			distanceAlongLeft[i] = distanceAlongLeft[i - 1] + left.get(i - 1).distanceTo(left.get(i));
		}

	}

	/** returns the length of the strip's left outline */
	public double getLength() {
		return distanceAlongLeft[left.size() - 1];
	}

	/**
	 * a position within the strip
	 *
	 * @param distanceAlong   distance along the left outline
	 * @param fractionAcross  0 at the left outline, 1 at the right outline
	 * @param width           the strip's width at that position
	 */
	public record StripPosition(double distanceAlong, double fractionAcross, double width) {}

	/**
	 * determines a point's position within the strip.
	 * Uses the quadrilateral which contains the point or, if there is none, the closest one.
	 *
	 * @author Claude Opus 5.5
	 */
	public StripPosition positionOf(VectorXZ p) {

		StripPosition best = null;
		double bestError = Double.POSITIVE_INFINITY;

		for (int i = 0; i + 1 < left.size(); i++) {

			double[] uv = inverseBilinear(p, left.get(i), left.get(i + 1), right.get(i + 1), right.get(i));
			if (uv == null) continue;

			// how far outside the quadrilateral the point is (0 if it is inside)
			double error = max(0, max(-uv[0], uv[0] - 1)) + max(0, max(-uv[1], uv[1] - 1));

			if (error < bestError) {
				double u = min(1, max(0, uv[0]));
				double v = min(1, max(0, uv[1]));
				double distanceAlong = distanceAlongLeft[i] + u * (distanceAlongLeft[i + 1] - distanceAlongLeft[i]);
				double width = (1 - u) * left.get(i).distanceTo(right.get(i))
						+ u * left.get(i + 1).distanceTo(right.get(i + 1));
				best = new StripPosition(distanceAlong, v, width);
				bestError = error;
				if (error == 0) break;
			}

		}

		if (best == null) {
			throw new IllegalArgumentException("cannot determine strip position for " + p);
		}

		return best;

	}

	/**
	 * finds the parameters (u, v) such that p = bilinear interpolation of the quadrilateral a, b, c, d,
	 * where u goes from a to b (and d to c), and v goes from a to d (and b to c).
	 *
	 * @return  {u, v}, which are between 0 and 1 for points inside the quadrilateral; null if there is no solution
	 * @author Claude Opus 5.5
	 */
	static double[] inverseBilinear(VectorXZ p, VectorXZ a, VectorXZ b, VectorXZ c, VectorXZ d) {

		VectorXZ e = b.subtract(a);
		VectorXZ f = d.subtract(a);
		VectorXZ g = a.subtract(b).add(c).subtract(d);
		VectorXZ h = p.subtract(a);

		double k2 = cross(g, f);
		double k1 = cross(e, f) + cross(h, g);
		double k0 = cross(h, e);

		List<Double> vCandidates = new ArrayList<>(2);

		if (abs(k2) < 1e-12 * max(1, abs(k1))) {
			// opposite edges are parallel, the equation is linear
			if (abs(k1) < 1e-12) return null;
			vCandidates.add(-k0 / k1);
		} else {
			double discriminant = k1 * k1 - 4 * k0 * k2;
			if (discriminant < 0) return null;
			double sqrt = Math.sqrt(discriminant);
			vCandidates.add((-k1 - sqrt) / (2 * k2));
			vCandidates.add((-k1 + sqrt) / (2 * k2));
		}

		/* determine u for each candidate, and use the solution which is closest to being inside */

		double[] best = null;
		double bestError = Double.POSITIVE_INFINITY;

		for (double v : vCandidates) {

			// use the component with the larger denominator for numerical stability
			VectorXZ denominator = e.add(g.mult(v));
			double u;
			if (abs(denominator.x) > abs(denominator.z)) {
				u = (h.x - f.x * v) / denominator.x;
			} else if (denominator.z != 0) {
				u = (h.z - f.z * v) / denominator.z;
			} else {
				continue;
			}

			double error = max(0, max(-u, u - 1)) + max(0, max(-v, v - 1));
			if (error < bestError) {
				best = new double[] {u, v};
				bestError = error;
			}

		}

		return best;

	}

	private static double cross(VectorXZ a, VectorXZ b) {
		return a.x * b.z - a.z * b.x;
	}

}
