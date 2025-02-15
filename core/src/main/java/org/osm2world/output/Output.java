package org.osm2world.output;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.scene.Scene;
import org.osm2world.world.data.WorldObject;

/**
 * A sink for rendering/writing {@link WorldObject}s to.
 */
public interface Output {

	void setConfiguration(O2WConfig config);
	O2WConfig getConfiguration();

	/**
	 * writes an entire {@link Scene} to this output.
	 * Will close the output at the end so no further content can be written.
	 */
	void outputScene(Scene scene);

}
