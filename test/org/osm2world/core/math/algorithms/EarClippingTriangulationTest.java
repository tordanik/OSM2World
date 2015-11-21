package org.osm2world.core.math.algorithms;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;


public class EarClippingTriangulationTest {

	private static final VectorXZ outlineA0 = new VectorXZ(-1.1f, -1.1f);
	private static final VectorXZ outlineA1 = new VectorXZ(-1.1f, 1.1f);
	private static final VectorXZ outlineA2 = new VectorXZ(1.1f, 1.1f);
	private static final VectorXZ outlineA3 = new VectorXZ(1.1f, -1.1f);
	private static final VectorXZ outlineA4 = new VectorXZ(0f, 1f);
		
	private static final List<VectorXZ> outlineA = Arrays.asList(
			outlineA0, outlineA1, outlineA2, outlineA3, outlineA0
	);
	
	private static final List<VectorXZ> outlineB = Arrays.asList(
			outlineA0, outlineA1, outlineA2, outlineA3, outlineA4, outlineA0
	);
	
	private static final SimplePolygonXZ holeA = new SimplePolygonXZ(Arrays.asList(
			new VectorXZ(0, 0f),
			new VectorXZ(1f, 0f),
			new VectorXZ(0f, 1f),
			new VectorXZ(0, 0f)
	));
	
	private static final SimplePolygonXZ holeB = new SimplePolygonXZ(Arrays.asList(
			new VectorXZ(0.6f, 0.6f),
			new VectorXZ(0.6f, 0f),
			new VectorXZ(0f, 0.6f),
			new VectorXZ(0.6f, 0.6f)
	));
		
	@Test
	public void testFindVisibleOutlineVertex1() {
		
		assertNotNull(EarClippingTriangulationUtil.findVisibleOutlineVertex(
						outlineA, new VectorXZ(0.55f, 0.55f),
						Collections.<SimplePolygonXZ>emptyList()));
				
	}

	@Test
	public void testFindVisibleOutlineVertex2() {
		
		assertEquals(outlineA2, outlineA.get(
				EarClippingTriangulationUtil.findVisibleOutlineVertex(
						outlineA, new VectorXZ(0.55f, 0.55f),
						Arrays.asList(holeA))));

	}

	@Test
	public void testFindVisibleOutlineVertex3() {
		
		assertNull(
				EarClippingTriangulationUtil.findVisibleOutlineVertex(
						outlineA, new VectorXZ(0.55f, 0.55f),
						Arrays.asList(holeA, holeB)));
				
	}
	
	/** rearrange, no invert */
	@Test
	public void testRearrangeOutline1() {
		
		List<VectorXZ> newOutline =
			EarClippingTriangulationUtil.rearrangeOutline(outlineA, 3, false);
		
		assertSame(5, newOutline.size());
		assertEquals(outlineA3, newOutline.get(0));
		assertEquals(outlineA0, newOutline.get(1));
		assertEquals(outlineA1, newOutline.get(2));
		assertEquals(outlineA2, newOutline.get(3));
		assertEquals(outlineA3, newOutline.get(4));
		
	}
	
	/** rearrange, invert */
	@Test
	public void testRearrangeOutline2() {
		
		List<VectorXZ> newOutline =
			EarClippingTriangulationUtil.rearrangeOutline(outlineA, 3, true);
		
		assertSame(5, newOutline.size());
		assertEquals(outlineA3, newOutline.get(0));
		assertEquals(outlineA2, newOutline.get(1));
		assertEquals(outlineA1, newOutline.get(2));
		assertEquals(outlineA0, newOutline.get(3));
		assertEquals(outlineA3, newOutline.get(4));
		
	}
	
	/** no invert, same start */
	@Test
	public void testRearrangeOutline3() {
		
		List<VectorXZ> newOutline =
			EarClippingTriangulationUtil.rearrangeOutline(outlineA, 0, false);
		
		assertSame(5, newOutline.size());
		assertEquals(outlineA0, newOutline.get(0));
		assertEquals(outlineA1, newOutline.get(1));
		assertEquals(outlineA2, newOutline.get(2));
		assertEquals(outlineA3, newOutline.get(3));
		assertEquals(outlineA0, newOutline.get(4));
		
	}
	
	/** invert, same start */
	@Test
	public void testRearrangeOutline4() {
		
		List<VectorXZ> newOutline =
			EarClippingTriangulationUtil.rearrangeOutline(outlineA, 0, true);
		
		assertSame(5, newOutline.size());
		assertEquals(outlineA0, newOutline.get(0));
		assertEquals(outlineA3, newOutline.get(1));
		assertEquals(outlineA2, newOutline.get(2));
		assertEquals(outlineA1, newOutline.get(3));
		assertEquals(outlineA0, newOutline.get(4));
		
	}
	
