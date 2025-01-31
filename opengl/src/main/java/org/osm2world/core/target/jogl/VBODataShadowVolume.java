package org.osm2world.core.target.jogl;

import static com.jogamp.opengl.GL.*;
import static org.osm2world.core.math.algorithms.GeometryUtil.triangleVertexListFromTriangleFan;
import static org.osm2world.core.math.algorithms.GeometryUtil.triangleVertexListFromTriangleStrip;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.math.algorithms.GeometryUtil;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;

import com.jogamp.opengl.GL3;

/**
 * class that keeps a VBO id along with associated information for shadow volumes
 */
abstract class VBODataShadowVolume<BufferT extends Buffer> {

	protected GL3 gl;
	protected AbstractPrimitiveShader shader;

	/** position of the light source casting the shadow volumes */
	protected VectorXYZW lightPos;

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

	/** returns the size of each value in the vbo */
	protected abstract int valueTypeSize();

	/** returns the gl constant for the value type in the vbo */
	protected abstract int glValueType();

	/** add 4d shadow volume vertex data to the vbo buffer */
	protected abstract void put(BufferT buffer, VectorXYZW sv);

	/**
	 * Creates a new vertex buffer object, calculates the shadow volumes for all
	 * primitives and adds them to the buffer and uploads it to graphics memory.
	 */
	public VBODataShadowVolume(GL3 gl, Collection<Primitive> primitives, VectorXYZW lightPos) {

		this.gl = gl;
		this.lightPos = lightPos;

		valueTypeSize = valueTypeSize();
		glValueType = glValueType();

		//vertexCount = VBOData.countVertices(primitives)*8;

		/* create the buffer */

		id = new int[1];
		gl.glGenBuffers(1, id, 0);

		/* collect the data for the buffer */

		List<VectorXYZW> shadowVolumeVertices = new ArrayList<VectorXYZW>();
		for (Primitive primitive : primitives) {
			shadowVolumeVertices.addAll(getPrimitivesShadowVolumes(primitive));
		}
		vertexCount = shadowVolumeVertices.size();

		BufferT valueBuffer = createBuffer(
				vertexCount * getValuesPerVertex());

		addVerticesToValueBuffer(valueBuffer, shadowVolumeVertices);

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
	 * Set the shader this VBO uses when rendering the shadow volumes.
	 */
	public void setShader(AbstractPrimitiveShader shader) {
		this.shader = shader;
	}

	/**
	 * Put the values of a shadow volume into the buffer.
	 */
	protected void addVerticesToValueBuffer(BufferT buffer,
			List<VectorXYZW> shadowVolumeVertices) {

		/* put the values into the buffer, in the right order */
		for (VectorXYZW v : shadowVolumeVertices) {
			put(buffer, v);
		}

	}

	/**
	 * Calculate the shadow volume for a primitive.
	 */
	protected List<VectorXYZW> getPrimitivesShadowVolumes(Primitive primitive) {

		/*
		 * rearrange the lists of vertices, normals and texture coordinates
		 * to turn triangle strips and triangle fans into separate triangles
		 */

		List<VectorXYZ> primVertices = primitive.vertices;

		if (primitive.type == Type.TRIANGLE_STRIP) {

			primVertices = triangleVertexListFromTriangleStrip(primVertices);

		} else if (primitive.type == Type.TRIANGLE_FAN) {

			primVertices = triangleVertexListFromTriangleFan(primVertices);

		}

		/*
		 *  NOTE: performance could be improved a lot if shadow volume geometry would be minimized.
		 *  Suggestions:
		 *   * calculate volumes only for silhouette
		 *   * skip back facing triangles (from light perspective). All shadow casting objectes have to be closed then.
		 *   * use low poly model for shadow volume generation (only useful for high poly objects)
		 */
		List<VectorXYZW> shadowVolumeVertices = GeometryUtil.calculateShadowVolumesPerTriangle(primVertices, lightPos);
		return shadowVolumeVertices;

	}

	/**
	 * Bind and render this vertex buffer object.
	 */
	public void render() {
		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);

		setPointerLayout();

		gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);
	}

	private void setPointerLayout() {

		int stride = valueTypeSize * getValuesPerVertex();

		int offset = 0;

		shader.glVertexAttribPointer(shader.getVertexPositionID(), 4, glValueType(), false, stride, offset);
	}

	/**
	 * Returns the number of values for each vertex in the vertex buffer layout.
	 */
	protected int getValuesPerVertex() {
		return 4;
	}

	/**
	 * Delete the vertex buffer object from graphics memory.
	 */
	public void delete() {
		gl.glDeleteBuffers(id.length, id, 0);
	}
}
