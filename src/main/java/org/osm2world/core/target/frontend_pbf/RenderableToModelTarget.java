package org.osm2world.core.target.frontend_pbf;

import org.osm2world.core.target.Renderable;

public interface RenderableToModelTarget extends Renderable {

	public void renderTo(ModelTarget<?> target);

}
