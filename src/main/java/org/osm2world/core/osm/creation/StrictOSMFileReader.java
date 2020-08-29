package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.osm2world.core.osm.data.OSMData;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;

/**
 * {@link OSMDataReader} providing information from a single .osm file. The file is read
 * during the {@link #getData()} call, there will be no updates when the file is
 * changed later. This class internally uses osm4j to read the file.
 *
 * Use the regular {@link OSMDataReader} if you also want to read files which
 * don't exactly conform to the standard, such as files produced by JOSM.
 */
public class StrictOSMFileReader implements OSMDataReader {

	private final File file;

	public StrictOSMFileReader(File file) throws FileNotFoundException {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	private static enum CompressionMethod {
		None,
		GZip,
		BZip2;
	}

	@Override
	public OSMData getData() throws IOException {

		boolean pbf = false;
		CompressionMethod compression = CompressionMethod.None;

		if (file.getName().endsWith(".pbf")) {
			pbf = true;
		} else if (file.getName().endsWith(".gz")) {
			compression = CompressionMethod.GZip;
		} else if (file.getName().endsWith(".bz2")) {
			compression = CompressionMethod.BZip2;
		}

		if (pbf) {

			try (FileInputStream is = new FileInputStream(file)) {

				OsmIterator iterator = new PbfIterator(is, true);

				InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
				return new OSMData(data);

			}

		} else {

			// TODO: handle compression!
			OsmIterator iterator = new OsmXmlIterator(file, true);

			InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
			return new OSMData(data);

		}

	}

}
