package org.osm2world.console.commands.mixins;

import static java.lang.Math.*;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.*;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.rendering.MutableCamera;
import org.osm2world.output.common.rendering.OrthographicUtil;
import org.osm2world.output.common.rendering.PerspectiveProjection;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.util.Resolution;

import picocli.CommandLine;

/**
 * options describing a camera: either {@link OviewOptions} or {@link PviewOptions}
 */
public class CameraOptions {

	public static final double DEFAULT_ASPECT_RATIO = 4 / 3.0;

	/**
	 * options for an orthographic view camera
	 */
	public static class OviewOptions {

		@CommandLine.Option(names = {"--oview.angle"}, description = "downwards angle of orthographic view in degrees",
				defaultValue = "30")
		double angle;

		@CommandLine.Option(names = {"--oview.from"}, description = "direction from which the orthographic view is rendered",
				paramLabel = "[NESW]", defaultValue = "S")
		CardinalDirection from;

		@CommandLine.Option(names = {"--oview.bbox"}, arity = "2..*",
				description = "lat,lon pairs defining a bounding box for orthographic view")
		List<LatLon> bbox;

		@CommandLine.Option(names = {"--oview.tiles"}, arity = "1..*",
				description = "zoom,x,y triples of tiles defining a bounding box for orthographic view")
		List<TileNumber> tiles;

		public Pair<MutableCamera, Projection> createCameraAndProjection(MapProjection mapProjection,
				@Nullable AxisAlignedRectangleXZ defaultBounds) {

			AxisAlignedRectangleXZ bounds;

			if (bbox != null) {
				bounds = bbox(bbox.stream().map(mapProjection::toXZ).toList());
			} else if (tiles != null) {
				bounds = OrthographicUtil.boundsForTiles(mapProjection, tiles);
			} else {
				bounds = defaultBounds;
			}

			MutableCamera camera = OrthographicUtil.cameraForBounds(bounds, angle, from);
			Projection projection = OrthographicUtil.projectionForBounds(bounds, angle, from);

			return Pair.of(camera, projection);

		}

	}

	/**
	 * options for a perspective view camera
	 */
	public static class PviewOptions {

		@CommandLine.Option(names = {"--pview.pos"}, description = "lat,lon,ele of camera position for perspective view",
				required = true)
		LatLonEle pos;

		@CommandLine.Option(names = {"--pview.lookAt"}, description = "lat,lon,ele of camera look-at for perspective view",
				required = true)
		LatLonEle lookAt;

		@CommandLine.Option(names = {"--pview.fovy"}, description = "vertical field of view angle for perspective view, in degrees",
				defaultValue = "45")
		double fovy;

		@CommandLine.Option(names = {"--pview.aspect"}, description = "aspect ratio (width / height) for perspective view")
		Double aspect = null;

		public Pair<MutableCamera, Projection> createCameraAndProjection(MapProjection mapProjection,
				@Nullable Resolution resolution) {

			MutableCamera camera = new MutableCamera();
			VectorXYZ posXYZ = mapProjection.toXZ(pos.lat, pos.lon).xyz(pos.ele);
			VectorXYZ lookAtXYZ = mapProjection.toXZ(lookAt.lat, lookAt.lon).xyz(lookAt.ele);
			camera.setCamera(posXYZ, lookAtXYZ);

			Projection projection = new PerspectiveProjection(
					aspect != null ? aspect :
							resolution != null ? (double) resolution.getAspectRatio()
									: DEFAULT_ASPECT_RATIO,
					fovy, 1, 50000);

			return Pair.of(camera, projection);

		}

	}

	@CommandLine.ArgGroup(exclusive = false)
	@Nullable OviewOptions oviewOptions = null;

	@CommandLine.ArgGroup(exclusive = false)
	@Nullable PviewOptions pviewOptions = null;

	@CommandLine.Option(names = {"--resolution"}, description = "output size in pixels",
			defaultValue = "800,600", paramLabel = "w,h")
	@Nullable
	Resolution resolution = null;

	public Resolution getResolution() {

		double aspectRatio = pviewOptions != null
				? requireNonNullElse(pviewOptions.aspect, DEFAULT_ASPECT_RATIO)
				: 1.0 / sin(toRadians(oviewOptions.angle));

		return requireNonNullElse(resolution,
				new Resolution(800, (int) round(800 / aspectRatio)));

	}

	public Pair<MutableCamera, Projection> createCameraAndProjection(MapProjection mapProjection,
			AxisAlignedRectangleXZ defaultBounds) {

		if (pviewOptions != null) {
			return pviewOptions.createCameraAndProjection(mapProjection, resolution);
		} {
			return oviewOptions.createCameraAndProjection(mapProjection, defaultBounds);
		}

	}

}
