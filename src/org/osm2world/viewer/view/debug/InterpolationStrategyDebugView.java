package org.osm2world.viewer.view.debug;

import static java.util.Arrays.asList;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;

import org.osm2world.TerrainInterpolator;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorGridXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.model.RenderOptions;

public abstract class InterpolationStrategyDebugView extends DebugView {

	protected abstract TerrainInterpolator buildStrategy();
	
	private static final double SAMPLE_DIST = 6;
	
	private static final Material TERRAIN_MAT =
			new ImmutableMaterial(Lighting.FLAT, Color.WHITE);
	private static final Color SITE_COL = Color.RED;

	private final RenderOptions renderOptions;
	
	private MapProjection mapProjection = null;
	
	protected InterpolationStrategyDebugView(RenderOptions renderOptions) {
		this.renderOptions = renderOptions;
	}

	@Override
	public boolean canBeUsed() {
		return map != null && mapProjection != null && eleData != null;
	}

	@Override
	public void setConversionResults(Results conversionResults) {
		super.setConversionResults(conversionResults);
		mapProjection = conversionResults.getMapProjection();
	}

	@Override
	public void fillTarget(JOGLTarget target) {

		target.setRenderingParameters(new JOGLRenderingParameters(null,
    			renderOptions.isWireframe(), true));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
		try {
			
			Collection<VectorXYZ> sites = eleData.getSites(map);
			
			TerrainInterpolator strategy = buildStrategy();
			strategy.setKnownSites(sites);
			
			AxisAlignedBoundingBoxXZ bound = map.getDataBoundary();
			
			VectorGridXZ sampleGrid = new VectorGridXZ(bound, SAMPLE_DIST);
			
			VectorXYZ[][] samples = new VectorXYZ[sampleGrid.sizeX()][sampleGrid.sizeZ()];
			
			long startTimeMillis = System.currentTimeMillis();
			
			for (int x = 0; x < sampleGrid.sizeX(); x++) {
				for (int z = 0; z < sampleGrid.sizeZ(); z++) {
					
					samples[x][z] = strategy.interpolateEle(sampleGrid.get(x, z));
										
				}
				
				if (x % 100 == 0) {
					long finishedSamples = x * sampleGrid.sizeZ();
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
							null);
					
				}
			}
			
			/* draw sites */
			
			/*
			for (VectorXYZ site : sites) {
				target.drawLineStrip(SITE_COL, 1, site, site.y(site.y+10));
			}
			*/
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
						
	}
	
}
