package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;
import static org.osm2world.math.geo.LatLon.LonLat;
import static org.osm2world.math.geo.LatLonEle.LonLatEle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.osm2world.math.geo.LatLonEle;

public class TerrainEleDataGridTest {

	private static final TerrainEleDataGrid testData;

	static {

		var sites = new LatLonEle[51][101];

		for (int x = 0; x < 51; x++) {
			for (int z = 0; z < 101; z++) {
				sites[x][z] = LonLatEle(42 + 0.01 * x, 0.01 * z, 9);
			}
		}

		testData = new TerrainEleDataGrid(sites);

	}

	@Test
	public void testSize() {

		assertFalse(testData.isEmpty());
		assertEquals(51 * 101, testData.size());
		assertEquals(51 * 101, testData.sites().size());

	}

	@Test
	public void testBounds() {

		assertEquals(42.0, testData.bounds().minlon, 0);
		assertEquals(42.5, testData.bounds().maxlon, 0);
		assertEquals(0.0, testData.bounds().minlat, 0);
		assertEquals(1.0, testData.bounds().maxlat, 0);

	}

	@Test
	public void testFindSites() {

		var pos1 = LonLat(42.424, 0.777);

		assertTrue(testData.findClosestSites(pos1, 0).isEmpty());

		assertEquals(List.of(LonLatEle(42.42, 0.78, 9)), testData.findClosestSites(pos1, 1));

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos1, 4);
		List<LatLonEle> surroundingSites = testData.findSurroundingSites(pos1);

		assertEquals(4, closest4Sites.size());
		assertEquals(List.of(LonLatEle(42.42, 0.77, 9),
						LonLatEle(42.43, 0.77, 9),
						LonLatEle(42.42, 0.78, 9),
						LonLatEle(42.43, 0.78, 9)),
				surroundingSites);
		assertEquals(new HashSet<>(closest4Sites), new HashSet<>(surroundingSites));

		List<LatLonEle> closest6Sites = testData.findClosestSites(pos1, 6);
		assertEquals(Set.of(LonLatEle(42.42, 0.77, 9),
						LonLatEle(42.43, 0.77, 9),
						LonLatEle(42.41, 0.78, 9),
						LonLatEle(42.42, 0.78, 9),
						LonLatEle(42.43, 0.78, 9),
						LonLatEle(42.42, 0.79, 9)),
				new HashSet<>(closest6Sites));

		List<LatLonEle> closest8Sites = testData.findClosestSites(pos1, 8);
		assertEquals(Set.of(LonLatEle(42.41, 0.77, 9),
						LonLatEle(42.42, 0.77, 9),
						LonLatEle(42.43, 0.77, 9),
						LonLatEle(42.41, 0.78, 9),
						LonLatEle(42.42, 0.78, 9),
						LonLatEle(42.43, 0.78, 9),
						LonLatEle(42.42, 0.79, 9),
						LonLatEle(42.43, 0.79, 9)),
				new HashSet<>(closest8Sites));

		List<LatLonEle> closestSitesAll = testData.findClosestSites(pos1, 101 * 51);
		assertEquals(new HashSet<>(testData.sites()), new HashSet<>(closestSitesAll));

	}

	@Test
	public void testFindSites_pointNearCorner() {

		var pos1 = LonLat(42.002, 0.003);

		assertTrue(testData.findClosestSites(pos1, 0).isEmpty());

		assertEquals(List.of(LonLatEle(42.00, 0.00, 9)), testData.findClosestSites(pos1, 1));

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos1, 4);
		List<LatLonEle> surroundingSites = testData.findSurroundingSites(pos1);

		assertEquals(4, closest4Sites.size());
		assertEquals(List.of(LonLatEle(42.00, 0.00, 9),
						LonLatEle(42.01, 0.00, 9),
						LonLatEle(42.00, 0.01, 9),
						LonLatEle(42.01, 0.01, 9)),
				surroundingSites);
		assertEquals(new HashSet<>(closest4Sites), new HashSet<>(surroundingSites));

		List<LatLonEle> closest6Sites = testData.findClosestSites(pos1, 6);
		assertEquals(Set.of(LonLatEle(42.00, 0.00, 9),
						LonLatEle(42.01, 0.00, 9),
						LonLatEle(42.02, 0.00, 9),
						LonLatEle(42.00, 0.01, 9),
						LonLatEle(42.01, 0.01, 9),
						LonLatEle(42.00, 0.02, 9)),
				new HashSet<>(closest6Sites));

		List<LatLonEle> closest8Sites = testData.findClosestSites(pos1, 8);
		assertEquals(Set.of(LonLatEle(42.00, 0.00, 9),
						LonLatEle(42.01, 0.00, 9),
						LonLatEle(42.02, 0.00, 9),
						LonLatEle(42.00, 0.01, 9),
						LonLatEle(42.01, 0.01, 9),
						LonLatEle(42.02, 0.01, 9),
						LonLatEle(42.00, 0.02, 9),
						LonLatEle(42.01, 0.02, 9)),
				new HashSet<>(closest8Sites));

	}

	@Test
	public void testFindSites_edgeCases() {

		var positions = List.of(
				LonLat(42.0, 0.0),
				LonLat(42.5, 1.0)
		);

		for (var pos : positions) {
			assertEquals(4, testData.findSurroundingSites(pos).size());
			assertEquals(99, testData.findClosestSites(pos, 99).size());
		}

	}

}