	@Test
	public void testInsertVertexInPolygonOutline() {
		
		VectorXZ point = new VectorXZ(0.55f, 0.55f);
		
		List<VectorXZ> newOutline = new LinkedList<VectorXZ>(outlineA);
		
		EarClippingTriangulationUtil.insertVertexInPolygonOutline(newOutline, point);
		
		assertSame(7, newOutline.size());
		assertTrue(newOutline.contains(point));
				
		assertEquals(newOutline.get(newOutline.indexOf(point) - 1),
				newOutline.get(newOutline.indexOf(point) + 1));
				
	}
	
	@Test
	public void testInsertHoleInPolygonOutline() {
		
		List<VectorXZ> newOutline = new LinkedList<VectorXZ>(outlineA);
		
		EarClippingTriangulationUtil.insertHoleInPolygonOutline(newOutline, holeB, Arrays.asList(holeA));
		
		assertSame(outlineA.size() + holeB.getVertexLoop().size() + 1,
				newOutline.size());
		
		for (VectorXZ innerVertex : holeB.getVertexLoop()) {
			assertTrue(newOutline.contains(innerVertex));
		}
		
	}
	
	@Test
	public void testTriangulateSimplePolygon() {
		
		List<TriangleXZ> triangles = new ArrayList<TriangleXZ>(
			EarClippingTriangulationUtil.triangulateSimplePolygon(new ArrayList<VectorXZ>(outlineA)));
		
		assertSame(triangles.size(), 2);
		
		Collection<VectorXZ> vsT0 =
			Arrays.asList(triangles.get(0).v1,  triangles.get(0).v2,  triangles.get(0).v3);
		Collection<VectorXZ> vsT1 =
			Arrays.asList(triangles.get(1).v1,  triangles.get(1).v2,  triangles.get(1).v3);
		
		if (vsT0.contains(outlineA0) && vsT0.contains(outlineA1) && vsT0.contains(outlineA2)) {
			assertTrue(vsT1.contains(outlineA0) && vsT1.contains(outlineA3) && vsT1.contains(outlineA2));
		} else if (vsT0.contains(outlineA1) && vsT0.contains(outlineA2) && vsT0.contains(outlineA3)) {
			assertTrue(vsT1.contains(outlineA1) && vsT1.contains(outlineA0) && vsT1.contains(outlineA3));
		} else if (vsT0.contains(outlineA2) && vsT0.contains(outlineA3) && vsT0.contains(outlineA0)) {
			assertTrue(vsT1.contains(outlineA2) && vsT1.contains(outlineA1) && vsT1.contains(outlineA0));
		} else if (vsT0.contains(outlineA3) && vsT0.contains(outlineA0) && vsT0.contains(outlineA1)) {
			assertTrue(vsT1.contains(outlineA3) && vsT1.contains(outlineA2) && vsT1.contains(outlineA1));
		} else {
			fail();
		}
		
	}
	
	@Test
	public void testIsConvex1() {
		
		for (int i=0; i < outlineA.size(); i++) {
			assertTrue(EarClippingTriangulationUtil.isConvex(i, outlineA));
		}
		
	}
	
	@Test
	public void testIsEarTip1() {
		
		for (int i=0; i < outlineA.size(); i++) {
			assertTrue(EarClippingTriangulationUtil.isEarTip(i, outlineA));
		}
		
	}

	@Test
	public void testIsConvex2() {
		
		List<VectorXZ> outlineNoDup = outlineB.subList(0, outlineB.size() - 1);
		
		for (int i=0; i < outlineNoDup.size(); i++) {
			if (i == 4) {
				assertFalse(EarClippingTriangulationUtil.isConvex(i, outlineNoDup));
			} else {
				assertTrue("at " + i, EarClippingTriangulationUtil.isConvex(i, outlineNoDup));
			}
		}
				
	}
	
	@Test
	public void testIsEarTip2() {
		
		List<VectorXZ> outlineNoDup = outlineB.subList(0, outlineB.size() - 1);
		
		assertTrue(EarClippingTriangulationUtil.isEarTip(0, outlineNoDup));
		assertFalse(EarClippingTriangulationUtil.isEarTip(1, outlineNoDup));
		assertFalse(EarClippingTriangulationUtil.isEarTip(2, outlineNoDup));
		assertTrue(EarClippingTriangulationUtil.isEarTip(3, outlineNoDup));
		assertFalse(EarClippingTriangulationUtil.isEarTip(4, outlineNoDup));
				
	}
	
}
