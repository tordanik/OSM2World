package org.osm2world.world.surfaces;

import static java.util.Collections.newSetFromMap;
import static org.osm2world.math.algorithms.JTSConversionUtil.*;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bboxUnion;

import java.util.*;

import javax.annotation.Nullable;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.overlayng.CoverageUnion;
import org.locationtech.jts.operation.overlayng.UnaryUnionNG;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.CAGUtil;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.world.connectors.AttachmentSurface;

/**
 * divides surfaces between the {@link SurfaceClaim}s placed on them.
 * <p>
 * Where claims on the same surface overlap, the claim which ranks higher keeps the contested area:
 * <ol>
 *     <li>the claim with the higher priority</li>
 *     <li>if the priorities are equal, the claim with the smaller raw footprint,
 *     which is typically the more detailed feature</li>
 *     <li>if the areas are equal as well, the claim which comes first</li>
 * </ol>
 * Claims with priority {@link Integer#MAX_VALUE} are never reduced, even if they overlap each other.
 * Claims on different surfaces never affect each other.
 * <p>
 * The boundaries of all claims on a surface are noded together, with coordinates rounded to {@link #PRECISION}.
 * As a result, neighboring footprints share the vertices along their common boundary.
 *
 * @author Claude Opus 5.5
 */
public final class SurfacePartitionUtil {

	/** prevents instantiation */
	private SurfacePartitionUtil() {}

	/** the precision of the resulting footprints: coordinates are rounded to 1 mm */
	public static final PrecisionModel PRECISION = new PrecisionModel(1000);

	/**
	 * resolves overlaps between claims and sets each claim's {@link SurfaceClaim#getFootprint()}.
	 * Claims without a surface keep their raw footprint.
	 *
	 * @param claims  claims which have been assigned to surfaces, in a deterministic order
	 */
	public static void resolveOverlaps(List<SurfaceClaim> claims) {

		/* group the claims by surface */

		Map<AttachmentSurface, List<SurfaceClaim>> claimsBySurface = new IdentityHashMap<>();
		List<SurfaceClaim> groundClaims = new ArrayList<>();

		for (SurfaceClaim claim : claims) {
			if (claim.isOnGround()) {
				groundClaims.add(claim);
			} else if (claim.getSurface() != null) {
				claimsBySurface.computeIfAbsent(claim.getSurface(), s -> new ArrayList<>()).add(claim);
			} else {
				claim.setFootprint(List.of(claim.getRawFootprint()));
			}
		}

		/* resolve overlaps on each surface */

		resolveOverlapsOnSurface(groundClaims);
		claimsBySurface.values().forEach(SurfacePartitionUtil::resolveOverlapsOnSurface);

	}

	/** resolves overlaps between claims which are all on the same surface */
	private static void resolveOverlapsOnSurface(List<SurfaceClaim> claims) {

		if (claims.isEmpty()) return;

		Map<SurfaceClaim, Integer> order = new IdentityHashMap<>();
		for (int i = 0; i < claims.size(); i++) {
			order.put(claims.get(i), i);
		}

		IndexGrid<SurfaceClaim> index = new IndexGrid<>(bboxUnion(claims).pad(1), 50.0, 50.0);
		claims.forEach(index::insert);

		Map<SurfaceClaim, List<PolygonShapeXZ>> footprints;

		try {
			footprints = partitionIntoFaces(claims, order, index);
		} catch (Exception e) {
			ConversionLog.error("Could not build a noded partition, using a simpler fallback", e,
					claims.get(0).getOwner());
			footprints = subtractHigherRankedClaims(claims, order, index);
		}

		for (SurfaceClaim claim : claims) {
			claim.setFootprint(footprints.get(claim));
		}

	}

