package org.osm2world.output;

import static java.util.stream.Collectors.toList;
import static org.osm2world.util.FaultTolerantIterationUtil.DEFAULT_EXCEPTION_HANDLER;
import static org.osm2world.util.FaultTolerantIterationUtil.forEach;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXZ;
import org.osm2world.util.functions.CheckedConsumer;
import org.osm2world.world.data.WorldObject;

import com.google.gson.JsonIOException;

public final class OutputUtil {

	public enum Compression { NONE, ZIP, GZ }

	private OutputUtil() {}

	/**
	 * renders all {@link WorldObject}s to an output.
	 * Also sends {@link Output#beginObject(WorldObject)} calls.
	 */
	public static void renderWorldObjects(Output output, MapData mapData, boolean renderUnderground) {

		for (MapElement mapElement : mapData.getMapElements()) {
			forEach(mapElement.getRepresentations(), (WorldObject r) -> {
				if (r.getParent() == null) {
					if (renderUnderground || r.getGroundState() != GroundState.BELOW) {
						renderObject(output, r);
					}
				}
			}, (e, r) -> DEFAULT_EXCEPTION_HANDLER.accept(e, r.getPrimaryMapElement()));
		}
	}

	/**
	 * renders one {@link WorldObject} to an output.
	 * Also sends {@link Output#beginObject(WorldObject)} calls.
	 */
	public static final void renderObject(Output output, WorldObject object) {
		output.beginObject(object);
		object.buildMeshes().forEach(output::drawMesh);
		object.getSubModels().forEach(it -> it.render(output));
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

		try (var fileOutputStream = new FileOutputStream(outputFile)) {

			writeWithCompression(fileOutputStream, compression, outputFile.getName(), writeToStream);

		} catch (JsonIOException | IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * optionally applies a compression while writing data to an output stream
	 *
	 * @param outputStream  the stream to write to
	 * @param compression  the compression to use, can be {@link Compression#NONE}
	 * @param archiveName  the name of the entry inside the archive, used to name entries inside, optional
	 * @param writeToStream  the function which produces the content that should be written to the file
	 */
	public static <E extends Exception> void writeWithCompression(OutputStream outputStream,
			Compression compression, @Nullable String archiveName, CheckedConsumer<OutputStream, E> writeToStream) throws E {

		archiveName = archiveName != null ? archiveName : "content";

		try {

			OutputStream contentOutputStream = switch (compression) {
				case NONE -> outputStream;
				case GZ -> new GZIPOutputStream(outputStream);
				case ZIP -> {
					var zipOS = new ZipOutputStream(outputStream);
					zipOS.putNextEntry(new ZipEntry(archiveName.replace(".gz", "")));
					yield zipOS;
				}
			};

			try (contentOutputStream) {

				writeToStream.accept(contentOutputStream);

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
