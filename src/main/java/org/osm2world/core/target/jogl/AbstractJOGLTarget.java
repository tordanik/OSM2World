package org.osm2world.core.target.jogl;

import static java.util.Arrays.asList;
import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2.GL_POLYGON;
import static org.osm2world.core.target.jogl.NonAreaPrimitive.Type.*;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

/**
 * Common implementation base for the new shader based {@link JOGLTargetShader} and the old {@link JOGLTargetFixedFunction}
 */
public abstract class AbstractJOGLTarget extends PrimitiveTarget implements JOGLTarget {

	protected static final float AMBIENT_FACTOR = 0.5f;
	protected static final float SPECULAR_FACTOR = 0.0f;
	protected static final int SHININESS = 1;

	protected PrimitiveBuffer primitiveBuffer;
	protected List<NonAreaPrimitive> nonAreaPrimitives;
	protected JOGLRenderer renderer;
	protected JOGLTextureManager textureManager;
	protected JOGLRenderingParameters renderingParameters;
	protected GlobalLightingParameters globalLightingParameters;

	public AbstractJOGLTarget(GL gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		this.textureManager = new JOGLTextureManager(gl);
		this.renderingParameters = renderingParameters;
		this.globalLightingParameters = globalLightingParameters;
	}

	@Override
	public void render(Camera camera, Projection projection) {
		renderPart(camera, projection, 0, 1, 0, 1);
	}

	@Override
	protected void drawPrimitive(Primitive.Type type, Material material,
			List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {

		primitiveBuffer.drawPrimitive(type, material, vertices, normals, texCoordLists);

		// cache textures. they should not be loaded in the render function (see https://www.opengl.org/wiki/Common_Mistakes#glGenTextures_in_render_function)
		// in some situations even errors were encountered
		if (material.getNumTextureLayers() > 0) {
			for (TextureLayer layer : material.getTextureLayers()) {
				textureManager.getTextureForTextureData(layer.baseColorTexture);
			}
		}

	}

	private void drawNonAreaPrimitive(NonAreaPrimitive.Type type,
			Color color, int width, List<VectorXYZ> vs) {

		nonAreaPrimitives.add(new NonAreaPrimitive(
				type, color, width, vs));

	}

	@Override
	public void drawPoints(Color color, VectorXYZ... vs) {
		drawNonAreaPrimitive(POINTS, color, 1, asList(vs));
	}

	@Override
	public void drawLineStrip(Color color, int width, VectorXYZ... vs) {
		drawNonAreaPrimitive(LINE_STRIP, color, width, asList(vs));
	}

	@Override
	public void drawLineStrip(Color color, int width, List<VectorXYZ> vs) {
		drawNonAreaPrimitive(LINE_STRIP, color, width, vs);
	}

	@Override
	public void drawLineLoop(Color color, int width, List<VectorXYZ> vs) {
		drawNonAreaPrimitive(LINE_LOOP, color, width, vs);
	}

	/**
	 * set global lighting parameters. Using this method affects all primitives
	 * (even those from previous draw calls).
	 *
	 * @param parameters  parameter object; null disables lighting
	 */
	public void setGlobalLightingParameters(
			GlobalLightingParameters parameters) {

		this.globalLightingParameters = parameters;

	}

	/**
	 * set global rendering parameters. Using this method affects all primitives
	 * (even those from previous draw calls).
	 */
	public void setRenderingParameters(
			JOGLRenderingParameters renderingParameters) {

		this.renderingParameters = renderingParameters;

	}

	@Override
	public void reset() {
		this.primitiveBuffer = new PrimitiveBuffer();
		this.nonAreaPrimitives = new ArrayList<NonAreaPrimitive>();

		if (renderer != null) {
			renderer.freeResources();
			renderer = null;
		}
	}

	@Override
	public void freeResources() {

		textureManager.releaseAll();

		reset();

	}

	@Override
	public boolean isFinished() {
		return renderer != null;
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
	 * clears the rendering surface, the z buffer and the stencil buffer
	 *
	 * @param clearColor  background color before rendering any primitives;
	 *                     null uses a previously defined clear color
	 */
	public static final void clearGL(GL gl, Color clearColor) {

		if (clearColor != null) {
			float[] c = {0f, 0f, 0f};
			clearColor.getColorComponents(c);
			gl.glClearColor(c[0], c[1], c[2], clearColor.getAlpha()/255f);
		}

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

	}

	static final FloatBuffer getFloatBuffer(Color color) {
		float colorArray[] = {0, 0, 0, color.getAlpha() / 255f};
		color.getRGBColorComponents(colorArray);
		return FloatBuffer.wrap(colorArray);
	}
}

