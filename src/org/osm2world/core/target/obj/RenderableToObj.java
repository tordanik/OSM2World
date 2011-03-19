package org.osm2world.core.target.obj;

import org.osm2world.core.target.Renderable;

public interface RenderableToObj extends Renderable {

	public void renderTo(ObjTarget target);

}
