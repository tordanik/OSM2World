package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_FLOAT;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.GL2GL3.GL_DOUBLE;
import static org.osm2world.core.math.GeometryUtil.triangleNormalListFromTriangleStrip;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleFan;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleStrip;
import static org.osm2world.core.math.algorithms.NormalCalculationUtil.calculateTriangleNormals;
import static org.osm2world.core.target.common.Primitive.Type.TRIANGLES;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

public class ShaderTest implements GLEventListener {

	private static boolean fullscreen = false;
	private static Point center;
	private int w = 1024;
	private int h = 768;
	
	private boolean vao = false;
	private boolean vbo2 = false;
	private DefaultShader shader;
	private int vaoHandle;
	private int vboHandle;
	private VBOData<?> vbodata;
	private GL2ES2 gl;
	PMVMatrix pmvMatrix;

	public static void main(String[] args) {
		System.out.println("GL_PROFILE_LIST_ALL:");
		System.out.println(Arrays.toString(GLProfile.GL_PROFILE_LIST_ALL));
		System.out.println("GL_PROFILE_LIST_MAX:");
		System.out.println(Arrays.toString(GLProfile.GL_PROFILE_LIST_MAX));
		
		Frame frame = new Frame("JOGL Events");

		// Toolkit t=Toolkit.getDefaultToolkit();

		Image img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		// Cursor pointer=t.createCustomCursor(img, new Point(0,0), "none");

		//GLProfile profile = GLProfile.getDefault();
		GLProfile profile = GLProfile.get(GLProfile.GL2ES2);
		System.out.println("GLProfile Name: "+profile.getName());
		System.out.println("GLProfile Implementation Name: "+profile.getImplName());
		GLCanvas canvas = new GLCanvas(new GLCapabilities(profile));

		canvas.addGLEventListener(new ShaderTest());

		canvas.setFocusable(true);

		canvas.requestFocus();

		frame.add(canvas);

		frame.setUndecorated(true);

		frame.setSize(1024, 768);

		frame.setLocationRelativeTo(null);

		// frame.setCursor(pointer);

		frame.setVisible(true);

		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();

		if (fullscreen) {

			ge.getDefaultScreenDevice().setFullScreenWindow(frame);

		}

		final Animator animator = new Animator(canvas);

		animator.setRunAsFastAsPossible(true);

		animator.start();

		Rectangle r = frame.getBounds();

		center = new Point(r.x + r.width / 2, r.y + r.height / 2);

	}

	public void init(GLAutoDrawable drawable) {

		this.gl = drawable.getGL().getGL2ES2();

		if (!gl.isExtensionAvailable("GL_ARB_vertex_buffer_object")) {

			System.out.println("Error: VBO support is missing");

			// quit = true;

		}
		
		shader = new DefaultShader(gl.getGL3());
		
		if (vao)
			vaoHandle = constructVertexArrayObject(gl.getGL3());
		else if (vbo2)
			vboHandle = constructVertexBufferObject2(gl.getGL3());
		else
			vbodata = constructVertexBufferObject();

		//GLU glu = new GLU();

		gl.glViewport(0, 0, w, h);

		//gl.getGL2().glMatrixMode(GL2.GL_PROJECTION);

		//glu.gluPerspective(45.0f, ((float) w / (float) h), 0.1f, 100.0f);
		
		pmvMatrix = new PMVMatrix();
		pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.gluPerspective(45.0f, ((float) w / (float) h), 0.1f, 100.0f);
		pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
		//gl.glUniformMatrix4fv(shader.getProjectionMatrixID(), 1, false, pmvMatrix.glGetPMatrixf());

		//gl.getGL2().glMatrixMode(GL2.GL_MODELVIEW);

		//gl.getGL2().glShadeModel(GL2.GL_SMOOTH);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		gl.glClearDepth(1.0f);

		gl.glEnable(GL.GL_DEPTH_TEST);

		gl.glDepthFunc(GL.GL_LEQUAL);

		//gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(shader.getProgram());
		

		pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.glTranslatef(1, 0, 0);
		pmvMatrix.update();
		System.out.println(pmvMatrix);
		gl.glUniformMatrix4fv(shader.getProjectionMatrixID(), 1, false, pmvMatrix.glGetPMatrixf());
		gl.glUniformMatrix4fv(shader.getModelViewMatrixID(), 1, false, pmvMatrix.glGetMvMatrixf());
		pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
		pmvMatrix.glPushMatrix();
		pmvMatrix.glMultMatrixf(pmvMatrix.glGetMvMatrixf());
		gl.glUniformMatrix4fv(shader.getModelViewProjectionMatrixID(), 1, false, pmvMatrix.glGetMvMatrixf());
		pmvMatrix.glPopMatrix();
		pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
		
		
		
		if (vao) {

			// bind vertex and color data
			gl.glBindVertexArray(vaoHandle);
			gl.glEnableVertexAttribArray(0); // VertexPosition
			gl.glEnableVertexAttribArray(1); // VertexColor

			// draw VAO
			gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
		} else if (vbo2) {
			
			 // bind vertex and color data
			 gl.glBindVertexArray(vaoHandle);
			 gl.glEnableVertexAttribArray(0); // VertexPosition
			 gl.glEnableVertexAttribArray(1); // VertexColor
			
			 // draw VAO
			 gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
		} else {
			vbodata.render();
		}
		
		// check for errors
		int error = gl.glGetError();
		if (error != GL.GL_NO_ERROR) {
			throw new RuntimeException("OpenGL error: " + error + " ("
					+ new GLU().gluErrorString(error) + ")");
		}

		gl.glFlush();
	}
	
