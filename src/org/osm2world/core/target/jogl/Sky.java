package org.osm2world.core.target.jogl;


import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;


import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import org.osm2world.core.target.common.lighting.GlobalLightingParameters;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.PMVMatrix;


// TODO Consider refactoring this
/**
 * Class that handles generation of procedural sky and writing that sky to a cubemap.
 **/
public class Sky {
	private static Framebuffer skyBuffer;
	private static SkyShader skyShader;
	
	public static Cubemap getSky() {
		if(skyBuffer == null)
			return null;
		return skyBuffer.getCubemap();
	}

	public static void updateSky(GL3 gl) {
		if(Sky.skyBuffer == null) {
			Sky.skyBuffer = new Framebuffer(GL3.GL_TEXTURE_CUBE_MAP);
			Sky.skyBuffer.init(gl);
		}

		if(Sky.skyShader == null) {
			Sky.skyShader = new SkyShader(gl);
		}

		Framebuffer skyBuffer = Sky.skyBuffer;
		SkyShader 	skyShader = Sky.skyShader;
		
		PMVMatrix cubePMV = new PMVMatrix();
		cubePMV.glMatrixMode(GL_MODELVIEW);
		cubePMV.glLoadIdentity();

		skyShader.useShader();
		skyShader.setLighting(GlobalLightingParameters.DEFAULT);

		// Buffer the cube vertices, this buffer will be used six times
		int[] t = new int[1];
		gl.glGenBuffers(1, t, 0);

		FloatBuffer vertBuf = FloatBuffer.wrap(Cubemap.VERTS);

		gl.glBindBuffer(GL_ARRAY_BUFFER, t[0]);
		gl.glBufferData(
				GL_ARRAY_BUFFER,
				vertBuf.capacity() * Buffers.SIZEOF_FLOAT,
				vertBuf,
				GL_STATIC_DRAW);

		gl.glEnableVertexAttribArray(skyShader.getVertexPositionID());
		gl.glVertexAttribPointer(skyShader.getVertexPositionID(), 3, GL.GL_FLOAT, false, 0, 0);
			
		for(int i = 0; i < 6; i ++) {
			skyBuffer.bind(GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i);
			gl.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
			gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
			
			cubePMV.glLoadIdentity();

			// TODO Verify these camera positions
			switch(i + GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X) {
				case GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X:
					cubePMV.gluLookAt(
								0.0f, 0.0f, 0.0f,
								1.0f, 0.0f, 0.0f,
								0.0f, -1.0f, 0.0f);
					break;

				case GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
					cubePMV.gluLookAt(
								0.0f, 0.0f, 0.0f,
								-1.0f, 0.0f, 0.0f,
								0.0f, -1.0f, 0.0f);
					break;

				case GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
					cubePMV.gluLookAt(
								0.0f, 0.0f, 0.0f,
								0.0f, 1.0f, 0.0f,
								0.0f, 0.0f, 1.0f);
					break;

				case GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
					cubePMV.gluLookAt(
								0.0f, 0.0f, 0.0f,
								0.0f, -1.0f, 0.0f,
								0.0f, 0.0f, 1.0f);
					break;

				case GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
					cubePMV.gluLookAt(
								0.0f, 0.0f, 0.0f,
								0.0f, 0.0f, 1.0f,
								0.0f, -1.0f, 0.0f);
					break;

				case GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
					cubePMV.gluLookAt(
								0.0f, 0.0f, 0.0f,
								0.0f, 0.0f, -1.0f,
								0.0f, -1.0f, 0.0f);
					break;

			}

			skyShader.setPMVMatrix(cubePMV);

			gl.glDrawArrays(GL.GL_TRIANGLES, 0, Cubemap.VERTS.length / 3);
			
			skyBuffer.unbind();
		}

		gl.glDisableVertexAttribArray(skyShader.getVertexPositionID());
		skyShader.disableShader();
	}
}

