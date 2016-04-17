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
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.osm.creation.OSMDataReader;
import org.osm2world.core.osm.creation.StrictOSMFileReader;
import org.osm2world.core.util.functions.Factory;

public class Data extends Observable {
	
	private Configuration config = new BaseConfiguration();
	private File osmFile = null;
	private Results conversionResults = null;
	
	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		
		this.config = config;
		
		this.setChanged();
		this.notifyObservers();
		
	}
	
	/**
	 * @param interpolatorFactory
	 * @param enforcerFactory
	 * 
	 */
	public void loadOSMData(OSMDataReader reader, boolean failOnLargeBBox,
			Factory<? extends TerrainInterpolator> interpolatorFactory,
			Factory<? extends EleConstraintEnforcer> enforcerFactory,
			ProgressListener listener)
					throws IOException, BoundingBoxSizeException {
		
		try {
			
			if (reader instanceof StrictOSMFileReader) {
				this.osmFile = ((StrictOSMFileReader)reader).getFile();
			} else {
				this.osmFile = null;
			}
			
			ConversionFacade converter = new ConversionFacade();
			converter.setTerrainEleInterpolatorFactory(interpolatorFactory);
			converter.setEleConstraintEnforcerFactory(enforcerFactory);
			
			converter.addProgressListener(listener);
			
			if (failOnLargeBBox) {
				config.addProperty("maxBoundingBoxDegrees", 1);
			}
			
			conversionResults = converter.createRepresentations(
					reader.getData(), null, config, null);
			
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
