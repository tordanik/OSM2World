package org.osm2world.viewer.view.debug;

import static java.lang.Math.*;
import static java.util.Arrays.asList;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;

import org.osm2world.EleInterpolationStrategy;
import org.osm2world.Hardcoded;
import org.osm2world.SRTMData;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.model.RenderOptions;

public abstract class InterpolationStrategyDebugView extends DebugView {

	protected abstract EleInterpolationStrategy buildStrategy();
	
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
		return map != null && mapProjection != null;
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
			
			SRTMData srtmData = new SRTMData(Hardcoded.SRTM_DIR, mapProjection);
			
			Collection<VectorXYZ> sites = srtmData.getSites(map);
						
			EleInterpolationStrategy strategy = buildStrategy();
			strategy.setKnownSites(sites);
			
			AxisAlignedBoundingBoxXZ bound = map.getDataBoundary();
			
			
			int startX = (int)floor(bound.minX / SAMPLE_DIST);
			int endX = (int)ceil(bound.maxX / SAMPLE_DIST);
			int startZ = (int)floor(bound.minZ / SAMPLE_DIST);
			int endZ = (int)ceil(bound.maxZ / SAMPLE_DIST);
			
			int numSamplesX = endX-startX;
			int numSamplesZ = endZ-startZ;
			
			VectorXYZ[][] samples = new VectorXYZ[numSamplesX][numSamplesZ];

			long totalSamples = numSamplesX * numSamplesZ;
			
			long startTimeMillis = System.currentTimeMillis();
			
			for (int x = startX; x < endX; x++) {
				for (int z = startZ; z < endZ; z++) {
					
					VectorXZ pos = new VectorXZ(x * SAMPLE_DIST, z * SAMPLE_DIST);
					samples[x-startX][z-startZ] = strategy.interpolateEle(pos);
										
				}
				
				if ((x-startX) % 100 == 0) {
					long finishedSamples = (x-startX + 1) * numSamplesZ;
					System.out.println(finishedSamples + "/" + totalSamples
							+ " after " + ((System.currentTimeMillis() - startTimeMillis) / 1000f));
				}
			}

			/* draw surface from samples */
			
			for (int x = 0; x+1 < numSamplesX; x++) {
				for (int z = 0; z+1 < numSamplesZ; z++) {
					
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