	/**
	 * nodes the boundaries of all claims together, assigns each of the resulting faces to the claim(s) covering it,
	 * and combines the faces of each claim.
	 */
	private static Map<SurfaceClaim, List<PolygonShapeXZ>> partitionIntoFaces(List<SurfaceClaim> claims,
			Map<SurfaceClaim, Integer> order, IndexGrid<SurfaceClaim> index) {

		/* node the boundaries of all claims */

		List<Geometry> boundaries = new ArrayList<>(claims.size());
		for (SurfaceClaim claim : claims) {
			boundaries.add(toJTS(claim.getRawFootprint()).getBoundary());
		}

		Geometry nodedLines = UnaryUnionNG.union(GF.buildGeometry(boundaries), PRECISION);

		/* create the faces of the resulting planar subdivision */

		Polygonizer polygonizer = new Polygonizer();
		polygonizer.add(nodedLines);

		@SuppressWarnings("unchecked")
		Collection<Polygon> faces = polygonizer.getPolygons();

		/* assign each face to the claims it belongs to */

		Map<SurfaceClaim, PreparedGeometry> preparedFootprints = new IdentityHashMap<>();
		Map<SurfaceClaim, List<Polygon>> facesByClaim = new IdentityHashMap<>();

		for (Polygon face : faces) {

			Point interiorPoint = face.getInteriorPoint();
			VectorXZ p = fromJTS(interiorPoint.getCoordinate());

			Set<SurfaceClaim> nearbyClaims = newSetFromMap(new IdentityHashMap<>());
			index.probe(bbox(List.of(p))).forEach(nearbyClaims::add);

			List<SurfaceClaim> containingClaims = new ArrayList<>();
			for (SurfaceClaim claim : nearbyClaims) {
				PreparedGeometry prepared = preparedFootprints.computeIfAbsent(claim,
						c -> PreparedGeometryFactory.prepare(toJTS(c.getRawFootprint())));
				if (prepared.contains(interiorPoint)) {
					containingClaims.add(claim);
				}
			}
			containingClaims.sort(Comparator.comparing(order::get));

			for (SurfaceClaim winner : selectWinners(containingClaims, order)) {
				facesByClaim.computeIfAbsent(winner, c -> new ArrayList<>()).add(face);
			}

		}

		/* combine the faces of each claim */

		Map<SurfaceClaim, List<PolygonShapeXZ>> result = new IdentityHashMap<>();

		for (SurfaceClaim claim : claims) {
			List<Polygon> claimFaces = facesByClaim.getOrDefault(claim, List.of());
			if (claimFaces.isEmpty()) {
				result.put(claim, List.of());
			} else {
				Geometry union = CoverageUnion.union(GF.buildGeometry(claimFaces));
				result.put(claim, List.copyOf(polygonsFromJTS(union)));
			}
		}

		return result;

	}

	/**
	 * returns the claims which keep a face covered by all the given claims:
	 * all claims with priority {@link Integer#MAX_VALUE} if there are any, the highest ranking claim otherwise
	 */
	private static Collection<SurfaceClaim> selectWinners(List<SurfaceClaim> containingClaims,
			Map<SurfaceClaim, Integer> order) {

		List<SurfaceClaim> maxPriorityClaims = containingClaims.stream()
				.filter(c -> c.getPriority() == Integer.MAX_VALUE)
				.toList();

		if (!maxPriorityClaims.isEmpty()) {
			return maxPriorityClaims;
		}

		SurfaceClaim winner = null;
		for (SurfaceClaim claim : containingClaims) {
			if (winner == null || outranks(claim, winner, order)) {
				winner = claim;
			}
		}

		return winner == null ? List.of() : List.of(winner);

	}

	/**
	 * fallback which subtracts the raw footprints of higher-ranked claims from each claim individually.
	 * Unlike {@link #partitionIntoFaces(List, Map, IndexGrid)},
	 * this doesn't guarantee shared vertices along common boundaries.
	 */
	private static Map<SurfaceClaim, List<PolygonShapeXZ>> subtractHigherRankedClaims(List<SurfaceClaim> claims,
			Map<SurfaceClaim, Integer> order, IndexGrid<SurfaceClaim> index) {

		Map<SurfaceClaim, List<PolygonShapeXZ>> result = new IdentityHashMap<>();

		for (SurfaceClaim claim : claims) {

			if (claim.getPriority() == Integer.MAX_VALUE) {
				result.put(claim, List.of(claim.getRawFootprint()));
				continue;
			}

			Set<SurfaceClaim> others = newSetFromMap(new IdentityHashMap<>());
			index.probe(claim).forEach(others::add);

			List<PolygonShapeXZ> subtractPolys = new ArrayList<>();
			for (SurfaceClaim other : others) {
				if (other != claim && outranks(other, claim, order)
						&& other.boundingBox().overlaps(claim.boundingBox())) {
					subtractPolys.add(other.getRawFootprint());
				}
			}

			result.put(claim, subtract(claim, subtractPolys));

		}

		return result;

	}

	private static List<PolygonShapeXZ> subtract(SurfaceClaim claim, List<PolygonShapeXZ> subtractPolys) {

		if (subtractPolys.isEmpty()) {
			return List.of(claim.getRawFootprint());
		}

		try {
			return List.copyOf(CAGUtil.subtractPolygons(claim.getRawFootprint(), subtractPolys));
		} catch (Exception e) {
			ConversionLog.error("Could not resolve overlaps, using the raw footprint", e, claim.getOwner());
			return List.of(claim.getRawFootprint());
		}

	}

	/** returns true if claim a keeps an area which it shares with claim b */
	static boolean outranks(SurfaceClaim a, SurfaceClaim b, @Nullable Map<SurfaceClaim, Integer> order) {
		if (a.getPriority() != b.getPriority()) {
			return a.getPriority() > b.getPriority();
		}
		double areaA = a.getRawFootprint().getArea();
		double areaB = b.getRawFootprint().getArea();
		if (areaA != areaB) {
			return areaA < areaB;
		}
		return order != null && order.get(a) < order.get(b);
	}

}
