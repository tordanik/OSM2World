package org.osm2world.util.functions;

/** equivalent to a  {@link java.util.function.Consumer} that throws checked exceptions */
@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception> {
	void accept(T t) throws E;
}
