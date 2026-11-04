package org.osm2world.world.surfaces;

import static org.osm2world.math.algorithms.JTSConversionUtil.GF;
import static org.osm2world.math.algorithms.JTSConversionUtil.toJTS;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bboxUnion;
import static org.osm2world.world.surfaces.SurfaceClaim.GROUND_SURFACE_TYPE;

import java.util.*;

import javax.annotation.Nullable;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.world.connectors.AttachmentSurface;
import org.osm2world.world.data.WorldObject;

/**
 * chooses the surface for each {@link SurfaceClaim}.
 * <p>
 * Surfaces are chosen based on their XZ geometry only, so this can happen before elevations are known.
 * This relies on the {@link AttachmentSurface}s not changing their XZ projection after they have been created,
 * only their heights.
 *
 * @author Claude Opus 5.5
 */
public final class SurfaceAssignmentUtil {

	/** prevents instantiation */
	private SurfaceAssignmentUtil() {}

	/**
	 * minimum normal Y component of the faces which can take claims.
	 * Excludes vertical faces such as walls and downward-facing ones such as ceilings.
	 */
	static final double MIN_NORMAL_Y = 0.1;

	/** minimum overlap of a claim's raw footprint with a surface for the surface to be considered (in m²) */
	static final double MIN_OVERLAP_AREA = 1e-4;

	/**
	 * the surface chosen for a claim
	 *
	 * @param surfaceType  the type from {@link SurfaceClaim#getCompatibleSurfaceTypes()} which was used,
	 *                     null if no compatible surface was found
	 * @param surface      the chosen surface, null for {@link SurfaceClaim#GROUND_SURFACE_TYPE}
	 *                     or if no compatible surface was found
	 */
	public record Choice(@Nullable String surfaceType, @Nullable AttachmentSurface surface) {}

	/** returns the {@link SurfaceClaim}s of all the given objects */
	public static List<SurfaceClaim> collectClaims(Iterable<? extends WorldObject> objects) {

		List<SurfaceClaim> claims = new ArrayList<>();

		for (WorldObject object : objects) {
			try {
				claims.addAll(object.getSurfaceClaims());
			} catch (Exception e) {
				ConversionLog.error(e, object);
			}
		}

		return claims;

	}

	/**
	 * chooses a surface for each claim and assigns it using {@link SurfaceClaim#assign(String, AttachmentSurface)}.
	 *
	 * @see #chooseSurfaces(List, Collection)
	 */
	public static void assignSurfaces(List<SurfaceClaim> claims, Collection<AttachmentSurface> surfaces) {

		List<Choice> choices = chooseSurfaces(claims, surfaces);

		for (int i = 0; i < claims.size(); i++) {
			claims.get(i).assign(choices.get(i).surfaceType(), choices.get(i).surface());
		}

	}

	/**
	 * chooses a surface for each claim, without modifying the claims or surfaces.
	 * For each claim, the compatible surface types are tried in order of preference.
	 * The first type with at least one surface of that type overlapping the claim's raw footprint is used.
	 * If several surfaces of that type overlap it, the one with the largest overlap is chosen.
	 *
	 * @param surfaces  all surfaces which might be chosen. Only faces which are not vertical or upside-down are used.
	 * @return  one choice for each claim, in the same order as the claims
	 */
	public static List<Choice> chooseSurfaces(List<SurfaceClaim> claims, Collection<AttachmentSurface> surfaces) {
		SurfaceLookup lookup = new SurfaceLookup(surfaces, claims);
		return claims.stream().map(claim -> choose(claim, lookup)).toList();
	}

	private static Choice choose(SurfaceClaim claim, SurfaceLookup lookup) {

		Geometry footprint = toJTS(claim.getRawFootprint());

		for (String surfaceType : claim.getCompatibleSurfaceTypes()) {

			if (GROUND_SURFACE_TYPE.equals(surfaceType)) {
				return new Choice(surfaceType, null);
			}

			AttachmentSurface bestSurface = null;
			double bestOverlap = MIN_OVERLAP_AREA;

			for (AttachmentSurface surface : lookup.candidates(claim, surfaceType)) {
				double overlap = lookup.overlapArea(surface, footprint);
				if (overlap > bestOverlap) {
					bestSurface = surface;
					bestOverlap = overlap;
				}
			}

			if (bestSurface != null) {
				return new Choice(surfaceType, bestSurface);
			}

		}

		return new Choice(null, null);

	}

	/** finds surfaces near a claim and calculates the overlap with their XZ projection */
	private static class SurfaceLookup {

		/** all surfaces with suitable faces, in their original order */
		private final List<AttachmentSurface> surfaces;
		private final @Nullable IndexGrid<AttachmentSurface> index;

		/** XZ projections of the suitable faces of each surface, calculated when needed */
		private final Map<AttachmentSurface, List<Polygon>> facesXZ = new IdentityHashMap<>();

		SurfaceLookup(Collection<AttachmentSurface> allSurfaces, List<SurfaceClaim> claims) {

			surfaces = allSurfaces.stream()
					.filter(s -> s.getFaces().stream().anyMatch(SurfaceLookup::isSuitableFace))
					.toList();

			if (surfaces.isEmpty() || claims.isEmpty()) {
				index = null;
			} else {
				List<BoundedObject> boundedObjects = new ArrayList<>(surfaces);
				claims.forEach(c -> boundedObjects.add(c.getRawFootprint()));
				index = new IndexGrid<>(bboxUnion(boundedObjects).pad(1), 100.0, 100.0);
				surfaces.forEach(index::insert);
			}

		}

		/** returns the surfaces of the given type which might overlap the claim, in their original order */
		List<AttachmentSurface> candidates(SurfaceClaim claim, String surfaceType) {

			if (index == null) return List.of();

			Set<AttachmentSurface> nearby = Collections.newSetFromMap(new IdentityHashMap<>());
			index.probe(claim.getRawFootprint()).forEach(nearby::add);

			return surfaces.stream()
					.filter(nearby::contains)
					.filter(s -> s.getTypes().contains(surfaceType))
					.toList();

		}

		double overlapArea(AttachmentSurface surface, Geometry footprint) {
			double area = 0;
			for (Polygon face : facesXZ.computeIfAbsent(surface, SurfaceLookup::projectFaces)) {
				if (face.getEnvelopeInternal().intersects(footprint.getEnvelopeInternal())) {
					try {
						area += face.intersection(footprint).getArea();
					} catch (TopologyException e) {
						// skip faces with invalid geometry, e.g. degenerate triangles
					}
				}
			}
			return area;
		}

		private static List<Polygon> projectFaces(AttachmentSurface surface) {
			List<Polygon> result = new ArrayList<>();
			for (FlatSimplePolygonShapeXYZ face : surface.getFaces()) {
				if (isSuitableFace(face)) {
					Coordinate[] coords = face.vertices().stream()
							.map(v -> toJTS(v.xz()))
							.toArray(Coordinate[]::new);
					result.add(GF.createPolygon(coords));
				}
			}
			return result;
		}

		private static boolean isSuitableFace(FlatSimplePolygonShapeXYZ face) {
			VectorXYZ normal = face.getNormal();
			return normal.y > MIN_NORMAL_Y;
		}

	}

}
