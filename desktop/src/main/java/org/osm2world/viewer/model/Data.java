package org.osm2world.viewer.model;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.annotation.Nonnull;

import org.osm2world.ConversionFacade;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.osm.creation.GeodeskReader;
import org.osm2world.osm.creation.MbtilesReader;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.osm.creation.OSMFileReader;
import org.osm2world.osm.data.OSMData;
import org.osm2world.scene.Scene;

public class Data extends Observable {

	private final @Nonnull List<File> configFiles;
	private O2WConfig config;
	private File osmFile = null;
	private Scene conversionResults = null;

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

	public void loadOSMData(OSMDataReaderView reader, boolean failOnLargeBBox, ProgressListener listener,
			Map<String, Object> extraOptions) throws IOException {

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

			converter.addProgressListener(listener);

			OSMData osmData = reader.getAllData();

			if (failOnLargeBBox) {
				double maxBoundingBoxDegrees = 1;
				if (osmData.getLatLonBounds().sizeLat() > maxBoundingBoxDegrees
						|| osmData.getLatLonBounds().sizeLon() > maxBoundingBoxDegrees) {
					throw new BoundingBoxSizeException();
				}
			}

			O2WConfig configForLoad = config;
			for (var entry : extraOptions.entrySet()) {
				configForLoad = configForLoad.withProperty(entry.getKey(), entry.getValue());
			}

			// disable LOD restrictions to make all LOD available through the LOD selector
			configForLoad.withProperty("lod", null);

			conversionResults = converter.createRepresentations(
					osmData, null, configForLoad, null);

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

	public Scene getConversionResults() {
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
