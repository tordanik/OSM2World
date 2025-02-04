package org.osm2world.core.target.common;

import javax.annotation.Nonnull;

import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.target.Target;

/**
 * superclass for {@link Target} implementations that defines some
 * of the required methods using others. Extending it reduces the number of
 * methods that have to be provided by the implementation
 */
public abstract class AbstractTarget implements Target {

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
