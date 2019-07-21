package org.osm2world.core.target.jogl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Shader to render a single texture on screen.
 */
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

		this.validateShader();
	}

	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 * @param pmvMatrix the PMVMatrix containing all matrices
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat);
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
	}

	/**
	 * Returns the id to use to bind the vertex position attribute.
	 */
	public int getVertexPositionID() {
		return vertexPositionID;
	}

	/**
	 * Returns the id to use to bind the vertex texture coordinate attribute.
	 */
	public int getVertexTexCoordID() {
		return vertexTexCoord;
	}

	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}

	/**
	 * Returns the id to use to bind the texture attribute.
	 */
	public int getTextureID() {
		return textureID;
	}

}