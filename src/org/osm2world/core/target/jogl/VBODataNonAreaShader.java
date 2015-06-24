package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_TEXTURE_COORD_ARRAY;

import java.awt.Color;
import java.nio.Buffer;
import java.util.Collection;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.material.Material;

/**
 * class that keeps a VBO id along with associated information
 */
abstract class VBODataNonAreaShader<BufferT extends Buffer> {
	
	/** array with one element containing the VBO id */
	protected final int[] id;
	
	/** number of vertices in the vbo */
	protected final int vertexCount;
	
	/** size of each value in the vbo */
	protected final int valueTypeSize;
	
	/** gl constant for the value type in the vbo */
	protected final int glValueType;

	protected abstract BufferT createBuffer(int numValues);
	
	protected abstract void put(BufferT buffer, Color color);
	protected abstract void put(BufferT buffer, VectorXYZ v);
	
	protected abstract int valueTypeSize();
	protected abstract int glValueType();
	
	protected GL3 gl;
	private NonAreaShader shader;
	private int primitiveType;
	
	public VBODataNonAreaShader(GL3 gl, NonAreaShader shader, NonAreaPrimitive nonAreaPrimitive) {
		this.gl = gl;
		this.shader = shader;
		primitiveType= AbstractJOGLTarget.getGLConstant(nonAreaPrimitive.type);
		
		valueTypeSize = valueTypeSize();
		glValueType = glValueType();
		
		vertexCount = nonAreaPrimitive.vs.size();
		
		/* create the buffer */
		
		id = new int[1];
		gl.glGenBuffers(1, id, 0);
		
		/* collect the data for the buffer */
		
		BufferT valueBuffer = createBuffer(
				vertexCount * getValuesPerVertex());
					
		/* put the values into the buffer, in the right order */
		
		for (int i = 0; i < nonAreaPrimitive.vs.size(); i++) {
			
			put(valueBuffer, nonAreaPrimitive.vs.get(i));
			put(valueBuffer, nonAreaPrimitive.color);
			
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
	
	public void render() {
		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);

		setPointerLayout();
		
		gl.glDrawArrays(primitiveType, 0, vertexCount);
	}
	
	private void setPointerLayout() {
		
		int stride = valueTypeSize * getValuesPerVertex();
		
		int offset = 0;
		
		gl.glVertexAttribPointer(shader.getVertexPositionID(), 3, glValueType(), false, stride, offset);
		gl.glVertexAttribPointer(shader.getVertexColorID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 3);
		
	}
	
	private int getValuesPerVertex() {
		
		// 3 for vertex position, 4 for vertex color
		return 7;
	}
	
}
