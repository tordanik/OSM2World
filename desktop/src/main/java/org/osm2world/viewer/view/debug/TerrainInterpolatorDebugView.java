package org.osm2world.viewer.view.debug;

import static java.util.Arrays.asList;

import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.osm2world.map_elevation.creation.SRTMData;
import org.osm2world.map_elevation.creation.TerrainElevationData;
import org.osm2world.map_elevation.creation.TerrainInterpolator;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.datastructures.VectorGridXZ;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.ImmutableMaterial;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.viewer.model.RenderOptions;

public abstract class TerrainInterpolatorDebugView extends DebugView {

	protected abstract TerrainInterpolator buildInterpolator();

	private static final double SAMPLE_DIST = 3;

	private static final Material TERRAIN_MAT =
			new ImmutableMaterial(Interpolation.FLAT, Color.WHITE);

	private final RenderOptions renderOptions;

	private MapProjection mapProjection = null;

	protected TerrainInterpolatorDebugView(RenderOptions renderOptions, String interpolatorName) {
		super(interpolatorName + " debug view",
				"shows empty terrain approximated by a " + interpolatorName);
		this.renderOptions = renderOptions;
	}

	@Override
	public boolean canBeUsed() {
		return scene != null && mapProjection != null && config != null && config.srtmDir() != null;
	}

	@Override
	public void setConversionResults(Scene conversionResults) {
		super.setConversionResults(conversionResults);
		mapProjection = conversionResults.getMapProjection();
	}

	@Override
	public void fillTarget(JOGLOutput target) {

		target.setRenderingParameters(new JOGLRenderingParameters(null,
    			renderOptions.isWireframe(), true));

		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);

		try {

			TerrainElevationData eleData = new SRTMData(config.srtmDir(), mapProjection);

			AxisAlignedRectangleXZ bound = scene.getBoundary();

			Collection<VectorXYZ> sites = eleData.getSites(scene.getBoundary().pad(10));

			TerrainInterpolator strategy = buildInterpolator();
			strategy.setKnownSites(sites);

			VectorGridXZ sampleGrid = new VectorGridXZ(bound, SAMPLE_DIST);

			VectorXYZ[][] samples = new VectorXYZ[sampleGrid.sizeX()][sampleGrid.sizeZ()];

			long startTimeMillis = System.currentTimeMillis();

			for (int x = 0; x < sampleGrid.sizeX(); x++) {
				for (int z = 0; z < sampleGrid.sizeZ(); z++) {

					samples[x][z] = strategy.interpolateEle(sampleGrid.get(x, z));

				}

				if (x % 100 == 0) {
					long finishedSamples = x * (long) sampleGrid.sizeZ();
					System.out.println(finishedSamples + "/" + sampleGrid.size()
							+ " after " + ((System.currentTimeMillis() - startTimeMillis) / 1000f));
				}
			}

			/* draw surface from samples */

			for (int x = 0; x+1 < samples.length; x++) {
				for (int z = 0; z+1 < samples[x].length; z++) {

					target.drawTriangleFan(TERRAIN_MAT,
							asList(samples[x][z], samples[x+1][z],
									samples[x+1][z+1], samples[x][z+1]),
							List.of());

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
