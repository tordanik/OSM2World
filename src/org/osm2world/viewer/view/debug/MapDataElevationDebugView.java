package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL2;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * shows the {@link MapData} with elevation information
 */
public class MapDataElevationDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows the MapData (with elevation) as a network of nodes, lines and areas";
	}
	
	private static final Color LINE_COLOR = Color.WHITE;
	private static final Color NODE_COLOR = Color.YELLOW;
	private static final Color POINT_WITH_ELE_COLOR = Color.LIGHT_GRAY;
	
	private static final float HALF_NODE_WIDTH = 0.4f;
	private static final float HALF_POINT_WITH_ELE_WIDTH = 0.2f;

	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	public void renderToImpl(GL2 gl, Camera camera) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);
		
		for (MapArea area : map.getMapAreas()) {
			if (area.getElevationProfile() == null) continue;
			
			/* draw points with non-interpolated elevation */
			
			for (VectorXYZ pointWithEle : area.getElevationProfile().getPointsWithEle()) {
				drawBoxAround(target, pointWithEle,
						POINT_WITH_ELE_COLOR, HALF_POINT_WITH_ELE_WIDTH);
			}
			
		}
				
		for (MapWaySegment line : map.getMapWaySegments()) {
			if (line.getElevationProfile() == null) continue;
			
			/* draw line itself */
			
			target.drawArrow(LINE_COLOR, 0.7f,
					line.getElevationProfile().getPointsWithEle().toArray(new VectorXYZ[0]));
			
			/* draw points with non-interpolated elevation */
			
			for (VectorXYZ pointWithEle : line.getElevationProfile().getPointsWithEle()) {
				drawBoxAround(target, pointWithEle,
						POINT_WITH_ELE_COLOR, HALF_POINT_WITH_ELE_WIDTH);
			}
			
		}
		
		for (MapNode node : map.getMapNodes()) {
			if (node.getElevationProfile() == null) continue;
			drawBoxAround(target, node.getElevationProfile().getPointWithEle(),
					NODE_COLOR, HALF_NODE_WIDTH);
		}
		
	}
	
}
