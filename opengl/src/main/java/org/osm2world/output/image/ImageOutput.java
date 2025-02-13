package org.osm2world.output.image;

import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bboxUnion;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.union;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.MeshOutput;
import org.osm2world.output.common.mesh.Mesh;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.util.Resolution;

/**
 * renders the 3D scene to a raster image.
 * Internally uses OpenGL (through {@link JOGLOutput}) for rendering.
 * If you're exporting multiple images, consider using {@link ImageExporter} directly so you can re-use it.
 */
public class ImageOutput extends MeshOutput {

	private final File outputFile;
	private final ImageOutputFormat imageFormat;
	private final Resolution resolution;
	private final Camera camera;
	private final Projection projection;
	private final @Nullable AxisAlignedRectangleXZ dataBounds;

	public ImageOutput(File outputFile, ImageOutputFormat imageFormat, Resolution resolution, Camera camera,
			Projection projection, @Nullable AxisAlignedRectangleXZ dataBounds) {
		this.outputFile = outputFile;
		this.imageFormat = imageFormat;
		this.resolution = resolution;
		this.camera = camera;
		this.projection = projection;
		this.dataBounds = dataBounds;
	}

	@Override
	public void finish() {

		List<Mesh> meshes = getMeshes();

		try {

			AxisAlignedRectangleXZ bbox = dataBounds;

			if (bbox == null) {
				for (Mesh mesh : meshes) {
					var box = bboxUnion(mesh.geometry.asTriangles().triangles);
					bbox = bbox == null ? box : union(bbox, box);
				}
			}

			ImageExporter exporter = ImageExporter.create(
					config,
					bbox,
					target -> meshes.forEach(target::drawMesh),
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
