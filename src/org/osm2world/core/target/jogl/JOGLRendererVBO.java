package org.osm2world.core.target.jogl;

import static java.lang.Math.*;
import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2GL3.GL_DOUBLE;
import static javax.media.opengl.fixedfunc.GLPointerFunc.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection.closestCardinal;
import static org.osm2world.core.target.jogl.JOGLTargetFixedFunction.*;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.media.opengl.GL2;

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
public class JOGLRendererVBO extends JOGLRenderer {
	
	private static final boolean DOUBLE_PRECISION_RENDERING = false;
	
	/** VBOs with static, non-alphablended geometry for each material */
	private List<VBOData<?>> vbos = new ArrayList<VBOData<?>>();
	
	/** alphablended primitives, need to be sorted by distance from camera */
	private List<PrimitiveWithMaterial> transparentPrimitives =
			new ArrayList<PrimitiveWithMaterial>();
	
	/**
	 * the camera direction that was the basis for the previous sorting
	 * of {@link #transparentPrimitives}.
	 */
	private CardinalDirection currentPrimitiveSortDirection = null;
	
	/**
	 * class that keeps a VBO id along with associated information
	 */
	private abstract class VBOData<BufferT extends Buffer> {
		
		/** material associated with this VBO, determines VBO layout */
		private Material material;
		
		/** array with one element containing the VBO id */
		private final int[] id;
		
		/** number of vertices in the vbo */
		private final int vertexCount;
		
		/** size of each value in the vbo */
		protected final int valueTypeSize;
		
		/** gl constant for the value type in the vbo */
		protected final int glValueType;

		
		protected abstract BufferT createBuffer(int numValues);
		
		protected abstract void put(BufferT buffer, VectorXZ texCoord);
		protected abstract void put(BufferT buffer, VectorXYZ v);
		
		protected abstract int valueTypeSize();
		protected abstract int glValueType();
		
		public VBOData(Material material, Collection<Primitive> primitives) {
			
			this.material = material;
			
			valueTypeSize = valueTypeSize();
			glValueType = glValueType();
			
			vertexCount = countVertices(primitives);
			
			/* create the buffer */
			
			id = new int[1];
			gl.glGenBuffers(1, id, 0);
			
			/* collect the data for the buffer */
			
			BufferT valueBuffer = createBuffer(
					vertexCount * getValuesPerVertex(material));
						
			for (Primitive primitive : primitives) {
				addPrimitiveToValueBuffer(valueBuffer, primitive);
			}
			
			valueBuffer.rewind();
			
			/* write the data into the buffer */
			
			gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);
			
			gl.glBufferData(
					GL_ARRAY_BUFFER,
					valueBuffer.capacity() * valueTypeSize,
					valueBuffer,
					GL_STATIC_DRAW);
			
		}
		
		/**
		 * returns the number of vertices required to represent a collection
		 * of primitives with individual triangles
		 */
		private int countVertices(Collection<Primitive> primitives) {
			
			int vertexCount = 0;
			
			for (Primitive primitive : primitives) {
				if (primitive.type == Type.TRIANGLES) {
					vertexCount += primitive.vertices.size();
				} else {
					vertexCount += 3 * (primitive.vertices.size() - 2);
				}
			}
			
			return vertexCount;
			
		}
		
		/**
		 * put the values for a primitive's vertices into the buffer
		 */
		private void addPrimitiveToValueBuffer(BufferT buffer,
				Primitive primitive) {
						
			/*
			 * rearrange the lists of vertices, normals and texture coordinates
			 * to turn triangle strips and triangle fans into separate triangles
			 */

			List<VectorXYZ> primVertices = primitive.vertices;
			List<VectorXYZ> primNormals = primitive.normals;
			List<List<VectorXZ>> primTexCoordLists = primitive.texCoordLists;
			
			if (primitive.type == Type.TRIANGLE_STRIP) {
				
				primVertices = triangleVertexListFromTriangleStrip(primVertices);
				primNormals = triangleNormalListFromTriangleStrip(primNormals);
				
				if (primTexCoordLists != null) {
					List<List<VectorXZ>> newPrimTexCoordLists = new ArrayList<List<VectorXZ>>();
					for (List<VectorXZ> primTexCoordList : primTexCoordLists) {
						newPrimTexCoordLists.add(triangleVertexListFromTriangleStrip(primTexCoordList));
					}
					primTexCoordLists = newPrimTexCoordLists;
				}
				
			} else if (primitive.type == Type.TRIANGLE_FAN) {
				
				primVertices = triangleVertexListFromTriangleFan(primVertices);
				primNormals = triangleVertexListFromTriangleFan(primNormals);
				
				if (primTexCoordLists != null) {
					List<List<VectorXZ>> newPrimTexCoordLists = new ArrayList<List<VectorXZ>>();
					for (List<VectorXZ> primTexCoordList : primTexCoordLists) {
						newPrimTexCoordLists.add(triangleVertexListFromTriangleFan(primTexCoordList));
					}
					primTexCoordLists = newPrimTexCoordLists;
				}
				
			}
			
			/* put the values into the buffer, in the right order */
			
			for (int i = 0; i < primVertices.size(); i++) {
				
				assert (primTexCoordLists == null
						&& material.getNumTextureLayers() == 0)
					|| (primTexCoordLists != null
						&& primTexCoordLists.size() == material.getNumTextureLayers())
					: "WorldModules need to provide the correct number of tex coords";
				
				if (primTexCoordLists == null && material.getNumTextureLayers() > 0) {
					System.out.println(material);
				}
					
				for (int t = 0; t < material.getNumTextureLayers(); t++) {
					VectorXZ textureCoord =	primTexCoordLists.get(t).get(i);
					put(buffer, textureCoord);
				}
				
				put(buffer, primNormals.get(i));
				put(buffer, primVertices.get(i));
				
			}
			
		}
		
