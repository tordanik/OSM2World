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
	private DefaultShader shader;
	
	public VBODataShader(GL3 gl, DefaultShader shader, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		super(gl, textureManager, material, primitives);
		this.gl = gl;
		this.shader = shader;
	}
	
	@Override
	public void render() {
		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);

		setPointerLayout();
		shader.setMaterial(material, textureManager);
		
		gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);
		for (int i=1; i<DefaultShader.MAX_TEXTURE_LAYERS; i++) {
			gl.glDisableVertexAttribArray(shader.getVertexTexCoordID(i));
		}
	}
	
	private void setPointerLayout() {
		
		int stride = valueTypeSize * JOGLRendererVBO.getValuesPerVertex(material);
		
		int offset = 0;
		
		for (int i = 0; i < material.getNumTextureLayers(); i++) {

			gl.glEnableVertexAttribArray(shader.getVertexTexCoordID(i));
			gl.glVertexAttribPointer(shader.getVertexTexCoordID(i), 2, glValueType(), false, stride, offset);
			offset += 2 * valueTypeSize;
			
		}
		
		gl.glVertexAttribPointer(shader.getVertexNormalID(), 3, glValueType(), false, stride, offset);
		gl.glVertexAttribPointer(shader.getVertexPositionID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 3);
		//gl.glVertexAttribPointer(shader.getVertexColorID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 6);
		
	}
	
}
