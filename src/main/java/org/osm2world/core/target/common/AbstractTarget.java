package org.osm2world.core.target.common;

import javax.annotation.Nonnull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.core.target.Target;

/**
 * superclass for {@link Target} implementations that defines some
 * of the required methods using others. Extending it reduces the number of
 * methods that have to be provided by the implementation
 */
public abstract class AbstractTarget implements Target {

	protected @Nonnull Configuration config = new PropertiesConfiguration();

	@Override
	public Configuration getConfiguration() {
		return config;
	}

	@Override
	public void setConfiguration(Configuration config) {
		if (config != null) {
			this.config = config;
		} else {
			this.config = new PropertiesConfiguration();
		}
	}

	@Override
	public void finish() {}

}
