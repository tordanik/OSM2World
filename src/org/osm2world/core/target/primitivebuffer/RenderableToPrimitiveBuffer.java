package org.osm2world.core.target.primitivebuffer;

import org.osm2world.core.target.Renderable;

public interface RenderableToPrimitiveBuffer extends Renderable {

	public void renderTo(PrimitiveBuffer primitiveBuffer);
	
}
