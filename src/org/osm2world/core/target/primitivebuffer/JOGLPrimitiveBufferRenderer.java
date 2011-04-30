package org.osm2world.core.target.primitivebuffer;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 * 
 * This class attempts to use the {@link JOGL_VBOPrimitiveBufferRenderer}
 * if VertexBufferObjects are supported, and falls back to slower individual
 * calls otherwise.
 */
public class JOGLPrimitiveBufferRenderer {
	
	private final GL gl;
	private final PrimitiveBuffer primitiveBuffer;
	
	JOGL_VBOPrimitiveBufferRenderer vboRenderer;
	
	public JOGLPrimitiveBufferRenderer(GL gl, PrimitiveBuffer primitiveBuffer) {
		
		this.gl = gl;
		this.primitiveBuffer = primitiveBuffer;
		
		primitiveBuffer.optimize();
		
		if (JOGL_VBOPrimitiveBufferRenderer.isSupported(gl)) {
			vboRenderer = new JOGL_VBOPrimitiveBufferRenderer(gl, primitiveBuffer);
		}
		
	}
	
	public void render() {
		
		if (vboRenderer != null) {
			vboRenderer.render();
			return;
		}
		
		for (Material material : primitiveBuffer.getMaterials()) {

			JOGLTarget.setMaterial(gl, material);
			
			for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {
								
				gl.glBegin(JOGLTarget.getGLConstant(primitive.type));
		        
				int i = 0;
				for (int index : primitive.indices) {
					VectorXYZ v = primitiveBuffer.getVertex(index);
					gl.glNormal3d(primitive.normals[i].x, 
			        		 primitive.normals[i].y, 
			        		-primitive.normals[i].z);
			        gl.glVertex3d(v.x, v.y, -v.z);
			        ++ i;
				}
				
		        gl.glEnd();
		        
			}
		
		}
		
	}
	
}
