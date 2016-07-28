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
	private int viewMatrixID;
	private int proMatrixID;

	private int scatterColorID;

	public SkyShader(GL3 gl) {
		super(gl, "/shaders/sky");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		
		// get indices of uniform variables
		sunVectorID = gl.glGetUniformLocation(shaderProgram, "sunVector");
		intensityID = gl.glGetUniformLocation(shaderProgram, "sunIntensity");

		viewMatrixID = gl.glGetUniformLocation(shaderProgram, "viewMat");
		proMatrixID = gl.glGetUniformLocation(shaderProgram, "proMat");

		scatterColorID = gl.glGetUniformLocation(shaderProgram, "Kr");
		
		this.validateShader();
	}
	
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		gl.glUniformMatrix4fv(proMatrixID, 1, false, pmvMatrix.glGetPMatrixf());
		gl.glUniformMatrix4fv(viewMatrixID, 1, false, pmvMatrix.glGetMvMatrixf());
	}

	public void setLighting(GlobalLightingParameters sun) {
		gl.glUniform3f(sunVectorID
				, (float) sun.lightFromDirection.x
				, (float) sun.lightFromDirection.y
				, (float) sun.lightFromDirection.z);
		gl.glUniform1f(intensityID, sun.intensity);
		gl.glUniform3f(scatterColorID
				, (float) sun.scatterColor.getRed() / 255.0f
				, (float) sun.scatterColor.getGreen() / 255.0f
				, (float) sun.scatterColor.getBlue() / 255.0f);
	}
	
	/**
	 * Returns the id to use to bind the vertex position attribute.
	 */
	public int getVertexPositionID() {
		return vertexPositionID;
	}
}

