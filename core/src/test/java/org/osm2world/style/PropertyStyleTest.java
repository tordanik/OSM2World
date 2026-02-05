package org.osm2world.style;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.scene.color.Color;

public class PropertyStyleTest {

	@Test
	public void testResolveMaterial() {

		var style = new PropertyStyle(new O2WConfig(Map.of(
				"material_ASPHALT_color", "#AAAAAA",
				"material_COPPER_ROOF_color", "#C3DBB9"
		)));

		assertEquals(Color.decode("#AAAAAA"),
				requireNonNull(style.resolveMaterial("asphalt")).color());
		assertEquals(Color.decode("#AAAAAA"),
				requireNonNull(style.resolveMaterial("Asphalt")).color());
		assertEquals(Color.decode("#C3DBB9"),
				requireNonNull(style.resolveMaterial("COPPER_ROOF")).color());

	}

}