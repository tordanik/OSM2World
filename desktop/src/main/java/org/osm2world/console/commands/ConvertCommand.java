package org.osm2world.console.commands;

import static java.lang.Math.*;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.console.commands.mixins.CameraOptions.*;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.output.gltf.GltfOutput.GltfFlavor;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.osm2world.O2WConverter;
import org.osm2world.console.commands.mixins.*;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.*;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.osm.creation.*;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.common.rendering.MutableCamera;
import org.osm2world.output.common.rendering.OrthographicUtil;
import org.osm2world.output.common.rendering.PerspectiveProjection;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.frontend_pbf.FrontendPbfOutput;
import org.osm2world.output.gltf.GltfOutput;
import org.osm2world.output.image.ImageExporter;
import org.osm2world.output.image.ImageOutputFormat;
import org.osm2world.output.obj.ObjMultiFileOutput;
import org.osm2world.output.obj.ObjOutput;
import org.osm2world.output.povray.POVRayOutput;
import org.osm2world.scene.Scene;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.util.Resolution;

import picocli.CommandLine;

@Command(name = "convert", description = "Convert OSM data to 3D models and output the result.")
public class ConvertCommand implements Callable<Integer> {

	@Option(names = {"--output", "-o"}, description = "output files", arity = "1..", required = true,
			paramLabel = "<path>")
	List<File> outputFiles;

	@Option(names = {"--tile"}, description = "the tile to convert", paramLabel = "zoom,x,y")
	@Nullable
	TileNumber tile;

	@Option(names = {"--lod"}, description = "level of detail of the output, given as a number between 0 and 4",
			paramLabel="<number>")
	@Nullable
	LevelOfDetail lod = null;

	@CommandLine.Option(names = {"--resolution"}, description = "output size in pixels",
			paramLabel = "w,h")
	@Nullable
	Resolution resolution = null;

	@Option(names = {"--input_bbox"}, arity = "2..*", paramLabel="lat,lon",
			description="input bounding box (does not work with all data sources)")
	@Nullable
	List<LatLon> inputBbox;

	@Option(names = {"--input_query"}, description = "Overpass API query string", paramLabel = "<query>")
	@Nullable String inputQuery = null;

	@CommandLine.ArgGroup()
	@Nullable CameraOptions cameraOptions = null;

	/* mixins */

	@CommandLine.Mixin
	private InputOptions inputOptions;

	@CommandLine.Mixin
	private ConfigOptions configOptions;

	@CommandLine.Mixin
	private LoggingOptions loggingOptions;

	@CommandLine.Mixin
	private MetadataOptions metadataOptions;

	private record SceneInputs(OSMDataReaderView osmReaderView, List<File> configFiles,
							   Map<String, Object> extraProperties) {}
	private final Map<SceneInputs, Scene> cachedScenes = new HashMap<>();

