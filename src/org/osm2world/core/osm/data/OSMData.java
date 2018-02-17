package org.osm2world.core.osm.data;

import java.util.ArrayList;
import java.util.Collection;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;

/**
 * OSM dataset containing nodes, areas and relations
 */
public class OSMData 
{

	private final Collection<OsmBounds> bounds;
	private InMemoryMapDataSet data;

	public OSMData(InMemoryMapDataSet data)
	{
		bounds = new ArrayList<OsmBounds>();
		if (data.hasBounds()) {
			bounds.add(data.getBounds());
		}
		this.data = data;
	}
	
	public OSMData(Collection<OsmBounds> bounds, Collection<? extends OsmNode> nodes,
			Collection<? extends OsmWay> ways, Collection<? extends OsmRelation> relations)
	{
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
	
	public Collection<OsmBounds> getBounds()
	{
		return bounds;
	}

	public InMemoryMapDataSet getData()
	{
		return data;
	}

	public OsmEntityProvider getEntityProvider()
	{
		return data;
	}

}
