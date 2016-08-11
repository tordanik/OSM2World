package org.osm2world.core.target.jogl;


import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.PMVMatrix;

import java.awt.Color;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.math.VectorXYZ;

import java.util.Calendar;
import org.osm2world.core.target.common.lighting.LightSource;


// TODO Absolutly refactor this
/**
 * Class that handles generation of procedural sky and writing that sky to a cubemap.
 **/
public class Sky {
	private static Framebuffer skyBuffer;
	private static SkyShader skyShader;

	private static LightSource sun;
	private static long time;
	public static Color scatterColor = new Color(0.55f, 0.7f, 0.8f);

	static {
		Calendar c = Calendar.getInstance();
		c.set(2001,0,1,12,0,0);
		Sky.setTime(c);
	}
	
	public static Cubemap getSky() {
		if(skyBuffer == null)
			return null;
		return skyBuffer.getCubemap();
	}

	public static long getTime() {
		return Sky.time;
	}

	public static void updateSky(GL3 gl) {
		System.out.println("Update sky");
		if(Sky.skyBuffer == null) {
			Sky.skyBuffer = new Framebuffer(GL3.GL_TEXTURE_CUBE_MAP, 800, 800, false);
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
		skyShader.setSun(Sky.sun, scatterColor);

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
								0.0f, 0.0f, -1.0f);
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

	public static void setTime(Calendar date)
	{
		System.out.println("SkyTime");
		double lon = 0;//48.75;
		double lat = 0;//13.46;

		Sky.time = date.getTimeInMillis();

		System.out.println("Calculating sun position on: " + date.getTime().toString() + "...");
		int days = date.get(Calendar.DAY_OF_YEAR);

		double time = date.get(Calendar.HOUR_OF_DAY) + (date.get(Calendar.MINUTE) / 60.0);

		double solarMeridian = Math.round(lon / 15) * 15;

		// Correction due to earth's position in orbit
		double b = Math.toRadians(360.0/365.0 * (days - 81));
		double equationOfTime = 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b);

		// Correction due to longitudinal variations
		double timeCorrection = 4 * (lon - solarMeridian) + equationOfTime;

		double solarTime = time + (timeCorrection / 60.0);

		double hourAngle = Math.toRadians(15) * (solarTime - 12);

		double declinationAngle = Math.asin(Math.sin(Math.toRadians(23.45)) * Math.sin(b));

		double elevationAngle = Math.asin(Math.sin(declinationAngle) * Math.sin(lat)
						+ Math.cos(declinationAngle) * Math.cos(lat) * Math.cos(hourAngle));

		double zenithAngle = (Math.PI / 2.0) - elevationAngle;

		// From https://en.wikipedia.org/wiki/Solar_azimuth_angle
		double azimuthAngle = Math.acos((Math.sin(declinationAngle) - Math.cos(zenithAngle) * Math.sin(lat))
						/ (Math.sin(zenithAngle) * Math.cos(lat)));

		if(hourAngle > 0) azimuthAngle = (Math.PI * 2 - azimuthAngle);

		System.out.println("Elevation Angle: " + Math.toDegrees(elevationAngle));
		System.out.println("Azimuth Angle: " + Math.toDegrees(azimuthAngle));
		System.out.println(elevationAngle > 0?"Day":"Night");

		double x = Math.cos(elevationAngle) * Math.sin(azimuthAngle);
		double y = Math.cos(elevationAngle) * Math.cos(azimuthAngle);
		double z = Math.sin(elevationAngle);

		System.out.println("Sun Vector: <" + x +", " + y +", " + z + ">");
		System.out.println("------------------");
		System.out.println("");

		VectorXYZW sunVector = new VectorXYZW(x, z, y, 0.0);

		Color night = new Color(0.0f, 0f, 0.2f);
		Color twilight = new Color(0.2f, 0.2f, 0.4f);
		Color day = new Color(1.0f, 1.0f, 0.95f);
		Color sunset = new Color(1.0f, 197.0f/255.0f, 143.0f/255.0f);

		Color la;
		if(Math.toDegrees(elevationAngle) > 8)
			la = day;
		else if(Math.toDegrees(elevationAngle) > 0)
			la = sunset;
		else if(Math.toDegrees(elevationAngle) > -19)
			la = twilight;
		else
			la = night;

		Sky.sun = new LightSource(sunVector, la);

		// Moonlight
		// if(Math.toDegrees(elevationAngle) < 0) {
		//	lightFromDirection = new VectorXYZ(-x, -z, y);
		//	intensity = 0.3f;
		// }
		// else
		// 	intensity = 1.0f;
	}

	public static LightSource getSun() {
		return Sky.sun;
	}
}

