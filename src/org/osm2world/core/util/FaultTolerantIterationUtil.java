package org.osm2world.core.util;

/**
 * utility class that allows iterations where Exceptions in the processing
 * of a single element don't cause program failure
 */
final public class FaultTolerantIterationUtil {

	private FaultTolerantIterationUtil() { }
	
	public static interface Operation<T> {
		public void perform(T input);
	}
	
	public static final <T> void iterate(
			Iterable<T> collection, Operation<T> operation) {
		
		for (T input : collection) {
			try {
				operation.perform(input);
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
