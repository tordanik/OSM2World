package org.osm2world.core.map_data.creation;

import org.osm2world.core.ConversionFacade;
import org.osm2world.core.osm.data.OSMData;

import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;


/**
 * abstract map projection superclass with configurable coordinate origin
 */
public abstract class OriginMapProjection implements MapProjection {

	/**
	 * the origin.
	 *
	 * TODO make this final when future Java versions offer a replacement for
	 * current factories in {@link ConversionFacade}
	 */
	protected LatLon origin;

	@Override
	public LatLon getOrigin() {
		return origin;
	}

	/**
	 * sets a new origin.
	 *
	 * Calling {@link #calcLat(org.osm2world.core.math.VectorXZ)},
	 * {@link #calcLon(org.osm2world.core.math.VectorXZ)} or
	 * {@link #calcPos(LatLon)} before the origin has been set
	 * will result in an {@link IllegalStateException}.
	 */
	public void setOrigin(LatLon origin) {
		this.origin = origin;
	}

	/**
	 * sets a new origin. It is placed at the center of the bounds,
	 * or else at the first node's coordinates.
	 *
	 * @see #setOrigin(LatLon)
	 */
	public void setOrigin(OSMData osmData) {

		if (osmData.getBounds() != null && !osmData.getBounds().isEmpty()) {

			OsmBounds firstBound = osmData.getBounds().iterator().next();

			setOrigin(new LatLon(
					(firstBound.getTop() + firstBound.getBottom()) / 2,
					(firstBound.getLeft() + firstBound.getRight()) / 2));

		} else {

			if (osmData.getData().getNodes().isEmpty()) {
				throw new IllegalArgumentException(
						"OSM data must contain bounds or nodes");
			}

			OsmNode firstNode = osmData.getData().getNodes().valueCollection().iterator().next();
			setOrigin(new LatLon(firstNode.getLatitude(), firstNode.getLongitude()));

		}

	}

}
