package org.osm2world.core.target.jogl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import com.jogamp.opengl.util.PMVMatrix;


/**
 * Renders a cubemap at the far clipping plane of the provided camera
 **/
public class CubemapShader extends AbstractShader {
	private int vertexPositionID;
	private int cubemapSamplerID;

	private int viewMatrixID;
	private int proMatrixID;
	private int pmvMatrixID;

	public CubemapShader(GL3 gl) {
		super(gl, "/shaders/cubemap");

		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");

		viewMatrixID = gl.glGetUniformLocation(shaderProgram, "viewMat");
		proMatrixID = gl.glGetUniformLocation(shaderProgram, "proMat");

		cubemapSamplerID = gl.glGetUniformLocation(shaderProgram, "cubemap");

		this.validateShader();
	}
	
	public void setCubemap(Cubemap cube)
	{
		if(cube != null) {
			cube.bind(gl, 0);
		}
	}

	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		gl.glUniformMatrix4fv(proMatrixID, 1, false, pmvMatrix.glGetPMatrixf());
		gl.glUniformMatrix4fv(viewMatrixID, 1, false, pmvMatrix.glGetMvMatrixf());
	}

	public int getVertexPositionID() {
		return vertexPositionID;
	}
}

