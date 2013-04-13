package org.osm2world.core.util.functions;

/**
 * Creates instances of another object type.
 *
 * @param <T>  the type of objects created by this factory
 */
public interface Factory<T> {
	
	/** creates a new instance */
	T make();
	
}
