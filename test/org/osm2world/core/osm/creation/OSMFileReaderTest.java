package org.osm2world.core.osm.creation;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;
import static de.topobyte.osm4j.core.model.util.OsmModelUtil.nodesAsList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.core.osm.data.OSMData;

import com.slimjars.dist.gnu.trove.list.TLongList;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;


public class OSMFileReaderTest {
	
	@Test
	public void testValidFile() throws IOException, EntityNotFoundException {
		
		File testFile = new File("test"+File.separator+"files"
				+File.separator+"validFile.osm");
		
		OsmXmlIterator iterator = new OsmXmlIterator(testFile, false);
		
		InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
		OSMData osmData = new OSMData(data);
		
		assertSame(4, osmData.getData().getNodes().size());
		assertSame(1, osmData.getData().getWays().size());
		assertSame(1, osmData.getData().getRelations().size());
		
		OsmWay way = osmData.getData().getWays().valueCollection().iterator().next();
		assertSame(3, way.getNumberOfNodes());
		
		TLongList nodeIds = nodesAsList(way);
		OsmEntityProvider ep = osmData.getEntityProvider();
		
		OsmNode node1 = ep.getNode(nodeIds.get(1));
		assertEquals("traffic_signals", getTagsAsMap(node1).get("highway"));
		
		OsmRelation relation = osmData.getData().getRelations().valueCollection().iterator().next();		
		assertEquals("associatedStreet",  getTagsAsMap(relation).get("type"));
		
	}

}
