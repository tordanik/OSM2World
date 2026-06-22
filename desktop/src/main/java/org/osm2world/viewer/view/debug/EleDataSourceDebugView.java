package org.osm2world.viewer.view.debug;

import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.osm2world.map_elevation.creation.TerrainEleData;
import org.osm2world.map_elevation.creation.TerrainEleDataGridGroup;
import org.osm2world.map_elevation.creation.TerrainEleDataSource;
import org.osm2world.map_elevation.creation.TerrainEleDataUtil;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.Scene;
import org.osm2world.scene.color.Color;

public class EleDataSourceDebugView extends StaticDebugView {

	private static final boolean DRAW_SITES = false;

	private MapProjection mapProjection;

	public EleDataSourceDebugView() {
		super("Elevation data sources", "shows ele data grids and boundaries");
	}

	@Override
	public boolean canBeUsed() {
		return scene != null && mapProjection != null && config != null
				&& TerrainEleDataUtil.eleDataSourceFromConfig(config) != null;
	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		TerrainEleDataSource eleDataSource = TerrainEleDataUtil.eleDataSourceFromConfig(config);
		assert eleDataSource != null; // due to canBeUsed check

		try {

			TerrainEleData data = eleDataSource.getSites(scene.getBoundary().pad(10), mapProjection);

			List<TerrainEleData> dataElements = new ArrayList<>();

			if (data instanceof TerrainEleDataGridGroup group) {
				dataElements.addAll(group.grids());
			} else if (data != null) {
				dataElements.add(data);
			}

			/* draw each source's boundary in a different color */

			var random = new Random(500);

			for (TerrainEleData dataElement : dataElements) {

				List<VectorXYZ> vs = dataElement.bounds().getCorners().stream()
						.map(mapProjection::toXZ)
						.map(VectorXZ::xyz)
						.toList();

				var color = new Color((int)(random.nextDouble() * 0x1000000));

				output.drawLineStrip(color, 2, closeLoop(vs));

				if (DRAW_SITES) {
					for (LatLonEle site : dataElement.sites()) {
						VectorXYZ sitePos = mapProjection.toXZ(site.latLon()).xyz();
						drawBoxAround(output, sitePos, color, 0.05f);
					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void setConversionResults(Scene conversionResults) {
		super.setConversionResults(conversionResults);
		mapProjection = conversionResults.getMapProjection();
	}

}
