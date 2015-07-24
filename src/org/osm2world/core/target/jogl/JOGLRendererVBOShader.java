package org.osm2world.core.target.jogl;

import static java.lang.Math.*;
import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2GL3.GL_DOUBLE;
import static javax.media.opengl.fixedfunc.GLPointerFunc.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection.closestCardinal;
import static org.osm2world.core.target.jogl.JOGLTargetShader.*;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;

import com.jogamp.common.nio.Buffers;

/**
 * renders the contents of a {@link PrimitiveBuffer} using JOGL.
 * Uses vertex buffer objects (VBO) to speed up the process.
 * 
 * If you don't need the renderer anymore, it's recommended to manually call
 * {@link #freeResources()} to delete the VBOs and other resources.
 */
public class JOGLRendererVBOShader extends JOGLRendererVBO {
	
	protected GL3 gl;
	protected AbstractPrimitiveShader shader;
	protected double[] boundingBox = null;
	
	
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
	
	JOGLRendererVBOShader(GL3 gl, JOGLTextureManager textureManager,
			PrimitiveBuffer primitiveBuffer, AxisAlignedBoundingBoxXZ xzBoundary) {
		
		super(textureManager);
		this.gl = gl;
		this.init(primitiveBuffer);
		
		for (Material m : primitiveBuffer.getMaterials()) {
			for (Primitive p : primitiveBuffer.getPrimitives(m)) {
				for (VectorXYZ v : p.vertices) {
					if (xzBoundary == null || xzBoundary.contains(v.xz())) {
						if (boundingBox == null) {
							boundingBox = new double[]{v.x, v.x, v.y, v.y, -v.z, -v.z};
						} else {
							if (v.x < boundingBox[0]) { boundingBox[0] = v.x; }
							if (v.x > boundingBox[1]) { boundingBox[1] = v.x; }
							if (v.y < boundingBox[2]) { boundingBox[2] = v.y; }
							if (v.y > boundingBox[3]) { boundingBox[3] = v.y; }
							if (-v.z < boundingBox[4]) { boundingBox[4] = -v.z; }
							if (-v.z > boundingBox[5]) { boundingBox[5] = -v.z; }
						}
					}
				}
			}
		}
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
	
	public void setShader(AbstractPrimitiveShader shader) {
		this.shader = shader;
	}
	
	public double[] getBoundingBox() {
		return boundingBox;
	}
}
