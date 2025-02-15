package org.osm2world.output.image;

import java.io.File;
import java.io.IOException;

import org.osm2world.output.common.AbstractOutput;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.Scene;
import org.osm2world.util.Resolution;

/**
 * renders the 3D scene to a raster image.
 * Internally uses OpenGL (through {@link JOGLOutput}) for rendering.
 * If you're exporting multiple images, consider using {@link ImageExporter} directly so you can re-use it.
 */
public class ImageOutput extends AbstractOutput {

	private final File outputFile;
	private final ImageOutputFormat imageFormat;
	private final Resolution resolution;
	private final Camera camera;
	private final Projection projection;

	public ImageOutput(File outputFile, ImageOutputFormat imageFormat, Resolution resolution, Camera camera,
			Projection projection) {
		this.outputFile = outputFile;
		this.imageFormat = imageFormat;
		this.resolution = resolution;
		this.camera = camera;
		this.projection = projection;
	}

	@Override
	public void outputScene(Scene scene) {

		try {

			ImageExporter exporter = ImageExporter.create(
					config,
					scene.getBoundary(),
					output -> output.outputScene(scene),
					resolution);

			exporter.writeImageFile(outputFile, imageFormat,
					resolution.width, resolution.height,
					camera, projection);

			exporter.freeResources();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
