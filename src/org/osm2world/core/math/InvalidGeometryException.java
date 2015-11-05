package org.osm2world.core.math;

/** 
 * exception that is used when attempting to construct geometry that
 * does not exhibit required properties - such as self-intersecting polygons.
 * In cases where these exceptions can be caused by problems in the data,
 * it makes sense to catch and report them.
 */
public class InvalidGeometryException extends RuntimeException {

	private static final long serialVersionUID = -7755970537446437611L; //generated serialVersionUID

	public InvalidGeometryException() {
		super();
	}

	public InvalidGeometryException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidGeometryException(String message) {
		super(message);
	}

	public InvalidGeometryException(Throwable cause) {
		super(cause);
	}
	
}
