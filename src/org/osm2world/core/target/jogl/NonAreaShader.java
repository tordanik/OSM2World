package org.osm2world.core.target.jogl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

public class NonAreaShader extends AbstractShader {

	private int modelViewProjectionMatrixID;
	private int vertexPositionID;
	private int vertexColorID;
	
	public NonAreaShader(GL3 gl) {
		super(gl, "/shaders/nonarea");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		vertexColorID = gl.glGetAttribLocation(shaderProgram, "VertexColor");
		
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
	
	public int getVertexColorID() {
		return vertexColorID;
	}
	
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}
	
}