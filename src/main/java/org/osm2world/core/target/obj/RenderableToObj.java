package org.osm2world.core.target.obj;

import org.osm2world.core.target.common.RenderableToPrimitiveTarget;

public interface RenderableToObj extends RenderableToPrimitiveTarget {

	public void renderTo(ObjTarget target);

}
