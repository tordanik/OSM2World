package org.osm2world.core.osm.data;

import static java.util.Collections.*;

import java.util.Collection;

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

	public Collection<OsmBounds> getBounds() {
		return bounds;
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

}
