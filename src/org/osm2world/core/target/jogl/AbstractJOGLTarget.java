package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_LINES;
import static javax.media.opengl.GL.GL_LINE_LOOP;
import static javax.media.opengl.GL.GL_LINE_STRIP;
import static javax.media.opengl.GL.GL_POINTS;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.GL.GL_TRIANGLE_FAN;
import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;
import static javax.media.opengl.GL2.GL_POLYGON;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.media.opengl.GL2;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.world.data.WorldObject;

import com.jogamp.opengl.util.awt.TextRenderer;

public abstract class AbstractJOGLTarget implements JOGLTarget {

	@Override
	public Class<RenderableToJOGL> getRenderableType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void render(RenderableToJOGL renderable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beginObject(WorldObject object) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawTriangleStrip(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawTriangleFan(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawConvexPolygon(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawBox(Material material, VectorXYZ bottomCenter,
			VectorXZ faceDirection, double height, double width, double depth) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawColumn(Material material, Integer corners, VectorXYZ base,
			double height, double radiusBottom, double radiusTop,
			boolean drawBottom, boolean drawTop) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub

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
	public void setConfiguration(Configuration config) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void render(Camera camera, Projection projection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderPart(Camera camera, Projection projection, double xStart,
			double xEnd, double yStart, double yEnd) {
		// TODO Auto-generated method stub

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
	
	static final int getGLConstant(Type type) {
		switch (type) {
		case TRIANGLE_STRIP: return GL_TRIANGLE_STRIP;
		case TRIANGLE_FAN: return GL_TRIANGLE_FAN;
		case TRIANGLES: return GL_TRIANGLES;
		case CONVEX_POLYGON: return GL_POLYGON;
		default: throw new Error("programming error: unhandled primitive type");
		}
	}

	static final int getGLConstant(NonAreaPrimitive.Type type) {
		switch (type) {
		case POINTS: return GL_POINTS;
		case LINES: return GL_LINES;
		case LINE_STRIP: return GL_LINE_STRIP;
		case LINE_LOOP: return GL_LINE_LOOP;
		default: throw new Error("programming error: unhandled primitive type");
		}
	}
	
	/**
	 * clears the rendering surface and the z buffer
	 * 
	 * @param clearColor  background color before rendering any primitives;
	 *                     null uses a previously defined clear color
	 */
	public static final void clearGL(GL2 gl, Color clearColor) {
		
		if (clearColor != null) {
			float[] c = {0f, 0f, 0f};
			clearColor.getColorComponents(c);
			gl.glClearColor(c[0], c[1], c[2], 1.0f);
		}
		
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
	}
	
	private static final TextRenderer textRenderer = new TextRenderer(
			new Font("SansSerif", Font.PLAIN, 12), true, false);
	//needs quite a bit of memory, so it must not create an instance for each use!
	
	public static final void drawText(String string, Vector3D pos, Color color) {
		textRenderer.setColor(color);
		textRenderer.begin3DRendering();
		textRenderer.draw3D(string,
				(float)pos.getX(), (float)pos.getY(), -(float)pos.getZ(),
				0.05f);
	}

	public static final void drawText(String string, int x, int y,
			int screenWidth, int screenHeight, Color color) {
		textRenderer.beginRendering(screenWidth, screenHeight);
		textRenderer.setColor(color);
		textRenderer.draw(string, x, y);
		textRenderer.endRendering();
	}
	
}
