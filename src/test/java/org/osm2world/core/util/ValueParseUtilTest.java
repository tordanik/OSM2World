package org.osm2world.core.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.*;

import java.awt.Color;

import org.junit.Test;

public class ValueParseUtilTest {

	@Test
	public void testParseUInt() {
		assertEquals((Integer)5, parseUInt("5"));
		assertEquals((Integer)0, parseUInt("0"));
		assertNull(parseUInt("-5"));
		assertNull(parseUInt("1.5"));
		assertNull(parseUInt("foobar"));
		assertNull(parseUInt(null));
	}

    /* speed */

    @Test
    public void testParseSpeedDefault() {
        assertClose(50, parseSpeed("50"));
    }

    @Test
    public void testParseSpeedKmh() {
        assertClose(30, parseSpeed("30 km/h"));
        assertClose(100, parseSpeed("100km/h"));
    }

    @Test
    public void testParseSpeedMph() {
        assertClose(40.234f, parseSpeed("25mph"));
        assertClose(40.234f, parseSpeed("25 mph"));
    }

    @Test
    public void testParseSpeedInvalid() {
        assertNull(parseSpeed("lightspeed"));
    }

    /* measure */

    @Test
    public void testParseMeasureDefault() {
        assertClose(3.5f, parseMeasure("3.5"));
    }

    @Test
    public void testParseMeasureM() {
        assertClose(2, parseMeasure("2m"));
        assertClose(5.5f, parseMeasure("5.5 m"));
    }

    @Test
    public void testParseMeasureKm() {
        assertClose(1000, parseMeasure("1 km"));
        assertClose(7200, parseMeasure("7.2km"));
    }

    @Test
    public void testParseMeasureMi() {
        assertClose(1609.344f, parseMeasure("1 mi"));
    }

    @Test
    public void testParseMeasureFeetInches() {
        assertClose(3.6576f, parseMeasure("12'0\""));
        assertClose(1.9812f, parseMeasure("6' 6\""));
    }

    @Test
    public void testParseMeasureInvalid() {
        assertNull(parseMeasure("very long"));
        assertNull(parseMeasure("6' 16\""));
    }

    /* weight */

    @Test
    public void testParseWeightDefault() {
        assertClose(3.6f, parseWeight("3.6"));
    }

    @Test
    public void testParseWeightT() {
        assertClose(30, parseWeight("30t"));
        assertClose(3.5f, parseWeight("3.5 t"));
    }

    @Test
    public void testParseWeightInvalid() {
        assertNull(parseWeight("heavy"));
    }

    /* angle */

    @Test
    public void testParseAngleDefault() {
        assertClose( 47, parseAngle("47"));
        assertClose(  0, parseAngle("360"));
    }

    @Test
    public void testParseAngleLetters() {
        assertClose(  0.0f, parseAngle("N"));
        assertClose(225.0f, parseAngle("SW"));
        assertClose(112.5f, parseAngle("ESE"));
    }

    @Test
    public void testParseAngleInvalid() {
        assertNull(parseAngle("forward"));
        assertNull(parseAngle("-90"));
    }

    @Test
    public void testParseColorDefault() {
    	assertEquals(new Color(255, 0, 0), parseColor("#ff0000"));
    	assertEquals(new Color(1, 2, 3), parseColor("#010203"));
    }

    @Test
    public void testParseColorNamed() {
    	assertEquals(new Color(255, 0, 0), parseColor("red", CSS_COLORS));
    }

    @Test
    public void testParseColorInvalid() {
    	assertNull(parseColor("#"));
    	assertNull(parseColor("ff0000"));
    	assertNull(parseColor("ff0000", CSS_COLORS));
    	assertNull(parseColor("nosuchvalue", CSS_COLORS));
    	assertNull(parseColor(null, CSS_COLORS));
    }

    @Test
    public void testParseLevels() {
    	assertEquals(asList(-5), parseLevels("-5"));
    	assertEquals(asList(13, 14), parseLevels("13 - 14"));
    	assertEquals(asList(-1, 0, 1, 2, 3), parseLevels("-1-3"));
    	assertEquals(asList(-4, -3), parseLevels("-4--3"));
    	assertEquals(asList(5, 6, 7), parseLevels("6;5 ; 7"));
    	assertEquals(asList(-3, 0, 1, 2, 3), parseLevels(" -3; 0-2 ;3"));
    }

    @Test
    public void testParseLevelsInvalid() {
    	assertNull(parseLevels("ground floor"));
    	assertNull(parseLevels("3-1"));
    	assertNull(parseLevels("5.5"));
    }

    /* utility methods for testing */

    private static final void assertClose(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError("expected " + expected + ", was " + actual);
        }
    }

}
