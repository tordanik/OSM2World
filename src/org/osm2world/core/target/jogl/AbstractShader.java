package org.osm2world.core.target.jogl;

import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

public abstract class AbstractShader {
	
	protected GL3 gl;
	protected int vertexShader;
	protected int fragmentShader;
	protected int shaderProgram;
	
	public AbstractShader(GL3 gl, String name) {
		this.gl = gl;
		shaderProgram = gl.glCreateProgram();

		if (shaderProgram < 1)
			throw new RuntimeException("Unable to create shader program.");

		/*
		 * set up the vertex and fragment shaders. use static methods
		 * for setting up the shaders. These methods return the unique int
		 * identifiers GL assigns to each shader
		 */
		vertexShader = ShaderManager.createVertShader(gl, name+".vertex");
		fragmentShader = ShaderManager.createFragShader(gl, name+".fragment");
		System.out.printf("Vertex: %d, Fragment: %d\n",vertexShader,fragmentShader);

		// attach the shaders to the shader program and link
		gl.glAttachShader(shaderProgram, vertexShader);
		gl.glAttachShader(shaderProgram, fragmentShader);

		gl.glLinkProgram(shaderProgram);
		// validate linking
		IntBuffer linkStatus = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderProgram, GL3.GL_LINK_STATUS, linkStatus);
		if (linkStatus.get() == GL.GL_FALSE) {
			ShaderManager.printProgramInfoLog(gl, shaderProgram);
			throw new RuntimeException("could not link shader");
		}
		ShaderManager.printProgramInfoLog(gl, shaderProgram);

		// tell GL to validate the shader program and grab the created log
		gl.glValidateProgram(shaderProgram);
		// perform general validation that the program is usable
		IntBuffer validateStatus = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderProgram, GL3.GL_VALIDATE_STATUS, validateStatus);
		if (validateStatus.get() == GL.GL_FALSE) {
			ShaderManager.printProgramInfoLog(gl, shaderProgram);
			throw new RuntimeException("could not validate shader");
		}
		ShaderManager.printProgramInfoLog(gl, shaderProgram);
	}
	
	public void useShader() {
		gl.glUseProgram(this.getProgram());
	}
	
	public void disableShader() {
		gl.glUseProgram(0);
	}
	
	public int getProgram() {
		return shaderProgram;
	}

	public void freeResources() {
	    gl.glDetachShader(shaderProgram, vertexShader);
	    gl.glDetachShader(shaderProgram, fragmentShader);
	    gl.glDeleteProgram(shaderProgram);
	    gl.glDeleteShader(vertexShader);
	    gl.glDeleteShader(fragmentShader);
	    System.out.printf("Deleted Vertex: %d, Fragment: %d\n",vertexShader,fragmentShader);
	    gl = null;
	}
	
	@Override
	protected void finalize() {
		freeResources();
	}
}
