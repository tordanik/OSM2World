package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

/**
 * DataSource providing information from a single .osm file. The file is read
 * during the constructor call, there will be no updates when the file is
 * changed later. This class uses osmosis to read the file.
 */
public class OSMFileReader extends OsmosisReader {
	
	private final File file;
	
	public OSMFileReader(File file) throws IOException {
		super(createSourceForFile(file));
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	
	private static final RunnableSource createSourceForFile(File file)
			throws FileNotFoundException {
		
		boolean pbf = false;
		CompressionMethod compression = CompressionMethod.None;
		
		if (file.getName().endsWith(".pbf")) {
			pbf = true;
		} else if (file.getName().endsWith(".gz")) {
			compression = CompressionMethod.GZip;
		} else if (file.getName().endsWith(".bz2")) {
			compression = CompressionMethod.BZip2;
		}
		
		RunnableSource reader;
		
		if (pbf) {
			reader = new crosby.binary.osmosis.OsmosisReader(
					new FileInputStream(file));
		} else {
			reader = new XmlReader(file, false, compression);
		}
		
		return reader;
		
	}
	
}
