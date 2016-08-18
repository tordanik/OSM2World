package org.osm2world.core.target.jogl;

import javax.media.opengl.GL3;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;

/**
 * Base class for shaders that render primitives with materials. Is used by {@link JOGLRendererVBOShader}.
 */
public abstract class AbstractPrimitiveShader extends AbstractShader {

	/**
	 * Render objects that are semi transparent (see {@link Transparency.TRUE})
	 */
	protected boolean renderSemiTransparent = true;
	
	/**
	 * Render only objects that are semi transparent (see {@link Transparency.TRUE})
	 */
	protected boolean renderOnlySemiTransparent = false;

	public AbstractPrimitiveShader(GL3 gl, String name) {
		super(gl, name);
	}

	/**
	 * Prepare the shader to use the given material when rendering the primitives later.
	 * @param material the material to apply
	 * @param textureManager the texture manager to use if the material has textures
	 * @return <code>true</code> if this shader wants to render primitives with the given material at all, <code>false</code> otherwise.
	 */
	public boolean setMaterial(JOGLMaterial joglMaterial, JOGLTextureManager textureManager) {
		Material material = joglMaterial.getBaseMaterial();
		
		if (!renderSemiTransparent && material.getTransparency() == Transparency.TRUE) {
			return false;
		} else if (renderOnlySemiTransparent && material.getTransparency() != Transparency.TRUE) {
			return false;
		}
		return true;
		
	}
	
	/**
	 * see {@link #renderSemiTransparent}
	 */
	public void setRenderSemiTransparent(boolean renderSemiTransparent) {
		this.renderSemiTransparent  = renderSemiTransparent;
	}
	
	/**
	 * see {@link #renderOnlySemiTransparent}
	 */
	public void setRenderOnlySemiTransparent(boolean renderOnlySemiTransparent) {
		this.renderOnlySemiTransparent  = renderOnlySemiTransparent;
	}

	/**
	 * Returns the id to use by {@link JOGLRendererVBOShader} to bind the vertex position attribute.
	 * May be -1 if the attribute is unused.
	 */
	public abstract int getVertexPositionID();

	/**
	 * Returns the id to use by {@link JOGLRendererVBOShader} to bind the vertex normal attribute.
	 * May be -1 if the attribute is unused.
	 */
	public abstract int getVertexNormalID();

	/**
	 * Returns the id to use by {@link JOGLRendererVBOShader} to bind the vertex texture coordinate attribute.
	 * May be -1 if the attribute is unused.
	 */
	public abstract int getVertexTexCoordID(int i);
	
	/**
	 * Returns the id to use by {@link JOGLRendererVBOShader} to bind the vertex bumpmap coordinate attribute.
	 * May be -1 if the attribute is unused.
	 */
	public abstract int getVertexBumpMapCoordID();

	/**
	 * Returns the id to use by {@link JOGLRendererVBOShader} to bind the vertex tangent attribute.
	 * May be -1 if the attribute is unused.
	 */
	public abstract int getVertexTangentID();
	
	/**
	 * Enable a vertex attribute. Attributes with -1 are ignored.
	 * @param index the index of the attribute. Can safely be -1
	 */
	public void glEnableVertexAttribArray(int index) {
		if (index != -1) {
			gl.glEnableVertexAttribArray(index);
		}
	}
	
	/**
	 * Disable a vertex attribute. Attributes with -1 are ignored.
	 * @param index the index of the attribute. Can safely be -1
	 */
	public void glDisableVertexAttribArray(int index) {
		if (index != -1) {
			gl.glDisableVertexAttribArray(index);
		}
	}
	
	/**
	 * Setup the vertex attribute pointer. Attributes with index -1 are ignores.
	 */
	public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
		if (index != -1) {
			gl.glVertexAttribPointer(index, size, type, normalized, stride, offset);
		}
	}
}
