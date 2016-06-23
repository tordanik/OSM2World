package org.osm2world.core.target.jogl;

import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

import java.util.Arrays;
import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

public class SkyShader extends AbstractShader {
	private int vertexPositionID;
	private int sunVectorID;
	private int intensityID;
	private int invProjID;
	private int invViewID;

	public SkyShader(GL3 gl) {
		super(gl, "/shaders/sky");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		
		// get indices of uniform variables
		sunVectorID = gl.glGetUniformLocation(shaderProgram, "sunVector");
		intensityID = gl.glGetUniformLocation(shaderProgram, "sunIntensity");

		invProjID = gl.glGetUniformLocation(shaderProgram, "inv_proj");
		invViewID = gl.glGetUniformLocation(shaderProgram, "inv_view");
		
		this.validateShader();
	}
	
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		// Send the inverse view matrix
		FloatBuffer ivMat = pmvMatrix.glGetMviMatrixf().asReadOnlyBuffer();
		FloatBuffer ivMatBuf = FloatBuffer.allocate(16);

		for(int i = 0; i < 16; i++)
			ivMatBuf.put(ivMat.get());

		ivMatBuf.rewind();

		gl.glUniformMatrix4fv(invViewID, 1, false, ivMatBuf);


		// Caclulate and Send the inverse projection matrix
		FloatBuffer pMatBuf = pmvMatrix.glGetPMatrixf().asReadOnlyBuffer();
		float[] pMat = new float[16];

		for(int i = 0; i < 16; i++)
			pMat[i] = pMatBuf.get();

		// TODO Cache this
		FloatBuffer iPMat = FloatBuffer.allocate(16);
		iPMat.put(1.0f/pMat[0 * 4 + 0]);
		iPMat.put(0);
		iPMat.put(0);
		iPMat.put(0);

		iPMat.put(0);
		iPMat.put(1.0f/pMat[1 * 4 + 1]);
		iPMat.put(0);
		iPMat.put(0);

		iPMat.put(0);
		iPMat.put(0);
		iPMat.put(0);
		iPMat.put(1.0f/pMat[3 * 4 + 2]);

		iPMat.put(0);
		iPMat.put(0);
		iPMat.put(1.0f/pMat[2 * 4 + 3]);
		iPMat.put(-pMat[2 * 4 + 2]/(pMat[2 * 4 + 3] * pMat[3 * 4 + 2]));

		iPMat.rewind();

		gl.glUniformMatrix4fv(invProjID, 1, false, iPMat);
	}

	public void setLighting(GlobalLightingParameters sun) {
		gl.glUniform3f(sunVectorID
				, (float) sun.lightFromDirection.x
				, (float) sun.lightFromDirection.y
				, (float) sun.lightFromDirection.z);
		gl.glUniform1f(intensityID, sun.intensity);
	}
	
	/**
	 * Returns the id to use to bind the vertex position attribute.
	 */
	public int getVertexPositionID() {
		return vertexPositionID;
	}
}

