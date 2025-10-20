package org.osm2world.util.color;

import static java.util.Arrays.asList;
import static org.osm2world.scene.color.Color.*;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.color.LColor;

public class LColorTest {

	@Test
	public void testAwtConversion() {

		assertAlmostEquals(RED, new LColor(1, 0, 0).toRGB());

		for (Color testColor : asList(RED, GREEN, BLUE, BLACK, WHITE, YELLOW)) {
			assertAlmostEquals(testColor, LColor.fromRGB(testColor).toRGB());
		}

	}

}
