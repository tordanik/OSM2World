package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;

public class TerrainNormalsDebugView extends DebugView {

	private static final Color FLAT_NORMALS_COLOR = Color.YELLOW;
	private static final Color SMOOTH_NORMALS_COLOR = Color.ORANGE;

	@Override
	public String getDescription() {
		return "draws terrain normals as arrows";
	}
	
	@Override
	protected void renderToImpl(GL gl, Camera camera) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);
		
		for (Material material : terrainPrimitiveBuffer.getMaterials()) {
					
			Color color = material.getLighting() == Lighting.FLAT ?
					FLAT_NORMALS_COLOR : SMOOTH_NORMALS_COLOR;
			
			for (Primitive primitive : terrainPrimitiveBuffer.getPrimitives(material)) {
				
				for (int i = 0; i < primitive.indices.length; i++) {
					int index = primitive.indices[i];
					VectorXYZ v = terrainPrimitiveBuffer.getVertex(index);
					target.drawArrow(color, 0.3f, v, v.add(primitive.normals[i]));
				}
		        
			}
		
		}
		
	}

}
