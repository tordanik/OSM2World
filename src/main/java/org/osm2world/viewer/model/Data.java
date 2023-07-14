package org.osm2world.viewer.model;

import java.io.File;
import java.io.IOException;
import java.util.Observable;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.BoundingBoxSizeException;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.osm.creation.GeodeskReader;
import org.osm2world.core.osm.creation.MbtilesReader;
import org.osm2world.core.osm.creation.OSMDataReader;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.util.ConfigUtil;
import org.osm2world.core.util.functions.Factory;

public class Data extends Observable {

	private final File configFile;
	private Configuration config;
	private File osmFile = null;
	private Results conversionResults = null;

	public Data(File configFile, Configuration config) {
		this.configFile = configFile;
		this.config = config;
	}

	public Configuration getConfig() {
		return config;
	}

	/** reloads the configuration from the config file */
	public void reloadConfig() throws ConfigurationException {

		if (configFile != null) {

			PropertiesConfiguration fileConfig = new PropertiesConfiguration();
			fileConfig.setListDelimiter(';');
			fileConfig.load(configFile);

			this.config = fileConfig;

			ConfigUtil.parseFonts(fileConfig);

			if (conversionResults != null) {
				this.setChanged();
				this.notifyObservers();
			}

		}

	}

	public void loadOSMData(OSMDataReader reader, boolean failOnLargeBBox,
			Factory<? extends TerrainInterpolator> interpolatorFactory,
			Factory<? extends EleConstraintEnforcer> enforcerFactory,
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
