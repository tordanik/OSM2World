package org.osm2world.core.target;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.statistics.StatisticsTarget.Stat.PRIMITIVE_COUNT;
import static org.osm2world.core.util.FaultTolerantIterationUtil.DEFAULT_EXCEPTION_HANDLER;
import static org.osm2world.core.util.FaultTolerantIterationUtil.forEach;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.statistics.StatisticsTarget;
import org.osm2world.core.util.functions.CheckedConsumer;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.data.WorldObject;

import com.google.gson.JsonIOException;

public final class TargetUtil {

	public enum Compression { NONE, ZIP }

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
		if (object instanceof LegacyWorldObject) {
			((LegacyWorldObject)object).renderTo(target);
		} else {
			object.buildMeshes().forEach(target::drawMesh);
			object.getSubModels().forEach(it -> target.drawModel(it.model, it.params));
		}
	}

	public static final List<List<VectorXZ>> flipTexCoordsVertically(List<List<VectorXZ>> texCoordLists) {
		return texCoordLists.stream().map(list ->
				list.stream().map(v -> new VectorXZ(v.x, 1.0 - v.z)).collect(toList())).collect(toList());
	}

	/**
	 * writes content to a file and optionally applies a compression
	 *
	 * @param outputFile  the file to write to
	 * @param compression  the compression to use, can be {@link Compression#NONE}
	 * @param writeToStream  the function which produces the content that should be written to the file
	 */
	public static <E extends Exception> void writeFileWithCompression(File outputFile,
				Compression compression, CheckedConsumer<OutputStream, E> writeToStream) throws E {

		try {

			OutputStream outputStream;

			if (compression == Compression.ZIP) {
				var zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
				zipOutputStream.putNextEntry(new ZipEntry(outputFile.getName().replace(".gz", "")));
				outputStream = zipOutputStream;
			} else {
				outputStream = new FileOutputStream(outputFile);
			}

			try (outputStream) {

				writeToStream.accept(outputStream);

				if (outputStream instanceof ZipOutputStream zipOutputStream) {
					try {
						zipOutputStream.closeEntry();
					} catch (IOException ignored) { /* stream was already closed */ }
				}

			}

		} catch (JsonIOException | IOException e) {
			throw new RuntimeException(e);
		}

	}

}
