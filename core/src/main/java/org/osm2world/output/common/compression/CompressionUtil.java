package org.osm2world.output.common.compression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.osm2world.util.functions.CheckedConsumer;

import com.google.gson.JsonIOException;

public final class CompressionUtil {

	private CompressionUtil() {}

	/**
	 * writes content to a file and optionally applies a compression
	 *
	 * @param outputFile  the file to write to
	 * @param compression  the compression to use, can be {@link Compression#NONE}
	 * @param writeToStream  the function which produces the content that should be written to the file
	 */
	public static <E extends Exception> void writeFileWithCompression(File outputFile,
				Compression compression, CheckedConsumer<OutputStream, E> writeToStream) throws E {

		outputFile.getAbsoluteFile().getParentFile().mkdirs();

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
