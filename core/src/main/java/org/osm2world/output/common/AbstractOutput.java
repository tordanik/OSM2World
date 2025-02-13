package org.osm2world.output.common;

import javax.annotation.Nonnull;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.output.Output;

/**
 * superclass for {@link Output} implementations that defines some
 * of the required methods using others. Extending it reduces the number of
 * methods that have to be provided by the implementation
 */
public abstract class AbstractOutput implements Output {

	protected @Nonnull O2WConfig config = new O2WConfig();

	@Override
	public O2WConfig getConfiguration() {
		return config;
	}

	@Override
	public void setConfiguration(O2WConfig config) {
		if (config != null) {
			this.config = config;
		} else {
			this.config = new O2WConfig();
		}
	}

	@Override
	public void finish() {}

}
