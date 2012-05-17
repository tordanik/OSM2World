package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.WorldObject;

/**
 * shows information from elevation calculation
 */
public class ClearingDebugView extends DebugView {

	private static final int LINE_WIDTH = 5;
	private static final float HALF_NODE_WIDTH = 0.4f;
	private static final int NODE_COLUMN_WIDTH = 5;
	
	private static final Color LINE_SURFACE_COLOR = Color.LIGHT_GRAY;
	private static final Color LINE_BELOW_COLOR = Color.YELLOW;
	private static final Color LINE_ABOVE_COLOR = Color.BLUE;

	@Override
	public void renderToImpl(GL gl, Camera camera, Projection projection) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);

		for (MapWaySegment line : map.getMapWaySegments()) {

			for (WorldObject rep : line.getRepresentations()) {

				WaySegmentElevationProfile profile = line.getElevationProfile();
				List<VectorXYZ> pointsWithEle = profile.getPointsWithEle();
				int size = pointsWithEle.size();

				VectorXYZ[] linePoints = new VectorXYZ[size];
				VectorXYZ[] upperClearingPoints = new VectorXYZ[size];
				VectorXYZ[] lowerClearingPoints = new VectorXYZ[size];

				VectorXYZ[] clearingPolygonPoints = new VectorXYZ[2*size];

				for (int i = 0; i < size; i++) {
					VectorXYZ p = pointsWithEle.get(i);

					linePoints[i] = p;

					final VectorXYZ pMin = p.y(p.y-rep.getClearingBelow(p.xz()));
					final VectorXYZ pMax = p.y(p.y+rep.getClearingAbove(p.xz()));

					upperClearingPoints[i] = pMin;
					lowerClearingPoints[size-1-i] = pMax;

					clearingPolygonPoints[i] = pMin;
					clearingPolygonPoints[2*size-1-i] = pMax;

					//TODO: this isn't necessarily the maximum clearing precision!

				}

				Color color = getColorForState(rep.getGroundState());

				target.drawLineStrip(color, LINE_WIDTH, linePoints);
				target.drawLineStrip(color, upperClearingPoints);
				target.drawLineStrip(color, lowerClearingPoints);

				gl.glEnable(GL.GL_POLYGON_STIPPLE);
				gl.glPolygonStipple(STIPPLE_PATTERN, 0);
				target.drawPolygon(color, clearingPolygonPoints);
				gl.glDisable(GL.GL_POLYGON_STIPPLE);

			}
			
		}

		for (MapNode node : map.getMapNodes()) {
			
			for (NodeWorldObject rep : node.getRepresentations()) {
				
				NodeElevationProfile profile = node.getElevationProfile();
				Color color = getColorForState(rep.getGroundState());
				VectorXYZ p = profile.getPointWithEle();
				
				drawBoxAround(target, profile.getPointWithEle(), color, HALF_NODE_WIDTH);
				
				target.drawLineStrip(color, NODE_COLUMN_WIDTH,
						p.y(p.y-rep.getClearingBelow(p.xz())),
						p.y(p.y+rep.getClearingAbove(p.xz())));
				
			}
			
		}

	}
	
	private static Color getColorForState(GroundState state) {
		if (state == GroundState.ABOVE) {
			return LINE_ABOVE_COLOR;
		} else if (state == GroundState.BELOW) {
			return LINE_BELOW_COLOR;
		} else {
			return LINE_SURFACE_COLOR;
		}
	}
		
	private static final byte STIPPLE_PATTERN[] =
	  { (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x11, (byte) 0x11,
	    (byte) 0x11, (byte) 0x11, (byte) 0x88, (byte) 0x88, (byte) 0x88,
	    (byte) 0x88, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x11,
	    (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x11, (byte) 0x11, (byte) 0x11,
	    (byte) 0x11, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88,
	    (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x11, (byte) 0x11,
	    (byte) 0x11, (byte) 0x11, (byte) 0x88, (byte) 0x88, (byte) 0x88,
	    (byte) 0x88, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x11,
	    (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x11, (byte) 0x11, (byte) 0x11,
	    (byte) 0x11, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88,
	    (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x11, (byte) 0x11,
	    (byte) 0x11, (byte) 0x11, (byte) 0x88, (byte) 0x88, (byte) 0x88,
	    (byte) 0x88, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x11,
	    (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x11, (byte) 0x11, (byte) 0x11,
	    (byte) 0x11, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88,
	    (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x88,
	    (byte) 0x88, (byte) 0x88, (byte) 0x88 };
	
}
