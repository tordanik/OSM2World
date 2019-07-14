package org.osm2world.core.world.modules.common;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.world.creation.WorldModule;

/**
 * simple superclass for {@link WorldModule}s that stores a configuration set by
 * {@link #setConfiguration(org.apache.commons.configuration.Configuration)}
 */
public abstract class ConfigurableWorldModule implements WorldModule {
	
	protected Configuration config;
	
	@Override
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
}
