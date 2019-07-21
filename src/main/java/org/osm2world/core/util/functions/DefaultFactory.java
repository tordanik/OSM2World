package org.osm2world.core.util.functions;


/**
 * creates new instances of a class by calling the standard constructor.
 * In many cases, this is the simplest way of obtaining a {@link Factory}
 * for a given class.
 */
public class DefaultFactory<T> implements Factory<T> {

	private final Class<? extends T> c;

	public DefaultFactory(Class<? extends T> c) {
		this.c = c;
	}

	@Override
	public T make() {
		try {
			return c.newInstance();
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

}
