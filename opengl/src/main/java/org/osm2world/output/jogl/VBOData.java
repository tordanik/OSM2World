package org.osm2world.output.jogl;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static org.osm2world.math.algorithms.GeometryUtil.*;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.Primitive;
import org.osm2world.output.common.Primitive.Type;
import org.osm2world.output.common.material.Material;
import org.osm2world.output.common.material.Material.Interpolation;

import com.jogamp.opengl.GL;

/**
 * Base class that keeps a VBO id along with associated information.
 */
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

	/** create a buffer to store the vbo data for upload to graphics memory */
	protected abstract BufferT createBuffer(int numValues);

	/** add a texture coordinate to the vbo buffer */
	protected abstract void put(BufferT buffer, VectorXZ texCoord);

	/** add 3d vertex data to the vbo buffer */
	protected abstract void put(BufferT buffer, VectorXYZ v);

	/** returns the size of each value in the vbo */
	protected abstract int valueTypeSize();

	/** returns the gl constant for the value type in the vbo */
	protected abstract int glValueType();

	private GL gl;
	protected JOGLTextureManager textureManager;

	/**
	 * Creates a new vertex buffer object, adds all primitives to the buffer and uploads it to graphics memory.
	 */
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

			primVertices = triangleVertexListFromTriangleStrip(primVertices);

			if (material.getInterpolation() == Interpolation.FLAT) {
				primNormals = triangleNormalListFromTriangleStripOrFan(primNormals);
			} else {
				primNormals = triangleVertexListFromTriangleStrip(primNormals);
			}

			if (primTexCoordLists != null) {
				List<List<VectorXZ>> newPrimTexCoordLists = new ArrayList<List<VectorXZ>>();
				for (List<VectorXZ> primTexCoordList : primTexCoordLists) {
					newPrimTexCoordLists.add(triangleVertexListFromTriangleStrip(primTexCoordList));
				}
				primTexCoordLists = newPrimTexCoordLists;
			}

		} else if (primitive.type == Type.TRIANGLE_FAN) {

			primVertices = triangleVertexListFromTriangleFan(primVertices);

			if (material.getInterpolation() == Interpolation.FLAT) {
				primNormals = triangleNormalListFromTriangleStripOrFan(primNormals);
			} else {
				primNormals = triangleVertexListFromTriangleFan(primNormals);
			}

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

			for (int t = 0; t < material.getNumTextureLayers(); t++) {
				if (primTexCoordLists == null || primTexCoordLists.get(t) == null) {
					put(buffer, VectorXZ.NULL_VECTOR);
					//TODO print some kind of warning (or do it earlier, in AbstractJoglTarget.drawPrimitive)
				} else {
					VectorXZ textureCoord = primTexCoordLists.get(t).get(i);
					put(buffer, textureCoord);
				}
			}

			put(buffer, primNormals.get(i));
			put(buffer, primVertices.get(i));

		}

	}

	/**
	 * Bind and render this vertex buffer object.
	 */
	public abstract void render();

	/**
	 * Returns the number of values for each vertex in the vertex buffer layout appropriate for a given material.
	 */
	protected abstract int getValuesPerVertex(Material material);

	/**
	 * Delete the vertex buffer object from graphics memory.
	 */
	public void delete() {
		gl.glDeleteBuffers(id.length, id, 0);
	}
}
