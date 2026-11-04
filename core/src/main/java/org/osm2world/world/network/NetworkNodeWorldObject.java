package org.osm2world.world.network;

import static java.util.stream.Collectors.toSet;

import java.util.*;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.data.OutlineNodeWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.surfaces.SurfaceClaim;

/**
 * parent type for junctions and connectors between {@link NetworkWaySegmentWorldObject}s, see there for more info
 * @param <S>  the type of {@link NetworkWaySegmentWorldObject} connected by this network node
 */
public abstract class NetworkNodeWorldObject<S extends NetworkWaySegmentWorldObject> extends OutlineNodeWorldObject {

	private final Class<S> segmentType;

	protected NetworkNodeWorldObject(MapNode node, Class<S> segmentType) {
		super(node);
		this.segmentType = segmentType;
	}

	/** version of {@link #getConnectedNetworkSegments(Predicate)} without a filter */
	public List<S> getConnectedNetworkSegments() {
		return NetworkUtil.getConnectedNetworkSegments(node, segmentType, null);
	}

	/** @see NetworkUtil#getConnectedNetworkSegments(MapNode, Class, Predicate) */
	public List<S> getConnectedNetworkSegments(Predicate<? super S> filter) {
		return NetworkUtil.getConnectedNetworkSegments(node, segmentType, filter);
	}

	@Override
	public GroundState getGroundState() {

		Set<GroundState> connectedStates = getConnectedNetworkSegments()
			.stream()
			.map(WorldObject::getGroundState)
			.collect(toSet());

		if (connectedStates.size() == 1) {
			return connectedStates.iterator().next();
		} else {
			// transition between different states, must be ground level
			return GroundState.ON;
		}

	}

	/** cached result of {@link #getSurfaceClaims()} */
	private @Nullable List<SurfaceClaim> surfaceClaims = null;

	@Override
	public List<SurfaceClaim> getSurfaceClaims() {
		if (surfaceClaims == null) {
			surfaceClaims = SurfaceClaim.createDefaultClaims(this);
		}
		return surfaceClaims;
	}

	@Override
	public Collection<PolygonShapeXZ> getRawGroundFootprint() {
		if (getOutlinePolygonXZ() == null) {
			return List.of();
		} else {
			return List.of(getOutlinePolygonXZ());
		}
	}

	@Override
	protected List<String> getAttachmentTypes() {

		List<String> types = new ArrayList<>(super.getAttachmentTypes());

		for (S segment : getConnectedNetworkSegments()) {

			Set<String> segmentTypes = null;

			for (Connector connector : segment.getConnectors()) {
				List<String> segmentConnectorTypes = connector.attachmentProperties == null ? List.of()
						: connector.attachmentProperties.compatibleSurfaceTypes();
				if (connector.originalPos.distanceToXZ(node.getPos()) < 0.5) {
					types.addAll(segmentConnectorTypes);
				}
				if (segmentTypes == null) {
					segmentTypes = new HashSet<>(segmentConnectorTypes);
				} else {
					segmentTypes.retainAll(segmentConnectorTypes);
				}
			}

			if (segmentTypes != null) {
				types.addAll(segmentTypes);
			}

		}

		return types;

	}
}
