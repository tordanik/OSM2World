package org.osm2world.output.jogl;

import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL2GL3.GL_DOUBLE;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXYZW;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedBoundingBoxXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.Primitive;
import org.osm2world.scene.material.Material;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;

/**
 * Renders the contents of a {@link PrimitiveBuffer} using JOGL and the new shader based OpenGL pipeline.
 * Uses vertex buffer objects (VBO) to speed up the process.
 *
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the VBOs and other resources.
 */
public class JOGLRendererVBOShader extends JOGLRendererVBO {

	protected GL3 gl;
	protected AbstractPrimitiveShader shader;
	protected AxisAlignedBoundingBoxXYZ boundingBox = null;

	private final class VBODataDouble extends VBODataShader<DoubleBuffer> {

		public VBODataDouble(GL3 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
			super(gl, textureManager, material, primitives);
		}

		@Override
		protected DoubleBuffer createBuffer(int numValues) {
			return Buffers.newDirectDoubleBuffer(numValues);
		}

		@Override
		protected void put(DoubleBuffer buffer, VectorXZ texCoord) {
			buffer.put(texCoord.x);
			buffer.put(texCoord.z);
		}

		@Override
		protected void put(DoubleBuffer buffer, VectorXYZ v) {
			buffer.put(v.x);
			buffer.put(v.y);
			buffer.put(-v.z);
		}

		@Override
		protected void put(DoubleBuffer buffer, VectorXYZW t) {
			buffer.put(t.x);
			buffer.put(t.y);
			buffer.put(-t.z);
			buffer.put(t.w);
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

	private final class VBODataFloat extends VBODataShader<FloatBuffer> {

		public VBODataFloat(GL3 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
			super(gl, textureManager, material, primitives);
		}

		@Override
		protected FloatBuffer createBuffer(int numValues) {
			return Buffers.newDirectFloatBuffer(numValues);
		}

		@Override
		protected void put(FloatBuffer buffer, VectorXZ texCoord) {
			buffer.put((float)texCoord.x);
			buffer.put((float)texCoord.z);
		}

		@Override
		protected void put(FloatBuffer buffer, VectorXYZ v) {
			buffer.put((float)v.x);
			buffer.put((float)v.y);
			buffer.put((float)-v.z);
		}

		@Override
		protected void put(FloatBuffer buffer, VectorXYZW t) {
			buffer.put((float)t.x);
			buffer.put((float)t.y);
			buffer.put((float)-t.z);
			buffer.put((float)t.w);
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
	 * Creates vertex buffer objects for all primitives and computes a bounding box around them.
	 * @param primitiveBuffer the primitives to create the VBOs for
	 * @param xzBoundary the boundary of the OSM file. Used to tighten the bounding box to only primitives within these bounds.
	 */
	JOGLRendererVBOShader(GL3 gl, JOGLTextureManager textureManager,
			PrimitiveBuffer primitiveBuffer, AxisAlignedRectangleXZ xzBoundary) {

		super(textureManager);
		this.gl = gl;
		this.init(primitiveBuffer);

		ArrayList<VectorXYZ> boundedVertices = new ArrayList<VectorXYZ>();
		for (Material m : primitiveBuffer.getMaterials()) {
			for (Primitive p : primitiveBuffer.getPrimitives(m)) {
				for (VectorXYZ v : p.vertices) {
					if (xzBoundary == null || xzBoundary.contains(v.xz())) {
						boundedVertices.add(new VectorXYZ(v.x, v.y, -v.z));
					}
				}
			}
		}
		boundingBox = new AxisAlignedBoundingBoxXYZ(boundedVertices);
	}

	@Override
	VBOData<?> createVBOData(JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		if (DOUBLE_PRECISION_RENDERING)
			return new VBODataDouble(gl, textureManager, material, primitives);
		else
			return new VBODataFloat(gl, textureManager, material, primitives);
	}

	/**
	 * Render the stored VBOS. Uses the currently set shader. Transparent primitives are not sorted.
	 * If they have to be sorted for the set shader then use {@link #render(Camera, Projection)}.
	 */
	public void render() {

		/* render static geometry */

		shader.glEnableVertexAttribArray(shader.getVertexPositionID());
		shader.glEnableVertexAttribArray(shader.getVertexNormalID());

		for (VBOData<?> vboData : vbos) {
			((VBODataShader<?>)vboData).setShader(shader);
			vboData.render();
		}

		/* render transparent primitives unsorted */

		for (PrimitiveWithMaterial p : transparentPrimitives) {
			((VBODataShader<?>)p.vbo).setShader(shader);
			p.vbo.render();
		}

		shader.glDisableVertexAttribArray(shader.getVertexPositionID());
		shader.glDisableVertexAttribArray(shader.getVertexNormalID());

	}

	/**
	 * Render the stored VBOs. Uses the currently set shader. Transparent objects get sorted first back to front
	 * relative to the given camera and projection.
	 */
	@Override
	public void render(final Camera camera, final Projection projection) {

		/* render static geometry */

		shader.glEnableVertexAttribArray(shader.getVertexPositionID());
		shader.glEnableVertexAttribArray(shader.getVertexNormalID());

		for (VBOData<?> vboData : vbos) {
			((VBODataShader<?>)vboData).setShader(shader);
			vboData.render();
		}

		/* render transparent primitives back-to-front */

		sortPrimitivesBackToFront(camera, projection);

		for (PrimitiveWithMaterial p : transparentPrimitives) {
			((VBODataShader<?>)p.vbo).setShader(shader);
			p.vbo.render();
		}

		shader.glDisableVertexAttribArray(shader.getVertexPositionID());
		shader.glDisableVertexAttribArray(shader.getVertexNormalID());

	}

	@Override
	public void freeResources() {
		gl = null;
		super.freeResources();
	}

	/**
	 * Change the shader used to render the VBOs
	 */
	public void setShader(AbstractPrimitiveShader shader) {
		this.shader = shader;
	}

	/**
	 * Get the bounding box around all relevant primitives computed at {@link #JOGLRendererVBOShader(GL3, JOGLTextureManager, PrimitiveBuffer, AxisAlignedRectangleXZ)}
	 */
	public AxisAlignedBoundingBoxXYZ getBoundingBox() {
		return boundingBox;
	}
}
