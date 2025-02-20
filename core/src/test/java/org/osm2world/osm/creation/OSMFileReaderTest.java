package org.osm2world.osm.creation;

import static de.topobyte.osm4j.core.model.util.OsmModelUtil.getTagsAsMap;
import static de.topobyte.osm4j.core.model.util.OsmModelUtil.nodesAsList;
import static org.junit.Assert.*;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.osm.data.OSMData;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;


public class OSMFileReaderTest {

	@Test
	public void testValidFile() throws IOException, EntityNotFoundException {

		File testFile = getTestFile("validFile.osm");
		OSMData osmData = new OSMFileReader(testFile).getAllData();

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

		File testFile = getTestFile("josmTest01.osm");
		OSMData osmData = new OSMFileReader(testFile).getAllData();

		assertSame(5, osmData.getNodes().size());
		assertSame(1, osmData.getWays().size());
		assertSame(0, osmData.getRelations().size());

	}

	@Test
	public void testJosmFileWithEmoji() throws IOException, EntityNotFoundException {

		File testFile = getTestFile("josm_emoji.osm");
		OSMData osmData = new OSMFileReader(testFile).getAllData();

		assertSame(1, osmData.getNodes().size());
		assertNotNull(osmData.getNode(123123123123L));

	}

}
