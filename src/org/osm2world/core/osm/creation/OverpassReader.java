package org.osm2world.core.osm.creation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.osm.data.OSMData;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;

public class OverpassReader implements OSMDataReader {
	
	public static final String DEFAULT_API_URL = "http://www.overpass-api.de/api/interpreter";
	private String apiURL;
	private String queryString;
	
	/** fetches data within a bounding box from Overpass API */
	public OverpassReader(LatLon min, LatLon max) {
		this(DEFAULT_API_URL, min, max);
	}
	
	/** fetches data within a bounding box from any Overpass API instance */
	public OverpassReader(String apiURL, LatLon min, LatLon max) {
		this(apiURL, "[bbox:"+min.lat+","+min.lon+","+max.lat+","+max.lon+"];(node;rel(bn)->.x;way;node(w)->.x;rel(bw););out meta;");
	}
	
	/** fetches data from Overpass API according to an arbitrary query */
	public OverpassReader(String queryString) {
		this(DEFAULT_API_URL, queryString);
	}
	
	/** fetches data from any Overpass API instance according to an arbitrary query. */
	public OverpassReader(String apiURL, String queryString) {
		this.apiURL = apiURL;
		this.queryString = queryString;
	}
	
	public OSMData getData() throws IOException {
	
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
				
				OsmXmlIterator iterator = new OsmXmlIterator(inputStream, false);
				
				InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
				OSMData osmData = new OSMData(data);
				
				inputStream.close();
								
				return osmData;
			} catch (IOException e) {
				
				System.err.println("could not get input data from Overpass API."
						+ "\nQuery: " + queryString
						+ "\nCause: ");
				
				e.printStackTrace();
				
				InMemoryMapDataSet dataSet = new InMemoryMapDataSet();
				return new OSMData(dataSet);
				
			}
		
	}

	
}