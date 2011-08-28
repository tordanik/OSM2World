package org.openstreetmap.josm.plugins.graphview.util;

import static junit.framework.Assert.assertNull;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.*;

import org.junit.Test;

public class ValueStringParserTest {

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
        assertClose(112.5f, parseAngle("SEE"));
    }

    @Test
    public void testParseAngleInvalid() {
        assertNull(parseAngle("forward"));
        assertNull(parseAngle("-90"));
    }
    
    
    /* utility methods for testing */
    
    private static final void assertClose(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError("expected " + expected + ", was " + actual);
        }
    }

}
