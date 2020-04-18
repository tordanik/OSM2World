package org.osm2world.core.target;

/**
 * object that can be rendered/exported to a {@link Target}
 */
public interface Renderable {

	/**
	 * outputs the 3D geometry.
	 * Most objects will use the same code for all {@link Target} implementations,
	 * but some may use special-case handling with instanceof checks.
	 */
	public void renderTo(Target target);

}
