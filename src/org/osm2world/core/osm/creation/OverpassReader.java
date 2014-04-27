package org.osm2world.core.osm.creation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.openstreetmap.osmosis.core.misc.v0_6.EmptyReader;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.osm2world.core.map_data.creation.LatLon;

public class OverpassReader extends OsmosisReader {
	
	/** fetches data within a bounding box from Overpass API */
	public OverpassReader(LatLon min, LatLon max) throws IOException {
		// FIXME not just nodes!
		this("node("+min.lat+","+min.lon+","+max.lat+","+max.lon+");out meta;");
	}
	
	/** fetches data from Overpass API according to an arbitrary query
	 * @throws IOException */
	public OverpassReader(String queryString) throws IOException {
		super(new OverpassSource(queryString));
	}
	
	/**
	 * source component for an Osmosis pipeline that reads data from Overpass API
	 */
	private static class OverpassSource implements RunnableSource {
		
		private static final String API_URL = "http://www.overpass-api.de/api/interpreter";
		
		private final String queryString;
		
		private Sink sink;
		
		public OverpassSource(String queryString) {
			this.queryString = queryString;
		}
		
		@Override
		public void setSink(Sink sink) {
			this.sink = sink;
		}
		
		@Override
		public void run() {
			
			try {
				
				URL apiURL = new URL(API_URL);
				
				HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				
				DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
				printout.writeBytes("data=" + URLEncoder.encode(queryString, "utf-8"));
				printout.flush();
				printout.close();
				
				InputStream inputStream = connection.getInputStream();
				
				XmlStreamReader xmlReader = new XmlStreamReader(inputStream, true, CompressionMethod.None);
				xmlReader.setSink(sink);
				xmlReader.run();
				
				inputStream.close();
								
			} catch (IOException e) {
				
				System.err.println("could not get input data from Overpass API."
						+ "\nQuery: " + queryString
						+ "\nCause: ");
				
				e.printStackTrace();
				
				EmptyReader emptyReader = new EmptyReader();
				emptyReader.setSink(sink);
				emptyReader.run();
				
			}
			
		}
		
	}
	
}