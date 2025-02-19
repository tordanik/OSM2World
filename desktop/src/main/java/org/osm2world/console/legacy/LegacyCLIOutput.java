package org.osm2world.console.legacy;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.osm2world.console.legacy.CLIArgumentsUtil.getOutputMode;
import static org.osm2world.console.legacy.CLIArgumentsUtil.getResolution;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import org.osm2world.O2WConverter;
import org.osm2world.console.commands.mixins.CameraOptions;
import org.osm2world.console.commands.mixins.MetadataOptions;
import org.osm2world.console.legacy.CLIArgumentsUtil.OutputMode;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.CardinalDirection;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.osm.creation.OSMDataReaderView;
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
import org.osm2world.util.Resolution;

final class LegacyCLIOutput {

	private LegacyCLIOutput() {}

	public static void output(O2WConfig config,
			CLIArgumentsGroup argumentsGroup)
		throws IOException {

		CLIArguments sharedArgs = argumentsGroup.getRepresentative();

		var converter = new O2WConverter();

		/* augment and set the config */

		if (sharedArgs.isLogDir()) {
			config = config.withProperty("logDir", sharedArgs.getLogDir().toString());
		}

		MetadataOptions.addMetadataToConfig(sharedArgs.getMetadataFile(), sharedArgs.getTile(), config);

		converter.setConfig(config);

		/* create the scene */

		OSMDataReaderView osmReaderView = CLIArgumentsUtil.getOsmDataView(sharedArgs);

		Scene scene = converter.convert(osmReaderView, null, null);

		/* iterate over the sets of arguments for different outputs */

		ImageExporter exporter = null;

		for (CLIArguments args : argumentsGroup.getCLIArgumentsList()) {

			/* set camera and projection */

			MutableCamera camera;
			Projection projection;

			if (args.isPviewPos()) {

				/* perspective projection */

				MapProjection proj = scene.getMapProjection();

				LatLonEle pos = args.getPviewPos();
				LatLonEle lookAt = args.getPviewLookat();

				camera = new MutableCamera();
				VectorXYZ posXYZ = proj.toXZ(pos.lat, pos.lon).xyz(pos.ele);
				VectorXYZ lookAtXYZ = proj.toXZ(lookAt.lat, lookAt.lon).xyz(lookAt.ele);
				camera.setCamera(posXYZ, lookAtXYZ);

				projection = new PerspectiveProjection(
						args.isPviewAspect() ? args.getPviewAspect() :
								args.isResolution() ? (double) args.getResolution().getAspectRatio()
										: CameraOptions.DEFAULT_ASPECT_RATIO,
						args.getPviewFovy(),
						1, 50000);

			} else {

				/* orthographic projection */

				double angle = args.getOviewAngle();
				CardinalDirection from = args.getOviewFrom();

				AxisAlignedRectangleXZ bounds;

				if (args.isOviewBoundingBox()) {
					bounds = bbox(args.getOviewBoundingBox().stream()
							.map(scene.getMapProjection()::toXZ)
							.toList());
				} else if (args.isOviewTiles()) {
					bounds = OrthographicUtil.boundsForTiles(scene.getMapProjection(), args.getOviewTiles());
				} else if (args.isTile()) {
					bounds = OrthographicUtil.boundsForTile(scene.getMapProjection(), args.getTile());
				} else {
					bounds = scene.getBoundary();
				}

				camera = OrthographicUtil.cameraForBounds(bounds, angle, from);
				projection = OrthographicUtil.projectionForBounds(bounds, angle, from);

			}

			/* perform the actual output */

			for (File outputFile : args.getOutput()) {

				outputFile.getAbsoluteFile().getParentFile().mkdirs();

				OutputMode outputMode = CLIArgumentsUtil.getOutputMode(outputFile);

				switch (outputMode) {

					case OBJ: {
						Integer primitiveThresholdOBJ = config.getInteger("primitiveThresholdOBJ", null);
						var output = (primitiveThresholdOBJ == null)
								? new ObjOutput(outputFile, scene.getMapProjection())
								: new ObjMultiFileOutput(outputFile, scene.getMapProjection(), primitiveThresholdOBJ);
						output.setConfiguration(config);
						output.outputScene(scene);
					}
					break;

					case GLTF, GLB, GLTF_GZ, GLB_GZ: {
						AxisAlignedRectangleXZ bounds;
						if (args.isTile()) {
							bounds = OrthographicUtil.boundsForTile(scene.getMapProjection(), args.getTile());
						} else {
							bounds = scene.getBoundary();
						}
						GltfOutput.GltfFlavor gltfFlavor = EnumSet.of(OutputMode.GLB, OutputMode.GLB_GZ).contains(outputMode)
								? GltfOutput.GltfFlavor.GLB : GltfOutput.GltfFlavor.GLTF;
						Compression compression = EnumSet.of(OutputMode.GLTF_GZ, OutputMode.GLB_GZ).contains(outputMode)
								? Compression.GZ : Compression.NONE;
						GltfOutput output = new GltfOutput(outputFile, gltfFlavor, compression, bounds);
						output.setConfiguration(config);
						output.outputScene(scene);
					}
					break;

					case POV: {
						POVRayOutput output = new POVRayOutput(outputFile, camera, projection);
						output.setConfiguration(config);
						output.outputScene(scene);
					}
					break;

					case WEB_PBF, WEB_PBF_GZ: {
						AxisAlignedRectangleXZ bbox;
						if (args.isTile()) {
							bbox = OrthographicUtil.boundsForTile(scene.getMapProjection(), args.getTile());
						} else {
							bbox = scene.getBoundary();
						}
						Compression compression = outputMode == OutputMode.WEB_PBF_GZ ? Compression.GZ : Compression.NONE;
						FrontendPbfOutput output = new FrontendPbfOutput(outputFile, compression, bbox);
						output.setConfiguration(config);
						output.outputScene(scene);
					}
					break;

					case PNG:
					case PPM:
					case GD:
						if (camera == null || projection == null) {
							System.err.println("camera or projection missing");
						}
						if (exporter == null) {
							PerformanceParams performanceParams = determinePerformanceParams(config, argumentsGroup);
							exporter = ImageExporter.create(config, scene.getBoundary(),
									output -> output.outputScene(scene),
									performanceParams.resolution(), performanceParams.unbufferedRendering());
						}
						Resolution resolution = CLIArgumentsUtil.getResolution(args);
						ImageOutputFormat imageFormat = switch (outputMode) {
							case PNG -> ImageOutputFormat.PNG;
							case PPM -> ImageOutputFormat.PPM;
							case GD -> ImageOutputFormat.GD;
							default -> throw new IllegalStateException("Not an image format: " + outputMode);
						};
						exporter.writeImageFile(outputFile, imageFormat,
								resolution.width, resolution.height,
								camera, projection);
						break;

				}

			}

		}

		if (exporter != null) {
			exporter.freeResources();
			exporter = null;
		}

	}

