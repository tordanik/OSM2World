package org.osm2world.util.color;

import static java.awt.Color.*;
import static java.util.Arrays.asList;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.awt.Color;

import org.junit.Test;

public class LColorTest {

	@Test
	public void testAwtConversion() {

		assertAlmostEquals(RED, new LColor(1, 0, 0).toAWT());

		for (Color testColor : asList(RED, GREEN, BLUE, BLACK, WHITE, YELLOW)) {
			assertAlmostEquals(testColor, LColor.fromAWT(testColor).toAWT());
		}

	}

}
