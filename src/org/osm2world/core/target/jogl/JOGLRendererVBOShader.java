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

import org.osm2world.core.math.VectorXYZ;
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
	private DefaultShader shader;
	
	
	
	private final class VBODataDouble extends VBODataShader<DoubleBuffer> {

		public VBODataDouble(GL3 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
			super(gl, shader, textureManager, material, primitives);
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
	
	private final class VBODataFloat extends VBODataShader<FloatBuffer> {

		public VBODataFloat(GL3 gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
			super(gl, shader, textureManager, material, primitives);
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
	
	JOGLRendererVBOShader(GL3 gl, DefaultShader shader, JOGLTextureManager textureManager,
			PrimitiveBuffer primitiveBuffer) {
		
		super(textureManager);
		this.gl = gl;
		this.shader = shader;
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

		
		gl.glEnableVertexAttribArray(shader.getVertexPositionID());
		//gl.glEnableVertexAttribArray(shader.getVertexColorID());
		gl.glEnableVertexAttribArray(shader.getVertexNormalID());
		
		for (VBOData<?> vboData : vbos) {
			vboData.render();
		}
		
//		for (int t = 0; t < JOGLTargetFixedFunction.MAX_TEXTURE_LAYERS; t++) {
//			gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(t));
//			gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
//		}
//		
//		gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		
		/* render transparent primitives back-to-front */
		
		sortPrimitivesBackToFront(camera, projection);
		
//		Material previousMaterial = null;
		
		for (PrimitiveWithMaterial p : transparentPrimitives) {
			
//			if (!p.material.equals(previousMaterial)) {
//				shader.setMaterial(p.material, textureManager);
//				previousMaterial = p.material;
//			}
			
			p.vbo.render();
//			drawPrimitive(gl, AbstractJOGLTarget.getGLConstant(p.primitive.type),
//					p.primitive.vertices, p.primitive.normals,
//					p.primitive.texCoordLists);
			
		}
		
		gl.glDisableVertexAttribArray(shader.getVertexPositionID());
		//gl.glDisableVertexAttribArray(shader.getVertexColorID());
		gl.glDisableVertexAttribArray(shader.getVertexNormalID());
		//gl.glDisableVertexAttribArray(shader.getVertexTexCoordID());
		
	}
	
	@Override
	public void freeResources() {
		gl = null;
		super.freeResources();
	}
	
}
