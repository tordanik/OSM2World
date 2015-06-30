package org.osm2world.core.target.jogl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

public class BackgroundShader extends AbstractShader {

	private int modelViewProjectionMatrixID;
	private int vertexPositionID;
	private int vertexTexCoord;
	private int textureID;
	
	public BackgroundShader(GL3 gl) {
		super(gl, "/shaders/background");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		vertexTexCoord = gl.glGetAttribLocation(shaderProgram, "VertexTexCoord");
		
		// get indices of uniform variables
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");
		textureID = gl.glGetUniformLocation(shaderProgram, "Tex");
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
	
	public int getVertexTexCoordID() {
		return vertexTexCoord;
	}
	
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}
	
	public int getTextureID() {
		return textureID;
	}
	
}