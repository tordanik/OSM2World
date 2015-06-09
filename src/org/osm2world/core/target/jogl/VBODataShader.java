package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_TEXTURE_COORD_ARRAY;

import java.nio.Buffer;
import java.util.Collection;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;

/**
 * class that keeps a VBO id along with associated information
 */
abstract class VBODataShader<BufferT extends Buffer> extends VBOData<BufferT> {
	
	protected GL3 gl;
	private Shader shader;
	
	public VBODataShader(GL3 gl, Shader shader, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		super(gl, textureManager, material, primitives);
		this.gl = gl;
		this.shader = shader;
	}
	
	@Override
	public void render() {
		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);
		
		shader.setMaterial(material, textureManager);
		setPointerLayout();
		
		gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);
	}
	
	private void setPointerLayout() {
		
		int stride = valueTypeSize * JOGLRendererVBO.getValuesPerVertex(material);
		
		int offset = 0;
		
		for (int i = 0; i < material.getNumTextureLayers(); i++) {
			
			offset += 2 * valueTypeSize;
			
		}
		
		gl.glVertexAttribPointer(shader.getVertexNormalID(), 3, glValueType(), false, stride, offset);
		gl.glVertexAttribPointer(shader.getVertexPositionID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 3);
		//gl.glVertexAttribPointer(shader.getVertexColorID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 6);
		
	}
	
}
