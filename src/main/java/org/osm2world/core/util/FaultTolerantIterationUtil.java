package org.osm2world.core.util;

import java.util.function.Consumer;

/**
 * utility class that allows iterations where Exceptions in the processing
 * of a single element don't cause program failure
 */
final public class FaultTolerantIterationUtil {

	private FaultTolerantIterationUtil() { }

	public static final <T> void iterate(
			Iterable<? extends T> collection, Consumer<T> operation) {

		for (T input : collection) {
			try {
				operation.accept(input);
			} catch (Exception e) {
				System.err.println("ignored exception:");
				//TODO proper logging
				e.printStackTrace();
				System.err.println("this exception occurred for the following input:\n"
						+ input);
			}
		}

	}

}
