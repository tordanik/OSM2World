package org.osm2world.map_elevation.creation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.OrthographicAzimuthalMapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

public class SRTMDataTest {

	@Test
	public void testGetSites() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File srtmDir = new File(classLoader.getResource("srtm").getFile());

		var projection = new OrthographicAzimuthalMapProjection(new LatLon(4, 33));
		var srtmData = new SRTMData(srtmDir, projection);

		var bounds1 = new LatLonBounds(4.1, 33.1, 4.2, 33.2);
		Assert.assertFalse(srtmData.getSites(projectBounds(projection, bounds1)).isEmpty());

		var bounds2 = new LatLonBounds(4.1, 34.1, 4.2, 34.2);
		Assert.assertFalse(srtmData.getSites(projectBounds(projection, bounds2)).isEmpty());

	}

	private static AxisAlignedRectangleXZ projectBounds(OrthographicAzimuthalMapProjection projection, LatLonBounds latLonBounds) {
		return AxisAlignedRectangleXZ.bbox(List.of(
				projection.toXZ(latLonBounds.getMin()), projection.toXZ(latLonBounds.getMax())));
	}

}
