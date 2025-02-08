package org.osm2world.world.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.world.data.WaySegmentWorldObject;

/** methods related to networked features (road network, rail network, ...) */
public final class NetworkUtil {

	private NetworkUtil() {}

	/**
	 * Returns the connected {@link NetworkWaySegmentWorldObject}s.
	 * The result is ordered in the same fashion as {@link MapNode#getConnectedWaySegments()}
	 *
	 * @param type  the type of network segment to look for
	 */
	public static final <S extends NetworkWaySegmentWorldObject> List<S> getConnectedNetworkSegments(
			MapNode node, Class<S> type, @Nullable Predicate<? super S> additionalFilter) {

		List<S> result = new ArrayList<S>();

		for (MapWaySegment segment : node.getConnectedWaySegments()) {
			for (WaySegmentWorldObject representation : segment.getRepresentations()) {
				if (type.isInstance(representation)) {
					@SuppressWarnings("unchecked") //the isInstance check ensures that this cast works
					S sRepresentation = (S)representation;
					if (additionalFilter == null || additionalFilter.test(sRepresentation)) {
						result.add(sRepresentation);
					}
				}
			}
		}

		return result;

	}

}