	private VBOData<?> constructVertexBufferObject()
	{
		Collection<Primitive> prim = new LinkedList<Primitive>();
		
		List<TriangleXYZ> triangles = new LinkedList<TriangleXYZ>();
		triangles.add(new TriangleXYZ(new VectorXYZ(0, 0, 0), new VectorXYZ(-1, 0, 0), new VectorXYZ(0, 1, 0)));
		
		List<VectorXYZ> vectors = new ArrayList<VectorXYZ>(triangles.size()*3);
		
		for (TriangleXYZ triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
		}
		
		Primitive p = new Primitive(TRIANGLES, vectors,
				calculateTriangleNormals(vectors,false), null);

		prim.add(p);
		VBOData<FloatBuffer> vbodata = new VBODataFloat(new ImmutableMaterial(null, null), prim);
		return vbodata;
	}
	
	
	/**
	 * Create Vertex Array Object necessary to pass data to the shader
	 */
	private int constructVertexArrayObject(GL3 gl)
	{
		// create vertex data 
		float[] positionData = new float[] {
		    	0f,		0f,		0f,
		    	-1f,	0f, 	0f,
		    	0f,		1f,		0f
		};
 
		// create color data
		float[] colorData = new float[]{
				0f,			0f,			1f,
				1f,			0f,			0f,
				0f,			1f,			0f
		};
 
		// convert vertex array to buffer
		FloatBuffer positionBuffer = FloatBuffer.allocate(positionData.length);
		positionBuffer.put(positionData);
		positionBuffer.flip();
 
		// convert color array to buffer
		FloatBuffer colorBuffer = FloatBuffer.allocate(colorData.length);
		colorBuffer.put(colorData);
		colorBuffer.flip();
 
		// create vertex byffer object (VBO) for vertices
		int[] positionBufferHandle = new int[1];
		gl.glGenBuffers(1, positionBufferHandle, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionBufferHandle[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, positionBuffer.capacity()*Buffers.SIZEOF_FLOAT, positionBuffer, GL.GL_STATIC_DRAW);
 
		// create VBO for color values
		int[] colorBufferHandle = new int[1];
		gl.glGenBuffers(1, colorBufferHandle, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorBufferHandle[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, positionBuffer.capacity()*Buffers.SIZEOF_FLOAT, colorBuffer, GL.GL_STATIC_DRAW);
 
		// unbind VBO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
 
		// create vertex array object (VAO)
		int[] vaoHandle = new int[1];
		gl.glGenVertexArrays(1, vaoHandle, 0);
		gl.glBindVertexArray(vaoHandle[0]);
		gl.glEnableVertexAttribArray(0);
		gl.glEnableVertexAttribArray(1);
 
		// assign vertex VBO to slot 0 of VAO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionBufferHandle[0]);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0);
 
		// assign vertex VBO to slot 1 of VAO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorBufferHandle[0]);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0);
 
		// unbind VBO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
 
		return vaoHandle[0];
	}
	
	/**
	 * Create Vertex Array Object necessary to pass data to the shader
	 */
	private int constructVertexBufferObject2(GL3 gl)
	{
		// create vertex data 
		float[] positionData = new float[] {
		    	0f,		0f,		0f,
		    	-1f,	0f, 	0f,
		    	0f,		1f,		0f
		};
 
		// create color data
		float[] colorData = new float[]{
				0f,			0f,			1f,
				1f,			0f,			0f,
				0f,			1f,			0f
		};
		
		// create color data
		float[] interleavedData = new float[]{
				0f,0f,1f,
		    	0f,		0f,		0f,
				0f,			1f,			0f,
				0f,0f,1f,
		    	-1f,	0f, 	0f,
				0f,			1f,			0f,
				0f,0f,1f,
		    	0f,		1f,		0f,
				0f,			1f,			0f
		};
 
		// convert vertex array to buffer
		FloatBuffer positionBuffer = FloatBuffer.allocate(interleavedData.length);
		positionBuffer.put(interleavedData);
		positionBuffer.flip();
 
		// create vertex byffer object (VBO) for vertices
		int[] positionBufferHandle = new int[1];
		gl.glGenBuffers(1, positionBufferHandle, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionBufferHandle[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, positionBuffer.capacity()*Buffers.SIZEOF_FLOAT, positionBuffer, GL.GL_STATIC_DRAW);
 
		// unbind VBO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
 
		// create vertex array object (VAO)
		//int[] vaoHandle = new int[1];
		//gl.glGenVertexArrays(1, vaoHandle, 0);
		//gl.glBindVertexArray(vaoHandle[0]);
		gl.glEnableVertexAttribArray(shader.getVertexPositionID());
		//gl.glEnableVertexAttribArray(shader.getVertexColorID());
 
		// assign vertex VBO to slot 0 of VAO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionBufferHandle[0]);
		gl.glVertexAttribPointer(shader.getVertexPositionID(), 3, GL.GL_FLOAT, false, Buffers.SIZEOF_FLOAT*9, Buffers.SIZEOF_FLOAT*3);
 
		// assign vertex VBO to slot 1 of VAO
		//gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionBufferHandle[0]);
		//gl.glVertexAttribPointer(shader.getVertexColorID(), 3, GL.GL_FLOAT, false, Buffers.SIZEOF_FLOAT*9, Buffers.SIZEOF_FLOAT*6);
		
		// unbind VBO
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
 
		return positionBufferHandle[0];
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		GL3 gl = drawable.getGL().getGL3();

		//GLU glu = new GLU();

		if (height <= 0) {
			height = 1;
		}

		final float h = (float) width / (float) height;

		gl.glViewport(0, 0, width, height);

		//gl.glMatrixMode(GL2.GL_PROJECTION);

		//gl.glLoadIdentity();

		//glu.gluPerspective(45.0f, h, 1.0, 20.0);

		//gl.glMatrixMode(GL2.GL_MODELVIEW);

		//gl.glLoadIdentity();
		

		pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.gluPerspective(45.0f, h, 1.0f, 20.0f);
		pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
		//gl.glUniformMatrix4fv(shader.getProjectionMatrixID(), 1, false, pmvMatrix.glGetPMatrixf());

		center = new Point(x + width / 2, y + height / 2);

	}
	
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
		protected abstract void put(BufferT buffer, Color c);
		
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
			for (int i=0;i<valueBuffer.limit();i++) {
				System.out.println(((FloatBuffer) valueBuffer).get(i));
			}
			
			/* write the data into the buffer */
			
			gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);
			
			gl.glBufferData(
					GL_ARRAY_BUFFER,
					valueBuffer.capacity() * valueTypeSize,
					valueBuffer,
					GL_STATIC_DRAW);
			
			// unbind buffer
			gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
			
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
			
			System.out.println(primVertices);
			System.out.println(primNormals);
			
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
				System.out.println(primNormals.get(i));
				put(buffer, primVertices.get(i));
				System.out.println(primVertices.get(i));
				put(buffer, Color.GREEN);
				
			}
			
		}
		
