package org.osm2world.core.target.primitivebuffer;

import static javax.media.opengl.GL2.GL_COMPILE;

import javax.media.opengl.GL2;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTextureManager;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 * Uses display lists to speed up the process.
 * 
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the display lists. Otherwise, this will
 * not be done before a destructor call.
 */
public class JOGLPrimitiveBufferRenderer {
	
	private GL2 gl;
	private Integer displayListPointer;
	
	private JOGLTextureManager textureManager;
	
	public JOGLPrimitiveBufferRenderer(GL2 gl, PrimitiveBuffer primitiveBuffer) {
		
		this.gl = gl;
		this.textureManager = new JOGLTextureManager(gl);
		
		primitiveBuffer.optimize();
		
		displayListPointer = gl.glGenLists(1);
		
		gl.glNewList(displayListPointer, GL_COMPILE);

		for (Material material : primitiveBuffer.getMaterials()) {
						
			JOGLTarget.setMaterial(gl, material, textureManager);

			for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {
				
				gl.glBegin(JOGLTarget.getGLConstant(primitive.type));
				
				int i = 0;
				for (int index : primitive.indices) {
					
					if (primitive.texCoordLists != null
							&& !primitive.texCoordLists.isEmpty()) {
						VectorXZ textureCoord =
								primitive.texCoordLists.get(0).get(i);
						gl.glTexCoord2d(textureCoord.x, textureCoord.z);
					}
					
					gl.glNormal3d(primitive.normals.get(i).x,
							primitive.normals.get(i).y,
							-primitive.normals.get(i).z);
					
					VectorXYZ v = primitiveBuffer.getVertex(index);
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
		
		textureManager.releaseAll();
		
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
