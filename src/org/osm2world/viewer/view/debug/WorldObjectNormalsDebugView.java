package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.Material.Lighting;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;

public class WorldObjectNormalsDebugView extends DebugView {

	private static final Color FLAT_NORMALS_COLOR = Color.YELLOW;
	private static final Color SMOOTH_NORMALS_COLOR = Color.ORANGE;

	@Override
	public String getDescription() {
		return "draws world object normals as arrows";
	}
	
	@Override
	protected void renderToImpl(GL gl, Camera camera) {
		
		JOGLTarget target = new JOGLTarget(gl);
		
		for (Material material : mapDataPrimitiveBuffer.getMaterials()) {
			
			Color color = material.lighting == Lighting.FLAT ?
					FLAT_NORMALS_COLOR : SMOOTH_NORMALS_COLOR;

			// calculate some vectors for easily culling some of the vertices
			VectorXZ cam = camera.getPos().xz();
			VectorXZ rightOfCam = camera.getPos().add(camera.getRight()).xz();
			
			for (Primitive primitive : mapDataPrimitiveBuffer.getPrimitives(material)) {
				
				for (int i = 0; i < primitive.indices.length; i++) {
					int index = primitive.indices[i];
					VectorXYZ v = mapDataPrimitiveBuffer.getVertex(index);
					if (GeometryUtil.isRightOf(v.xz(), cam, rightOfCam)) {
						target.drawArrow(color, 0.3f, v, v.add(primitive.normals[i]));
					}
				}
		        
			}			
		
		}
		
	}

}
