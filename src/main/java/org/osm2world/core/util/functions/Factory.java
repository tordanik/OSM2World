package org.osm2world.core.util.functions;

import java.util.function.Supplier;

/**
 * Creates instances of another object type.
 * Unlike {@link Supplier}, this must create a new instance for each call of {@link #get()}.
 *
 * @param <T>  the type of objects created by this factory
 */
@FunctionalInterface
public interface Factory<T> extends Supplier<T> {}
