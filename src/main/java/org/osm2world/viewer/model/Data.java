package org.osm2world.viewer.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;

import javax.annotation.Nonnull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.osm2world.console.OSM2World;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.BoundingBoxSizeException;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_elevation.creation.EleCalculator;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.osm.creation.GeodeskReader;
import org.osm2world.core.osm.creation.MbtilesReader;
import org.osm2world.core.osm.creation.OSMDataReader;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.util.ConfigUtil;
import org.osm2world.core.util.functions.Factory;

public class Data extends Observable {

	private final @Nonnull List<File> configFiles;
	private Configuration config;
	private File osmFile = null;
	private Results conversionResults = null;

	public Data(List<File> configFiles, Configuration config) {
		this.configFiles = configFiles;
		this.config = config;
	}

	public Configuration getConfig() {
		return config;
	}

	/** updates the configuration */
	public void setConfig(Configuration config) {

		this.config = config;

		ConfigUtil.parseFonts(config);

		if (conversionResults != null) {
			this.setChanged();
			this.notifyObservers();
		}

	}

	/** reloads the configuration from the config file */
	public void reloadConfig(RenderOptions options) throws ConfigurationException {
		Configuration config = OSM2World.loadConfigFiles(options.lod, configFiles.toArray(new File[0]));
		this.setConfig(config);
	}

	public void loadOSMData(OSMDataReader reader, boolean failOnLargeBBox,
			Factory<? extends TerrainInterpolator> interpolatorFactory,
			Factory<? extends EleCalculator> eleCalculatorFactory,
			ProgressListener listener)
					throws IOException, BoundingBoxSizeException {

		try {

			if (reader instanceof OSMFileReader r) {
				this.osmFile = r.getFile();
			} else if (reader instanceof GeodeskReader r) {
				this.osmFile = r.getFile();
			} else if (reader instanceof MbtilesReader r) {
				this.osmFile = r.getFile();
			} else {
				this.osmFile = null;
			}

			ConversionFacade converter = new ConversionFacade();
			converter.setTerrainEleInterpolatorFactory(interpolatorFactory);
			converter.setEleCalculatorFactory(eleCalculatorFactory);

			converter.addProgressListener(listener);

			if (failOnLargeBBox) {
				config.addProperty("maxBoundingBoxDegrees", 1);
			}

			conversionResults = converter.createRepresentations(
					reader.getData(), null, null, config, null);

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
