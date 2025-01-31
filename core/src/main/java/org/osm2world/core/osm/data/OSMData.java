package org.osm2world.core.osm.data;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.math.geo.LatLon;
import org.osm2world.core.math.geo.LatLonBounds;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;

/**
 * OSM dataset containing nodes, areas and relations
 */
public class OSMData implements OsmEntityProvider {

	private final Collection<OsmBounds> bounds;
	private final InMemoryMapDataSet data;

	public OSMData(InMemoryMapDataSet data) {

		if (data.hasBounds()) {
			bounds = singletonList(data.getBounds());
		} else {
			bounds = emptyList();
		}

		this.data = data;

	}

	public OSMData(Collection<OsmBounds> bounds, Collection<? extends OsmNode> nodes,
			Collection<? extends OsmWay> ways, Collection<? extends OsmRelation> relations) {

		this.bounds = bounds;

		data = new InMemoryMapDataSet();
		for (OsmNode node : nodes) {
			data.getNodes().put(node.getId(), node);
		}
		for (OsmWay way : ways) {
			data.getWays().put(way.getId(), way);
		}
		for (OsmRelation relation : relations) {
			data.getRelations().put(relation.getId(), relation);
		}

	}

	public InMemoryMapDataSet getData() {
		return data;
	}

	public Collection<OsmNode> getNodes() {
		return data.getNodes().valueCollection();
	}

	@Override
	public OsmNode getNode(long id) throws EntityNotFoundException {
		return data.getNode(id);
	}

	public Collection<OsmWay> getWays() {
		return data.getWays().valueCollection();
	}

	@Override
	public OsmWay getWay(long id) throws EntityNotFoundException {
		return data.getWay(id);
	}

	public Collection<OsmRelation> getRelations() {
		return data.getRelations().valueCollection();
	}

	@Override
	public OsmRelation getRelation(long id) throws EntityNotFoundException {
		return data.getRelation(id);
	}

	public Collection<LatLonBounds> getExplicitBounds() {
		return bounds.stream()
				.map(b -> new LatLonBounds(b.getBottom(), b.getLeft(), b.getTop(), b.getRight()))
				.collect(toList());
	}

	public @Nullable LatLonBounds getUnionOfExplicitBounds() {
		if (getExplicitBounds().isEmpty()) {
			return null;
		} else {
			return LatLonBounds.union(getExplicitBounds());
		}
	}

	/** returns the center of the bounds or, if there are no explicit bounds, the center of all the nodes */
	public LatLonBounds getLatLonBounds() {
		if (getUnionOfExplicitBounds() != null) {
			return getUnionOfExplicitBounds();
		} else if (!getNodes().isEmpty()) {
			List<LatLon> nodeCoords = getNodes().stream()
				.map(n -> new LatLon(n.getLatitude(), n.getLongitude()))
				.collect(toList());
			return LatLonBounds.ofPoints(nodeCoords);
		} else {
			throw new IllegalArgumentException("OSM data must contain bounds or nodes");
		}
	}

	/** returns the center of {@link #getLatLonBounds()} */
	public LatLon getCenter() {
		return getLatLonBounds().getCenter();
	}

}
