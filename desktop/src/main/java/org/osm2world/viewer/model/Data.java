package org.osm2world.viewer.model;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.annotation.Nonnull;

import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.map_elevation.creation.EleCalculator;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.osm.creation.GeodeskReader;
import org.osm2world.core.osm.creation.MbtilesReader;
import org.osm2world.core.osm.creation.OSMDataReaderView;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.util.functions.Factory;

public class Data extends Observable {

	private final @Nonnull List<File> configFiles;
	private O2WConfig config;
	private File osmFile = null;
	private Results conversionResults = null;

	public Data(List<File> configFiles, O2WConfig config) {
		this.configFiles = configFiles;
		this.config = config;
	}

	public O2WConfig getConfig() {
		return config;
	}

	/** updates the configuration */
	public void setConfig(O2WConfig config) {

		this.config = config;

		if (conversionResults != null) {
			this.setChanged();
			this.notifyObservers();
		}

	}

	/** reloads the configuration from the config file */
	public void reloadConfig(RenderOptions options) {
		var config = new O2WConfig(Map.of("lod", options.lod.ordinal()), configFiles.toArray(new File[0]));
		this.setConfig(config);
	}

	public void loadOSMData(OSMDataReaderView reader, boolean failOnLargeBBox,
			Factory<? extends TerrainInterpolator> interpolatorFactory,
			Factory<? extends EleCalculator> eleCalculatorFactory,
			ProgressListener listener)
					throws IOException {

		try {

			if (reader.reader instanceof OSMFileReader r) {
				this.osmFile = r.file();
			} else if (reader.reader instanceof GeodeskReader r) {
				this.osmFile = r.file();
			} else if (reader.reader instanceof MbtilesReader r) {
				this.osmFile = r.file();
			} else {
				this.osmFile = null;
			}

			ConversionFacade converter = new ConversionFacade();
			converter.setTerrainEleInterpolatorFactory(interpolatorFactory);
			converter.setEleCalculatorFactory(eleCalculatorFactory);

			converter.addProgressListener(listener);

			OSMData osmData = reader.getAllData();

			if (failOnLargeBBox) {
				double maxBoundingBoxDegrees = 1;
				if (osmData.getLatLonBounds().sizeLat() > maxBoundingBoxDegrees
						|| osmData.getLatLonBounds().sizeLon() > maxBoundingBoxDegrees) {
					throw new BoundingBoxSizeException();
				}
			}

			// disable LOD restrictions to make all LOD available through the LOD selector
			O2WConfig configForLoad = config.withProperty("lod", null);

			conversionResults = converter.createRepresentations(
					osmData, null, null, configForLoad, null);

		} catch (IOException | BoundingBoxSizeException e) {

			osmFile = null;
			conversionResults = null;

			throw e;

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

	/**
	 * exception to be thrown if the OSM input data covers an area that may be too large for the viewer to handle
	 */
	public static class BoundingBoxSizeException extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 2841146365929523046L; //generated VersionID

		private BoundingBoxSizeException() {}

		@Override
		public String toString() {
			return "Oversize bounding box";
		}

	}

}
