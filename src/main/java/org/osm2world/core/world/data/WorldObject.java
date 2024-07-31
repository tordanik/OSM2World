package org.osm2world.core.world.data;

import static java.util.Collections.emptyList;
import static org.osm2world.core.map_elevation.data.GroundState.ON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.ModelInstance;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;

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
		getSubModels().forEach(it -> result.addAll(it.model.buildMeshes(it.params)));
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
	 * will have their {@link Renderable#renderTo(org.osm2world.core.target.Target)} methods called.
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
	 * returns all {@link EleConnector}s used by this WorldObject
	 */
	public Iterable<EleConnector> getEleConnectors();

	/**
	 * lets this object add constraints for the relative elevations of its
	 * {@link EleConnector}s. Called after {@link #getEleConnectors()}.
	 */
	public default void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	/**
	 * returns this object's surfaces that other objects can attach themselves to
	 * @see AttachmentSurface
	 */
	public default Collection<AttachmentSurface> getAttachmentSurfaces() {
		return emptyList();
	}

	/**
	 * returns all {@link AttachmentConnector}s used by this WorldObject
	 */
	public default Iterable<AttachmentConnector> getAttachmentConnectors() {
		return emptyList();
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
	 * returns a list of polygons defining an object's ground footprint in the xz plane.
	 * This area will not be covered by terrain (i.e. it will be a "hole" in the terrain surface)
	 * or by other objects with a lower {@link #getOverlapPriority()}
	 *
	 * @return collection of outline polygons, empty list if the world object doesn't cover any area
	 */
	public default Collection<PolygonShapeXZ> getTerrainBoundariesXZ() {
		return emptyList();
	}

	/**
	 * priority relative to other objects when it comes to resolving overlaps.
	 * This is relevant when the same area would otherwise be covered by multiple objects,
	 * and the objects are both on the ground (according to {@link #getGroundState()}),
	 * or otherwise directly in conflict (e.g. both attached to the same roof).
	 *
	 * Typical values:
	 * - MAX_VALUE: will always be left intact (e.g. linear roads); this is the default
	 * - 50: highway polygons
	 * - 30: surface areas with a distinct physical representation (e.g. car parking)
	 * - 20: areas with explicitly mapped surface, landcover and similar features (
	 * - 10: areas with implied surface (e.g. parks)
	 * - MIN_VALUE: default terrain that is used to fill gaps where nothing has been mapped
	 */
	public default int getOverlapPriority() {
		return Integer.MAX_VALUE;
	}

	/**
	 * calculates the true ground footprint of this area by removing area covered by overlapping features
	 * with a higher {@link #getOverlapPriority()}.
	 *
	 * TODO: move to {@link ProceduralWorldObject} once all procedural objects use that interface
	 */
	default Collection<PolygonShapeXZ> getGroundFootprint() {

		if (getOutlinePolygonXZ() == null) {
			return emptyList();
		} else if (getOverlapPriority() == Integer.MAX_VALUE) {
			// this has the highest possible priority, nothing will be subtracted
			return List.of(getOutlinePolygonXZ());
		}

		SimplePolygonShapeXZ outerPoly = getOutlinePolygonXZ().getOuter();
		List<PolygonShapeXZ> subtractPolys = new ArrayList<>(getOutlinePolygonXZ().getHoles());

		/* collect the outlines of overlapping objects */

		for (MapOverlap<?, ?> overlap : getPrimaryMapElement().getOverlaps()) {
			for (WorldObject otherWO : overlap.getOther(getPrimaryMapElement()).getRepresentations()) {

				/* TODO: A world object might overlap even if the OSM element does not (e.g. a wide highway=* way).
				 * Perhaps overlaps should be calculated on world objects, not elements. */

				// check that both are on the ground, otherwise there is usually no conflict
				// TODO: there can also be a conflict if both are attached to the same AttachmentSurface (e.g. the same roof)
				boolean bothOnGround = this.getGroundState() == ON && otherWO.getGroundState() == ON;

				if (bothOnGround && otherWO.getOverlapPriority() > this.getOverlapPriority()) {

					if (overlap.type == MapOverlapType.CONTAIN
							&& overlap.e1 == getPrimaryMapElement()) {
						// completely within other element, no ground area left
						return emptyList();
					}

					try {
						subtractPolys.addAll(otherWO.getTerrainBoundariesXZ());
					} catch (InvalidGeometryException ignored) {
						// Prevent errors in other objects from affecting this object
					}

				}

			}

		}

		/* create "leftover" polygons by subtracting the existing ones */

		if (subtractPolys.isEmpty()) {
			return getTerrainBoundariesXZ();
		} else {
			return new ArrayList<>(CAGUtil.subtractPolygons(outerPoly, subtractPolys));
		}

	}

}