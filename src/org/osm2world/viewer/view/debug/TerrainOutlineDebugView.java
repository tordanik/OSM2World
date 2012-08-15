package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.EarClippingTriangulationUtil;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.terrain.data.GenericTerrainPatch;
import org.osm2world.core.terrain.data.TerrainPatch;

/**
 * shows the terrain polygons immediately before triangulation
 */
public class TerrainOutlineDebugView extends DebugView {

	private static final Color OUTLINE_COLOR = Color.GREEN;
	private static final Color POINT_COLOR = Color.YELLOW;
	
	@Override
	public String getDescription() {
		return "shows the terrain polygons immediately before triangulation";
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		
		for (TerrainPatch patch : terrain.getPatches()) {
			if (patch instanceof GenericTerrainPatch) {
				GenericTerrainPatch p = (GenericTerrainPatch) patch;
				
				List<VectorXZ> outline = new ArrayList<VectorXZ>(
							p.getPolygon().getOuter().getVertexLoop());
				
				EarClippingTriangulationUtil.insertHolesInPolygonOutline(
						outline, p.getPolygon().getHoles());
				
				target.drawLineStrip(OUTLINE_COLOR, 1, VectorXZ.listXYZ(outline, 0));
				
				for (VectorXZ point : p.getPoints()) {
					drawBoxAround(target, point, POINT_COLOR, 0.5f);
				}
				
			}
		}
		
	}
	
}
