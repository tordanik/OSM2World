package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static org.osm2world.core.math.GeometryUtil.triangleNormalListFromTriangleStripOrFan;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleFan;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleStrip;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.material.Material;

public abstract class VBOData<BufferT extends Buffer> {
	
	/** material associated with this VBO, determines VBO layout */
	protected Material material;
	
	/** array with one element containing the VBO id */
	protected final int[] id;
	
	/** number of vertices in the vbo */
	protected final int vertexCount;
	
	/** size of each value in the vbo */
	protected final int valueTypeSize;
	
	/** gl constant for the value type in the vbo */
	protected final int glValueType;

	protected abstract BufferT createBuffer(int numValues);
	
	protected abstract void put(BufferT buffer, VectorXZ texCoord);
	protected abstract void put(BufferT buffer, VectorXYZ v);
	
	protected abstract int valueTypeSize();
	protected abstract int glValueType();

	private GL gl;
	protected JOGLTextureManager textureManager;
	public VBOData(GL gl, JOGLTextureManager textureManager, Material material, Collection<Primitive> primitives) {
		
		this.gl = gl;
		this.textureManager = textureManager;
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
	static int countVertices(Collection<Primitive> primitives) {
		
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
	protected void addPrimitiveToValueBuffer(BufferT buffer,
			Primitive primitive) {
					
		/*
		 * rearrange the lists of vertices, normals and texture coordinates
		 * to turn triangle strips and triangle fans into separate triangles
		 */

		List<VectorXYZ> primVertices = primitive.vertices;
		List<VectorXYZ> primNormals = primitive.normals;
		List<List<VectorXZ>> primTexCoordLists = primitive.texCoordLists;
		
		if (primitive.type == Type.TRIANGLE_STRIP) {

			// TODO: support smooth interpolation of normals
			primVertices = triangleVertexListFromTriangleStrip(primVertices);
			primNormals = triangleNormalListFromTriangleStripOrFan(primNormals);
			
			if (primTexCoordLists != null) {
				List<List<VectorXZ>> newPrimTexCoordLists = new ArrayList<List<VectorXZ>>();
				for (List<VectorXZ> primTexCoordList : primTexCoordLists) {
					newPrimTexCoordLists.add(triangleVertexListFromTriangleStrip(primTexCoordList));
				}
				primTexCoordLists = newPrimTexCoordLists;
			}
			
		} else if (primitive.type == Type.TRIANGLE_FAN) {

			// TODO: support smooth interpolation of normals
			primVertices = triangleVertexListFromTriangleFan(primVertices);
			primNormals = triangleNormalListFromTriangleStripOrFan(primNormals);
			
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
	
	public abstract void render();
	
	protected abstract int getValuesPerVertex(Material material);

	public void delete() {
		gl.glDeleteBuffers(id.length, id, 0);
	}
}
