package org.osm2world.core.target.jogl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Simple shader to render shadow volumes.
 * @see JOGLRendererVBOShadowVolume
 */
public class ShadowVolumeShader extends AbstractPrimitiveShader {

	private int modelViewProjectionMatrixID;
	private int vertexPositionID;

	public ShadowVolumeShader(GL3 gl) {
		super(gl, "/shaders/shadowvolume");

		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");

		// get indices of uniform variables
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");

		this.validateShader();
	}

	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 * @param pmvMatrix the PMVMatrix containing all matrices
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrix(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat.array());
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
	}

	/**
	 * Returns the id to use to bind the vertex position attribute.
	 */
	public int getVertexPositionID() {
		return vertexPositionID;
	}

	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}

	@Override
	public int getVertexNormalID() {
		return -1; // unused
	}

	@Override
	public int getVertexTexCoordID(int i) {
		return -1; // unused
	}

	@Override
	public int getVertexBumpMapCoordID() {
		return -1; // unused
	}

	@Override
	public int getVertexTangentID() {
		return -1; // unused
	}
}