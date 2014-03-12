package org.osm2world.core.map_data.creation;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMNode;


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
	 */
	public void setOrigin(LatLon origin) {
		this.origin = origin;
	}
	
	/**
	 * sets a new origin. It is placed at the center of the bounds,
	 * or else at the first node's coordinates.
	 */
	public void setOrigin(OSMData osmData) {
		
		if (osmData.getBounds() != null && !osmData.getBounds().isEmpty()) {
			
			Bound firstBound = osmData.getBounds().iterator().next();
			
			setOrigin(new LatLon(
					(firstBound.getTop() + firstBound.getBottom()) / 2,
					(firstBound.getLeft() + firstBound.getRight()) / 2));
			
		} else {
			
			if (osmData.getNodes().isEmpty()) {
				throw new IllegalArgumentException(
						"OSM data must contain bounds or nodes");
			}
			
			OSMNode firstNode = osmData.getNodes().iterator().next();
			setOrigin(new LatLon(firstNode.lat, firstNode.lon));
			
		}
		
	}
	
}
