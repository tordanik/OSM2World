package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;
import static org.osm2world.math.geo.LatLon.LonLat;
import static org.osm2world.math.geo.LatLonEle.LonLatEle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.osm2world.math.geo.LatLonEle;

public class TerrainEleDataGridGroupTest {

	private static final TerrainEleDataGridGroup testData;

	static {

		var grid0 = new LatLonEle[3][6];

		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 6; z++) {
				grid0[x][z] = LonLatEle(x, z, -7);
			}
		}

		var grid1 = new LatLonEle[2][5];

		for (int x = 0; x < 2; x++) {
			for (int z = 0; z < 5; z++) {
				grid1[x][z] = LonLatEle(3 + x, z * 1.25, -8);
			}
		}

		testData = new TerrainEleDataGridGroup(List.of(
			new TerrainEleDataGrid(grid0),
			new TerrainEleDataGrid(grid1)
		));

	}

	@Test
	public void testSize() {

		assertFalse(testData.isEmpty());
		assertEquals(28, testData.size());
		assertEquals(28, testData.sites().size());

	}

	@Test
	public void testGridAt() {

		assertNull(testData.gridAt(LonLat(3, -1)));

		TerrainEleDataGrid result0 = testData.gridAt(LonLat(1, 4));
		assertNotNull(result0);
		assertEquals(18, result0.size());

		TerrainEleDataGrid result1 = testData.gridAt(LonLat(3.5, 2));
		assertNotNull(result1);
		assertEquals(10, result1.size());

	}

	@Test
	public void testFindSites_grid0() {

		var pos = LonLat(1.4, 0.4);

		assertTrue(testData.findClosestSites(pos, 0).isEmpty());

		assertEquals(List.of(LonLatEle(1, 0, -7)), testData.findClosestSites(pos, 1));

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos, 4);
		assertEquals(Set.of(LonLatEle(1, 0, -7),
						LonLatEle(1, 1, -7),
						LonLatEle(2, 0, -7),
						LonLatEle(2, 1, -7)),
				new HashSet<>(closest4Sites));

		List<LatLonEle> closestSitesAll = testData.findClosestSites(pos, 28);
		assertEquals(new HashSet<>(testData.sites()), new HashSet<>(closestSitesAll));

		var posOutside = LonLat(-100, 0.5);

		assertEquals(Set.of(LonLatEle(0, 0, -7),
						LonLatEle(0, 1, -7)),
				new HashSet<>(testData.findClosestSites(posOutside, 2)));

	}

	@Test
	public void testFindSites_betweenGrids() {

		var pos = LonLat(2.3, 3.75);

		assertTrue(testData.findClosestSites(pos, 0).isEmpty());

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos, 3);
		assertEquals(Set.of(LonLatEle(2, 3, -7),
						LonLatEle(2, 4, -7),
						LonLatEle(3, 3.75, -8)),
				new HashSet<>(closest4Sites));

		List<LatLonEle> closestSitesAll = testData.findClosestSites(pos, 28);
		assertEquals(new HashSet<>(testData.sites()), new HashSet<>(closestSitesAll));

	}


}
