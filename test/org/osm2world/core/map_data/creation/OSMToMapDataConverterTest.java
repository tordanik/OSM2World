package org.osm2world.core.map_data.creation;

import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.osm.creation.OsmosisReader;
import org.osm2world.core.osm.data.OSMData;

public class OSMToMapDataConverterTest {

	/**
	 * test code for a group multipolygon test files which
	 * represent the same case with different multipolygon variants
	 */
	private void genericMultipolygonTest(String filename) throws IOException {
		
		File testFile = new File("test"+File.separator+"files"
				+File.separator+filename);
		OSMData osmData = new OsmosisReader(testFile).getData();
		
		MapProjection mapProjection = new HackMapProjection(osmData);
		MapData grid = new OSMToMapDataConverter(mapProjection).createMapData(osmData);
		
		assertSame(13, grid.getMapNodes().size());
		assertSame(0, grid.getMapWaySegments().size());
		assertSame(1, grid.getMapAreas().size());
		
		MapArea area = grid.getMapAreas().iterator().next();

		assertSame(2, area.getHoles().size());
		assertSame(6, area.getOuterPolygon().size());
		assertSame(13, area.getAreaSegments().size());
		
	}
	
	@Test
	public void testMultipolygon() throws IOException {
		genericMultipolygonTest("mp_two_holes.osm");
	}
	
	@Test
	public void testMultipolygonOuterTagged() throws IOException {
		genericMultipolygonTest("mp_two_holes_outer_tagged.osm");
	}
	
	@Ignore
	@Test
	public void testMultipolygonOuterAdvanced() throws IOException {
		genericMultipolygonTest("mp_two_holes_advanced.osm");
	}
	
}
