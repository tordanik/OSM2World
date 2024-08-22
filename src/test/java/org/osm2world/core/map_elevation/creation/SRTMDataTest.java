package org.osm2world.core.map_elevation.creation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.map_data.creation.OrthographicAzimuthalMapProjection;
import org.osm2world.core.math.AxisAlignedRectangleXZ;

public class SRTMDataTest {

	@Test
	public void testGetSites() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File srtmDir = new File(classLoader.getResource("srtm").getFile());

		var projection = new OrthographicAzimuthalMapProjection(new LatLon(4, 33));
		var srtmData = new SRTMData(srtmDir, projection);

		var bounds1 = new LatLonBounds(4.1, 33.1, 4.2, 33.2);
		Assert.assertFalse(srtmData.getSites(projectBounds(projection, bounds1)).isEmpty());

	}

	private static AxisAlignedRectangleXZ projectBounds(OrthographicAzimuthalMapProjection projection, LatLonBounds latLonBounds) {
		return AxisAlignedRectangleXZ.bbox(List.of(
				projection.toXZ(latLonBounds.getMin()), projection.toXZ(latLonBounds.getMax())));
	}

}
