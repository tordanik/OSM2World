package org.osm2world.core.world.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
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
	 * returns the meshes making up this {@link WorldObject}, including {@Link #getSubModels()}.
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

}