package org.osm2world.console.commands;

import static org.osm2world.output.gltf.GltfOutput.GltfFlavor.GLB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.tileset.TilesetOutput;
import org.osm2world.scene.Scene;
import org.osm2world.scene.mesh.LevelOfDetail;

import picocli.CommandLine;

@CommandLine.Command(name = "tileset", description = "Create 3D Tiles for some geographic area.")
public class TilesetCommand implements Callable<Integer> {

	private static class Bounds {

		@CommandLine.Option(names = {"--bbox"}, arity = "2..*", paramLabel="lat,lon",
				description="area to create tiles for", required = true)
		List<LatLon> bboxPoints = List.of();

		@CommandLine.Option(names = {"--bboxTiles"}, arity = "1.*", paramLabel = "zoom,x,y",
				description = "area to create tiles for", required = true)
		List<TileNumber> bboxTiles = List.of();

		public LatLonBounds constructBbox() {

			List<LatLonBounds> bounds = new ArrayList<>();

			if (bboxPoints.size() > 1) {
				bounds.add(LatLonBounds.ofPoints(bboxPoints));
			}

			if (!bboxTiles.isEmpty()) {
				bboxTiles.forEach(tile -> bounds.add(tile.latLonBounds()));
			}

			return bounds.isEmpty() ? null : LatLonBounds.union(bounds);

		}

	}

	@CommandLine.Option(names = {"--baseDir"}, description = "base directory for output files", required = true,
			paramLabel = "<path>")
	Path baseDir;

	@CommandLine.Option(names = {"--lod"}, description = "level of detail of the output, given as a number between 0 and 4",
			paramLabel="<number>")
	@Nullable
	LevelOfDetail lod = null;

	@CommandLine.ArgGroup(multiplicity = "1")
	Bounds bounds;

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

		LatLonBounds bbox = bounds.constructBbox();

		if (bbox == null) {
			System.err.println("Not enough parameters to construct bbox");
			return 1;
		}

		// shrink bounds a tiny bit to prevent the neighboring tiles from being generated as well
		bbox = new LatLonBounds(bbox.minlat + 1e-5, bbox.minlon + 1e-5,
				bbox.maxlat - 1e-5, bbox.maxlon - 1e-5);

		List<TileNumber> tileNumbers = TileNumber.tilesForBounds(ZOOM, bbox);

		createTiles(tileNumbers);

		// TODO merge tilesets

		return 0;

	}

	private void createTiles(List<TileNumber> tileNumbers) {

		for (TileNumber tile : tileNumbers) {

			try {

				/* construct config - TODO: deduplicate with ConvertCommand */

				var extraProperties = new HashMap<>(
						MetadataOptions.configOptionsFromMetadata(metadataOptions.metadataFile, tile));

				if (lod != null) { extraProperties.put("lod", lod.ordinal()); }
				if (loggingOptions.logDir != null) { extraProperties.put("logDir", loggingOptions.logDir.toString()); }

				O2WConfig config = configOptions.getO2WConfig(extraProperties);

				/* create glb and tileset files */

				File tilesetJsonFile = baseDir
						.resolve("" + tile.zoom)
						.resolve("" + tile.x)
						.resolve(tile.y + ".tileset.json")
						.toFile();

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

				var output = new TilesetOutput(tilesetJsonFile, GLB, Compression.NONE, mapProjection, boundsXZ);
				output.setConfiguration(config);

				output.outputScene(scene);

			} catch (IOException e) {
				// TODO handle exception
				e.printStackTrace();
			}

		}

	}

}
