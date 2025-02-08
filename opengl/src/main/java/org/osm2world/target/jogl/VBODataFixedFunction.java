package org.osm2world.target.jogl;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.fixedfunc.GLPointerFunc.GL_TEXTURE_COORD_ARRAY;

import java.nio.Buffer;
import java.util.Collection;

import org.osm2world.target.common.Primitive;
import org.osm2world.target.common.material.Material;

import com.jogamp.opengl.GL2;

/**
 * class that keeps a VBO id along with associated information for the fixed function OpenGL pipeline
 */
abstract class VBODataFixedFunction<BufferT extends Buffer> extends VBOData<BufferT> {

	protected GL2 gl;

	/**
	 * @see VBOData#VBOData(com.jogamp.opengl.GL, JOGLTextureManager, Material, Collection)
	 */
	public VBODataFixedFunction(GL2 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		super(gl, textureManager, material, primitives);
		this.gl = gl;
	}

	@Override
	public void render() {

		for (int i = 0; i < JOGLTargetFixedFunction.MAX_TEXTURE_LAYERS; i++) {

			gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(i));

			if (i >= material.getNumTextureLayers()) {

				gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);

			} else {

				gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

			}

		}

		gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(0));

		JOGLTargetFixedFunction.setMaterial(gl, material, textureManager);

		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);

		setPointerLayout();

		gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);

	}

	private void setPointerLayout() {

		int stride = valueTypeSize * JOGLRendererVBO.getValuesPerVertex(material);

		int offset = 0;

		for (int i = 0; i < material.getNumTextureLayers(); i++) {

			gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(i));
			gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

			gl.glActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(i));
			gl.glTexCoordPointer(2, glValueType, stride, offset);

			offset += 2 * valueTypeSize;

		}

		gl.glVertexPointer(3, glValueType, stride, offset + valueTypeSize() * 3);
		gl.glNormalPointer(glValueType, stride, offset);

	}

	@Override
	protected int getValuesPerVertex(Material material) {
		return JOGLRendererVBO.getValuesPerVertex(material);
	}
}
