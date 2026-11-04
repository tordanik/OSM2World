package org.osm2world.world.surfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapElement;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.world.connectors.AttachmentSurface;
import org.osm2world.world.connectors.AttachmentUtil;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.BridgeModule;
import org.osm2world.world.modules.TunnelModule;

/**
 * a claim by a {@link WorldObject} to part of a surface, such as the terrain, a roof or a bridge deck.
 * <p>
 * Where the raw footprints of several claims on the same surface overlap,
 * the claim with the highest priority keeps the contested area.
 * Similar to {@link org.osm2world.world.connectors.Connector}s, claims are created by their owner
 * and then updated during the conversion.
 */
public class SurfaceClaim implements BoundedObject {

	/** surface type for the terrain */
	public static final String GROUND_SURFACE_TYPE = "ground";

	private final WorldObject owner;
	private final PolygonShapeXZ rawFootprint;
	private final int priority;
	private final List<String> compatibleSurfaceTypes;

	private boolean assigned = false;
	private @Nullable String surfaceType = null;
	private @Nullable AttachmentSurface surface = null;

	private @Nullable List<PolygonShapeXZ> footprint = null;

	/**
	 * @param owner                   the object which makes this claim
	 * @param rawFootprint            the area claimed in the XZ plane, before resolving overlaps with other claims
	 * @param priority                priority relative to other claims on the same surface,
	 *                                see {@link WorldObject#getOverlapPriority()} for typical values
	 * @param compatibleSurfaceTypes  the types of surfaces this claim can be placed on, in order of preference.
	 *                                Uses the same type names as
	 *                                {@link AttachmentUtil#getCompatibleSurfaceTypes(MapElement)},
	 *                                plus {@link #GROUND_SURFACE_TYPE} for the terrain.
	 */
	public SurfaceClaim(WorldObject owner, PolygonShapeXZ rawFootprint, int priority,
			List<String> compatibleSurfaceTypes) {
		if (compatibleSurfaceTypes.isEmpty()) {
			throw new IllegalArgumentException("a surface claim needs at least one compatible surface type");
		}
		this.owner = owner;
		this.rawFootprint = rawFootprint;
		this.priority = priority;
		this.compatibleSurfaceTypes = List.copyOf(compatibleSurfaceTypes);
	}

	public WorldObject getOwner() {
		return owner;
	}

	public PolygonShapeXZ getRawFootprint() {
		return rawFootprint;
	}

	public int getPriority() {
		return priority;
	}

	public List<String> getCompatibleSurfaceTypes() {
		return compatibleSurfaceTypes;
	}

	/**
	 * places this claim on a surface. Only for use during the conversion.
	 *
	 * @param surfaceType  the type from {@link #getCompatibleSurfaceTypes()} which was used to choose the surface,
	 *                     null if no compatible surface was found
	 * @param surface      the chosen surface, null for {@link #GROUND_SURFACE_TYPE}
	 *                     or if no compatible surface was found
	 */
	public void assign(@Nullable String surfaceType, @Nullable AttachmentSurface surface) {
		if (assigned) {
			throw new IllegalStateException("this claim has already been assigned");
		} else if (surfaceType != null && !compatibleSurfaceTypes.contains(surfaceType)) {
			throw new IllegalArgumentException("incompatible surface type: " + surfaceType);
		} else if ((surface == null) != (surfaceType == null || GROUND_SURFACE_TYPE.equals(surfaceType))) {
			throw new IllegalArgumentException("a surface is required for all types except the ground");
		}
		this.assigned = true;
		this.surfaceType = surfaceType;
		this.surface = surface;
		if (surface != null) {
			surface.addClaim(this);
		}
	}

	/** returns whether {@link #assign(String, AttachmentSurface)} has been called */
	public boolean isAssigned() {
		return assigned;
	}

	/** returns the type which was used to choose the surface, null if not assigned or no surface was found */
	public @Nullable String getSurfaceType() {
		return surfaceType;
	}

	/** returns the surface this claim has been placed on, null if not assigned, on the ground or none was found */
	public @Nullable AttachmentSurface getSurface() {
		return surface;
	}

	public boolean isOnGround() {
		return GROUND_SURFACE_TYPE.equals(surfaceType);
	}

	/**
	 * sets the result of resolving overlaps with other claims. Only for use during the conversion.
	 *
	 * @param footprint  the part of {@link #getRawFootprint()} which this claim keeps, can be empty
	 */
	public void setFootprint(List<PolygonShapeXZ> footprint) {
		if (this.footprint != null) {
			throw new IllegalStateException("the footprint has already been set");
		}
		this.footprint = List.copyOf(footprint);
	}

	/**
	 * returns the part of {@link #getRawFootprint()} which this claim keeps
	 * after overlaps with higher-priority claims on the same surface have been resolved.
	 *
	 * @throws IllegalStateException  if overlaps have not been resolved yet
	 */
	public List<PolygonShapeXZ> getFootprint() {
		if (footprint == null) {
			throw new IllegalStateException("overlaps have not been resolved yet for " + this);
		}
		return footprint;
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return rawFootprint.boundingBox();
	}

	@Override
	public String toString() {
		return "SurfaceClaim{" + owner + ", " + compatibleSurfaceTypes + ", priority " + priority
				+ (assigned ? ", on " + surfaceType : "") + "}";
	}

	/**
	 * creates the claims for objects which claim their {@link WorldObject#getRawGroundFootprint()}:
	 * one claim for each polygon, using {@link WorldObject#getOverlapPriority()}
	 * and {@link #defaultSurfaceTypes(WorldObject)}.
	 */
	public static List<SurfaceClaim> createDefaultClaims(WorldObject object) {

		Collection<PolygonShapeXZ> rawFootprint = object.getRawGroundFootprint();
		if (rawFootprint.isEmpty()) return List.of();

		List<String> surfaceTypes = defaultSurfaceTypes(object);
		if (surfaceTypes.isEmpty()) return List.of();

		int priority = object.getOverlapPriority();

		return rawFootprint.stream()
				.map(polygon -> new SurfaceClaim(object, polygon, priority, surfaceTypes))
				.toList();

	}

	/**
	 * returns the compatible surface types for an object.
	 * These are the types from {@link AttachmentUtil#getCompatibleSurfaceTypes(MapElement)},
	 * followed by {@link #GROUND_SURFACE_TYPE} unless the element is on a bridge, a tunnel or a roof
	 * according to {@link WorldObject#getGroundState()} and its tags.
	 */
	public static List<String> defaultSurfaceTypes(WorldObject object) {

		MapElement element = object.getPrimaryMapElement();

		List<String> result = new ArrayList<>();

		if (element == null) {
    		result.add(GROUND_SURFACE_TYPE);
		} else {

			result.addAll(AttachmentUtil.getCompatibleSurfaceTypes(element));

			boolean onRoof = result.stream().anyMatch(type -> type.startsWith("roof"));

			if (!onRoof && !BridgeModule.isBridge(element.getTags()) && !TunnelModule.isTunnel(element.getTags())) {
				result.add(GROUND_SURFACE_TYPE);
			}

		}

		GroundState groundState = object.getGroundState();
		if (groundState == GroundState.ABOVE || groundState == GroundState.BELOW) {
			result.remove(GROUND_SURFACE_TYPE);
		}

		return result;

	}

}