	@Override
	public Integer call() throws Exception {

		var converter = new O2WConverter();

		/* validate arguments */

		String errorString = getErrorString();

		if (errorString != null) {
			System.err.println(errorString);
			return 1;
		}

		/* augment and set the config */

		var extraProperties = new HashMap<>(
				MetadataOptions.configOptionsFromMetadata(metadataOptions.metadataFile, tile));

		if (lod != null) { extraProperties.put("lod", lod.ordinal()); }
		if (loggingOptions.logDir != null) { extraProperties.put("logDir", loggingOptions.logDir.toString()); }

		O2WConfig config = configOptions.getO2WConfig(extraProperties);
		converter.setConfig(config);

		/* create the scene (or use a cached scene) */

		OSMDataReaderView osmReaderView = buildInput();

		// TODO: enable sharing (most) scene calculations across different LOD
		var sceneKey = new SceneInputs(osmReaderView, configOptions.getConfigFiles(), extraProperties);

		Scene scene = cachedScenes.containsKey(sceneKey)
				? cachedScenes.get(sceneKey)
				: converter.convert(osmReaderView, null, null);
		cachedScenes.put(sceneKey, scene);

		MapProjection proj = scene.getMapProjection();
		assert proj != null;

		ImageExporter exporter = null;

		{ // TODO: support sharing ImageExporter across multiple outputs

			/* set camera and projection */

			MutableCamera camera;
			Projection projection;

			ViewOptions viewOptions = getCameraViewOptions(cameraOptions);

			if (viewOptions instanceof PviewOptions pView) {

				/* perspective projection */

				LatLonEle pos = pView.pos;
				LatLonEle lookAt = pView.lookAt;

				camera = new MutableCamera();
				VectorXYZ posXYZ = proj.toXZ(pos.lat, pos.lon).xyz(pos.ele);
				VectorXYZ lookAtXYZ = proj.toXZ(lookAt.lat, lookAt.lon).xyz(lookAt.ele);
				camera.setCamera(posXYZ, lookAtXYZ);

				projection = new PerspectiveProjection(
						pView.aspect != null ? pView.aspect :
								resolution != null ? (double) resolution.getAspectRatio()
										: DEFAULT_ASPECT_RATIO,
						pView.fovy, 1, 50000);

			} else {

				/* orthographic projection */

				OviewOptions oView = (OviewOptions) viewOptions;

				double angle = oView.angle;
				CardinalDirection from = oView.from;

				AxisAlignedRectangleXZ bounds;

				if (oView.bbox != null && !oView.bbox.isEmpty()) {
					bounds = bbox(oView.bbox.stream().map(proj::toXZ).toList());
				} else if (oView.tiles != null && !oView.tiles.isEmpty()) {
					bounds = OrthographicUtil.boundsForTiles(proj, oView.tiles);
				} else if (tile != null) {
					bounds = OrthographicUtil.boundsForTile(proj, tile);
				} else {
					bounds = scene.getBoundary();
				}

				camera = OrthographicUtil.cameraForBounds(bounds, angle, from);
				projection = OrthographicUtil.projectionForBounds(bounds, angle, from);

			}

			/* perform the actual output */

			for (File outputFile : outputFiles) {

				outputFile.getAbsoluteFile().getParentFile().mkdirs();

				String fileName = outputFile.getName().toUpperCase();
				fileName = fileName.replaceAll("O2W\\.PBF", "O2W_PBF");

				Compression compression = Compression.NONE;
				for (Compression c : EnumSet.of(Compression.GZ, Compression.ZIP)) {
					if (fileName.endsWith("." + c)) {
						compression = c;
						fileName = fileName.replaceAll("\\." + c + "$", "");
					}
				}

				String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

				switch (extension) {

					case "OBJ" -> {
						Integer primitiveThresholdOBJ = config.getInteger("primitiveThresholdOBJ", null);
						var output = (primitiveThresholdOBJ == null)
								? new ObjOutput(outputFile, scene.getMapProjection())
								: new ObjMultiFileOutput(outputFile, scene.getMapProjection(), primitiveThresholdOBJ);
						output.setConfiguration(config);
						output.outputScene(scene);
					}

					case "GLTF", "GLB" -> {
						AxisAlignedRectangleXZ bounds;
						if (tile != null) {
							bounds = OrthographicUtil.boundsForTile(scene.getMapProjection(), tile);
						} else {
							bounds = scene.getBoundary();
						}
						GltfFlavor gltfFlavor = GltfFlavor.valueOf(extension);
						GltfOutput output = new GltfOutput(outputFile, gltfFlavor, compression, bounds);
						output.setConfiguration(config);
						output.outputScene(scene);
					}

					case "POV" -> {
						POVRayOutput output = new POVRayOutput(outputFile, camera, projection);
						output.setConfiguration(config);
						output.outputScene(scene);
					}

					case "O2W_PBF" -> {
						AxisAlignedRectangleXZ bbox;
						if (tile != null) {
							bbox = OrthographicUtil.boundsForTile(scene.getMapProjection(), tile);
						} else {
							bbox = scene.getBoundary();
						}
						FrontendPbfOutput output = new FrontendPbfOutput(outputFile, compression, bbox);
						output.setConfiguration(config);
						output.outputScene(scene);
					}

					case "PNG", "PPM", "GD" -> {
						if (exporter == null) {
							ImageExporter.PerformanceParams performanceParams = determineImageExporterParams(config);
							exporter = ImageExporter.create(config, scene.getBoundary(),
									output -> output.outputScene(scene),
									performanceParams.resolution(), performanceParams.unbufferedRendering());
						}
						Resolution resolution = determineResolution();
						ImageOutputFormat imageFormat = ImageOutputFormat.valueOf(extension) ;
						exporter.writeImageFile(outputFile, imageFormat,
								resolution.width, resolution.height,
								camera, projection);
					}

					default -> System.err.println(
							"Cannot determine output type for output file, skipping it: " + outputFile);

				}

			}

		}

		if (exporter != null) {
			exporter.freeResources();
		}

		return 0;

	}

