package org.osm2world.console;

import static java.util.Arrays.sort;
import static org.osm2world.conversion.ConversionLog.LogLevel.FATAL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osm2world.conversion.ConversionLog;

/**
 * implementation of the mode triggered by {@link CLIArguments#isParameterFileDir()}.
 * OSM2World will read, process and delete parameter files from a directory.
 * Each of these files contains a set of command line parameters which define a conversion run.
 */
public class ParamFileDirMode {

	/** maximum number of enqueued parameter files. Making it larger means waiting longer for new, high-prio files. */
	private static final int MAX_QUEUE_LENGTH = 2;

	public static void run(File paramFileDir) {

		if (!paramFileDir.isDirectory()) {
			System.err.println("parameterFileDir must be a directory!");
			return;
		}

		int numCores = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(numCores);
		AtomicInteger queueLength = new AtomicInteger();

		boolean containsFiles = false;

		do {

			File[] files = paramFileDir.listFiles();
			sort(files);

			containsFiles = files.length > 0;

			if (containsFiles) {

				while (queueLength.get() - numCores > MAX_QUEUE_LENGTH) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				}

				try {

					// create an temporary file (only to get unique names, it's immediately overwritten)
					Path tempFilePath = File.createTempFile("osm2world-", "-" + files[0].getName()).toPath();

					// move the parameter file to the temporary location
					Files.move(files[0].toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

					queueLength.incrementAndGet();

					executor.submit(() -> {

						System.out.println(tempFilePath);

						try {
							OSM2World.main(new String[]{"--parameterFile", tempFilePath.toString()});
						} catch (Exception e) {
							ConversionLog.log(FATAL, "Run failed for " + tempFilePath, e, null);
						}

						try {
							Files.delete(tempFilePath);
						} catch (IOException e) {
							System.err.println("Warning: Could not delete temporary file " + tempFilePath);
						}

						queueLength.decrementAndGet();

					});

				} catch (IOException e) {
					System.err.println("IO issue encountered in parameter file directory mode, exiting.\n" + e);
				}

			}

		} while (containsFiles);

		executor.shutdown();

		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {}

	}

}
