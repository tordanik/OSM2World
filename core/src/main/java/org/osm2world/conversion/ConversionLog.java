package org.osm2world.conversion;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapRelationElement;

/**
 * OSM2World's internal logging solution.
 */
public class ConversionLog {

	public enum LogLevel {WARNING, ERROR, FATAL}

	public record Entry (
			@Nonnull LogLevel level,
			@Nonnull Instant time,
			@Nonnull String message,
			@Nullable Throwable e,
			@Nullable MapRelationElement element
	) {

		@Override
		public @Nonnull String toString() {
			var sb = new StringBuilder(level.toString());
			if (element != null) {
				sb.append("[").append(element).append("]");
			}
			sb.append(": ").append(message);
			if (e != null) {
				var sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				sb.append("\nCaused by: ").append(sw);
			}
			return sb.toString();
		}

		boolean isAlmostIdenticalTo(Entry otherEntry) {
			return this.level == otherEntry.level
					&& Objects.equals(this.message, otherEntry.message)
					&& this.element == otherEntry.element();
		}

	}

	private static final ThreadLocal<List<Entry>> log = ThreadLocal.withInitial(ArrayList::new);
	private static final ThreadLocal<EnumSet<LogLevel>> consoleLogLevels =
			ThreadLocal.withInitial(() -> EnumSet.allOf(LogLevel.class));
	private static final ThreadLocal<Integer> suppressedCopiesOfLastEntry = ThreadLocal.withInitial(() -> 0);

	public static void setConsoleLogLevels(EnumSet<LogLevel> consoleLogLevels) {
		ConversionLog.consoleLogLevels.set(consoleLogLevels);
	}

	public static List<Entry> getLog() {
		flushSuppressedCopies();
		return Collections.unmodifiableList(log.get());
	}

	public static void clear() {
		log.get().clear();
	}

	public static void log(Entry entry) {

		List<Entry> log = ConversionLog.log.get();

		/* handle suppression of almost identical entries */
		if (!log.isEmpty()) {
			Entry previousEntry = log.get(log.size() - 1);
			if (previousEntry.isAlmostIdenticalTo(entry)) {
				suppressedCopiesOfLastEntry.set(suppressedCopiesOfLastEntry.get() + 1);
				return;
			} else {
				flushSuppressedCopies();
			}
		}

		/* log the entry */
		logAndPrint(entry);

	}

	private static void logAndPrint(Entry entry) {
		log.get().add(entry);
		if (consoleLogLevels.get().contains(entry.level)) {
			System.err.println(entry);
		}
	}

	public static void log(LogLevel level, String message, @Nullable Throwable e, @Nullable MapRelationElement element) {
		log(new Entry(level, Instant.now(), message, e, element));
	}

	public static void error(String message, Throwable e, MapRelationElement element) {
		log(LogLevel.ERROR, message, e, element);
	}

	public static void error(String message, Throwable e) {
		log(LogLevel.ERROR, message, e, null);
	}

	public static void error(String message, MapRelationElement element) {
		log(LogLevel.ERROR, message, null, element);
	}

	public static void error(String message) {
		log(LogLevel.ERROR, message, null, null);
	}

	public static void error(Throwable e, MapRelationElement element) {
		log(LogLevel.ERROR, messageFor(e), e, element);
	}

	public static void error(Throwable e) {
		log(LogLevel.ERROR, messageFor(e), e, null);
	}

	public static void warn(String message, Throwable e, MapRelationElement element) {
		log(LogLevel.WARNING, message, e, element);
	}

	public static void warn(String message, Throwable e) {
		log(LogLevel.WARNING, message, e, null);
	}

	public static void warn(String message, MapRelationElement element) {
		log(LogLevel.WARNING, message, null, element);
	}

	public static void warn(String message) {
		log(LogLevel.WARNING, message, null, null);
	}

	public static void warn(Throwable e, MapRelationElement element) {
		log(LogLevel.WARNING, messageFor(e), e, element);
	}

	public static void warn(Throwable e) {
		log(LogLevel.WARNING, messageFor(e), e, null);
	}

	private static String messageFor(Throwable e) {
		return (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
	}

	private static void flushSuppressedCopies() {
		Integer copies = suppressedCopiesOfLastEntry.get();
		if (copies > 0) {
			List<Entry> log = ConversionLog.log.get();
			Entry previousEntry = log.get(log.size() - 1);
			String message = copies + " similar log entr" + (copies > 1 ? "ies" : "y") + " suppressed";
			logAndPrint(new Entry(previousEntry.level, Instant.now(), message, null, previousEntry.element));
			suppressedCopiesOfLastEntry.set(0);
		}
	}

}
