package org.osm2world.core.osm.creation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;

import de.topobyte.osm4j.core.dataset.InMemoryMapDataSet;
import de.topobyte.osm4j.core.dataset.MapDataSetLoader;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;

/**
 * {@link OSMDataReader} fetching information from Overpass API.
 */
public record OverpassReader(String apiURL) implements OSMDataReader {

	public static final String DEFAULT_API_URL = "http://www.overpass-api.de/api/interpreter";

	/** accesses data from the default API at {@link #DEFAULT_API_URL} */
	public OverpassReader() {
		this(DEFAULT_API_URL);
	}

	@Override
	public OSMData getData(LatLonBounds bounds) throws IOException {
		return getData( "[bbox:"+bounds.minlat+","+bounds.minlon+","+bounds.maxlat+","+bounds.maxlon+"];"
				+ "(node;rel(bn)->.x;way;node(w)->.x;rel(bw););out meta;");
	}

	/** fetches data according to an arbitrary query. */
	public OSMData getData(String queryString) throws IOException {

		try {

			URL url = new URL(apiURL);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			try (DataOutputStream printout = new DataOutputStream(connection.getOutputStream())) {

				printout.writeBytes("data=" + URLEncoder.encode(queryString, StandardCharsets.UTF_8));
				printout.flush();

			}

			try (InputStream inputStream = connection.getInputStream()) {

				OsmXmlIterator iterator = new OsmXmlIterator(inputStream, false);

				InMemoryMapDataSet data = MapDataSetLoader.read(iterator, true, true, true);
				return new OSMData(data);

			}

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