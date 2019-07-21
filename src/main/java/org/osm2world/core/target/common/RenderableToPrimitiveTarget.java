package org.osm2world.core.target.common;

import org.osm2world.core.target.Renderable;

public interface RenderableToPrimitiveTarget extends Renderable {

	public void renderTo(PrimitiveTarget<?> target);

}
