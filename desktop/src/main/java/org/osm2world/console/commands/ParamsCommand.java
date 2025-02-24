package org.osm2world.console.commands;

import static java.util.Arrays.sort;
import static org.osm2world.conversion.ConversionLog.LogLevel.FATAL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osm2world.console.OSM2World;
import org.osm2world.conversion.ConversionLog;

import picocli.CommandLine;

/**
 * Mode where OSM2World will read, process and optionally delete text files from a directory.
 * Each line in each file contains a set of command line parameters which define a call to {@link ConvertCommand}.
 */
@CommandLine.Command(name = "params", description = "Run 'convert' with parameters from a text file or directory.")
public class ParamsCommand implements Callable<Integer> {

	/** maximum number of enqueued parameter files. Making it larger means waiting longer for new, high-prio files. */
	private static final int MAX_QUEUE_LENGTH = 2;

	@CommandLine.Parameters(arity = "1..*", description = "files containing one set of parameters per line, " +
			"or a directory containing an arbitrary amount of such files. " +
			"New files added to such a directory while OSM2World is running will be considered as well.")
	List<File> paths;

	@CommandLine.Option(names = "--deleteProcessedFiles",
			description = "delete parameter files after they have been executed")
	boolean deleteProcessedFiles = false;

	@Override
	public Integer call() throws Exception {

		if (paths.size() == 1 && paths.get(0).isDirectory()) {

			handleParamFileDir(paths.get(0), deleteProcessedFiles);

		} else {

			for (File paramFile : paths) {
				if (paramFile.isFile()) {
					handleParamFile(paramFile);
					if (deleteProcessedFiles) {
						boolean deleted = paramFile.delete();
						if (!deleted) {
							System.err.println("Could not delete parameter file: " + paramFile);
						}
					}
				} else {
					System.err.println("Not a file, skipping it: " + paramFile);
				}
			}

		}

		return 0;

	}

	private void handleParamFile(File paramFile) throws IOException {

		List<String[]> argGroups = getUnparsedParameterGroups(paramFile);

		/* TODO: collect parameter groups into compatible groups
		 * (groups of parameter groups that use the same input and config files) */

		/* execute conversions */

		CommandLine commandLine = OSM2World.buildCommandLine(new ConvertCommand());

		for (String[] args : argGroups) {
			commandLine.execute(args);
		}

	}

	public static void handleParamFileDir(File paramFileDir, boolean deleteProcessedFiles) {

		if (!paramFileDir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + paramFileDir);

		int numCores = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(numCores);
		AtomicInteger queueLength = new AtomicInteger();

		HashSet<File> handledFiles = new HashSet<>();

		boolean containsFiles;

		do {

			File[] files = paramFileDir.listFiles(it -> !handledFiles.contains(it));

			if (files == null) {
				containsFiles = false;
			} else {
				containsFiles = files.length > 0;
				sort(files);
			}

			if (containsFiles) {

				while (queueLength.get() - numCores > MAX_QUEUE_LENGTH) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException ignored) {}
				}

				try {

					// create a temporary file (only to get unique names, it's immediately overwritten)
					Path tempFilePath = File.createTempFile("osm2world-", "-" + files[0].getName()).toPath();

					// move or copy the parameter file to the temporary location
					if (deleteProcessedFiles) {
						Files.move(files[0].toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
					} else {
						Files.copy(files[0].toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
						handledFiles.add(files[0]);
					}

					queueLength.incrementAndGet();

					executor.submit(() -> {

						System.out.println(tempFilePath);

						try {
							CommandLine commandLine = OSM2World.buildCommandLine(new ParamsCommand());
							commandLine.execute(tempFilePath.toString());
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
		} catch (InterruptedException ignored) {}

	}

	public static List<String[]> getUnparsedParameterGroups(
			File parameterFile) throws IOException {

		try (BufferedReader in = new BufferedReader(new FileReader(parameterFile))) {

			List<String[]> result = new ArrayList<>();

			String line;

			while ((line = in.readLine()) != null) {

				if (line.startsWith("#")) continue;
				if (line.trim().isEmpty()) continue;

				line = line.trim();
				line = line.replaceFirst("^convert\\s", "");
				line = line.trim();

				List<String> argList = new ArrayList<>();

				Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
				Matcher matcher = regex.matcher(line);

				while (matcher.find()) {
					if (matcher.group(1) != null) {
						// Add double-quoted string without the quotes
						argList.add(matcher.group(1));
					} else if (matcher.group(2) != null) {
						// Add single-quoted string without the quotes
						argList.add(matcher.group(2));
					} else {
						// Add unquoted word
						argList.add(matcher.group());
					}
				}

				result.add(argList.toArray(new String[0]));

			}

			return result;

		}

	}

}
