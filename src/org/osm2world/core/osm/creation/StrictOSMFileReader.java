package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

/**
 * DataSource providing information from a single .osm file. The file is read
 * during the {@link #getData()} call, there will be no updates when the file is
 * changed later. This class internally uses osmosis to read the file.
 * 
 * Use the regular {@link OSMDataReader} if you also want to read files which
 * don't exactly conform to the standard, such as files produced by JOSM. 
 */
public class StrictOSMFileReader extends OsmosisReader  {
	
	private final File file;
	
	public StrictOSMFileReader(File file) throws FileNotFoundException {
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
