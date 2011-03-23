package org.osm2world.core.target;

import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.WorldObject;

public final class TargetUtil {

	private TargetUtil() {}
	
	/**
	 * render all world objects to a target instance
	 * that are compatible with that target type
	 */
	public static <R extends Renderable> void renderWorldObjects(
			final Target<R> target, final MapData mapData) {

		iterate(mapData.getMapElements(), new Operation<MapElement>() {
			@Override public void perform(MapElement e) {
				for (WorldObject r : e.getRepresentations()) {
					renderObject(target, r);
				}
			}
		});
		
	}
	
	/**
	 * renders any object to a target instance
	 * if it is a renderable compatible with that target type.
	 * Also sends {@link Target#beginObject(WorldObject)} calls.
	 */
	public static final <R extends Renderable> void renderObject(
			final Target<R> target, Object object) {
		
		Class<R> renderableType = target.getRenderableType();
		
		if (renderableType.isInstance(object)) {
			
			if (object instanceof WorldObject) {
				target.beginObject((WorldObject)object);
			} else {
				target.beginObject(null);
			}
			
			target.render(renderableType.cast(object));
			
		} else if (object instanceof RenderableToAllTargets) {
			
			if (object instanceof WorldObject) {
				target.beginObject((WorldObject)object);
			} else {
				target.beginObject(null);
			}
			
			((RenderableToAllTargets)object).renderTo(target);
			
		}
		
	}
	
}
