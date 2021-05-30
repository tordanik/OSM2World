package org.osm2world.core.target;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.statistics.StatisticsTarget.Stat.PRIMITIVE_COUNT;
import static org.osm2world.core.util.FaultTolerantIterationUtil.*;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.statistics.StatisticsTarget;
import org.osm2world.core.world.data.WorldObject;

public final class TargetUtil {

	private TargetUtil() {}

	/**
	 * render all world objects to a target instance
	 * that are compatible with that target type
	 */
	public static void renderWorldObjects(Target target, MapData mapData, boolean renderUnderground) {

		for (MapElement mapElement : mapData.getMapElements()) {
			forEach(mapElement.getRepresentations(), (WorldObject r) -> {
				if (r.getParent() == null) {
					if (renderUnderground || r.getGroundState() != GroundState.BELOW) {
						renderObject(target, r);
					}
				}
			}, (e, r) -> DEFAULT_EXCEPTION_HANDLER.accept(e, r.getPrimaryMapElement()));
		}
	}

	/**
	 * render all world objects to a target instances
	 * that are compatible with that target type.
	 * World objects are added to a target until the number of primitives
	 * reaches the primitive threshold, then the next target is retrieved
	 * from the iterator.
	 */
	public static void renderWorldObjects(Iterator<? extends Target> targetIterator, MapData mapData,
			int primitiveThresholdPerTarget) {

		final StatisticsTarget primitiveCounter = new StatisticsTarget();

		forEach(mapData.getMapElements(), new Consumer<MapElement>() {

			Target currentTarget = targetIterator.next();

			@Override public void accept(MapElement e) {
				for (WorldObject r : e.getRepresentations()) {

					renderObject(primitiveCounter, r);

					renderObject(currentTarget, r);

					if (primitiveCounter.getGlobalCount(PRIMITIVE_COUNT) >= primitiveThresholdPerTarget) {
						currentTarget = targetIterator.next();
						primitiveCounter.clear();
					}

				}
			}

		});

	}

	/**
	 * renders any object to a target instance.
	 * Also sends {@link Target#beginObject(WorldObject)} calls.
	 */
	public static final void renderObject(Target target, WorldObject object) {
		target.beginObject(object);
		object.renderTo(target);
	}

	public static final List<List<VectorXZ>> flipTexCoordsVertically(List<List<VectorXZ>> texCoordLists) {
		return texCoordLists.stream().map(list ->
				list.stream().map(v -> new VectorXZ(v.x, 1.0 - v.z)).collect(toList())).collect(toList());
	}

}
