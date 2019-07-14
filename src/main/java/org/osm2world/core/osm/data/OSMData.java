package org.osm2world.core.osm.data;

import java.util.Collection;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

/**
 * OSM dataset containing nodes, areas and relations
 */
public class OSMData {

	private final Collection<Bound> bounds;
	private final Collection<OSMNode> nodes;
	private final Collection<OSMWay> ways;
	private final Collection<OSMRelation> relations;
		
	public OSMData(Collection<Bound> bounds, Collection<OSMNode> nodes,
			Collection<OSMWay> ways, Collection<OSMRelation> relations) {
		
		this.bounds = bounds;
		this.nodes = nodes;
		this.ways = ways;
		this.relations = relations;
		
	}

	public Collection<OSMNode> getNodes() {
		return nodes;
	}
	
	public Collection<OSMWay> getWays() {
		return ways;
	}
	
	public Collection<OSMRelation> getRelations() {
		return relations;
	}
		
	public Collection<Bound> getBounds() {
		return bounds;
	}
	
}
