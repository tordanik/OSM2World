package org.osm2world.core.target;

import static org.osm2world.core.target.statistics.StatisticsTarget.Stat.PRIMITIVE_COUNT;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import java.util.Iterator;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.target.common.RenderableToPrimitiveTarget;
import org.osm2world.core.target.statistics.StatisticsTarget;
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
	 * render all world objects to a target instances
	 * that are compatible with that target type.
	 * World objects are added to a target until the number of primitives
	 * reaches the primitive threshold, then the next target is retrieved
	 * from the iterator.
	 */
	public static <R extends RenderableToPrimitiveTarget> void renderWorldObjects(
			final Iterator<? extends Target<R>> targetIterator,
			final MapData mapData, final int primitiveThresholdPerTarget) {
				
		final StatisticsTarget primitiveCounter = new StatisticsTarget();
		
		iterate(mapData.getMapElements(), new Operation<MapElement>() {

			Target<R> currentTarget = targetIterator.next();
			
			@Override public void perform(MapElement e) {
				for (WorldObject r : e.getRepresentations()) {
										
					renderObject(primitiveCounter, r);
					
					renderObject(currentTarget, r);
					
					if (primitiveCounter.getGlobalCount(PRIMITIVE_COUNT)
							>= primitiveThresholdPerTarget) {
						currentTarget = targetIterator.next();
						primitiveCounter.clear();
					}
					
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
