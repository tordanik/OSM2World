package org.osm2world.core.map_data.creation;

/**
 * exception which can be thrown when way nodes or relation members are missing
 */
public class IncompleteDataException extends Exception {

	public IncompleteDataException(String message) {
		super(message);
	}

}
