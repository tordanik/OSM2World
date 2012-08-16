package org.osm2world.viewer.model;

import java.io.File;
import java.io.IOException;
import java.util.Observable;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.BoundingBoxSizeException;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_elevation.creation.ElevationCalculator;

public class Data extends Observable {
	
	private Configuration config = new BaseConfiguration();
	private File osmFile = null;
	private Results conversionResults = null;
	
	public void setConfig(Configuration config) {
		this.config = config;
	}
	
	/**
	 * 
	 */
	public void loadOSMFile(File osmFile, ElevationCalculator eleCalculator,
			boolean failOnLargeBBox, ProgressListener listener)
					throws IOException, BoundingBoxSizeException {
		
		try {
			
			this.osmFile = osmFile;
			
			ConversionFacade converter = new ConversionFacade();
			converter.setElevationCalculator(eleCalculator);
			
			converter.addProgressListener(listener);
			
			if (failOnLargeBBox) {
				config.addProperty("maxBoundingBoxDegrees", 1);
			}
			
			conversionResults = converter.createRepresentations(
					osmFile, null, config, null);
			
		} catch (IOException e) {
			
			osmFile = null;
			conversionResults = null;
			
			throw e;
			
		} catch (BoundingBoxSizeException e) {
			
			osmFile = null;
			conversionResults = null;
			
			throw e;
			
		} finally {
			
			config.clearProperty("maxBoundingBoxDegrees");
			
		}
		
		this.setChanged();
		this.notifyObservers();
		
	}
	
	public File getOsmFile() {
		return osmFile;
	}
	
	public Results getConversionResults() {
		return conversionResults;
	}
	
}