		public void render() {
			
			for (int i = 0; i < JOGLTargetFixedFunction.MAX_TEXTURE_LAYERS; i++) {
				
				gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(i));
				
				if (i >= material.getNumTextureLayers()) {
					
					gl.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
					
				} else {
					
					gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
					
				}
				
			}
			
			gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(0));
			
			JOGLTargetFixedFunction.setMaterial(gl, material, textureManager);
			
			gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);
			
			setPointerLayout();
			
			gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);
			
		}

		private void setPointerLayout() {
			
			int stride = valueTypeSize * getValuesPerVertex(material);
			
			int offset = 0;
			
			for (int i = 0; i < material.getNumTextureLayers(); i++) {
				
				gl.glClientActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(i));
				gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
				
				gl.glActiveTexture(JOGLTargetFixedFunction.getGLTextureConstant(i));
				gl.glTexCoordPointer(2, glValueType, stride, offset);
				
				offset += 2 * valueTypeSize;
				
			}
			
			gl.glVertexPointer(3, glValueType, stride, offset + valueTypeSize() * 3);
			gl.glNormalPointer(glValueType, stride, offset);
			
		}

		public void delete() {
			gl.glDeleteBuffers(id.length, id, 0);
		}
		
	}
	
	private final class VBODataDouble extends VBOData<DoubleBuffer> {

		public VBODataDouble(Material material, Collection<Primitive> primitives) {
			super(material, primitives);
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
	
	private final class VBODataFloat extends VBOData<FloatBuffer> {

		public VBODataFloat(Material material, Collection<Primitive> primitives) {
			super(material, primitives);
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
	
	private static final class PrimitiveWithMaterial {
		
		public final Primitive primitive;
		public final Material material;
		
		private PrimitiveWithMaterial(Primitive primitive, Material material) {
			this.primitive = primitive;
			this.material = material;
		}
		
	}
	
	/**
	 * returns the number of values for each vertex
	 * in the vertex buffer layout appropriate for a given material.
	 */
	public static int getValuesPerVertex(Material material) {
		
		int numValues = 6; // vertex coordinates and normals
		
		if (material.getTextureDataList() != null) {
			numValues += 2 * material.getTextureDataList().size();
		}
		
		return numValues;
		
	}
	
	JOGLRendererVBO(GL2 gl, JOGLTextureManager textureManager,
			PrimitiveBuffer primitiveBuffer) {
		
		super(gl, textureManager);
		
		for (Material material : primitiveBuffer.getMaterials()) {
			
			if (material.getTransparency() == Transparency.TRUE) {
				
				for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {
					transparentPrimitives.add(
							new PrimitiveWithMaterial(primitive, material));
				}
				
			} else {
				
				Collection<Primitive> primitives = primitiveBuffer.getPrimitives(material);
				vbos.add(DOUBLE_PRECISION_RENDERING
						? new VBODataDouble(material, primitives)
						: new VBODataFloat(material, primitives));
				
			}
			
		}
		
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
	
	private void sortPrimitivesBackToFront(final Camera camera,
			final Projection projection) {
		
		if (projection.isOrthographic() &&
				abs(camera.getViewDirection().xz().angle() % (PI/2)) < 0.01 ) {
			
			/* faster sorting for cardinal directions */
			
			CardinalDirection closestCardinal = closestCardinal(camera.getViewDirection().xz().angle());
			
			if (closestCardinal.isOppositeOf(currentPrimitiveSortDirection)) {
				
				Collections.reverse(transparentPrimitives);
				
			} else if (closestCardinal != currentPrimitiveSortDirection) {
				
				Comparator<PrimitiveWithMaterial> comparator = null;
				
				switch(closestCardinal) {
				
				case N:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p2).z, primitivePos(p1).z);
						}
					};
					break;
					
				case E:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p2).x, primitivePos(p1).x);
						}
					};
					break;
					
				case S:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p1).z, primitivePos(p2).z);
						}
					};
					break;
					
				case W:
					comparator = new Comparator<PrimitiveWithMaterial>() {
						@Override
						public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
							return Double.compare(primitivePos(p1).x, primitivePos(p2).x);
						}
					};
					break;
					
				}
				
				Collections.sort(transparentPrimitives, comparator);
				
			}
			
			currentPrimitiveSortDirection = closestCardinal;
			
		} else {
			
			/* sort based on distance to camera */
			
			Collections.sort(transparentPrimitives, new Comparator<PrimitiveWithMaterial>() {
				@Override
				public int compare(PrimitiveWithMaterial p1, PrimitiveWithMaterial p2) {
					return Double.compare(
							distanceToCameraSq(camera, p2),
							distanceToCameraSq(camera, p1));
				}
			});
			
			currentPrimitiveSortDirection = null;
			
		}
		
	}
	
	private double distanceToCameraSq(Camera camera, PrimitiveWithMaterial p) {
		return primitivePos(p).distanceToSquared(camera.getPos());
	}
	
	private VectorXYZ primitivePos(PrimitiveWithMaterial p) {
		
		double sumX = 0, sumY = 0, sumZ = 0;
		
		for (VectorXYZ v : p.primitive.vertices) {
			sumX += v.x;
			sumY += v.y;
			sumZ += v.z;
		}
		
		return new VectorXYZ(sumX / p.primitive.vertices.size(),
				sumY / p.primitive.vertices.size(),
				sumZ / p.primitive.vertices.size());
		
	}
	
	@Override
	public void freeResources() {
		
		if (vbos != null) {
			for (VBOData<?> vbo : vbos) {
				vbo.delete();
			}
			vbos = null;
		}
		
		super.freeResources();
		
	}
	
}
