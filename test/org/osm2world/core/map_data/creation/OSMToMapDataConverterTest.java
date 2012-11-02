package org.osm2world.core.map_data.creation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.osm.creation.OsmosisReader;
import org.osm2world.core.osm.data.OSMData;

public class OSMToMapDataConverterTest {

	/**
	 * loads {@link MapData} from a file in the test files directory
	 */
	private static MapData loadMapData(String filename) throws IOException {
		
		File testFile = new File("test"+File.separator+"files"
				+File.separator+filename);
		
		OSMData osmData = new OsmosisReader(testFile).getData();
		MapProjection mapProjection = new HackMapProjection(osmData);
		
		return new OSMToMapDataConverter(mapProjection).createMapData(osmData);
				
	}
	
	/**
	 * test code for a group multipolygon test files which
	 * represent the same case with different multipolygon variants
	 */
	private void genericMultipolygonTest(String filename) throws IOException {
		
		MapData mapData = loadMapData(filename);
		
		assertSame(13, mapData.getMapNodes().size());
		assertSame(0, mapData.getMapWaySegments().size());
		assertSame(1, mapData.getMapAreas().size());
		
		MapArea area = mapData.getMapAreas().iterator().next();

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
	
	@Test
	public void testMultipolygonAdvanced() throws IOException {
		genericMultipolygonTest("mp_two_holes_advanced.osm");
	}
	
	/**
	 * reads two nodes with the same coordinates
	 */
	@Test
	public void testSameCoordNodes() throws IOException {
		
		MapData mapData = loadMapData("sameCoordNodes.osm");
		
		assertSame(2, mapData.getMapNodes().size());
		
		MapNode[] nodes = mapData.getMapNodes().toArray(new MapNode[2]);
		assertNotSame(nodes[0].getOsmNode().id, nodes[1].getOsmNode().id);
		
	}
	
	/**
	 * reads a self intersecting polygon (can be filtered, but must not crash)
	 */
	@Test
	public void testSelfIntersection() throws IOException {
		
		MapData mapData = loadMapData("self_intersection.osm");
		
	}
	
}
