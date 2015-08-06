package org.osm2world.core.target.jogl;

import javax.media.opengl.GL3;

import org.osm2world.core.target.common.material.Material;

public abstract class AbstractPrimitiveShader extends AbstractShader {

	public AbstractPrimitiveShader(GL3 gl, String name) {
		super(gl, name);
	}

	public abstract boolean setMaterial(Material material, JOGLTextureManager textureManager);

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
