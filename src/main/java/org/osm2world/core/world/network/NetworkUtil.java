package org.osm2world.core.world.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;

/** methods related to networked features (road network, rail network, ...) */
public final class NetworkUtil {

	private NetworkUtil() {}

	/**
	 * Returns the connected world objects
	 * @param type  the type of network segment to look for
	 */
	public static final <S extends NetworkWaySegmentWorldObject> List<S> getConnectedNetworkSegments(
			MapNode node, Class<S> type, @Nullable Predicate<S> additionalFilter) {

		List<S> result = new ArrayList<S>();

		for (MapWaySegment segment : node.getConnectedWaySegments()) {

			if (type.isInstance(segment.getPrimaryRepresentation())) {
				@SuppressWarnings("unchecked") //the isInstance check ensures that this cast works
				S representation = (S)segment.getPrimaryRepresentation();
				if (additionalFilter == null || additionalFilter.test(representation)) {
					result.add(representation);
				}
			}

		}

		return result;

	}

}
