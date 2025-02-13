package org.osm2world.output.jogl;

import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL2GL3.GL_DOUBLE;

import java.awt.Color;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXYZ;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;

/**
 * Renders the a bunch of {@link NonAreaPrimitive} objects using JOGL and the new shader based OpengGL pipeline.
 * Uses vertex buffer objects (VBO) to speed up the process.
 *
 * If you don't need the renderer anymore, it's recommended to manually call
 * #freeResources() to delete the VBOs and other resources.
 * TODO fix the reference to #freeResources() (method does not exist)
 */
public class JOGLRendererVBONonAreaShader {

	protected static final boolean DOUBLE_PRECISION_RENDERING = false;

	protected GL3 gl;
	private NonAreaShader shader;
	protected List<VBODataNonAreaShader<?>> vbos = new ArrayList<VBODataNonAreaShader<?>>();

	private final class VBODataDouble extends VBODataNonAreaShader<DoubleBuffer> {

		public VBODataDouble(GL3 gl, NonAreaPrimitive primitive) {
			super(gl, shader, primitive);
		}

		@Override
		protected DoubleBuffer createBuffer(int numValues) {
			return Buffers.newDirectDoubleBuffer(numValues);
		}

		@Override
		protected void put(DoubleBuffer buffer, Color color) {
			buffer.put(color.getRed()/255d);
			buffer.put(color.getGreen()/255d);
			buffer.put(color.getBlue()/255d);
			buffer.put(color.getAlpha()/255d);
		}

		@Override
		protected void put(DoubleBuffer buffer, VectorXYZ v) {
			buffer.put(v.x);
			buffer.put(v.y);
			buffer.put(-v.z);
		}

		@Override
		protected int valueTypeSize() {
			return Buffers.SIZEOF_DOUBLE;
		}

		@Override
		protected int glValueType() {
			return GL_DOUBLE;
		}

	}

	private final class VBODataFloat extends VBODataNonAreaShader<FloatBuffer> {

		public VBODataFloat(GL3 gl, NonAreaPrimitive primitive) {
			super(gl, shader, primitive);
		}

		@Override
		protected FloatBuffer createBuffer(int numValues) {
			return Buffers.newDirectFloatBuffer(numValues);
		}

		@Override
		protected void put(FloatBuffer buffer, Color color) {
			buffer.put(color.getRed()/255f);
			buffer.put(color.getGreen()/255f);
			buffer.put(color.getBlue()/255f);
			buffer.put(color.getAlpha()/255f);
		}

		@Override
		protected void put(FloatBuffer buffer, VectorXYZ v) {
			buffer.put((float)v.x);
			buffer.put((float)v.y);
			buffer.put((float)-v.z);
		}

		@Override
		protected int valueTypeSize() {
			return Buffers.SIZEOF_FLOAT;
		}

		@Override
		protected int glValueType() {
			return GL_FLOAT;
		}

	}

	/**
	 * Creates vertex buffer objects for all primitives.
	 * @param shader the shader used for rendering
	 * @param primitives the primitives to render
	 */
	public JOGLRendererVBONonAreaShader(GL3 gl, NonAreaShader shader, Collection<NonAreaPrimitive> primitives) {
		this.gl = gl;
		this.shader = shader;
		for (NonAreaPrimitive nonAreaPrimitive : primitives) {
			VBODataNonAreaShader<?> vbo;
			if (DOUBLE_PRECISION_RENDERING)
				vbo = new VBODataDouble(gl, nonAreaPrimitive);
			else
				vbo = new VBODataFloat(gl, nonAreaPrimitive);
			vbos.add(vbo);
		}
	}

	/**
	 * Renders all VBOs for the {@link NonAreaPrimitive} objects.
	 */
	public void render() {

		gl.glEnableVertexAttribArray(shader.getVertexPositionID());
		gl.glEnableVertexAttribArray(shader.getVertexColorID());

		// render non area primitives
		for (VBODataNonAreaShader<?> vbo : vbos) {

			vbo.render();

		}

		gl.glDisableVertexAttribArray(shader.getVertexPositionID());
		gl.glDisableVertexAttribArray(shader.getVertexColorID());
	}
}
