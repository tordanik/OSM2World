package org.osm2world.core.target.jogl;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;

import sun.font.FontFamily;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.opengl.util.glsl.ShaderState;

public class JOGLTargetShader extends AbstractJOGLTarget implements JOGLTarget {
	private Shader shader;
	private GL3 gl;
	
	public JOGLTargetShader(GL3 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		shader = new Shader(gl);
		this.gl = gl;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
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
	public boolean isFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void freeResources() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawBackgoundImage(GL2 gl, File backgroundImage,
			int startPixelX, int startPixelY, int pixelWidth, int pixelHeight,
			JOGLTextureManager textureManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void drawPrimitive(Type type, Material material,
			List<VectorXYZ> vs, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		// TODO Auto-generated method stub
		
	}
}
