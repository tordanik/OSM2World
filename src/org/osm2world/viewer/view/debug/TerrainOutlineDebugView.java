package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.EarClippingTriangulationUtil;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.terrain.data.GenericTerrainPatch;
import org.osm2world.core.terrain.data.TerrainPatch;

/**
 * shows the terrain polygons immediately before triangulation
 */
public class TerrainOutlineDebugView extends DebugView {

	private static final Color OUTLINE_COLOR = Color.GREEN;
	
	@Override
	public String getDescription() {
		return "shows the terrain polygons immediately before triangulation";
	}
	
	@Override
	protected void renderToImpl(GL gl, Camera camera) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);
		
		for (TerrainPatch patch : terrain.getPatches()) {
			if (patch instanceof GenericTerrainPatch) {
				GenericTerrainPatch p = (GenericTerrainPatch) patch;
				
				List<VectorXZ> outline =
					new ArrayList<VectorXZ>(p.getOuterPolygon().getVertexLoop());
				
				EarClippingTriangulationUtil.insertHolesInPolygonOutline(outline, p.getHoles());
				
				target.drawLineStrip(OUTLINE_COLOR, VectorXZ.listXYZ(outline, 0));
					
			}
		}
		
	}
	
}
