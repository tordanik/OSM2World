package org.osm2world.target.jogl;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;

/**
 * Utility class to manage low level shader creation.
 */
public class ShaderManager {

	/**
	 * Loads the vertex shader from a resource file, compiles it and does error checking.
	 * @param resourceName path to the resource containing the shader code
	 * @return handle of the created vertex shader
	 */
	public static int createVertexShader(GL3 gl, String resourceName) {

		// get the unique id
		int vertShader = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		if (vertShader == 0)
			throw new RuntimeException("Unable to create vertex shader.");

		// create a single String array index to hold the shader code
		String[] vertCode = new String[1];
		vertCode[0] = "";
		String line;

		// open the file and read the contents into the String array.
		InputStream stream = loadShaderFile(resourceName);
		if (stream == null) {
			throw new RuntimeException("Vertex shader not found in classpath: \""+ resourceName +"\"");
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

			while ((line = reader.readLine()) != null) {
				vertCode[0] += line + "\n";
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
				throw new IllegalStateException("compilation error for shader [" + resourceName + "].");
			}
			printShaderInfoLog(gl, vertShader);

			// the int returned is now associated with the compiled shader
			return vertShader;

		} catch (IOException e) {
			throw new RuntimeException("Failed reading vertex shader \"" + resourceName + "\".",e);
		}

	}

	/**
	 * Loads the fragment shader from a resource file, compiles it and does error checking.
	 * @param filename path to the resource containing the shader code
	 * @return handle of the created fragment shader
	 */
	public static int createFragmentShader(GL3 gl, String filename) {

		int fragShader = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		if (fragShader == 0)
			return 0;

		String[] fragCode = new String[1];
		fragCode[0] = "";
		String line;

		InputStream stream = loadShaderFile(filename);
		if (stream == null) {
			throw new RuntimeException("Fragment shader not found in classpath: \""+ filename +"\"");
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

			while ((line = reader.readLine()) != null) {
				fragCode[0] += line + "\n";
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

		} catch (IOException e) {
			throw new RuntimeException("Failed reading fragment shader \"" + filename + "\".",e);
		}

	}

	/**
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

	/**
	 * Reads the program log into a String.
	 * @param prog handle to the shader program
	 * @return the program log as String
	 */
	public static String getProgramInfoLog(GL3 gl, int prog) {
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
			return new String(infoBytes);
		}
		return "";
	}

	/**
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

	/**
	 * Save a depth buffer texture to a file as png.
	 */
	public static void saveDepthBuffer(File file, int depthBufferHandle, int width, int height, GL2GL3 gl) {
		// create buffer to store image
		FloatBuffer buffer=FloatBuffer.allocate(width*height);//ByteBuffer.allocate(shadowMapWidth*shadowMapHeight*4).asFloatBuffer();

		// load image in buffer
		gl.glBindTexture(GL.GL_TEXTURE_2D, depthBufferHandle);
		gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL3.GL_DEPTH_COMPONENT, GL.GL_FLOAT, buffer);
		buffer.rewind();

		// create buffered image
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

		// copy data to buffered image
		WritableRaster wr = img.getRaster();
		for (int col=0; col<img.getWidth(); col++) {
			for (int row=0; row<img.getHeight(); row++) {
				float f = buffer.get(row*width+col);
				int v = (int)(f*255);
				wr.setPixel(col, height-1-row, new int[] {v});
			}
		}

		// save to file
		try {
			ImageIO.write(img, "png", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Tries to load the shader file from classpath. If unsuccessful tries to
	 * load from resources folder in filesystem. If also unsuccessful return
	 * <code>null</code>.
	 */
	private static InputStream loadShaderFile(String resourceName) {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
		if (stream == null) {
			File file = new File("resources/" + (resourceName.charAt(0) == '/' ? resourceName.substring(1) : resourceName));
			try {
				stream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
			}
		}
		return stream;
	}

}
