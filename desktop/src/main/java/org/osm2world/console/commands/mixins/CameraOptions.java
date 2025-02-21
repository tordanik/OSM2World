package org.osm2world.console.commands.mixins;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.geo.CardinalDirection;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.TileNumber;

import picocli.CommandLine;

/**
 * options describing a camera: either {@link OviewOptions} or {@link PviewOptions}
 */
public class CameraOptions {

	public static final double DEFAULT_ASPECT_RATIO = 4 / 3.0;

	public sealed interface ViewOptions { }

	/**
	 * options for an orthographic view camera
	 */
	public static final class OviewOptions implements ViewOptions {

		@CommandLine.Option(names = {"--oview.angle"}, description = "downwards angle of orthographic view in degrees")
		public double angle = 30;

		@CommandLine.Option(names = {"--oview.from"}, description = "direction from which the orthographic view is rendered",
				paramLabel = "[NESW]")
		public CardinalDirection from = CardinalDirection.S;

		@CommandLine.Option(names = {"--oview.bbox"}, arity = "2..*", paramLabel="lat,lon",
				description = "bounding box for orthographic view")
		public List<LatLon> bbox;

		@CommandLine.Option(names = {"--oview.tiles"}, arity = "1..*", paramLabel="zoom,x,y",
				description = "tiles defining a bounding box for orthographic view")
		public List<TileNumber> tiles;

	}

	/**
	 * options for a perspective view camera
	 */
	public static final class PviewOptions implements ViewOptions {

		@CommandLine.Option(names = {"--pview.pos"}, description = "lat,lon,ele of camera position for perspective view",
				required = true)
		public LatLonEle pos;

		@CommandLine.Option(names = {"--pview.lookAt"}, description = "lat,lon,ele of camera look-at for perspective view",
				required = true)
		public LatLonEle lookAt;

		@CommandLine.Option(names = {"--pview.fovy"}, description = "vertical field of view angle for perspective view, in degrees")
		public double fovy = 45;

		@CommandLine.Option(names = {"--pview.aspect"}, description = "aspect ratio (width / height) for perspective view")
		public Double aspect = null;

	}

	@CommandLine.ArgGroup(exclusive = false)
	@Nullable
	public OviewOptions oviewOptions = null;

	@CommandLine.ArgGroup(exclusive = false)
	@Nullable
	public PviewOptions pviewOptions = null;

}
