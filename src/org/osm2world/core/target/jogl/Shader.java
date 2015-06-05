package org.osm2world.core.target.jogl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;

public class Shader {
	
	private int vertexShader;
	private int fragmentShader;
	private int shaderProgram;
	private int projectionMatrixID;
	private int modelViewMatrixID;
	private int modelViewProjectionMatrixID;
	private GL3 gl;
	
	public Shader(GL3 gl) {
		this.gl = gl;
		shaderProgram = gl.glCreateProgram();

		if (shaderProgram < 1)
			throw new RuntimeException("Unable to create shader program.");

		/*
		 * set up the vertex and fragment shaders. use static methods
		 * for setting up the shaders. These methods return the unique int
		 * identifiers GL assigns to each shader
		 */
		vertexShader = createVertShader(gl, "/shaders/simple.vertex");
		fragmentShader = createFragShader(gl, "/shaders/simple.fragment");
		System.out.printf("Vertex: %d, Fragment: %d\n",vertexShader,fragmentShader);

		// attach the shaders to the shader program and link
		gl.glAttachShader(shaderProgram, vertexShader);
		gl.glAttachShader(shaderProgram, fragmentShader);
		
		// bind named attributes in shader to attribute index
		gl.glBindAttribLocation (shaderProgram, getVertexPositionID(), "VertexPosition");
		gl.glBindAttribLocation (shaderProgram, getVertexColorID(), "VertexColor");
		gl.glBindAttribLocation (shaderProgram, getVertexNormalID(), "VertexNormal");

		gl.glLinkProgram(shaderProgram);
		// validate linking
		IntBuffer linkStatus = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderProgram, GL3.GL_LINK_STATUS, linkStatus);
		if (linkStatus.get() == GL.GL_FALSE) {
			Shader.printProgramInfoLog(gl, shaderProgram);
			throw new RuntimeException("could not link shader");
		}
		Shader.printProgramInfoLog(gl, shaderProgram);
		
		// get indices of uniform variables
		projectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ProjectionMatrix");
		modelViewMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewMatrix");
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");

		// tell GL to validate the shader program and grab the created log
		gl.glValidateProgram(shaderProgram);
		// perform general validation that the program is usable
		IntBuffer validateStatus = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderProgram, GL3.GL_VALIDATE_STATUS, validateStatus);
		if (validateStatus.get() == GL.GL_FALSE) {
			Shader.printProgramInfoLog(gl, shaderProgram);
			throw new RuntimeException("could not validate shader");
		}
		Shader.printProgramInfoLog(gl, shaderProgram);
	}
	
	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 * @param pmvMatrix
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		gl.glUniformMatrix4fv(this.getProjectionMatrixID(), 1, false, pmvMatrix.glGetPMatrixf());
		gl.glUniformMatrix4fv(this.getModelViewMatrixID(), 1, false, pmvMatrix.glGetMvMatrixf());
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat);
		pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
		pmvMatrix.glPushMatrix();
		pmvMatrix.glMultMatrixf(pmvMatrix.glGetMvMatrixf());
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
	}
	
	public int getProgram() {
		return shaderProgram;
	}
	
	public int getVertexPositionID() {
		return 0;
	}
	
	public int getVertexColorID() {
		return 1;
	}
	
	public int getVertexNormalID() {
		return 2;
	}
	
	public int getProjectionMatrixID() {
		return projectionMatrixID;
	}
	
	public int getModelViewMatrixID() {
		return modelViewMatrixID;
	}
	
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
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
	
	/*
	 * createVertShader is handed a String defining where to find the file
	 * that contains the shader code. It creates and compiles the shader
	 * and returns a unique int that GL associates with it
	 */
	public static int createVertShader(GL3 gl, String filename) {

		// get the unique id
		int vertShader = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		if (vertShader == 0)
			throw new RuntimeException("Unable to create vertex shader.");

		// create a single String array index to hold the shader code
		String[] vertCode = new String[1];
		vertCode[0] = "";
		String line;

		// open the file and read the contents into the String array.
		try {
			InputStream stream = System.class.getResourceAsStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			while ((line = reader.readLine()) != null) {
				vertCode[0] += line + "\n";
			}
		} catch (Exception e) {
			throw new RuntimeException("Fail reading vertex shader",e);
		}

		// Associate the code string with the unique id
		gl.glShaderSource(vertShader, 1, vertCode, null);
		// compile the vertex shader
		gl.glCompileShader(vertShader);

		// acquire compilation status
		IntBuffer shaderStatus = IntBuffer.allocate(1);
		gl.glGetShaderiv(vertShader, GL3.GL_COMPILE_STATUS, shaderStatus);

		// check whether compilation was successful
		if (shaderStatus.get() == GL.GL_FALSE) {
			printShaderInfoLog(gl, vertShader);
			throw new IllegalStateException("compilation error for shader ["
					+ filename + "].");
		}
		printShaderInfoLog(gl, vertShader);

		// the int returned is now associated with the compiled shader
		return vertShader;
	}

	/*
	 * 
	 * Essentially the same as the vertex shader
	 */
	public static int createFragShader(GL3 gl, String filename) {

		int fragShader = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		if (fragShader == 0)
			return 0;

		String[] fragCode = new String[1];
		fragCode[0] = "";
		String line;

		try {
			InputStream stream = System.class.getResourceAsStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			while ((line = reader.readLine()) != null) {
				fragCode[0] += line + "\n";
			}
		} catch (Exception e) {
			throw new RuntimeException("Fail reading fragment shader",e);
		}

		gl.glShaderSource(fragShader, 1, fragCode, null);
		gl.glCompileShader(fragShader);
		
		// acquire compilation status
		IntBuffer shaderStatus = IntBuffer.allocate(1);
		gl.glGetShaderiv(fragShader, GL3.GL_COMPILE_STATUS, shaderStatus);

		// check whether compilation was successful
		if (shaderStatus.get() == GL.GL_FALSE) {
			printShaderInfoLog(gl, fragShader);
			throw new IllegalStateException("compilation error for shader ["
					+ filename + "].");
		}
		printShaderInfoLog(gl, fragShader);

		return fragShader;
	}
	
	/*
	 * Prints the log to System.out
	 */
	public static boolean printShaderInfoLog(GL3 gl, int shader) {
		IntBuffer ival = IntBuffer.allocate(1);
		gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH,
				ival);

		int size = ival.get();
		if (size > 1) {
			ByteBuffer log = ByteBuffer.allocate(size);
			ival.flip();
			gl.glGetShaderInfoLog(shader, size, ival, log);
			byte[] infoBytes = new byte[size];
			log.get(infoBytes);
			System.out.println("Info log: " + new String(infoBytes));
			return true;
		}
		return false;
	 }
	
	/*
	 * Prints the log to System.out
	 */
	public static boolean printProgramInfoLog(GL3 gl, int prog) {
		IntBuffer ival = IntBuffer.allocate(1);
		gl.glGetProgramiv(prog, GL3.GL_INFO_LOG_LENGTH,
				ival);

		int size = ival.get();
		if (size > 1) {
			ByteBuffer log = ByteBuffer.allocate(size);
			ival.flip();
			gl.glGetProgramInfoLog(prog, size, ival, log);
			byte[] infoBytes = new byte[size];
			log.get(infoBytes);
			System.out.println("Info log: " + new String(infoBytes));
			return true;
		}
		return false;
	 }
}