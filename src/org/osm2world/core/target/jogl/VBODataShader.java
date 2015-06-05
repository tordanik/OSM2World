package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_TEXTURE_COORD_ARRAY;

import java.nio.Buffer;
import java.util.Collection;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;

/**
 * class that keeps a VBO id along with associated information
 */
abstract class VBODataShader<BufferT extends Buffer> extends VBOData<BufferT> {
	
	protected GL3 gl;
	
	public VBODataShader(GL3 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		super(gl, textureManager, material, primitives);
		this.gl = gl;
	}
	
	@Override
	public void render() {
		
	}
	
}
