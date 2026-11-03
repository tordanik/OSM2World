package org.osm2world.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.osm2world.conversion.ConversionLog;

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
	 * version of {@link #forEach(Iterable, Consumer, BiConsumer)} that uses {@link #DEFAULT_EXCEPTION_HANDLER}
	 */
	public static final <T> void forEach(Iterable<? extends T> iterable, Consumer<? super T> action) {
		forEach(iterable, action, DEFAULT_EXCEPTION_HANDLER);
	}

	/** a default exception handler that logs to {@link ConversionLog} */
	public static final BiConsumer<Throwable, Object> DEFAULT_EXCEPTION_HANDLER = ConversionLog::error;

}
