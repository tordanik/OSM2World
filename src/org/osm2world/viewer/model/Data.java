package org.osm2world.viewer.model;

import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import java.io.File;
import java.io.IOException;
import java.util.Observable;

import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.primitivebuffer.PrimitiveBuffer;
import org.osm2world.core.target.primitivebuffer.RenderableToPrimitiveBuffer;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;

public class Data extends Observable {
	
	private File osmFile = null;
	private Results conversionResults = null;
	private PrimitiveBuffer gridPrimitiveBuffer = null;
	private PrimitiveBuffer terrainPrimitiveBuffer = null;
	
	public void loadOSMFile(File osmFile, ProgressListener listener)
	throws IOException {
		
		try {
			
			this.osmFile = osmFile;
			
			ConversionFacade converter = new ConversionFacade();
			converter.addProgressListener(listener);
			
			conversionResults = converter.createRepresentations(osmFile);
			
			gridPrimitiveBuffer = createPrimitiveBuffer(conversionResults, true, false);
			terrainPrimitiveBuffer = createPrimitiveBuffer(conversionResults, false, true);
			
		} catch (IOException e) {
			
			osmFile = null;
			conversionResults = null;
			gridPrimitiveBuffer = null;
			terrainPrimitiveBuffer = null;
			
			throw e;
			
		}
		
		this.setChanged();
		this.notifyObservers();
		
	}
	
	private static PrimitiveBuffer createPrimitiveBuffer(Results results,
			boolean includeGrid, boolean includeTerrain) {
		
		final PrimitiveBuffer newPrimitiveBuffer = new PrimitiveBuffer();
		
		Iterable<Renderable> renderables =
			results.getRenderables(Renderable.class, includeGrid, includeTerrain);
		
		iterate(renderables, new Operation<Renderable>() {
			@Override public void perform(Renderable input) {
				
				if (input instanceof RenderableToPrimitiveBuffer) {
					((RenderableToPrimitiveBuffer) input)
					.renderTo(newPrimitiveBuffer);
				} else if (input instanceof RenderableToAllTargets) {
					((RenderableToAllTargets) input)
					.renderTo(newPrimitiveBuffer);
				}
				
			}
		});
		
		return newPrimitiveBuffer;
		
	}
	
	public File getOsmFile() {
		return osmFile;
	}
	
	public Results getConversionResults() {
		return conversionResults;
	}
	
	public PrimitiveBuffer getGridPrimitiveBuffer() {
		return gridPrimitiveBuffer;
	}
	
	public PrimitiveBuffer getTerrainPrimitiveBuffer() {
		return terrainPrimitiveBuffer;
	}
	
}
