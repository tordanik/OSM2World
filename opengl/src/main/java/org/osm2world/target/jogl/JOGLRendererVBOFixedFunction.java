package org.osm2world.target.jogl;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2GL3.GL_DOUBLE;
import static com.jogamp.opengl.fixedfunc.GLPointerFunc.*;
import static org.osm2world.target.jogl.JOGLTargetFixedFunction.drawPrimitive;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.target.common.Primitive;
import org.osm2world.target.common.material.Material;
import org.osm2world.target.common.rendering.Camera;
import org.osm2world.target.common.rendering.Projection;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

/**
 * Renders the contents of a {@link PrimitiveBuffer} using JOGL and the old fixed function OpengGL pipeline.
 * Uses vertex buffer objects (VBO) to speed up the process.
 *
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the VBOs and other resources.
 */
public class JOGLRendererVBOFixedFunction extends JOGLRendererVBO {

	protected GL2 gl;

	private final class VBODataDouble extends VBODataFixedFunction<DoubleBuffer> {

		public VBODataDouble(GL2 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
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
		protected int valueTypeSize() {
			return Buffers.SIZEOF_DOUBLE;
		}

		@Override
		protected int glValueType() {
			return GL_DOUBLE;
		}

	}

	private final class VBODataFloat extends VBODataFixedFunction<FloatBuffer> {

		public VBODataFloat(GL2 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
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
		protected int valueTypeSize() {
			return Buffers.SIZEOF_FLOAT;
		}

		@Override
		protected int glValueType() {
			return GL_FLOAT;
		}

	}

	JOGLRendererVBOFixedFunction(GL2 gl, JOGLTextureManager textureManager,
			PrimitiveBuffer primitiveBuffer) {

		super(textureManager);
		this.gl = gl;
		this.init(primitiveBuffer);
	}

	@Override
	VBOData<?> createVBOData(JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		if (DOUBLE_PRECISION_RENDERING)
			return new VBODataDouble(gl, textureManager, material, primitives);
		else
			return new VBODataFloat(gl, textureManager, material, primitives);
	}

	@Override
	public void render(final Camera camera, final Projection projection) {

		/* render static geometry */

		gl.glEnableClientState(GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL_NORMAL_ARRAY);

		for (VBOData<?> vboData : vbos) {
			vboData.render();
		}

		gl.glDisableClientState(GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL_NORMAL_ARRAY);

		for (int t = 0; t < JOGLTargetFixedFunction.MAX_TEXTURE_LAYERS; t++) {
			gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(t));
			gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		}

		gl.glBindBuffer(GL_ARRAY_BUFFER, 0);


		/* render transparent primitives back-to-front */

		sortPrimitivesBackToFront(camera, projection);

		Material previousMaterial = null;

		for (PrimitiveWithMaterial p : transparentPrimitives) {

			if (!p.material.equals(previousMaterial)) {
				JOGLTargetFixedFunction.setMaterial(gl, p.material, textureManager);
				previousMaterial = p.material;
			}

			drawPrimitive(gl, AbstractJOGLTarget.getGLConstant(p.primitive.type),
					p.primitive.vertices, p.primitive.normals,
					p.primitive.texCoordLists);

		}

	}

	@Override
	public void freeResources() {
		gl = null;
		super.freeResources();
	}

}
