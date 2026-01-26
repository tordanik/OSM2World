package org.osm2world.conversion;

import org.osm2world.style.Style;

/**
 * Context for a given conversion.
 * This is thread-local. That is, freely accessible from anywhere, but only valid within a thread.
 * Therefore, it can offer easy access to information which applies to a single conversion run
 * (of which several may be executed in parallel).
 */
public class ConversionContext {

	private static final ThreadLocal<O2WConfig> config = ThreadLocal.withInitial(() -> null);

	public static O2WConfig config() {
		O2WConfig c = config.get();
		if (c != null) {
			return c;
		} else {
			throw new IllegalStateException("No configuration available on this thread");
		}
	}

	public static Style mapStyle() {
		return config().mapStyle();
	}

	public static void setConfig(O2WConfig config) {
		ConversionContext.config.set(config);
	}

}
