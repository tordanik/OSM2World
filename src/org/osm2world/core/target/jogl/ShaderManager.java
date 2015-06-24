package org.osm2world.core.target.jogl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

public class ShaderManager {
	
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
	 * Prints the shader log to System.out
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
	 * Prints the program log to System.out
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