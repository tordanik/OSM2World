package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;

public class TerrainEleDataGridGroupTest {

	private static final TerrainEleDataGridGroup testData;

	static {

		LatLonEle[][] grid0 = new LatLonEle[6][3];

		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				grid0[i][j] = new LatLonEle(i, j, -7);
			}
		}

		LatLonEle[][] grid1 = new LatLonEle[5][2];

		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 2; j++) {
				grid1[i][j] = new LatLonEle(i * 1.25, 3 + j, -8);
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

		assertNull(testData.gridAt(new LatLon(-1, 3)));

		TerrainEleDataGrid result0 = testData.gridAt(new LatLon(4, 1));
		assertNotNull(result0);
		assertEquals(18, result0.size());

		TerrainEleDataGrid result1 = testData.gridAt(new LatLon(2, 3.5));
		assertNotNull(result1);
		assertEquals(10, result1.size());

	}

	@Test
	public void testFindSites_grid0() {

		var pos = new LatLon(0.4, 1.4);

		assertTrue(testData.findClosestSites(pos, 0).isEmpty());

		assertEquals(List.of(new LatLonEle(0, 1, -7)), testData.findClosestSites(pos, 1));

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos, 4);
		assertEquals(Set.of(new LatLonEle(0, 1, -7),
						new LatLonEle(1, 1, -7),
						new LatLonEle(0, 2, -7),
						new LatLonEle(1, 2, -7)),
				new HashSet<>(closest4Sites));

		List<LatLonEle> closestSitesAll = testData.findClosestSites(pos, 28);
		assertEquals(new HashSet<>(testData.sites()), new HashSet<>(closestSitesAll));

		var posOutside = new LatLon(0.5, -100);

		assertEquals(Set.of(new LatLonEle(0, 0, -7),
						new LatLonEle(1, 0, -7)),
				new HashSet<>(testData.findClosestSites(posOutside, 2)));

	}

	@Test
	public void testFindSites_betweenGrids() {

		var pos = new LatLon(3.75, 2.3);

		assertTrue(testData.findClosestSites(pos, 0).isEmpty());

		List<LatLonEle> closest4Sites = testData.findClosestSites(pos, 3);
		assertEquals(Set.of(new LatLonEle(3, 2, -7),
						new LatLonEle(4, 2, -7),
						new LatLonEle(3.75, 3, -8)),
				new HashSet<>(closest4Sites));

		List<LatLonEle> closestSitesAll = testData.findClosestSites(pos, 28);
		assertEquals(new HashSet<>(testData.sites()), new HashSet<>(closestSitesAll));

	}

}
