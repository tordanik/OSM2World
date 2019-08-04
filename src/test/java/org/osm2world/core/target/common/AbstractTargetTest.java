package org.osm2world.core.target.common;

import static com.google.common.collect.Lists.reverse;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.junit.Assert.*;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.material.Materials.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;

public class AbstractTargetTest {

	/**
	 * a test implementation of {@link Target}.
	 * It collects all triangles "drawn" with it in a list.
	 */
	private static class TestTarget extends AbstractTarget<RenderableToAllTargets> {

		private final List<TriangleXYZ> drawnTriangles = new ArrayList<TriangleXYZ>();

		public List<TriangleXYZ> getDrawnTriangles() {
			return drawnTriangles;
		}

		@Override
		public Class<RenderableToAllTargets> getRenderableType() {
			return RenderableToAllTargets.class;
		}

		@Override
		public void render(RenderableToAllTargets renderable) {
			renderable.renderTo(this);
		}

		@Override
		public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles,
				List<List<VectorXZ>> texCoordLists) {
			drawnTriangles.addAll(triangles);
		}

		@Override
		public void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles,
				List<List<VectorXZ>> texCoordLists) {
			drawnTriangles.addAll(triangles);
		}

	}

	@Test
	public void testDrawShape() {

		SimpleClosedShapeXZ shape = new SimplePolygonXZ(asList(
				new VectorXZ(-1, -1),
				new VectorXZ( 1, -1),
				new VectorXZ( 2,  1),
				new VectorXZ( 1,  1),
				new VectorXZ(-1, -1)));

		TestTarget target = new TestTarget();

		target.drawShape(PLASTIC, shape, new VectorXYZ(0, 5, 0), X_UNIT, Y_UNIT, 1.0);

		List<TriangleXYZ> result = target.getDrawnTriangles();

		assertContainsQuad(result, new VectorXYZ(0, 4, -1), new VectorXYZ(0, 4, 1),
				new VectorXYZ(0, 6, 2), new VectorXYZ(0, 6, 1));

	}

	/**
	 * tests extruding a line segment
	 */
	@Test
	public void testDrawExtrudedShape0() {

		ShapeXZ shape = new LineSegmentXZ(new VectorXZ(2, 0), new VectorXZ(-1, 0));

		List<VectorXYZ> path = asList(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0),
				new VectorXYZ(0, 1, 1));

		TestTarget target = new TestTarget();

		target.drawExtrudedShape(PLASTIC, shape, path, asList(Z_UNIT.invert(), Y_UNIT, Y_UNIT), null, null, null);

		List<TriangleXYZ> result = target.getDrawnTriangles();

		assertEquals(4, result.size());

		assertContainsQuad(result, new VectorXYZ(-2, 0, 0), new VectorXYZ(1, 0, 0),
				new VectorXYZ(1, 1, 0), new VectorXYZ(-2, 1, 0));
		assertContainsQuad(result, new VectorXYZ(-2, 1, 0), new VectorXYZ(1, 1, 0),
				new VectorXYZ(1, 1, 1), new VectorXYZ(-2, 1, 1));

	}

	/**
	 * tests extruding a triangle into a pyramid
	 */
	@Test
	public void testDrawExtrudedShape1() {

		ShapeXZ shape = new TriangleXZ(new VectorXZ(-1, 0), new VectorXZ(1, 0), new VectorXZ(0, 2));

		List<VectorXYZ> path = asList(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0));

		List<List<TriangleXYZ>> results = new ArrayList<List<TriangleXYZ>>();

		{ /* path from bottom to top */

			TestTarget target = new TestTarget();

			target.drawExtrudedShape(PLASTIC, shape, path, nCopies(2, Z_UNIT),
					asList(1.0, 0.0), null, EnumSet.of(ExtrudeOption.START_CAP));

			results.add(target.getDrawnTriangles());

		} { /* path from top to bottom (should produce same result for symmetric shape) */

			TestTarget target = new TestTarget();

			target.drawExtrudedShape(PLASTIC, shape, reverse(path), nCopies(2, Z_UNIT),
					asList(0.0, 1.0), null, EnumSet.of(ExtrudeOption.END_CAP));

			results.add(target.getDrawnTriangles());

		}

		/* check results */

		for (List<TriangleXYZ> result : results) {

			//TODO change to 4 once extrudeShape no longer necessarily uses strips
			assertEquals(7, result.size());

			assertTrue(containsTriangle(result,
					new VectorXYZ(-1, 0, 0),
					new VectorXYZ(1, 0, 0),
					new VectorXYZ(0, 1, 0)));
			assertTrue(containsTriangle(result,
					new VectorXYZ(1, 0, 0),
					new VectorXYZ(0, 0, 2),
					new VectorXYZ(0, 1, 0)));
			assertTrue(containsTriangle(result,
					new VectorXYZ(0, 0, 2),
					new VectorXYZ(-1, 0, 0),
					new VectorXYZ(0, 1, 0)));

			assertTrue(containsTriangle(result,
					new VectorXYZ(1, 0, 0),
					new VectorXYZ(-1, 0, 0),
					new VectorXYZ(0, 0, 2)));

		}

	}

	/**
	 * tests extruding a rectangle into a (slanted) wall with caps at both ends
	 */
	@Test
	public void testDrawExtrudedShape2() {

		ShapeXZ shape = new SimplePolygonXZ(asList(NULL_VECTOR, new VectorXZ(1, 0),
				new VectorXZ(1, 2), new VectorXZ(0, 1.5), NULL_VECTOR));

		List<VectorXYZ> path = asList(
				new VectorXYZ(5, 0, 0),
				new VectorXYZ(5, 0, -1));

		TestTarget target = new TestTarget();

		target.drawExtrudedShape(BRICK, shape, path, nCopies(2, Y_UNIT),
				null, null, EnumSet.of(ExtrudeOption.START_CAP, ExtrudeOption.END_CAP));

		List<TriangleXYZ> result = target.getDrawnTriangles();

		/* check results */

		assertEquals(12, result.size());

		//top
		assertContainsQuad(result, new VectorXYZ(5, 1.5, -1),new VectorXYZ(6, 2, -1),
				new VectorXYZ(6, 2, 0), new VectorXYZ(5, 1.5, 0));

		//bottom
		assertContainsQuad(result, new VectorXYZ(6, 0, -1),new VectorXYZ(5, 0, -1),
				new VectorXYZ(5, 0, 0), new VectorXYZ(6, 0, 0));

		//sides
		assertContainsQuad(result, new VectorXYZ(5, 0, 0),new VectorXYZ(5, 0, -1),
				new VectorXYZ(5, 1.5, -1), new VectorXYZ(5, 1.5, 0));
		assertContainsQuad(result, new VectorXYZ(6, 0, -1),new VectorXYZ(6, 0, 0),
				new VectorXYZ(6, 2, 0), new VectorXYZ(6, 2, -1));

		//end and start cap
		assertContainsQuad(result, new VectorXYZ(5, 0, -1),new VectorXYZ(6, 0, -1),
				new VectorXYZ(6, 2, -1), new VectorXYZ(5, 1.5, -1));
		assertContainsQuad(result, new VectorXYZ(6, 0, 0),new VectorXYZ(5, 0, 0),
				new VectorXYZ(5, 1.5, 0), new VectorXYZ(6, 2, 0));

	}

	/**
	 * asserts that the collection contains two triangles which together form the quad.
	 *
	 * @throws AssertionError  if the condition is not fulfilled
	 */
	private static final void assertContainsQuad(List<TriangleXYZ> collection,
			VectorXYZ a, VectorXYZ b, VectorXYZ c, VectorXYZ d) {

		assertTrue(containsTriangle(collection, a, b, c) && containsTriangle(collection, a, c, d)
				|| containsTriangle(collection, a, b, d) && containsTriangle(collection, b, c, d));

	}

	/**
	 * returns true iff the collection contains the triangle defined by the vertices.
	 * The winding is checked, but otherwise the order of vertices does not matter.
	 */
	private static final boolean containsTriangle(List<TriangleXYZ> collection,
			VectorXYZ v1, VectorXYZ v2, VectorXYZ v3) {

		for (TriangleXYZ t : collection) {

			if ((v1.equals(t.v1) && v2.equals(t.v2) && v3.equals(t.v3))
					|| v2.equals(t.v1) && v3.equals(t.v2) && v1.equals(t.v3)
					|| v3.equals(t.v1) && v1.equals(t.v2) && v2.equals(t.v3)) {
				return true;
			}

		}

		return false;

	}

}
