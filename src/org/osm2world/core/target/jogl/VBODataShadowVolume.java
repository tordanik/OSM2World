package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleFan;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleStrip;

import java.nio.Buffer;
import java.util.Collection;
import java.util.List;

import javax.media.opengl.GL3;

import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.material.Material;

/**
 * class that keeps a VBO id along with associated information
 */
abstract class VBODataShadowVolume<BufferT extends Buffer> {
	
	protected GL3 gl;
	protected AbstractPrimitiveShader shader;
	protected VectorXYZW lightPos;
	
	/** array with one element containing the VBO id */
	protected final int[] id;
	
	/** number of vertices in the vbo */
	protected final int vertexCount;
	
	/** size of each value in the vbo */
	protected final int valueTypeSize;
	
	/** gl constant for the value type in the vbo */
	protected final int glValueType;

	protected abstract BufferT createBuffer(int numValues);
	
	protected abstract int valueTypeSize();
	protected abstract int glValueType();
	
	protected abstract void put(BufferT buffer, VectorXYZW sv);

	
	public VBODataShadowVolume(GL3 gl, Material material, Collection<Primitive> primitives, VectorXYZW lightPos) {
		
		this.gl = gl;
		this.lightPos = lightPos;
		
		valueTypeSize = valueTypeSize();
		glValueType = glValueType();
		
		vertexCount = VBOData.countVertices(primitives)*8;
		
		/* create the buffer */
		
		id = new int[1];
		gl.glGenBuffers(1, id, 0);
		
		/* collect the data for the buffer */
		
		BufferT valueBuffer = createBuffer(
				vertexCount * getShadowVolumeVerticesPerVertex());
					
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
	
	public void setShader(AbstractPrimitiveShader shader) {
		this.shader = shader;
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
					
		/* put the values into the buffer, in the right order */
		
		for (int i = 0; i < shadowVolumeVertices.size(); i++) {
			
			put(buffer, shadowVolumeVertices.get(i));

		}
		
	}
	
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
	
	protected int getShadowVolumeVerticesPerVertex() {
		return 8;
	}
	
	protected int getValuesPerVertex() {
		return 4;
	}
	
	public void delete() {
		gl.glDeleteBuffers(id.length, id, 0);
	}
}
