package org.osm2world.core.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * utility class that allows iterations where Exceptions in the processing
 * of a single element don't cause program failure
 */
final public class FaultTolerantIterationUtil {

	private FaultTolerantIterationUtil() { }

	public static final <T> void forEach(Iterable<? extends T> iterable,
			Consumer<? super T> action, BiConsumer<? super Throwable, ? super T> exceptionHandler) {

		for (T t : iterable) {
			try {
				action.accept(t);
			} catch (Exception | AssertionError e) {
				exceptionHandler.accept(e, t);
			}
		}

	}

	/**
	 * version of {@link #forEach(Iterable, Consumer)} that uses {@link #DEFAULT_EXCEPTION_HANDLER}
	 */
	public static final <T> void forEach(Iterable<? extends T> iterable, Consumer<? super T> action) {
		forEach(iterable, action, DEFAULT_EXCEPTION_HANDLER);
	}

	/** a default exception handler that prints to System.err */
	public static final BiConsumer<Throwable, Object> DEFAULT_EXCEPTION_HANDLER = (Throwable e, Object o) -> {
		System.err.println("ignored exception:");
		e.printStackTrace();
		System.err.println("this exception occurred for the following input:\n" + o);
	};

}
