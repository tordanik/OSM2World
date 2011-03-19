package org.osm2world.core.osm.data;

import java.util.Collection;

/**
 * OSM dataset containing nodes, areas and relations
 */
public class OSMData {

	private final Collection<OSMNode> nodes;
	private final Collection<OSMWay> ways;
	private final Collection<OSMRelation> relations;
		
	public OSMData(Collection<OSMNode> nodes, Collection<OSMWay> ways,
			Collection<OSMRelation> relations) {
		
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
	
}
