package org.osm2world.core.target;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.target.common.RenderableToPrimitiveTarget;

public interface TargetProvider<R extends RenderableToPrimitiveTarget> {

	public Target<R> getTarget(MapElement e);

	public void close();

}
