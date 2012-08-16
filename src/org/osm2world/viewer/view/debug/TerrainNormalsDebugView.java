package org.osm2world.viewer.view.debug;

import java.awt.Color;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.PrimitiveBuffer;

public class TerrainNormalsDebugView extends DebugView {

	private static final Color FLAT_NORMALS_COLOR = Color.YELLOW;
	private static final Color SMOOTH_NORMALS_COLOR = Color.ORANGE;

	@Override
	public String getDescription() {
		return "draws terrain normals as arrows";
	}
	
	@Override
	public boolean canBeUsed() {
		return terrain != null;
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		
		PrimitiveBuffer terrainPrimitiveBuffer = new PrimitiveBuffer();
		
		terrain.renderTo(terrainPrimitiveBuffer);
		
		for (Material material : terrainPrimitiveBuffer.getMaterials()) {
			
			Color color = material.getLighting() == Lighting.FLAT ?
					FLAT_NORMALS_COLOR : SMOOTH_NORMALS_COLOR;
			
			for (Primitive primitive : terrainPrimitiveBuffer.getPrimitives(material)) {
				
				for (int i = 0; i < primitive.vertices.size(); i++) {
					VectorXYZ v = primitive.vertices.get(i);
					VectorXYZ n = primitive.normals.get(i);
					drawArrow(target, color, 0.3f, v, v.add(n));
				}
				
			}
			
		}
		
	}

}
