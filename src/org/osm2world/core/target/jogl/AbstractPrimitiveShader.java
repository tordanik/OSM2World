package org.osm2world.core.target.jogl;

import javax.media.opengl.GL3;

import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;

public abstract class AbstractPrimitiveShader extends AbstractShader {

	protected boolean renderSemiTransparent = true;
	protected boolean renderOnlySemiTransparent = false;

	public AbstractPrimitiveShader(GL3 gl, String name) {
		super(gl, name);
	}

	public boolean setMaterial(Material material, JOGLTextureManager textureManager) {
		
		if (!renderSemiTransparent && material.getTransparency() == Transparency.TRUE) {
			return false;
		} else if (renderOnlySemiTransparent && material.getTransparency() != Transparency.TRUE) {
			return false;
		}
		return true;
		
	}
	
	public void setRenderSemiTransparent(boolean renderSemiTransparent) {
		this.renderSemiTransparent  = renderSemiTransparent;
	}
	
	public void setRenderOnlySemiTransparent(boolean renderOnlySemiTransparent) {
		this.renderOnlySemiTransparent  = renderOnlySemiTransparent;
	}

	public abstract int getVertexPositionID();

	public abstract int getVertexNormalID();

	public abstract int getVertexTexCoordID(int i);

	public abstract int getVertexBumpMapCoordID();

	public abstract int getVertexTangentID();
	
	public void glEnableVertexAttribArray(int index) {
		if (index != -1) {
			gl.glEnableVertexAttribArray(index);
		}
	}
	
	public void glDisableVertexAttribArray(int index) {
		if (index != -1) {
			gl.glDisableVertexAttribArray(index);
		}
	}
	
	public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
		if (index != -1) {
			gl.glVertexAttribPointer(index, size, type, normalized, stride, offset);
		}
	}
}
