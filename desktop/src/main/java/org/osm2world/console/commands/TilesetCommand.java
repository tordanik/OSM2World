package org.osm2world.console.commands;

import static org.osm2world.output.gltf.GltfOutput.GltfFlavor.GLB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.osm2world.O2WConverter;
import org.osm2world.console.commands.mixins.ConfigOptions;
import org.osm2world.console.commands.mixins.InputOptions;
import org.osm2world.console.commands.mixins.LoggingOptions;
import org.osm2world.console.commands.mixins.MetadataOptions;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.geo.*;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.output.Output;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.gltf.GltfOutput;
import org.osm2world.output.tileset.TilesetOutput;
import org.osm2world.scene.Scene;
import org.osm2world.scene.mesh.LevelOfDetail;

import picocli.CommandLine;

@CommandLine.Command(name = "tileset", description = "Create 3D Tiles for some geographic area.")
public class TilesetCommand implements Callable<Integer> {

	private enum OverwriteMode {
		NEVER, OLDER, ALWAYS
	}

	private static class Bounds {

		@CommandLine.Option(names = {"--bbox"}, paramLabel="lat,lon lat,lon...",
				description="area to create tiles for, can be specified multiple times", required = true)
		@Nullable LatLonBounds bboxPoints = null;

		@CommandLine.Option(names = {"--bboxTiles"}, paramLabel = "zoom,x,y",
				description = "area to create tiles for, can be specified multiple times", required = true)
		@Nullable TileBounds bboxTiles;

		public GeoBounds constructBbox() {
			if (bboxPoints != null) {
				return bboxPoints;
			} else if (bboxTiles != null) {
				return bboxTiles;
			} else {
				throw new Error("Neither bboxPoints nor bboxTiles is set");
			}
		}

	}

	@CommandLine.Option(names = {"--baseDir"}, description = "base directory for output files", required = true,
			paramLabel = "<path>")
	Path baseDir;

	@CommandLine.Option(names = {"--lod"}, description = "level of detail of the output, given as a number between 0 and 4",
			paramLabel="<number>")
	@Nullable
	LevelOfDetail lod = null;

	@CommandLine.Option(names = {"--noJson"}, description = "skip creation of tileset.json files")
	boolean noJson = false;

	@CommandLine.Option(names = {"--overwrite"}, defaultValue = "ALWAYS",
			description = "when to overwrite existing tiles (never, when they older than the input data, or always)")
	OverwriteMode overwriteFiles;

	@CommandLine.ArgGroup(multiplicity = "1..")
	List<Bounds> bounds;

	@CommandLine.Mixin
	InputOptions inputOptions;

	@CommandLine.Mixin
	ConfigOptions configOptions;

	@CommandLine.Mixin
	LoggingOptions loggingOptions;

	@CommandLine.Mixin
	MetadataOptions metadataOptions;

	private static final int ZOOM = 15;

	public Integer call() {

		List<TileNumber> tileNumbers = new ArrayList<>();

		for (var b : bounds) {

			LatLonBounds bbox = b.constructBbox().latLonBounds();

			// shrink bounds a tiny bit to prevent the neighboring tiles from being generated as well
			bbox = new LatLonBounds(bbox.minlat + 1e-5, bbox.minlon + 1e-5,
					bbox.maxlat - 1e-5, bbox.maxlon - 1e-5);

			tileNumbers.addAll(TileNumber.tilesForBounds(ZOOM, bbox));

		}

		List<TileNumber> filteredTileNumbers = filterTileNumbers(tileNumbers);

		int skippedTiles = tileNumbers.size() - filteredTileNumbers.size();
		if (skippedTiles > 0) {
			System.out.println("Skipping " + skippedTiles + " existing tiles");
		}

		createTiles(filteredTileNumbers);

		// TODO merge tilesets

		return 0;

	}

	private List<TileNumber> filterTileNumbers(List<TileNumber> tileNumbers) {

		List<TileNumber> result = new ArrayList<>();

		for (TileNumber tile : tileNumbers) {
			if (fileIsMissingOrOverwritable(getTileFilename(tile, ".glb"))
					&& fileIsMissingOrOverwritable(getTileFilename(tile, ".tileset.json"))) {
				result.add(tile);
			}
		}

		return result;

	}

	private void createTiles(List<TileNumber> tileNumbers) {

		tileNumbers.parallelStream().forEach(tile -> {

			try {

				/* construct config - TODO: deduplicate with ConvertCommand */

				var extraProperties = new HashMap<>(
						MetadataOptions.configOptionsFromMetadata(metadataOptions.metadataFile, tile));

				if (lod != null) { extraProperties.put("lod", lod.ordinal()); }
				if (loggingOptions.logDir != null) { extraProperties.put("logDir", loggingOptions.logDir.toString()); }

				O2WConfig config = configOptions.getO2WConfig(extraProperties);

				/* render the scene */

				OSMDataReaderView readerView = inputOptions.buildInput(tile);

				var o2w = new O2WConverter();
				o2w.setConfig(config);
				Scene scene = o2w.convert(readerView, null, null);

				MapProjection mapProjection = scene.getMapProjection();
				assert mapProjection != null;

				var boundsLL = tile.latLonBounds();
				var boundsXZ = AxisAlignedRectangleXZ.bbox(List.of(
						mapProjection.toXZ(boundsLL.getMin()),
						mapProjection.toXZ(boundsLL.getMax())));

				/* create glb and tileset files */

				Output output;

				if (noJson) {

					File glbFile = getTileFilename(tile, ".glb");
					output = new GltfOutput(glbFile, GLB, Compression.NONE, boundsXZ);

				} else {

					File tilesetJsonFile = getTileFilename(tile, ".tileset.json");
					output = new TilesetOutput(tilesetJsonFile, GLB, Compression.NONE, mapProjection, boundsXZ);

				}

				output.setConfiguration(config);
				output.outputScene(scene);

			} catch (IOException e) {
				// TODO handle exception
				e.printStackTrace();
			}

		});

	}

	private File getTileFilename(TileNumber tile, String extension) {
		return baseDir
				.resolve("" + tile.zoom)
				.resolve("" + tile.x)
				.resolve(tile.y + extension)
				.toFile();
	}

	private boolean fileIsMissingOrOverwritable(File file) {

		if (!file.exists()) { return true; }

		Instant fileTimestamp = Instant.ofEpochMilli(file.lastModified());

		return switch (overwriteFiles) {
			case ALWAYS -> true;
			case NEVER -> false;
			case OLDER -> fileTimestamp.isBefore(inputOptions.getInputTimestamp());
		};

	}

}
