package org.osm2world.core.osm.creation;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import org.junit.Test;
import org.osm2world.core.osm.data.OSMData;

import java.io.File;
import java.io.IOException;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;
import static de.topobyte.osm4j.core.model.util.OsmModelUtil.nodesAsList;
import static org.junit.Assert.*;


public class OSMFileReaderTest {

	@Test
	public void testValidFile() throws IOException, EntityNotFoundException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource("validFile.osm").getFile());
		OSMData osmData = new OSMFileReader(testFile).getData();

		assertSame(4, osmData.getNodes().size());
		assertSame(1, osmData.getWays().size());
		assertSame(1, osmData.getRelations().size());

		OsmWay way = osmData.getWays().iterator().next();
		assertSame(3, way.getNumberOfNodes());

		OsmNode node1 = osmData.getNode(nodesAsList(way).get(1));
		assertEquals("traffic_signals", getTagsAsMap(node1).get("highway"));

		OsmRelation relation = osmData.getRelations().iterator().next();
		assertEquals("associatedStreet",  getTagsAsMap(relation).get("type"));

	}

	/** read a JOSM file with new, modified, and deleted elements and multiple bounds */
	@Test
	public void testJosmFileWithEdits() throws IOException, EntityNotFoundException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource("josmTest01.osm").getFile());
		OSMData osmData = new OSMFileReader(testFile).getData();

		assertSame(5, osmData.getNodes().size());
		assertSame(1, osmData.getWays().size());
		assertSame(0, osmData.getRelations().size());

	}

	@Test
	public void testJosmFileWithEmoji() throws IOException, EntityNotFoundException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource("josm_emoji.osm").getFile());
		OSMData osmData = new OSMFileReader(testFile).getData();

		assertSame(1, osmData.getNodes().size());
		assertNotNull(osmData.getNode(123123123123L));

	}

}
