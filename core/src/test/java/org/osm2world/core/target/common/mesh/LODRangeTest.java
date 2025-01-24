package org.osm2world.core.target.common.mesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.*;

import org.junit.Test;

public class LODRangeTest {

	@Test
	public void testIntersection() {

		assertEquals(new LODRange(LOD1, LOD3), LODRange.intersection(new LODRange(LOD1, LOD3)));
		assertEquals(new LODRange(LOD1, LOD3), LODRange.intersection(new LODRange(LOD0, LOD3), new LODRange(LOD1, LOD4)));
		assertEquals(new LODRange(LOD2), LODRange.intersection(new LODRange(LOD1, LOD2), new LODRange(LOD2, LOD4)));
		assertEquals(new LODRange(LOD2), LODRange.intersection(new LODRange(LOD1, LOD2), new LODRange(LOD1, LOD3), new LODRange(LOD2, LOD4)));
		assertNull(LODRange.intersection(new LODRange(LOD0, LOD1), new LODRange(LOD2, LOD3)));

	}

}