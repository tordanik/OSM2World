package org.osm2world.viewer.view.debug;

import static java.awt.Color.WHITE;
import static java.lang.Math.*;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * shows the latitude and longitude grid.
 */
public class LatLonDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows the latitude and longitude grid";
	}

	private static final double LINE_DIST = 1.0 / 3600;

	private MapProjection mapProjection = null;

	@Override
	public void setConversionResults(Results conversionResults) {
		super.setConversionResults(conversionResults);
		mapProjection = conversionResults.getMapProjection();
	}

	@Override
	public boolean canBeUsed() {
		return map != null && mapProjection != null;
	}

	@Override
	public void fillTarget(JOGLTarget target) {

		AxisAlignedRectangleXZ bound = map.getDataBoundary();

		double minLon = toDegrees(mapProjection.toLon(new VectorXZ(bound.minX, bound.minZ)));
		double minLat = toDegrees(mapProjection.toLat(new VectorXZ(bound.minX, bound.minZ)));
		double maxLon = toDegrees(mapProjection.toLon(new VectorXZ(bound.maxX, bound.maxZ)));
		double maxLat = toDegrees(mapProjection.toLat(new VectorXZ(bound.maxX, bound.maxZ)));

		for (int x = (int)floor(minLon / LINE_DIST); x < (int)ceil(maxLon / LINE_DIST); x++) {
			for (int z = (int)floor(minLat / LINE_DIST); z < (int)ceil(maxLat / LINE_DIST); z++) {

				int widthLat = (z % 3600 == 0) ? 6
						: (z % 60 == 0) ? 3 : 1;

				if (widthLat > 1 || camera.getPos().y < 1000) {
					target.drawLineStrip(WHITE, widthLat,
							mapProjection.toXZ(z * LINE_DIST, x * LINE_DIST).xyz(0),
							mapProjection.toXZ(z * LINE_DIST, (x+1) * LINE_DIST).xyz(0));
				}

				int widthLon = (x % 3600 == 0) ? 6
						: (x % 60 == 0) ? 3 : 1;

				if (widthLon > 1 || camera.getPos().y < 1000) {
					target.drawLineStrip(WHITE, widthLon,
							mapProjection.toXZ(z * LINE_DIST, x * LINE_DIST).xyz(0),
							mapProjection.toXZ((z+1) * LINE_DIST, x * LINE_DIST).xyz(0));
				}

			}
		}

	}

}