	/** parameters for optimizing the performance of an {@link ImageExporter} */
	private record PerformanceParams(Resolution resolution, boolean unbufferedRendering) {

		public PerformanceParams (int pBufferSizeX, int pBufferSizeY, boolean unbufferedRendering) {
			this(new Resolution(pBufferSizeX, pBufferSizeY), unbufferedRendering);
		}

	}

	/**
	 * Optimizes an {@link ImageExporter}'s performance settings for use with a particular config
	 * and a particular group of files, based on a {@link CLIArgumentsGroup}.
	 *
	 * @param expectedGroup  group that should contain at least the arguments
	 *                       for the files that will later be requested.
	 *                       Basis for optimization preparations.
	 */
	private static PerformanceParams determinePerformanceParams(O2WConfig config, CLIArgumentsGroup expectedGroup) {

		int canvasLimit = config.canvasLimit();

		/* find out what number and size of image file requests to expect */

		int expectedFileCalls = 0;
		int expectedMaxSizeX = 1;
		int expectedMaxSizeY = 1;
		boolean perspectiveProjection = false;

		for (CLIArguments args : expectedGroup.getCLIArgumentsList()) {

			for (File outputFile : args.getOutput()) {
				CLIArgumentsUtil.OutputMode outputMode = getOutputMode(outputFile);
				if (outputMode == CLIArgumentsUtil.OutputMode.PNG || outputMode == CLIArgumentsUtil.OutputMode.PPM || outputMode == CLIArgumentsUtil.OutputMode.GD) {
					expectedFileCalls += 1;
					expectedMaxSizeX = max(expectedMaxSizeX, getResolution(args).width);
					expectedMaxSizeY = max(expectedMaxSizeY, getResolution(args).height);
					perspectiveProjection |= args.isPviewPos();
				}
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

		return new PerformanceParams(pBufferSizeX, pBufferSizeY, unbufferedRendering);

	}

}
