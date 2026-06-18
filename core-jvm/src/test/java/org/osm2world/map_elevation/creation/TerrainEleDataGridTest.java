package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;

public class TerrainEleDataGridTest {

	private static final TerrainEleDataGrid testData;

	static {

		LatLonEle[][] sites = new LatLonEle[101][51];

		for (int i = 0; i < 101; i++) {
			for (int j = 0; j < 51; j++) {
				sites[i][j] = new LatLonEle(0.01 * i, 42 + 0.01 * j, 9);
			}
		}

		testData = new TerrainEleDataGrid(sites);

	}

	@Test
	public void testSize() {

		assertFalse(testData.isEmpty());
		assertEquals(101 * 51, testData.sites().size());

	}

	@Test
	public void testFindSites() {

		LatLon pos1 = new LatLon(0.777, 42.424);

		assertTrue(testData.findClosestSites(pos1, 0).isEmpty());

		assertEquals(List.of(new LatLonEle(0.78, 42.42, 9)), testData.findClosestSites(pos1, 1));

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos1, 4);
		List<LatLonEle> surroundingSites = testData.findSurroundingSites(pos1);

		assertEquals(4, closest4Sites.size());
		assertEquals(List.of(new LatLonEle(0.77, 42.42, 9),
						new LatLonEle(0.77, 42.43, 9),
						new LatLonEle(0.78, 42.42, 9),
						new LatLonEle(0.78, 42.43, 9)),
				surroundingSites);
		assertEquals(new HashSet<>(closest4Sites), new HashSet<>(surroundingSites));

		List<LatLonEle> closest6Sites = testData.findClosestSites(pos1, 6);
		assertEquals(Set.of(new LatLonEle(0.77, 42.42, 9),
						new LatLonEle(0.77, 42.43, 9),
						new LatLonEle(0.78, 42.41, 9),
						new LatLonEle(0.78, 42.42, 9),
						new LatLonEle(0.78, 42.43, 9),
						new LatLonEle(0.79, 42.42, 9)),
				new HashSet<>(closest6Sites));

		List<LatLonEle> closest8Sites = testData.findClosestSites(pos1, 8);
		assertEquals(Set.of(new LatLonEle(0.77, 42.41, 9),
						new LatLonEle(0.77, 42.42, 9),
						new LatLonEle(0.77, 42.43, 9),
						new LatLonEle(0.78, 42.41, 9),
						new LatLonEle(0.78, 42.42, 9),
						new LatLonEle(0.78, 42.43, 9),
						new LatLonEle(0.79, 42.42, 9),
						new LatLonEle(0.79, 42.43, 9)),
				new HashSet<>(closest8Sites));

	}

	@Test
	public void testFindSites_pointNearCorner() {

		LatLon pos1 = new LatLon(0.003, 42.002);

		assertTrue(testData.findClosestSites(pos1, 0).isEmpty());

		assertEquals(List.of(new LatLonEle(0.00, 42.00, 9)), testData.findClosestSites(pos1, 1));

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos1, 4);
		List<LatLonEle> surroundingSites = testData.findSurroundingSites(pos1);

		assertEquals(4, closest4Sites.size());
		assertEquals(List.of(new LatLonEle(0.00, 42.00, 9),
						new LatLonEle(0.00, 42.01, 9),
						new LatLonEle(0.01, 42.00, 9),
						new LatLonEle(0.01, 42.01, 9)),
				surroundingSites);
		assertEquals(new HashSet<>(closest4Sites), new HashSet<>(surroundingSites));

		List<LatLonEle> closest6Sites = testData.findClosestSites(pos1, 6);
		assertEquals(Set.of(new LatLonEle(0.00, 42.00, 9),
						new LatLonEle(0.00, 42.01, 9),
						new LatLonEle(0.00, 42.02, 9),
						new LatLonEle(0.01, 42.00, 9),
						new LatLonEle(0.01, 42.01, 9),
						new LatLonEle(0.02, 42.00, 9)),
				new HashSet<>(closest6Sites));

		List<LatLonEle> closest8Sites = testData.findClosestSites(pos1, 8);
		assertEquals(Set.of(new LatLonEle(0.00, 42.00, 9),
						new LatLonEle(0.00, 42.01, 9),
						new LatLonEle(0.00, 42.02, 9),
						new LatLonEle(0.01, 42.00, 9),
						new LatLonEle(0.01, 42.01, 9),
						new LatLonEle(0.01, 42.02, 9),
						new LatLonEle(0.02, 42.00, 9),
						new LatLonEle(0.02, 42.01, 9)),
				new HashSet<>(closest8Sites));

	}

	@Test
	public void testFindSites_edgeCases() {

		var positions = List.of(
				new LatLon(0.0, 42.0),
				new LatLon(1.0, 42.5)
		);

		for (var pos : positions) {
			assertEquals(4, testData.findSurroundingSites(pos).size());
			assertEquals(99, testData.findClosestSites(pos, 99).size());
		}

	}

}
