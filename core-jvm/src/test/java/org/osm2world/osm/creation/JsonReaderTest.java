package org.osm2world.osm.creation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.osm.data.OSMData;
import org.osm2world.util.json.JsonImplementationJvm;
import org.osm2world.util.json.JsonUtil;

import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;

public class JsonReaderTest {

	static {
		JsonUtil.setImplementation(new JsonImplementationJvm());
	}

	private static final String TEST_JSON = """
			{
				<<headers>>
				"elements": [
					{
						"type": "node",
						"id": 1,
						"lat": 42.0,
						"lon": -3.45,
						"tags": {
							"natural": "tree",
							"height": "10 m"
						}
					}, {
						"type": "way",
						"id": 444,
						"nodes": [10, 11, 12],
						"tags": {
							"highway": "tertiary",
							"surface": "asphalt",
							"name": "Main Street"
						}
					}, {
						"type": "relation",
						"id": 999,
						"members": [
							{
								"type": "way",
								"ref": 444,
								"role": "street"
							}
						],
						"tags": {
							"type": "street",
							"name": "Main Street"
						}
					}
				]
			}
			""";

	private static final List<String> HEADERS = List.of(
			"\"version\": 0.6,\n\"generator\": \"Overpass API\",",
			"\"version\": \"0.6\",\n\"generator\": \"CGImap 0.8.1 (6263 thorn-01.openstreetmap.org)\","
	);

	@Test
	public void testGetAllData() throws IOException {

		for (String headers : HEADERS) {

			var reader = new JsonStringReader(TEST_JSON.replaceFirst("<<headers>>", headers));

			OSMData result = reader.getAllData();

			assertEquals(1, result.getNodes().size());

			OsmNode node = result.getNodes().iterator().next();
			assertEquals(1L, node.getId());
			assertEquals(42.0, node.getLatitude(), 0.0001);
			assertEquals(-3.45, node.getLongitude(), 0.0001);
			assertEquals(2, node.getNumberOfTags());

			OsmWay way = result.getWays().iterator().next();
			assertEquals(444L, way.getId());
			assertEquals(3, way.getNumberOfTags());
			assertEquals(3, way.getNumberOfNodes());

			OsmRelation relation = result.getRelations().iterator().next();
			assertEquals(999L, relation.getId());
			assertEquals(2, relation.getNumberOfTags());
			assertEquals(1, relation.getNumberOfMembers());
			assertEquals(EntityType.Way, relation.getMember(0).getType());
			assertEquals(444L, relation.getMember(0).getId());
			assertEquals("street", relation.getMember(0).getRole());

		}

	}

}