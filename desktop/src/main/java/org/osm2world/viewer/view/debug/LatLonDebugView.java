package org.osm2world.viewer.view.debug;

import static java.awt.Color.WHITE;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;

import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.Scene;

/**
 * shows the latitude and longitude grid.
 */
public class LatLonDebugView extends DebugView {

	public LatLonDebugView() {
		super("Geographic coordinate grid", "shows the latitude and longitude grid");
	}

	private static final double LINE_DIST = 1.0 / 3600;

	private MapProjection mapProjection = null;

	@Override
	public void setConversionResults(Scene conversionResults) {
		super.setConversionResults(conversionResults);
		mapProjection = conversionResults.getMapProjection();
	}

	@Override
	public boolean canBeUsed() {
		return scene != null && mapProjection != null;
	}

	@Override
	protected void updateOutput(JOGLOutput output, boolean viewChanged, Camera camera, Projection projection) {

		if (!viewChanged) return;

		AxisAlignedRectangleXZ xzBounds = scene.getBoundary();
		var bounds = LatLonBounds.ofPoints(xzBounds.vertices().stream().map(mapProjection::toLatLon).toList());

		for (int x = (int)floor(bounds.minlon / LINE_DIST); x < (int)ceil(bounds.maxlon / LINE_DIST); x++) {
			for (int z = (int)floor(bounds.minlat / LINE_DIST); z < (int)ceil(bounds.maxlat / LINE_DIST); z++) {

				int widthLat = (z % 3600 == 0) ? 6
						: (z % 60 == 0) ? 3 : 1;

				if (widthLat > 1 || camera.pos().y < 1000) {
					output.drawLineStrip(WHITE, widthLat,
							mapProjection.toXZ(z * LINE_DIST, x * LINE_DIST).xyz(0),
							mapProjection.toXZ(z * LINE_DIST, (x+1) * LINE_DIST).xyz(0));
				}

				int widthLon = (x % 3600 == 0) ? 6
						: (x % 60 == 0) ? 3 : 1;

				if (widthLon > 1 || camera.pos().y < 1000) {
					output.drawLineStrip(WHITE, widthLon,
							mapProjection.toXZ(z * LINE_DIST, x * LINE_DIST).xyz(0),
							mapProjection.toXZ((z+1) * LINE_DIST, x * LINE_DIST).xyz(0));
				}

			}
		}

	}

}
