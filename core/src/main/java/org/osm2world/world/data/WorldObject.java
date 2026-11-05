package org.osm2world.world.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.world.connectors.AttachmentSurface;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.connectors.EleRelation;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.surfaces.SurfaceClaim;

public interface WorldObject {

	/**
	 * returns the meshes making up this {@link WorldObject}.
	 */
	public List<Mesh> buildMeshes();

	/**
	 * returns the meshes making up this {@link WorldObject}, including {@link #getSubModels()}.
	 */
	public default List<Mesh> buildMeshesForModelHierarchy() {
		List<Mesh> result = new ArrayList<>(buildMeshes());
		getSubModels().forEach(it -> result.addAll(it.getMeshes()));
		return result;
	}

	/**
	 * returns the "primary" {@link MapElement} for this WorldObject;
	 * i.e. the one it is most strongly associated with.
	 * Can be null if there is no (clear) primary element for this feature.
	 */
	public MapElement getPrimaryMapElement();

	/**
	 * returns another world object this is part of, if any (e.g. a room is part of a building).
	 * Parents are responsible for rendering their children, so only root objects (those returning null here)
	 * will have their {@link #buildMeshes()} and {@link #getSubModels()} methods called directly.
	 */
	public default @Nullable WorldObject getParent() { return null; }

	/**
	 * returns instances of {@link Model}s that are part of this {@link WorldObject}.
	 * Together with the result of {@link #buildMeshes()}, these compose the visual appearance of this object.
	 */
	public default List<ModelInstance> getSubModels() { return emptyList(); }

	/**
	 * returns whether this feature is on, above or below the ground.
	 * This is relevant for elevation calculations,
	 * because the elevation of features o.t.g. is directly
	 * determined by terrain elevation data.
	 * Elevation of features above/below t.g. depends on elevation of
	 * features o.t.g. as well as other features above/below t.g.
	 */
	public default GroundState getGroundState() {
		if (getParent() != null) {
			return getParent().getGroundState();
		} else {
			return GroundState.ON;
		}
	}

	/**
	 * returns all {@link Connector}s used by this WorldObject
	 */
	public Iterable<Connector> getConnectors();

	/**
	 * lets this object add constraints for the relative elevations of its
	 * {@link Connector}s. Called after {@link #getConnectors()}.
	 */
	public default void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	/**
	 * returns relations between the elevations of this object's {@link Connector}s,
	 * such as connectors which can only move up or down together.
	 * Called after {@link #getConnectors()}.
	 */
	public default Collection<EleRelation> getEleRelations() {
		return emptyList();
	}

	/**
	 * returns this object's surfaces that other objects can attach {@link Connector}s and {@link SurfaceClaim}s to.
	 * The surfaces' heights are provisional and may be updated later with {@link #updateAttachmentSurfaces(List)}.
	 * @see AttachmentSurface
	 */
	public default Collection<AttachmentSurface> getAttachmentSurfaces() {
		return emptyList();
	}

	/**
	 * recalculates the geometry of this object's attachment surfaces, e.g. after its connectors have moved.
	 * The surfaces are updated in place because connectors keep references to them.
	 * Connectors attached to the surfaces are not moved; that is the caller's responsibility.
	 * <p>
	 * The default implementation calls {@link #getAttachmentSurfaces()} again and copies the geometry of the
	 * resulting surfaces into the previous ones, matching them by order.
	 * This works for objects which create new surfaces on every call (such as {@link ProceduralWorldObject}s).
	 * Objects which instead cache their surfaces need to override this method
	 * if their surfaces' geometry can change.
	 *
	 * @param surfaces  the surfaces previously returned by {@link #getAttachmentSurfaces()}, in the same order
	 */
	public default void updateAttachmentSurfaces(List<AttachmentSurface> surfaces) {

		List<AttachmentSurface> newSurfaces = List.copyOf(getAttachmentSurfaces());

		if (newSurfaces.size() != surfaces.size()) {
			throw new IllegalStateException("number of attachment surfaces has changed for " + this
					+ ": " + surfaces.size() + " -> " + newSurfaces.size());
		}

		for (int i = 0; i < surfaces.size(); i++) {
			if (surfaces.get(i) != newSurfaces.get(i)) {
				surfaces.get(i).updateGeometryFrom(newSurfaces.get(i));
			}
		}

	}

	/**
	 * returns a counterclockwise polygon defining the object's ground footprint in the XZ plane.
	 * Can be used for purposes such as preventing bridge pillars from piercing through this WorldObject.
	 *
	 * @return outline polygon; null if this world object doesn't cover any area
	 */
	public default @Nullable PolygonShapeXZ getOutlinePolygonXZ() {
		return null;
	}

	/**
	 * returns a list of polygons defining an object's raw footprint in the xz plane.
	 * The footprint is on a surface, most commonly the terrain, but it could also be on a roof or bridge deck.
	 * The resolved footprint, {@link #getFootprint()}, may be smaller if other objects on the same surface
	 * have a higher {@link #getOverlapPriority()}.
	 *
	 * @return collection of outline polygons, empty if the world object doesn't cover any area
	 */
	public default Collection<PolygonShapeXZ> getRawFootprint() {
		return emptyList();
	}

	/**
	 * returns this object's footprints which will be placed on surfaces such as the terrain, roofs or bridge decks.
	 * Must return the same instances every time it is called, because the claims are updated during the conversion.
	 * <p>
	 * Objects which claim their {@link #getRawFootprint()} can create their claims with
	 * {@link SurfaceClaim#createDefaultClaims(WorldObject)}.
	 *
	 * @return  the claims, empty if this object doesn't claim any surface
	 */
	public default List<SurfaceClaim> getSurfaceClaims() {
		return List.of();
	}

	/**
	 * priority relative to other objects when it comes to resolving overlaps between {@link SurfaceClaim}s.
	 * <p>
	 * Typical values:
	 * - MAX_VALUE: will always be left intact (e.g. linear roads); this is the default
	 * - 50: highway polygons
	 * - 30: surface areas with a distinct physical representation (e.g. car parking)
	 * - 20: areas with explicitly mapped surface, landcover and similar features
	 * - 10: areas with implied surface (e.g. parks)
	 * -  5: implied surface around other features (e.g. ballast around rails)
	 * - MIN_VALUE: default terrain that is used to fill gaps where nothing has been mapped
	 */
	public default int getOverlapPriority() {
		return Integer.MAX_VALUE;
	}

	/**
	 * returns the part of the surface which this object covers after overlaps with other objects have been resolved.
	 * This is the combined {@link SurfaceClaim#getFootprint()} of its {@link #getSurfaceClaims()}.
	 * Objects without claims cover their {@link #getOutlinePolygonXZ()}.
	 * Only available once overlaps have been resolved, which happens before connectors are created.
	 *
	 * TODO: move to {@link ProceduralWorldObject} once all procedural objects use that interface
	 */
	default Collection<PolygonShapeXZ> getFootprint() {

		List<SurfaceClaim> claims = getSurfaceClaims();

		if (claims.isEmpty()) {
			PolygonShapeXZ outline = getOutlinePolygonXZ();
			return outline == null ? List.of() : List.of(outline);
		} else {
			return claims.stream().flatMap(c -> c.getFootprint().stream()).toList();
		}

	}

}