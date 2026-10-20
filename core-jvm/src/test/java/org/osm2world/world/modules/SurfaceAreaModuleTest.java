package org.osm2world.world.modules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.O2WConverter;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.osm.creation.OSMFileReader;
import org.osm2world.scene.Scene;
import org.osm2world.world.modules.SurfaceAreaModule.SurfaceArea;

public class SurfaceAreaModuleTest {

	/** checks that roads are subtracted from the empty terrain around them */
	@Test
	public void testRoadSubtractedFromEmptyTerrain() throws IOException {

		Scene results = new O2WConverter().convert(new OSMFileReader(getTestFile("overlapTest.osm")), null, null);

		List<SurfaceArea> terrainAreas = results.getMapData().getMapAreas().stream()
				.flatMap(a -> a.getRepresentations().stream())
				.filter(SurfaceArea.class::isInstance)
				.map(SurfaceArea.class::cast)
				.toList();

		assertFalse(terrainAreas.isEmpty());

		List<MapWaySegment> roadSegments = results.getMapData().getMapWaySegments().stream()
				.filter(s -> s.getTags().containsKey("highway"))
				.toList();

		assertFalse(roadSegments.isEmpty());

		for (SurfaceArea terrain : terrainAreas) {
			for (MapWaySegment segment : roadSegments) {
				assertTrue(terrain.getGroundFootprint().stream().noneMatch(p -> p.contains(segment.getCenter())));
			}
		}

	}

}
