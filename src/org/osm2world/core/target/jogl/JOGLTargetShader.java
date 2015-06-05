package org.osm2world.core.target.jogl;

import java.awt.Color;
import java.io.File;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

public class JOGLTargetShader extends AbstractJOGLTarget implements JOGLTarget {
	private Shader shader;
	private GL3 gl;
	
	public JOGLTargetShader(GL3 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		super(gl);
		shader = new Shader(gl);
		this.gl = gl;
		reset();
	}

	@Override
	public void drawPoints(Color color, VectorXYZ... vs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawLineStrip(Color color, int width, VectorXYZ... vs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawLineStrip(Color color, int width, List<VectorXYZ> vs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawLineLoop(Color color, int width, List<VectorXYZ> vs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGlobalLightingParameters(GlobalLightingParameters parameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRenderingParameters(
			JOGLRenderingParameters renderingParameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawBackgoundImage(GL2 gl, File backgroundImage,
			int startPixelX, int startPixelY, int pixelWidth, int pixelHeight,
			JOGLTextureManager textureManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renderPart(Camera camera, Projection projection, double xStart,
			double xEnd, double yStart, double yEnd) {
		if (renderer == null) {
			throw new IllegalStateException("finish must be called first");
		}
	}
	
	@Override
	public void finish() {
		if (isFinished()) return;
		
		renderer = new JOGLRendererShader(gl, textureManager, primitiveBuffer);
	}
}
