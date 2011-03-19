package org.osm2world.core.math;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class PolygonXZTest {
	
	private static final VectorXZ outlineA0 = new VectorXZ(-1.1f, -1.1f);
	private static final VectorXZ outlineA1 = new VectorXZ(-1.1f, 1.1f);
	private static final VectorXZ outlineA2 = new VectorXZ(1.1f, 1.1f);
	private static final VectorXZ outlineA3 = new VectorXZ(1.1f, -1.1f);
	private static final VectorXZ outlineA4 = new VectorXZ(0f, 1f);
	
	private static final List<VectorXZ> outlineA = Arrays.asList(outlineA0,
			outlineA1, outlineA2, outlineA3, outlineA0);
	
	private static final List<VectorXZ> outlineB = Arrays.asList(outlineA0,
			outlineA1, outlineA2, outlineA3, outlineA4, outlineA0);
	
	@Test
	public void testIsClockwise1() {
		
		assertTrue(new SimplePolygonXZ(outlineA).isClockwise());
		
		List<VectorXZ> outlineAInv = new ArrayList<VectorXZ>(outlineA);
		Collections.reverse(outlineAInv);
		assertFalse(new SimplePolygonXZ(outlineAInv).isClockwise());
		
	}
	
	@Test
	public void testIsClockwise2() {
		
		assertTrue(new SimplePolygonXZ(outlineB).isClockwise());
		
		List<VectorXZ> outlineBInv = new ArrayList<VectorXZ>(outlineB);
		Collections.reverse(outlineBInv);
		assertFalse(new SimplePolygonXZ(outlineBInv).isClockwise());
	}
	
	@Test
	public void testIsClockwise3() {
		
		// test case created from a former bug
		assertTrue(new SimplePolygonXZ(Arrays.asList(
				new VectorXZ(114266.61f,12953.262f),
				new VectorXZ(114258.74f,12933.117f),
				new VectorXZ(114257.69f,12939.848f),
				new VectorXZ(114266.61f,12953.262f))).isClockwise());
		
		
	}
	
	@Test
	public void testIsEquivalentTo_same() {
		
		PolygonXZ polyA = new PolygonXZ(outlineA);
		assertTrue(polyA.isEquivalentTo(polyA));
		
	}
	
	@Test
	public void testIsEquivalentTo_yes() {
		
		assertTrue(
				new PolygonXZ(Arrays.asList(outlineA0, outlineA1, outlineA4, outlineA0))
					.isEquivalentTo(
						new PolygonXZ(Arrays.asList(outlineA1, outlineA4, outlineA0, outlineA1))));
		
	}
	
	@Test
	public void testIsEquivalentTo_no() {
		
		assertFalse(
				new PolygonXZ(Arrays.asList(outlineA0, outlineA1, outlineA4, outlineA0))
					.isEquivalentTo(
						new PolygonXZ(Arrays.asList(outlineA0, outlineA4, outlineA1, outlineA0))));
		
	}
	
}
