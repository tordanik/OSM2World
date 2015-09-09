package org.osm2world.core.target.jogl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import org.osm2world.core.target.common.material.Material;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

public class ShadowVolumeShader extends AbstractPrimitiveShader {
	
	private int modelViewProjectionMatrixID;
	private int vertexPositionID;
	
	public ShadowVolumeShader(GL3 gl) {
		super(gl, "/shaders/shadowvolume");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		
		// get indices of uniform variables
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");
	}
	
	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 * @param pmvMatrix
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat);
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
	}
	
	public int getVertexPositionID() {
		return vertexPositionID;
	}
	
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}
	
	@Override
	public int getVertexNormalID() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getVertexTexCoordID(int i) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getVertexBumpMapCoordID() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getVertexTangentID() {
		// TODO Auto-generated method stub
		return -1;
	}
}