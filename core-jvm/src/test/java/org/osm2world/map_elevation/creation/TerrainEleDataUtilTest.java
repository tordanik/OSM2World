package org.osm2world.map_elevation.creation;

import static org.junit.Assert.assertTrue;
import static org.osm2world.map_elevation.creation.TerrainEleDataUtil.eleDataSourceFromConfig;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.osm2world.conversion.O2WConfig;

public class TerrainEleDataUtilTest {

	@Test
	public void testEleDataSourceFromConfig_SRTM() {

		File srtmDir = getTestFile("srtm");

		var config1 = new O2WConfig(Map.of("srtmDir", srtmDir.getAbsolutePath()));
		var data1 = eleDataSourceFromConfig(config1);
		assertTrue(data1 instanceof SRTMDataSource);

	}

	@Test
	public void testEleDataSourceFromConfig_TerrariumXYZ() {

		File terrariumDir = getTestFile("terrarium-xyz");

		var config1 = new O2WConfig(Map.of("eleDataUrl",
				"file:// " + terrariumDir.getAbsolutePath() + "{z}/{x}/{y}.webp"));
		var data1 = eleDataSourceFromConfig(config1);
		assertTrue(data1 instanceof TerrariumXYZDataSource);

		var config2 = new O2WConfig(Map.of("eleDataUrl", "https://tiles.mapterhorn.com/{z}/{x}/{y}.webp"));
		var data2 = eleDataSourceFromConfig(config2);
		assertTrue(data2 instanceof TerrariumXYZDataSource);

	}

	@Test
	public void testEleDataSourceFromConfig_MultipleValues() {

		File terrariumDir = getTestFile("terrarium-xyz");

		var config = new O2WConfig(Map.of("eleDataUrl",
				"file://" + terrariumDir.getAbsolutePath() + "{z}/{x}/{y}.webp;"
						+ terrariumDir.getAbsolutePath() + "/terrarium_12_2145_1434.pmtiles;   "
						+ "https://tiles.mapterhorn.com/{z}/{x}/{y}.webp"));
		var data = eleDataSourceFromConfig(config);

		if (!(data instanceof TerrainEleDataSourceWithFallback fallback1)) throw new AssertionError();

		assertTrue(fallback1.primaryDataSource instanceof TerrariumXYZDataSource);

		if (!(fallback1.secondaryDataSource instanceof TerrainEleDataSourceWithFallback fallback2)) throw new AssertionError();

		assertTrue(fallback2.primaryDataSource instanceof TerrariumXYZDataSource);
		assertTrue(fallback2.secondaryDataSource instanceof TerrariumXYZDataSource);

	}

}
