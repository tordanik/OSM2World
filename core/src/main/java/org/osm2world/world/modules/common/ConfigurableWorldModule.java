package org.osm2world.world.modules.common;

import javax.annotation.Nonnull;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.world.creation.WorldModule;

/**
 * simple superclass for {@link WorldModule}s that stores a configuration set by
 * {@link #setConfiguration(O2WConfig)}
 */
public abstract class ConfigurableWorldModule implements WorldModule {

	protected @Nonnull O2WConfig config = new O2WConfig();

	@Override
	public void setConfiguration(@Nonnull O2WConfig config) {
		this.config = config;
	}

}
