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
	
	public static final String DEFAULT_API_URL = "http://www.overpass-api.de/api/interpreter";
	
	/** fetches data within a bounding box from Overpass API */
	public OverpassReader(LatLon min, LatLon max) {
		this(DEFAULT_API_URL, min, max);
	}
	
	/** fetches data within a bounding box from any Overpass API instance */
	public OverpassReader(String apiURL, LatLon min, LatLon max) {
		this(apiURL, "[bbox:"+min.lat+","+min.lon+","+max.lat+","+max.lon+"];(node;rel(bn)->.x;way;node(w)->.x;rel(bw););out meta;");
	}
	
	/** fetches data from Overpass API according to an arbitrary query
	 * @throws IOException */
	public OverpassReader(String queryString) {
		this(DEFAULT_API_URL, queryString);
	}
	
	/** fetches data from any Overpass API instance according to an arbitrary query.
	 * @throws IOException */
	public OverpassReader(String apiURL, String queryString) {
		super(new OverpassSource(apiURL, queryString));
	}
	
	/**
	 * source component for an Osmosis pipeline that reads data from Overpass API
	 */
	private static class OverpassSource implements RunnableSource {
		
		private final String apiURL;		
		private final String queryString;
		
		private Sink sink;
		
		public OverpassSource(String apiURL, String queryString) {
			this.apiURL = apiURL;
			this.queryString = queryString;
		}
		
		@Override
		public void setSink(Sink sink) {
			this.sink = sink;
		}
		
		@Override
		public void run() {
			
			try {
				
				URL url = new URL(apiURL);
				
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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