package org.osm2world.scene.texcoord;

import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;
import static org.osm2world.scene.texcoord.StripCoordinates.StripPosition;

import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class StripCoordinatesTest {

	@Test
	public void testPositionOf() {

		var l = List.of(new VectorXZ(-1, 0), new VectorXZ(-1, 4), new VectorXZ(-5, 4));
		var r = List.of(new VectorXZ(+1, 0), new VectorXZ(+1, 6), new VectorXZ(-5, 6));
		var strip = new StripCoordinates(l, r);

		assertEquals(8, strip.getLength(), 0.001);

		assertEquals(new StripPosition(0, 0, 2), strip.positionOf(l.get(0)));
		assertEquals(new StripPosition(4, 0, 2 * sqrt(2)), strip.positionOf(l.get(1)));
		assertEquals(new StripPosition(8, 0, 2), strip.positionOf(l.get(2)));

		assertEquals(new StripPosition(0, 1, 2), strip.positionOf(r.get(0)));
		assertEquals(new StripPosition(4, 1, 2 * sqrt(2)), strip.positionOf(r.get(1)));
		assertEquals(new StripPosition(8, 1, 2), strip.positionOf(r.get(2)));

		assertEquals(new StripPosition(0, 0.25, 2), strip.positionOf(new VectorXZ(-0.5, 0)));

		StripPosition p = strip.positionOf(new VectorXZ(-2.5, 5));
		assertEquals(6, p.distanceAlong(), 0.001);
		assertEquals(0.5, p.fractionAcross(), 0.001);

	}

}
