package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.network.NetworkWaySegmentWorldObject;

/**
 * shows information about {@link NetworkWaySegmentWorldObject}s
 */
public class NetworkDebugView extends DebugView {

	private static final Color OFFSET_COLOR = Color.PINK;
	private static final Color CUT_COLOR = Color.ORANGE;
	
	@Override
	public void renderToImpl(GL gl, Camera camera) {
				
		JOGLTarget target = new JOGLTarget(gl, camera);
						
		for (MapWaySegment line : map.getMapWaySegments()) {
			for (WorldObject worldObject : line.getRepresentations()) {
				if (worldObject instanceof NetworkWaySegmentWorldObject) {
					
					NetworkWaySegmentWorldObject representation =
						(NetworkWaySegmentWorldObject) worldObject;

					/* draw lines */

//					util.drawLineStrip(LINE_COLOR,
//					line.getStartNode().getPos(), line.getEndNode().getPos());

					/* draw offsets */

					drawVectorAt(target, OFFSET_COLOR,
							representation.getStartOffset(), line.getStartNode());

					drawVectorAt(target, OFFSET_COLOR,
							representation.getEndOffset(), line.getEndNode());

					/* draw cuts */

					drawVectorAt(target, CUT_COLOR,
							representation.getStartCutVector(), line.getStartNode());

					drawVectorAt(target, CUT_COLOR,
							representation.getEndCutVector(), line.getEndNode());

				}
			}
		}
				
	}
	
	private static void drawVectorAt(JOGLTarget util,
			Color color, VectorXZ vector, MapNode start) {
		
		VectorXYZ startV = start.getPos().xyz(0);
		VectorXYZ endV = startV.add(vector);
		
		util.drawArrow(color, 0.3f * (float)vector.length(), startV, endV);
		
	}
	
}
