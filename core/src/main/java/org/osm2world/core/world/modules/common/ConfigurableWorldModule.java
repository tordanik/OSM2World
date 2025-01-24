package org.osm2world.core.world.modules.common;

import javax.annotation.Nonnull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.core.world.creation.WorldModule;

/**
 * simple superclass for {@link WorldModule}s that stores a configuration set by
 * {@link #setConfiguration(org.apache.commons.configuration.Configuration)}
 */
public abstract class ConfigurableWorldModule implements WorldModule {

	protected @Nonnull Configuration config = new PropertiesConfiguration();

	@Override
	public void setConfiguration(@Nonnull Configuration config) {
		this.config = config;
	}

}
