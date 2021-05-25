package org.osm2world.core.world.modules.traffic_sign;

import static org.junit.Assert.*;
import static org.osm2world.core.world.modules.traffic_sign.TrafficSignIdentifier.parseTrafficSignValue;

import java.util.List;

import org.junit.Test;

public class TrafficSignIdentifierTest {

	@Test
	public void testHumanReadableValue() {
		TrafficSignIdentifier result = parseTrafficSignValue("city_limit").get(0);
		assertNull(result.country);
		assertEquals("city_limit", result.sign);
		assertNull(result.bracketText);
		assertNull(result.subType());
		assertEquals("SIGN_CITY_LIMIT", result.configKey());
		assertEquals("SIGN_CITY_LIMIT", result.configKeyWithoutSubType());
	}

	@Test
	public void testComplexValue() {
		TrafficSignIdentifier result = parseTrafficSignValue("DE:327-50[800]").get(0);
		assertEquals("DE", result.country);
		assertEquals("327-50", result.sign);
		assertEquals("800", result.bracketText);
		assertEquals("50", result.subType());
		assertEquals("SIGN_DE_327_50", result.configKey());
		assertEquals("SIGN_DE_327", result.configKeyWithoutSubType());
	}

	@Test
	public void testMultipleValues() {
		List<TrafficSignIdentifier> result = parseTrafficSignValue("DE:260,1020-30; 265[3.8] ");
		assertEquals(3, result.size());
		assertEquals("DE:260", result.get(0).toString());
		assertEquals("DE:1020-30", result.get(1).toString());
		assertEquals("DE:265[3.8]", result.get(2).toString());
	}

}
