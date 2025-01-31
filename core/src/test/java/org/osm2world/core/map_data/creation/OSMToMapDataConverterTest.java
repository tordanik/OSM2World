package org.osm2world.core.map_data.creation;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.geo.LatLon;
import org.osm2world.core.math.geo.MapProjection;
import org.osm2world.core.math.geo.MetricMapProjection;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.osm.data.OSMData;

import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

public class OSMToMapDataConverterTest {

	/**
	 * loads {@link MapData} from a file in the test files directory
	 * @throws EntityNotFoundException
	 */
	private static MapData loadMapData(String filename) throws IOException, EntityNotFoundException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource(filename).getFile());

		OSMData osmData = new OSMFileReader(testFile).getAllData();
		MapProjection mapProjection = new MetricMapProjection(osmData.getCenter());

		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);
		return converter.createMapData(osmData, null);

	}

	/**
	 * test code for a group multipolygon test files which
	 * represent the same case with different multipolygon variants
	 * @throws EntityNotFoundException
	 */
	private void genericMultipolygonTest(String filename) throws IOException, EntityNotFoundException {

		MapData mapData = loadMapData(filename);

		assertEquals(13, mapData.getMapNodes().size());
		assertEquals(0, mapData.getMapWaySegments().size());
		assertEquals(1, mapData.getMapAreas().size());

		MapArea area = mapData.getMapAreas().iterator().next();

		assertEquals(2, area.getHoles().size());
		assertEquals(6, area.getOuterPolygon().size());
		assertEquals(13, area.getAreaSegments().size());

	}

	@Test
	public void testMultipolygon() throws IOException, EntityNotFoundException {
		genericMultipolygonTest("mp_two_holes.osm");
	}

	@Test
	public void testMultipolygonOuterTagged() throws IOException, EntityNotFoundException {
		genericMultipolygonTest("mp_two_holes_outer_tagged.osm");
	}

	@Test
	public void testMultipolygonAdvanced() throws IOException, EntityNotFoundException {
		genericMultipolygonTest("mp_two_holes_advanced.osm");
	}

	@Test
	public void testMultipolygonAdvanced2() throws IOException, EntityNotFoundException {
		genericMultipolygonTest("mp_two_holes_advanced2.osm");
	}

	@Ignore
	@Test
	public void testMultipolygonTouchingInners() throws IOException, EntityNotFoundException {
		genericMultipolygonTest("mp_two_holes_touching_inners.osm");
	}

	private void genericCoastlineTest(String filename, List<LatLon> landSites,
			List<LatLon> waterSites) throws IOException, EntityNotFoundException {

		/* create map data */

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource(filename).getFile());

		OSMData osmData = new OSMFileReader(testFile).getAllData();
		MapProjection mapProjection = new MetricMapProjection(osmData.getCenter());

		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);
		MapData mapData = converter.createMapData(osmData, null);

		/* check coastline properties */

		List<MapArea> waterAreas = new ArrayList<MapArea>();

		for (MapArea area : mapData.getMapAreas()) {
			if (area.getTags().contains("natural", "water")) {
				waterAreas.add(area);
			}
		}

		assertFalse(waterAreas.isEmpty());

		for (LatLon landSite : landSites) {

			VectorXZ v = mapProjection.toXZ(landSite);

			for (MapArea waterArea : waterAreas) {
				assertFalse(waterArea.getPolygon().contains(v));
			}

		}

		for (LatLon waterSite : waterSites) {

			VectorXZ v = mapProjection.toXZ(waterSite);

			boolean isWater = false;

			for (MapArea waterArea : waterAreas) {
				if (waterArea.getPolygon().contains(v)) {
					isWater = true;
				}
			}

			assertTrue(isWater);

		}

	}

	@Test
	public void	testCoastlineBigIsland() throws IOException, EntityNotFoundException {

		genericCoastlineTest("coastline_big_island.osm",
				asList(new LatLon(51.4946619, 2.1931507)),
				asList(new LatLon(51.4994015, 2.183386),
						new LatLon(51.4982682, 2.2522352),
						new LatLon(51.4590992, 2.2500837),
						new LatLon(51.4569336, 2.1838825)));

	}

	@Test
	public void	testCoastlineIslands() throws IOException, EntityNotFoundException {

		genericCoastlineTest("coastline_islands.osm",
				asList(new LatLon(51.4662933, 2.2364075),
						new LatLon(51.4780457, 2.2009898)),
				asList(new LatLon(51.4815502, 2.2271393),
						new LatLon(51.4596942, 2.1930457)));

	}

	@Test
	public void	testCoastlineIslandsAndCoast() throws IOException, EntityNotFoundException {

		genericCoastlineTest("coastline_islands_and_coast.osm",
				asList(new LatLon(51.4957716, 2.2466687),
						new LatLon(51.456188, 2.2522958),
						new LatLon(51.4662933, 2.2364075),
						new LatLon(51.4780457, 2.2009898)),
				asList(new LatLon(51.4815502, 2.2271393),
						new LatLon(51.4596942, 2.1930457)));

	}

	@Test
	public void	testCoastlineMultipleCoasts() throws IOException, EntityNotFoundException {

		genericCoastlineTest("coastline_multiple_coasts.osm",
				asList(new LatLon(51.4730977, 2.2165471)),
				asList(new LatLon(51.4654685, 2.2374005),
						new LatLon(51.4978323, 2.1844396),
						new LatLon(51.4555692, 2.2178711)));

	}

	/**
	 * reads two nodes with the same coordinates
	 * @throws EntityNotFoundException
	 */
	@Test
	public void testSameCoordNodes() throws IOException, EntityNotFoundException {

		MapData mapData = loadMapData("sameCoordNodes.osm");

		assertSame(2, mapData.getMapNodes().size());

		MapNode[] nodes = mapData.getMapNodes().toArray(new MapNode[2]);
		assertNotSame(nodes[0].getId(), nodes[1].getId());

	}

	/**
	 * reads a self intersecting polygon (can be filtered, but must not crash)
	 * @throws EntityNotFoundException
	 */
	@Test
	public void testSelfIntersection() throws IOException, EntityNotFoundException {

		loadMapData("self_intersection.osm");

	}

}
