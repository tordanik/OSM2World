package org.osm2world.core.target.primitivebuffer;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 * Uses display lists to speed up the process.
 * 
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the display lists. Otherwise, this will
 * not be done before a destructor call.
 */
public class JOGLPrimitiveBufferRenderer {
	
	private GL gl;
	private Integer displayListPointer;
	
	public JOGLPrimitiveBufferRenderer(GL gl, PrimitiveBuffer primitiveBuffer) {
		
		this.gl = gl;
		
		primitiveBuffer.optimize();
		
		displayListPointer = gl.glGenLists(1);
		
		gl.glNewList(displayListPointer, GL.GL_COMPILE);
		
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
		
		gl.glEndList();
		
	}
	
	public void render() {
		
		if (displayListPointer == null)
			throw new IllegalStateException("display list has been deleted");
		
		gl.glCallList(displayListPointer);
				
	}
	
	/**
	 * frees all OpenGL resources associated with this object.
	 * Rendering will no longer be possible afterwards!
	 */
	public void freeResources() {
		
		if (displayListPointer != null) {
			gl.glDeleteLists(displayListPointer, 1);
			displayListPointer = null;
			gl = null;
		}
	
	}
	
	@Override
	protected void finalize() {
		freeResources();
	}
	
}
