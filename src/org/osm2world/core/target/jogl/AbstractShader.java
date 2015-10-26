package org.osm2world.core.target.jogl;

import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

/**
 * Simple base class for a shader program. Manages vertex and fragment shaders, links and validates them.
 */
public abstract class AbstractShader {
	
	protected GL3 gl;
	protected int vertexShader;
	protected int fragmentShader;
	protected int shaderProgram;
	
	/**
	 * Loads the vertex and fragment shaders with the basename name and ending <i>vertex</i> and <i>fragment</i>
	 * and creates a shader program for them.
	 * @param gl
	 * @param name
	 */
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
		//System.out.printf("Vertex: %d, Fragment: %d\n",vertexShader,fragmentShader);

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
			
			/* The validation of shaders may fail due to driver bugs. This does not necessarily mean that the
			 * shader is invalid. Therefore this will only raise a warning. An example of such a common mistake is
			 * the access to texture unit 0, raised by some intel graphics drivers:
			 * "Texture unit 0 is accessed both as sampler2D and sampler2DShadow"
			 */
			String infoLog = ShaderManager.getProgramInfoLog(gl, shaderProgram);
			System.err.println("WARNING: could not validate shader. Info Log: '" + infoLog + "'");
		} else {
			ShaderManager.printProgramInfoLog(gl, shaderProgram);
		}
	}
	
	/**
	 * Make this shader program active.
	 */
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
	    gl = null;
	}
	
	@Override
	protected void finalize() {
		freeResources();
	}

	/**
	 * Load default values for the shader. Should be called after every {@link #useShader()} and before any draw calls.
	 */
	public void loadDefaults() { }
}
