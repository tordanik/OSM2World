package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;

public class TerrainBoundaryDebugView extends DebugView {

	private static final Color NODE_BOUNDARY_COLOR = Color.YELLOW;
	private static final Color WAY_BOUNDARY_COLOR = Color.GREEN;
	private static final Color AREA_BOUNDARY_COLOR = Color.BLUE;
	
	@Override
	public void renderToImpl(GL gl, Camera camera) {
				
		JOGLTarget target = new JOGLTarget(gl, camera);
				
		/* draw terrain boundaries */
		
		for (TerrainBoundaryWorldObject terrainBoundary :
			map.getWorldObjects(TerrainBoundaryWorldObject.class)) {

			if (terrainBoundary.getGroundState() == GroundState.ON) {

				Color color = NODE_BOUNDARY_COLOR;

				if (terrainBoundary instanceof WaySegmentWorldObject) {
					color = WAY_BOUNDARY_COLOR;
				} else if (terrainBoundary instanceof AreaWorldObject) {
					color = AREA_BOUNDARY_COLOR;
				}

				PolygonXYZ outlinePolygon = terrainBoundary.getOutlinePolygon();
				if (outlinePolygon != null) {
					target.drawLineLoop(color, outlinePolygon.getVertices());
				}

			}
			
		}
				
	}
	
}
