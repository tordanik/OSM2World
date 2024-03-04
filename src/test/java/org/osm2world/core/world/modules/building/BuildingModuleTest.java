package org.osm2world.core.world.modules.building;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.data.MapArea;

public class BuildingModuleTest {

	@Test
	public void testRoofWithoutWalls() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource("issue-203.osm").getFile());

		ConversionFacade facade = new ConversionFacade();
		Results results = facade.createRepresentations(testFile, null, null, null, null);

		Collection<MapArea> areas = results.getMapData().getMapAreas();
		assertEquals(5, areas.size());

		MapArea buildingArea = areas.stream().filter(a -> a.getId() == 103224).findFirst().get();
		assertTrue(buildingArea.getPrimaryRepresentation() instanceof Building);
		Building building = (Building) buildingArea.getPrimaryRepresentation();
		assertEquals(4, building.getParts().size());

		for (BuildingPart part : building.getParts()) {
			part.buildMeshes();
		}

	}

}
