package org.osm2world.core.target;

/**
 * representation that only uses methods from {@link Target}
 * and can therefore render to all targets supporting these features
 */
public interface RenderableToAllTargets extends Renderable {
	//TODO: extend all other Renderables

	public void renderTo(Target target);
	
}
