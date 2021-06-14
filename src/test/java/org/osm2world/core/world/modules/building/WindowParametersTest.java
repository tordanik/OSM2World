package org.osm2world.core.world.modules.building;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.world.modules.building.WindowParameters.WindowRegion.*;
import static org.osm2world.core.world.modules.building.WindowParameters.WindowShape.*;

import java.util.HashSet;

import org.junit.Test;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.util.enums.LeftRightBoth;


public class WindowParametersTest {

	@Test
	public void testWindowWithoutRegions() {

		TagSet tags = TagSet.of(
				"window:shape", "triangle",
				"window:width", "1.5 m",
				"window:height", "2 m",
				"window:breast", "0.5 m",
				"window:shutter", "both");

		WindowParameters result = new WindowParameters(tags, 3);

		assertEquals(0.5, result.breast, 0);
		assertEquals(LeftRightBoth.BOTH, result.shutterSide);

		assertEquals(TRIANGLE, result.overallProperties.shape);
		assertEquals(1.5, result.overallProperties.width, 0);
		assertEquals(2.0, result.overallProperties.height, 0);

		assertEquals(new HashSet<>(asList(CENTER)), result.regionProperties.keySet());

		assertTrue(result.regionProperties.containsKey(CENTER));
		assertEquals(1.5, result.regionProperties.get(CENTER).width, 0);
		assertEquals(2.0, result.regionProperties.get(CENTER).height, 0);

	}

	@Test
	public void testWindowCenterTop() {

		TagSet tags = TagSet.of(
				"window:center:shape", "rectangle",
				"window:top:shape", "semicircle",
				"window:height", "3 m",
				"window:width", "1 m");

		WindowParameters result = new WindowParameters(tags, 5);

		assertEquals(1.0, result.overallProperties.width, 0);
		assertEquals(3.0, result.overallProperties.height, 0);

		assertEquals(new HashSet<>(asList(CENTER, TOP)), result.regionProperties.keySet());

		assertEquals(RECTANGLE, result.regionProperties.get(CENTER).shape);
		assertEquals(1.0, result.regionProperties.get(CENTER).width, 0);
		assertEquals(2.0, result.regionProperties.get(CENTER).height, 0);

		assertEquals(SEMICIRCLE, result.regionProperties.get(TOP).shape);
		assertEquals(1.0, result.regionProperties.get(TOP).width, 0);
		assertEquals(1.0, result.regionProperties.get(TOP).height, 0);

	}

	@Test
	public void testWindowCenterTopExplicitHeight() {

		TagSet tags = TagSet.of(
				"window:top:shape", "triangle",
				"window:height", "1.5 m",
				"window:top:height", "0.5 m");

		WindowParameters result = new WindowParameters(tags, 5);

		assertEquals(1.5, result.overallProperties.height, 0);

		assertEquals(new HashSet<>(asList(CENTER, TOP)), result.regionProperties.keySet());

		assertEquals(RECTANGLE, result.regionProperties.get(CENTER).shape);
		assertEquals(1.0, result.regionProperties.get(CENTER).height, 0);

		assertEquals(TRIANGLE, result.regionProperties.get(TOP).shape);
		assertEquals(0.5, result.regionProperties.get(TOP).height, 0);

	}

	@Test
	public void testWindowAllRegionsExplicitWidth() {

		TagSet tags = TagSet.of(
				"window:center:shape", "rectangle",
				"window:top:shape", "semicircle",
				"window:bottom:shape", "semicircle",
				"window:left:shape", "semicircle",
				"window:right:shape", "semicircle",
				"window:height", "2 m",
				"window:left:width", "0.5 m",
				"window:right:width", "0.5 m",
				"window:center:width", "1.0 m");

		WindowParameters result = new WindowParameters(tags, 5);

		assertEquals(2.0, result.overallProperties.width, 0);
		assertEquals(2.0, result.overallProperties.height, 0);

		assertEquals(new HashSet<>(asList(CENTER, TOP, BOTTOM, LEFT, RIGHT)), result.regionProperties.keySet());

		assertEquals(RECTANGLE, result.regionProperties.get(CENTER).shape);
		assertEquals(1.0, result.regionProperties.get(CENTER).width, 0);
		assertEquals(1.0, result.regionProperties.get(CENTER).height, 0);

		assertEquals(SEMICIRCLE, result.regionProperties.get(LEFT).shape);
		assertEquals(0.5, result.regionProperties.get(LEFT).width, 0);
		assertEquals(1.0, result.regionProperties.get(LEFT).height, 0);

		assertEquals(SEMICIRCLE, result.regionProperties.get(RIGHT).shape);
		assertEquals(0.5, result.regionProperties.get(RIGHT).width, 0);
		assertEquals(1.0, result.regionProperties.get(RIGHT).height, 0);

		assertEquals(SEMICIRCLE, result.regionProperties.get(TOP).shape);
		assertEquals(1.0, result.regionProperties.get(TOP).width, 0);
		assertEquals(0.5, result.regionProperties.get(TOP).height, 0);

		assertEquals(SEMICIRCLE, result.regionProperties.get(BOTTOM).shape);
		assertEquals(1.0, result.regionProperties.get(BOTTOM).width, 0);
		assertEquals(0.5, result.regionProperties.get(BOTTOM).height, 0);

	}

	@Test
	public void testInvalidPanes() {

		TagSet tags = TagSet.of(
				"window:shape", "rectangle",
				"window:panes", "0x0");

		WindowParameters result = new WindowParameters(tags, 2.5);

		assertEquals(RECTANGLE, result.overallProperties.shape);
		assertTrue(result.overallProperties.panes.panesHorizontal > 0);
		assertTrue(result.overallProperties.panes.panesVertical > 0);

	}

}
