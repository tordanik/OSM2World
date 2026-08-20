package org.osm2world.console.commands;

import static org.osm2world.output.gltf.GltfFlavor.GLB;
import static org.osm2world.output.tileset.TilesetTreeUtil.generateTilesetTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.osm2world.O2WConverter;
import org.osm2world.console.commands.mixins.ConfigOptions;
import org.osm2world.console.commands.mixins.InputOptions;
import org.osm2world.console.commands.mixins.LoggingOptions;
import org.osm2world.console.commands.mixins.MetadataOptions;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.geo.*;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.output.Output;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.gltf.GltfOutput;
import org.osm2world.output.tileset.TilesetOutput;
import org.osm2world.output.tileset.TilesetTreeUtil;
import org.osm2world.scene.Scene;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.util.exception.InvalidGeometryException;

import me.tongfei.progressbar.ProgressBar;
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

	@CommandLine.Option(names = {"--lod"}, description = "level of detail of the output",
			paramLabel="[01234]", arity="1..")
	List<LevelOfDetail> lod = List.of(LevelOfDetail.LOD4);

	@CommandLine.Option(names = {"--noJson"}, description = "skip creation of tileset.json files")
	boolean noJson = false;

	@CommandLine.Option(names = {"--precompressedTiles"},
			description = "store tiles with .gz compression, but reference their uncompressed names in tilesets")
	boolean precompressedTiles = false;

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

		/* determine the list of tiles which should exist in this tileset */

		List<TileNumber> tileNumbers = new ArrayList<>();

		for (var b : bounds) {

			LatLonBounds bbox = b.constructBbox().latLonBounds();

			// shrink bounds a tiny bit to prevent the neighboring tiles from being generated as well
			bbox = new LatLonBounds(bbox.minlat + 1e-5, bbox.minlon + 1e-5,
					bbox.maxlat - 1e-5, bbox.maxlon - 1e-5);

			tileNumbers.addAll(TileNumber.tilesForBounds(ZOOM, bbox));

		}

		/* create the tiles for this tileset (unless they already exist and should not be overwritten */

		List<TileNumber> filteredTileNumbers = filterTileNumbers(tileNumbers);

		int skippedTiles = tileNumbers.size() - filteredTileNumbers.size();
		if (skippedTiles > 0) {
			System.out.println("Skipping " + skippedTiles + " existing tiles");
		}

		createTiles(filteredTileNumbers);

		/* create the tileset.json files tying the individual tiles together.
		 * This happens after the tiles because it uses the bounds of the actual tile content. */

		if (!noJson) {
			try {
				System.out.println("Generating JSON tileset tree...");
				generateTilesetTree(baseDir, tileNumbers, lod);
			} catch (IOException e) {
				System.err.println("Error writing tileset json files: " + e.getMessage());
				return 1;
			}
		}

		System.out.println("Tileset generation complete.");

		return 0;

	}

	private List<TileNumber> filterTileNumbers(List<TileNumber> tileNumbers) {

		List<TileNumber> result = new ArrayList<>();

		for (TileNumber tile : tileNumbers) {
			for (LevelOfDetail lod : this.lod) {
				if (fileIsMissingOrOverwritable(TilesetTreeUtil.tilePath(baseDir, tile, lod, ".glb" + getCompression().extension()))) {
					if (fileIsMissingOrOverwritable(TilesetTreeUtil.tilePath(baseDir, tile, lod, ".tileset.json"))) {
						result.add(tile);
						break;
					}
				}
			}
		}

		return result;

	}

	private void createTiles(List<TileNumber> tileNumbers) {

		if (tileNumbers.isEmpty()) { return; }

		var completedTiles = new AtomicInteger(0);
		try (var pb = new ProgressBar("Generate tiles", tileNumbers.size())) {

			tileNumbers.parallelStream().forEach(tile -> {

				for (LevelOfDetail lod : this.lod) {

					try {

						/* construct config - TODO: deduplicate with ConvertCommand */

						var extraProperties = new HashMap<>(metadataOptions.configOptionsFromMetadata(tile));

						extraProperties.put("lod", lod.ordinal());

						if (loggingOptions.logDir != null) {
							extraProperties.put("logDir", loggingOptions.logDir.toString());
						}

						O2WConfig config = configOptions.getO2WConfig(extraProperties);

						/* Set some default values specific to the tileset command */

						if (!config.containsKey("keepOsmElements")) {
							config = config.withProperty("keepOsmElements", "false");
						}
						if (!config.containsKey("clipToBounds")) {
							config = config.withProperty("clipToBounds", "true");
						}
						if (!config.containsKey("gltfExtensionWhitelist")) {
							// 3D Tiles clients can be expected to support this, and tile size matters a lot here
							config = config.withProperty("gltfExtensionWhitelist", GltfOutput.KHR_MESH_QUANTIZATION);
						}

						/* render the scene */

						OSMDataReaderView readerView = inputOptions.buildInput(tile);

						var o2w = new O2WConverter();
						o2w.setConfig(config);
						Scene scene = o2w.convert(readerView, null, null);

						MapProjection mapProjection = scene.getMapProjection();
						assert mapProjection != null;

						/* create glb and tileset files */

						Output output;

						if (noJson) {

							File glbFile = TilesetTreeUtil.tilePath(baseDir, tile, lod, ".glb" + getCompression().extension()).toFile();
							output = new GltfOutput(glbFile, GLB, null);

						} else {

							File tilesetJsonFile = TilesetTreeUtil.tilePath(baseDir, tile, lod, ".tileset.json").toFile();
							output = new TilesetOutput(tilesetJsonFile, GLB, getCompression(), mapProjection, scene.getBoundary());

						}

						output.setConfiguration(config);
						output.outputScene(scene);

					} catch (IOException | InvalidGeometryException e) {
						System.err.println("Failed to create tile " + tile + " at " + lod + ": " + e.getMessage());
					}

				}

				pb.stepTo(completedTiles.incrementAndGet());

			});

		}
	}

	private Compression getCompression() {
		return precompressedTiles ? Compression.GZ : Compression.NONE;
	}

	private boolean fileIsMissingOrOverwritable(Path path) {

		if (!path.toFile().exists()) { return true; }

		Instant fileTimestamp = Instant.ofEpochMilli(path.toFile().lastModified());

		return switch (overwriteFiles) {
			case ALWAYS -> true;
			case NEVER -> false;
			case OLDER -> fileTimestamp.isBefore(inputOptions.getInputTimestamp());
		};

	}

}
