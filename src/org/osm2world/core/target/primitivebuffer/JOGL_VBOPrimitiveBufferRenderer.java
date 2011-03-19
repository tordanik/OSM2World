package org.osm2world.core.target.primitivebuffer;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.jogl.JOGLTarget;

import com.sun.opengl.util.BufferUtil;

//FIXME: doesn't render anything (after fix, remove dummy return in isSupported)

/**
 * primitive buffer renderer that speeds up rendering using VertexBufferObjects
 */
public class JOGL_VBOPrimitiveBufferRenderer {

	private final GL gl;
	private final PrimitiveBuffer primitiveBuffer;
	
	private int[] vboName = new int[1];
    	
	public JOGL_VBOPrimitiveBufferRenderer(GL gl, PrimitiveBuffer primitiveBuffer) {
		
		assert isSupported(gl);
		
		this.gl = gl;
		this.primitiveBuffer = primitiveBuffer;
		
		createVertexBuffer();
		
	}
	
	private void createVertexBuffer() {

        List<VectorXYZ> vertices = primitiveBuffer.getVertices();
        
        DoubleBuffer verticesAsBuffer = DoubleBuffer.allocate(vertices.size() * 3);
        for (VectorXYZ vertex : vertices) {
        	verticesAsBuffer.put(vertex.getX());
        	verticesAsBuffer.put(vertex.getY());
        	verticesAsBuffer.put(-vertex.getZ());
        }
        
		// generate the buffer and instantly bind it
        gl.glGenBuffersARB(1, vboName, 0);
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vboName[0]);
        
        // load all vertex data into the buffer
		gl.glBufferDataARB(
				GL.GL_ARRAY_BUFFER_ARB,
				vertices.size() * 3 * BufferUtil.SIZEOF_DOUBLE,
				verticesAsBuffer,
				GL.GL_STATIC_DRAW_ARB);

		//Note: at this point, the vertices field in the primitive buffer
		//      wouldn't be necessary anymore - the data is in graphics memory
        
	}

	public void render() {
		
		// enable vertex buffers
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, vboName[0]);
        // Set The Vertex Pointer To The Vertex Buffer
        gl.glVertexPointer(3, GL.GL_DOUBLE, 0, 0);
        
		for (Material material : primitiveBuffer.getMaterials()) {
		
			//TODO introduce materials as in JOGLPrimitiveBufferRenderer
			
			gl.glColor3f(material.color.getRed()/255f,
					material.color.getGreen()/255f,
					material.color.getBlue()/255f);
			
			for (Primitive primitive : primitiveBuffer.getPrimitives(material)) {
				
				int mode = JOGLTarget.getGLConstant(primitive.type);
				
				gl.glDrawElements(mode, primitive.indices.length,
						GL.GL_INT, IntBuffer.wrap(primitive.indices));
				
//				gl.glBegin(primitiveType);
//
//				for (int index : primitive.indices) {
//					gl.glIndexi(index);
////					VectorXYZ v = primitiveBuffer.vertices.get(index);
////			        gl.glVertex3d(v.x, v.y, -v.z);
//				}
//
//		        gl.glEnd();
		        
			}
		
		}
		
		// disable vertex buffers
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		
	}

	public static boolean isSupported(GL testGL) {

		return false;
		
//		return testGL.isFunctionAvailable("glGenBuffersARB")
//			&& testGL.isFunctionAvailable("glBindBufferARB")
//			&& testGL.isFunctionAvailable("glBufferDataARB")
//			&& testGL.isFunctionAvailable("glDeleteBuffersARB");
		
	}
	
}
