package org.osm2world.world.modules.building.roof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.osm2world.O2WConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.Scene;
import org.osm2world.world.modules.building.BuildingPart;

public class RoofTest {

	/** tests that all known roof shapes can be constructed without exceptions or obvious geometry problems */
	@Test
	public void testRoofConstruction() {

		for (String roofShape : List.of("flat", "pyramidal", "onion", "skillion", "saltbox", "gabled",
				"hipped", "side_hipped", "half-hipped", "side_half-hipped", "gambrel", "mansard", "sawtooth",
				"dome", "round", "cone")) {
			for (String roofDirection : List.of("N", "E", "S", "W", "45", "135")) {

				var builder = new MapDataBuilder();

				var nodes = List.of(
						builder.createNode(-5, 0),
						builder.createNode(5, 0),
						builder.createNode(5, 15),
						builder.createNode(-5, 15)
				);

				TagSet tags = TagSet.of(
						"building", "yes",
						"roof:shape", roofShape,
						"roof:direction", roofDirection
				);

				builder.createWayArea(nodes, tags);

				try {

					var o2w = new O2WConverter();
					Scene result = o2w.convert(builder.build(), null);

					BuildingPart buildingPart = result.getWorldObjects(BuildingPart.class).iterator().next();
					Roof roof = buildingPart.getRoof();
					Assert.assertNotNull(roof);

					List<TriangleXYZ> roofTriangles = new ArrayList<>();
					result.getMeshes().stream()
							.filter(m -> m.material == roof.material)
							.forEach(m -> roofTriangles.addAll(m.geometry.asTriangles().triangles));

					assertFalse(roofTriangles.isEmpty());

					if (roof instanceof HeightfieldRoof) {
						double totalAreaXZ = roofTriangles.stream().mapToDouble(t -> t.xz().getArea()).sum();
						assertEquals(150.0, totalAreaXZ, 0.1);
					}

				} catch (Exception e) {
					Assert.fail("Failed for " + tags + ": " + e.getMessage());
				}

			}
		}

	}

}