		public void render() {
			
			gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);

			
			setPointerLayout();
			
			gl.glEnableVertexAttribArray(shader.getVertexPositionID());
			//gl.glEnableVertexAttribArray(shader.getVertexColorID());
			System.out.printf("vertexCount: %d\n",vertexCount);
			//gl.getGL2().glMatrixMode(GL2.GL_MODELVIEW);
			//gl.getGL2().glLoadIdentity();
			//gl.getGL2().glTranslated(1, 0, 0);
			gl.glDrawArrays(GL_TRIANGLES, 0, vertexCount);
				//gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
			
		}

		private void setPointerLayout() {
			
			int stride = valueTypeSize * getValuesPerVertex(material);
			System.out.printf("stride: %d\n",stride);
			System.out.printf("valueTypeSize: %d\n",valueTypeSize);
			System.out.printf("valuesPerVertex: %d\n",getValuesPerVertex(material));
			
			int offset = 0;
			
			for (int i = 0; i < material.getNumTextureLayers(); i++) {
				
				offset += 2 * valueTypeSize;
				
			}
			System.out.printf("offset: %d\n",offset);

			gl.glEnableVertexAttribArray(shader.getVertexPositionID());
			//gl.glEnableVertexAttribArray(shader.getVertexColorID());
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, id[0]);

				gl.glVertexAttribPointer(shader.getVertexPositionID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 3);
				//gl.glVertexAttribPointer(shader.getVertexColorID(), 3, glValueType(), false, stride, offset + valueTypeSize() * 6);
				//gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			
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
		protected void put(DoubleBuffer buffer, Color c) {
			buffer.put(c.getRed()/255d);
			buffer.put(c.getGreen()/255d);
			buffer.put(c.getBlue()/255d);
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
		protected void put(FloatBuffer buffer, Color c) {
			buffer.put(c.getRed()/255f);
			buffer.put(c.getGreen()/255f);
			buffer.put(c.getBlue()/255f);
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
		
		int numValues = 9; // vertex coordinates and normals and color
		
		if (material.getTextureDataList() != null) {
			numValues += 2 * material.getTextureDataList().size();
		}
		
		return numValues;
	}
}
