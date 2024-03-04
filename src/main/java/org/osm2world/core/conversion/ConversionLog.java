package org.osm2world.core.conversion;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapRelation;

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
			@Nullable MapRelation.Element element
	) {
		@Override
		public String toString() {
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
	}

	private static final ThreadLocal<List<Entry>> log = ThreadLocal.withInitial(ArrayList::new);
	private static final ThreadLocal<EnumSet<LogLevel>> consoleLogLevels =
			ThreadLocal.withInitial(() -> EnumSet.allOf(LogLevel.class));

	public static void setConsoleLogLevels(EnumSet<LogLevel> consoleLogLevels) {
		ConversionLog.consoleLogLevels.set(consoleLogLevels);
	}

	public static List<Entry> getLog() {
		return Collections.unmodifiableList(log.get());
	}

	public static void clear() {
		log.get().clear();
	}

	public static void log(Entry entry) {
		log.get().add(entry);
		if (consoleLogLevels.get().contains(entry.level)) {
			System.err.println(entry);
		}
	}

	public static void log(LogLevel level, String message, @Nullable Throwable e, @Nullable MapRelation.Element element) {
		log(new Entry(level, Instant.now(), message, e, element));
	}

	public static void error(String message, Throwable e, MapRelation.Element element) {
		log(LogLevel.ERROR, message, e, element);
	}

	public static void error(String message, Throwable e) {
		log(LogLevel.ERROR, message, e, null);
	}

	public static void error(String message, MapRelation.Element element) {
		log(LogLevel.ERROR, message, null, element);
	}

	public static void error(String message) {
		log(LogLevel.ERROR, message, null, null);
	}

	public static void error(Throwable e, MapRelation.Element element) {
		log(LogLevel.ERROR, e.getMessage(), e, element);
	}

	public static void error(Throwable e) {
		log(LogLevel.ERROR, e.getMessage(), e, null);
	}

	public static void warn(String message, Throwable e, MapRelation.Element element) {
		log(LogLevel.WARNING, message, e, element);
	}

	public static void warn(String message, Throwable e) {
		log(LogLevel.WARNING, message, e, null);
	}

	public static void warn(String message, MapRelation.Element element) {
		log(LogLevel.WARNING, message, null, element);
	}

	public static void warn(String message) {
		log(LogLevel.WARNING, message, null, null);
	}

	public static void warn(Throwable e, MapRelation.Element element) {
		log(LogLevel.WARNING, e.getMessage(), e, element);
	}

	public static void warn(Throwable e) {
		log(LogLevel.WARNING, e.getMessage(), e, null);
	}

}
