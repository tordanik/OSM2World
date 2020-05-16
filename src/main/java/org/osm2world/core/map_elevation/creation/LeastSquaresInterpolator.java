package org.osm2world.core.map_elevation.creation;

import static java.lang.Math.*;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IndexGrid;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * uses least squares method to approximate a polynomial at each site,
 * and calculates elevations based on the polynomials at the nearest sites.
 */
public class LeastSquaresInterpolator implements TerrainInterpolator {

	private static final double CELL_SIZE = 50; //should only affect performance
	private static final int SITES_FOR_APPROX = 9;
	private static final int SITES_FOR_INTERPOL = 29;

	private Collection<SiteWithPolynomial> sites;
	private IndexGrid<SiteWithPolynomial> siteGrid; //TODO: rename IntersectionGrid to something more generic

	@Override
	public void setKnownSites(Collection<VectorXYZ> siteVectors) {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		sites = new ArrayList<SiteWithPolynomial>(siteVectors.size());

		siteGrid = new IndexGrid<SiteWithPolynomial>(
				bbox(siteVectors).pad(CELL_SIZE/2),
				CELL_SIZE, CELL_SIZE);

		for (VectorXYZ siteVector : siteVectors) {
			SiteWithPolynomial s = new SiteWithPolynomial(siteVector);
			sites.add(s);
			siteGrid.insert(s);
		}

		System.out.println("  time grid: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();

		/* approximate a polynomial at each site */

		Map<SiteWithPolynomial, List<SiteWithPolynomial>> nearestSiteMap
			= new HashMap<SiteWithPolynomial, List<SiteWithPolynomial>>();

		for (SiteWithPolynomial site : sites) {

			List<SiteWithPolynomial> nearestSites =
					findNearestSites(site.pos.xz(), SITES_FOR_APPROX, false);

			nearestSiteMap.put(site, nearestSites);

		}

		System.out.println("  time nearest sites: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();

		calculatePolynomials:
		for (SiteWithPolynomial site : sites) {

			List<SiteWithPolynomial> nearestSites =
					nearestSiteMap.get(site);

			RealVector vector = new ArrayRealVector(SITES_FOR_APPROX);
			RealMatrix matrix = new Array2DRowRealMatrix(
					SITES_FOR_APPROX, DefaultPolynomial.NUM_COEFFS);

			for (int row = 0; row < SITES_FOR_APPROX; row++) {
				SiteWithPolynomial nearSite = nearestSites.get(row);
				DefaultPolynomial.populateMatrix(matrix, row, nearSite.pos.x, nearSite.pos.z);
				vector.setEntry(row, nearSite.pos.y);
			}

			QRDecomposition qr = new QRDecomposition(matrix);
			RealVector solution = qr.getSolver().solve(vector);

			double[] coeffs = solution.toArray();

			for (double coeff : coeffs) {
				if (coeff > 10e3) {
					continue calculatePolynomials;
				}
			}

			site.setPolynomial(new DefaultPolynomial(coeffs));

		}

		System.out.println("  time polyonmials: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();

	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {

		List<SiteWithPolynomial> nearestSites =
				findNearestSites(pos, SITES_FOR_INTERPOL, true);

		double eleSum = 0;
		double weightSum = 0;

		for (SiteWithPolynomial site : nearestSites) {

			double distance = site.pos.distanceToXZ(pos);

			double weight = max(1 - distance / 120, 0);

			weightSum += weight;

			eleSum += weight * site.getPolynomial().evaluateAt(pos.x, pos.z);

		}

		double ele = eleSum / weightSum;

		return pos.xyz(ele);

	}

	/**
	 * provides access to the polynomials approximated internally.
	 * This is usually only interesting for debugging or similar tasks.
	 */
	public Collection<SiteWithPolynomial> getSitesWithPolynomials() {
		return sites;
	}

	private List<SiteWithPolynomial> findNearestSites(
			final VectorXZ pos, int numberSites, boolean requirePolynomial) {

		PriorityQueue<SiteWithPolynomial> result =
				new PriorityQueue<SiteWithPolynomial>(numberSites,
						new Comparator<SiteWithPolynomial>() {
					@Override
					public int compare(SiteWithPolynomial s1, SiteWithPolynomial s2) {
						return - Double.compare(s1.pos.distanceToXZ(pos),
								s2.pos.distanceToXZ(pos));
					}
				});

		Collection<SiteWithPolynomial>[][] cellArray = siteGrid.getCellArray();
		int cellX = siteGrid.cellXForCoord(pos.x);
		int cellZ = siteGrid.cellZForCoord(pos.z);

		int cellRange = 0;

		do {

			for (int i = max(cellX-cellRange, 0); i < min(cellX+cellRange+1, cellArray.length); i++) {
				for (int j = max(cellZ-cellRange, 0); j < min(cellZ+cellRange+1, cellArray[i].length); j++) {

					//needs to be on the outer ring of cells (others have been checked before)
					if (i == cellX-cellRange || i == cellX+cellRange
							|| j == cellZ-cellRange || j == cellZ+cellRange) {

						Collection<SiteWithPolynomial> sitesInCell = cellArray[i][j];

						if (sitesInCell != null) {
							for (SiteWithPolynomial site : sitesInCell) {

								if (requirePolynomial && site.polynomial == null) continue;

								if (result.size() < numberSites) {

									result.add(site);

								} else if (site.pos.distanceToXZ(pos) <
										result.peek().pos.distanceToXZ(pos)) {

									result.remove();
									result.add(site);

								}
							}
						}

					}

				}
			}

			cellRange ++;

		} while (result.size() < numberSites
				|| cellRange * CELL_SIZE < result.peek().pos.distanceToXZ(pos));

		//TODO error handling (not enough sites)

		/* get the result as a list with ascending distance */

		List<SiteWithPolynomial> resultList =
				new ArrayList<SiteWithPolynomial>(result);

		Collections.reverse(resultList);

		return resultList;

	}

	public static interface Polynomial {

		public double evaluateAt(double x, double z);

	}

	public static final class DefaultPolynomial implements Polynomial {

		private static final int NUM_COEFFS = 6;

		private final double[] coeffs;

		private DefaultPolynomial(double[] coeffs) {
			assert coeffs.length == NUM_COEFFS;
			this.coeffs = coeffs;
		}

		@Override
		public double evaluateAt(double x, double z) {
			return coeffs[0]
					+ coeffs[1] * x
					+ coeffs[2] * z
					+ coeffs[3] * x*x
					+ coeffs[4] * x*z
					+ coeffs[5] * z*z;
		}

		public static void populateMatrix(RealMatrix matrix, int row,
				double x, double z) {

			matrix.setEntry(row, 0, 1);
			matrix.setEntry(row, 1, x);
			matrix.setEntry(row, 2, z);
			matrix.setEntry(row, 3, x*x);
			matrix.setEntry(row, 4, x*z);
			matrix.setEntry(row, 5, z*z);

		}

		@Override
		public String toString() {
			return String.format(Locale.US,
					"%.3f + %.3fx + %.3fz + %.3fx^2 + %.3fxz + %.3fz^2",
					coeffs[0], coeffs[1], coeffs[2],
					coeffs[3], coeffs[4], coeffs[5]);
		}

	}

	public static final class SiteWithPolynomial implements IntersectionTestObject {

		public final VectorXYZ pos;
		private Polynomial polynomial;

		public SiteWithPolynomial(VectorXYZ site) {
			this.pos = site;
		}

		public Polynomial getPolynomial() {
			return polynomial;
		}

		public void setPolynomial(Polynomial polynomial) {
			this.polynomial = polynomial;
		}

		@Override
		public AxisAlignedRectangleXZ boundingBox() {
			return pos.boundingBox();
		}

		@Override
		public String toString() {
			return String.format("{%s, %s}", pos.toString(), polynomial);
		}

	}

}
