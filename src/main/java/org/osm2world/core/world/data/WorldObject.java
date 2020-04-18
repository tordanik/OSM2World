package org.osm2world.core.world.data;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.target.Renderable;

public interface WorldObject extends Renderable {

	/**
	 * returns the "primary" {@link MapElement} for this WorldObject;
	 * i.e. the one it is most strongly associated with.
	 * Can be null if there is no (clear) primary element for this feature.
	 */
	public MapElement getPrimaryMapElement();

	/**
	 * returns whether this feature is on, above or below the ground.
	 * This is relevant for elevation calculations,
	 * because the elevation of features o.t.g. is directly
	 * determined by terrain elevation data.
	 * Elevation of features above/below t.g. depends on elevation of
	 * features o.t.g. as well as other features above/below t.g.
	 */
	public GroundState getGroundState();

	/**
	 * returns all {@link EleConnector}s used by this WorldObject
	 */
	public Iterable<EleConnector> getEleConnectors();

	/**
	 * lets this object add constraints for the relative elevations of its
	 * {@link EleConnector}s. Called after {@link #getEleConnectors()}.
	 */
	public void defineEleConstraints(EleConstraintEnforcer enforcer);

}