	/** validates the arguments and returns an error string iff they are incorrect */
	private @Nullable String getErrorString() {

		switch (inputOptions.inputMode) {

			case FILE -> {
				if (inputOptions.input == null) {
					return "input file parameter is required (or choose a different input mode)";
				} else {
					OSMDataReaderView input = buildInput();
					if (!(input.reader instanceof OSMFileReader) && inputBbox == null && tile == null) {
						return "a tile number or input bounding box is required for database input files";
					}
				}
			}

			case OVERPASS -> {
				if (inputQuery == null && inputBbox == null && tile == null) {
					return "either a bounding box, a tile, or a query string is required for Overpass";
				}
			}

		}

		if (getCameraViewOptions(cameraOptions) instanceof OviewOptions oView
				&& oView.tiles != null && oView.bbox != null) {
			return "define *either* tiles or bounding box for orthographic view";
		}

		return null;

	}


	private OSMDataReaderView buildInput() {

		OSMDataReader dataReader = switch (inputOptions.inputMode) {

			case FILE -> {
				File inputFile = inputOptions.input;
				String inputName = inputFile.getName();
				if (inputName.endsWith(".mbtiles")) {
					yield new MbtilesReader(inputFile);
				} else if (inputName.endsWith(".gol")) {
					yield new GeodeskReader(inputFile);
				} else {
					yield new OSMFileReader(inputFile);
				}
			}

			case OVERPASS -> new OverpassReader(inputOptions.overpassURL);

		};

		if (inputBbox != null) {
			return new OSMDataReaderView(dataReader, LatLonBounds.ofPoints(inputBbox));
		} else if (tile != null) {
			return new OSMDataReaderView(dataReader, tile);
		} else if (inputQuery != null && dataReader instanceof OverpassReader overpassReader) {
			return new OSMDataReaderView(overpassReader, inputQuery);
		} else {
			return new OSMDataReaderView(dataReader);
		}

	}

	private ViewOptions getCameraViewOptions(@Nullable CameraOptions cameraOptions) {
		if (cameraOptions != null) {
			return cameraOptions.oviewOptions != null ? cameraOptions.oviewOptions : cameraOptions.pviewOptions;
		} else {
			return new OviewOptions();
		}
	}

	private Resolution determineResolution() {

		ViewOptions viewOptions = getCameraViewOptions(cameraOptions);

		double oviewAngle = viewOptions instanceof OviewOptions oView
				? oView.angle
				: new OviewOptions().angle;

		double aspectRatio = viewOptions instanceof PviewOptions pView
				? requireNonNullElse(pView.aspect, DEFAULT_ASPECT_RATIO)
				: 1.0 / sin(toRadians(oviewAngle));

		return requireNonNullElse(resolution,
				new Resolution(800, (int) round(800 / aspectRatio)));

	}

	/**
	 * Optimizes an {@link ImageExporter}'s performance settings for use with a particular config
	 * and a particular group of outputs.
	 */
	private ImageExporter.PerformanceParams determineImageExporterParams(O2WConfig config) {

		int canvasLimit = config.canvasLimit();

		/* find out what number and size of image file requests to expect */

		int expectedFileCalls = 0;
		int expectedMaxSizeX = 1;
		int expectedMaxSizeY = 1;
		boolean perspectiveProjection = false;

		for (File outputFile : outputFiles) {

			if (stream(ImageOutputFormat.values()).anyMatch(it ->
					outputFile.getName().toUpperCase().contains("." + it))) {
				expectedFileCalls += 1;
				expectedMaxSizeX = max(expectedMaxSizeX, determineResolution().width);
				expectedMaxSizeY = max(expectedMaxSizeY, determineResolution().height);
				perspectiveProjection |= getCameraViewOptions(cameraOptions) instanceof PviewOptions;
			}

		}

		boolean onlyOneRenderPass = (expectedFileCalls <= 1
				&& expectedMaxSizeX <= canvasLimit
				&& expectedMaxSizeY <= canvasLimit);

		/* call the constructor */

		boolean unbufferedRendering = onlyOneRenderPass
				|| config.getBoolean("forceUnbufferedPNGRendering", false);

		int pBufferSizeX, pBufferSizeY;

		if (perspectiveProjection) {
			pBufferSizeX = expectedMaxSizeX;
			pBufferSizeY = expectedMaxSizeY;
		} else {
			pBufferSizeX = min(canvasLimit, expectedMaxSizeX);
			pBufferSizeY = min(canvasLimit, expectedMaxSizeY);
		}

		return new ImageExporter.PerformanceParams(pBufferSizeX, pBufferSizeY, unbufferedRendering);

	}

}
