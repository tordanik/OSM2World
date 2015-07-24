package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_CCW;
import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_CULL_FACE;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_TEXTURE0;
import static javax.media.opengl.GL.GL_TEXTURE1;
import static javax.media.opengl.GL.GL_TEXTURE2;
import static javax.media.opengl.GL.GL_TEXTURE3;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import jogamp.opengl.ProjectFloat;

import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.viewer.model.Defaults;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;

public class ShadowMapShader extends AbstractPrimitiveShader {
	
	public static final int shadowMapWidth = 4096;
	public static final int shadowMapHeight = 4096;
	
	private int modelViewProjectionMatrixID;
	private int vertexPositionID;
	
	public int depthBufferHandle;
	public int colorBufferHandle;
	private int frameBufferHandle;
	
	private int[] viewport = new int[4];
	
	/**
	 *  model view projection matrix of the shadow casting light source
	 */
	private PMVMatrix pmvMat;
	
	public ShadowMapShader(GL3 gl) {
		super(gl, "/shaders/shadowmap");
		
		// get indices of named attributes
		vertexPositionID = gl.glGetAttribLocation(shaderProgram, "VertexPosition");
		// get indices of uniform variables
		modelViewProjectionMatrixID = gl.glGetUniformLocation(shaderProgram, "ModelViewProjectionMatrix");
		
		pmvMat = new PMVMatrix();
		
		initializeShadowMap();
	}
	
	private void initializeShadowMap() {

		// create the shadow map texture / depth buffer
		int[] tmp = new int[1];
		gl.glGenTextures(1,tmp,0);
		depthBufferHandle = tmp[0];
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, depthBufferHandle);

		gl.glTexImage2D(GL.GL_TEXTURE_2D,          // target texture type
		        0,                                  // mipmap LOD level
		        GL3.GL_DEPTH_COMPONENT,         // internal pixel format
		                                            //GL_DEPTH_COMPONENT
		        shadowMapWidth,                     // width of generated image
		        shadowMapHeight,                    // height of generated image
		        0,                          // border of image
		        GL3.GL_DEPTH_COMPONENT,     // external pixel format 
		        GL.GL_UNSIGNED_BYTE,        // datatype for each value
		        null);  // buffer to store the texture in memory

		// some settings for the shadow map texture
		// GL_LINEAR might produce better results, but is slower. GL_NEAREST shows aliasing artifacts clearly
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		
		/* For texture access outside the shadow map use the highest depth value possible (1.0).
		 * This means the fragment lies outside of the lights frustum and no shadow should be applied.
		 * Therefore we use CLAMP_TO_BORDER with a border of (1.0, 0.0, 0.0, 0.0)
		 */
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_BORDER);
		float [] border = {1.0f, 0.0f, 0.0f, 0.0f};
		gl.glTexParameterfv(GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_BORDER_COLOR, border, 0);
		
		/* special for depth textures: do not retrieve the texture values, but the result of a comparison.
		 * compare the third value (r) of the texture coordinate against the depth value stored at the texture coordinate (s,t)
		 * result will be 1.0 if r is less than the texture value (which means the fragment is nearer) and 0.0 otherwise
		 */
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_COMPARE_MODE, GL3.GL_COMPARE_REF_TO_TEXTURE);     
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_COMPARE_FUNC, GL.GL_LESS);     

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, depthBufferHandle);
		
		
		/*gl.glGenTextures(1,tmp,0);
		colorBufferHandle = tmp[0];
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, colorBufferHandle);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

		gl.glTexImage2D(GL.GL_TEXTURE_2D,          // target texture type
		        0,                                  // mipmap LOD level
		        GL.GL_RGBA,         // internal pixel format
		                                            //GL_DEPTH_COMPONENT
		        shadowMapWidth,                     // width of generated image
		        shadowMapHeight,                    // height of generated image
		        0,                          // border of image
		        GL.GL_RGBA,     // external pixel format 
		        GL.GL_UNSIGNED_BYTE,        // datatype for each value
		        null);  // buffer to store the texture in memory
		*/
		
		// create the frame buffer object (FBO)
		gl.glGenFramebuffers(1, tmp, 0);
		frameBufferHandle = tmp[0];
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, frameBufferHandle);

		//Attach 2D texture to this FBO
		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
		        GL.GL_DEPTH_ATTACHMENT,
		        GL.GL_TEXTURE_2D,
		        depthBufferHandle,0);
		/*gl.glFramebufferTexture(GL.GL_FRAMEBUFFER,
		        GL.GL_DEPTH_ATTACHMENT,
		        depthBufferHandle,0);*/
		/*gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
		        GL.GL_COLOR_ATTACHMENT0,
		        GL.GL_TEXTURE_2D,
		        colorBufferHandle,0);*/
		
		// set target for fragment shader output: not used, we only need the depth buffer
		//int[] drawBuffers = {GL.GL_NONE};
		//gl.glDrawBuffers(1, drawBuffers, 0);
		//gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
		gl.glDrawBuffer(GL.GL_NONE);
		gl.glReadBuffer(GL.GL_NONE);

		//gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

		//Disable color buffer
		//http://stackoverflow.com/questions/12546368/render-the-depth-buffer-into-a-texture-using-a-frame-buffer
		//gl.glDrawBuffer(GL2.GL_NONE);
		//gl.glReadBuffer(GL2.GL_NONE);

		//Set pixels ((width*2)* (height*2))
		//It has to have twice the size of shadowmap size
		//pixels = GLBuffers.newDirectByteBuffer(shadowMapWidth*shadowMapHeight*4);

		//Set default frame buffer before doing the check
		//http://www.opengl.org/wiki/FBO#Completeness_Rules
		//gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

		int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);

		// Always check that our framebuffer is ok
		if(gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) != GL.GL_FRAMEBUFFER_COMPLETE)
		{
			throw new RuntimeException("Can not use FBO! Status error:" + status);
		}
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
	}
	
	/**
	 * Pass 1: Shadow map generation
	 */
	private void prepareShadowMapGeneration() {
		
		// bind FBO
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, frameBufferHandle);
		
		// set right viewport for the framebuffer size, store old to reset later
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        gl.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
		
		// clear shadow map
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// enable front face culling
		gl.glFrontFace(GL_CCW);
		gl.glCullFace(GL.GL_FRONT);
		gl.glEnable (GL_CULL_FACE);
		
		gl.glEnable(GL_DEPTH_TEST);
	}
	
	public void saveShadowMap(File file) {
		// create buffer to store image
		FloatBuffer buffer=FloatBuffer.allocate(shadowMapWidth*shadowMapHeight);//ByteBuffer.allocate(shadowMapWidth*shadowMapHeight*4).asFloatBuffer(); 
		
		// load image in buffer
		gl.glBindTexture(GL.GL_TEXTURE_2D, depthBufferHandle);
		gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL3.GL_DEPTH_COMPONENT, GL.GL_FLOAT, buffer);
		buffer.rewind();

		// create buffered image
		BufferedImage img = new BufferedImage(shadowMapWidth, shadowMapHeight, BufferedImage.TYPE_BYTE_GRAY);
		
		// copy data to buffered image
		WritableRaster wr = img.getRaster();
		for (int col=0; col<img.getWidth(); col++) {
			for (int row=0; row<img.getHeight(); row++) {
				float f = buffer.get(row*shadowMapHeight+col);
				int v = (int)(f*255);
				wr.setPixel(col, shadowMapHeight-1-row, new int[] {v});
			}
		}
		
		// save to file
		try {
			ImageIO.write(img, "png", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveColorBuffer(File file) {
		// create buffer to store image
		ByteBuffer buffer = ByteBuffer.allocate(shadowMapWidth*shadowMapHeight*4);
		
		// load image in buffer
		gl.glBindTexture(GL.GL_TEXTURE_2D, colorBufferHandle);
		gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
		buffer.rewind();

		// create buffered image
		BufferedImage img = new BufferedImage(shadowMapWidth, shadowMapHeight, BufferedImage.TYPE_INT_RGB);
		
		// copy data to buffered image
		for (int col=0; col<img.getWidth(); col++) {
			for (int row=0; row<img.getHeight(); row++) {
				byte r = buffer.get(row*shadowMapHeight*4+col*4+0);
				byte g = buffer.get(row*shadowMapHeight*4+col*4+1);
				byte b = buffer.get(row*shadowMapHeight*4+col*4+2);
				byte a = buffer.get(row*shadowMapHeight*4+col*4+3);
				int alpha = a & 0xFF;
				int red =   r & 0xFF;
				int green = g & 0xFF;
				int blue =  b & 0xFF;
				int rgb = (alpha << 24 | red << 16 | green << 8 | blue);
				img.setRGB(col, shadowMapHeight-1-row, rgb);
			}
		}
		

		// save to file
		try {
			ImageIO.write(img, "png", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Send uniform matrices "ProjectionMatrix, ModelViewMatrix and ModelViewProjectionMatrix" to vertex shader
	 * @param pmvMatrix
	 */
	public void setPMVMatrix(PMVMatrix pmvMatrix) {
		FloatBuffer pmvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(pmvMatrix.glGetPMatrixf(), pmvMatrix.glGetMvMatrixf(), pmvMat);
		gl.glUniformMatrix4fv(this.getModelViewProjectionMatrixID(), 1, false, pmvMat);
	}
	
	public PMVMatrix getPMVMatrix() {
		return pmvMat;
	}
	
	/**
	 * prepare and use PMVMatrix for rendering shadows from global lighting perspective using "Perspective Shadow Maps"
	 * (see http://www-sop.inria.fr/reves/Marc.Stamminger/psm/)
	 * @param lighting
	 */
	public void preparePMVMatrixPSM(GlobalLightingParameters lighting, PMVMatrix cameraPMV, double[] primitivesBoundingBox) {
		
		// camera PMV Matrix:
		FloatBuffer camPMvMat = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(cameraPMV.glGetPMatrixf(), cameraPMV.glGetMvMatrixf(), camPMvMat);
		
		// transform light into camera space (unit cube)
		float[] lightPos = {(float)lighting.lightFromDirection.x, (float)lighting.lightFromDirection.y, -(float)lighting.lightFromDirection.z, 0};
		float[] lightPosCam = new float[4];
		FloatUtil.multMatrixVecf(camPMvMat, lightPos, lightPosCam);
		
		// set view and projection matrices to light source
		PMVMatrix pmvMatL = new PMVMatrix();
		pmvMatL.glMatrixMode(GL_MODELVIEW);
		pmvMatL.glLoadIdentity();
		pmvMatL.gluLookAt(lightPosCam[0], lightPosCam[1], lightPosCam[2],
				0f, 0f, 0f,
				0f, 1f, 0f);
		
		pmvMatL.glMatrixMode(GL_PROJECTION);
		pmvMatL.glLoadIdentity();
		Projection projection = Defaults.PERSPECTIVE_PROJECTION;
		pmvMatL.gluPerspective(
				(float)(projection.getVertAngle()),
				(float)(projection.getAspectRatio()),
				(float)(projection.getNearClippingDistance()),
				(float)(projection.getFarClippingDistance()));
		//pmvMat.glOrthof(-1000,1000,-1000,1000,-1000,1500);
		
		//float[] frustum;
		/*frustum = intersectFrustum(calculateCameraLightFrustum(pmvMat, cameraPMV),
				calculatePrimitivesLightFrustum(pmvMat, primitivesBoundingBox));*/
		//frustum = calculatePrimitivesLightFrustum(pmvMat, primitivesBoundingBox);
		//System.out.println("shadow map frustum: " + Arrays.toString(frustum));
		//pmvMatL.glOrthof(frustum[0], frustum[1], frustum[2], frustum[3], frustum[4], frustum[5]);
		
		// M = M_cam
		pmvMat.glMatrixMode(GL_MODELVIEW);
		pmvMat.glLoadMatrixf(cameraPMV.glGetMvMatrixf());
		
		// P = P_light*Mv_light*P_cam
		pmvMat.glMatrixMode(GL_PROJECTION);
		pmvMat.glLoadMatrixf(pmvMatL.glGetPMatrixf());
		pmvMat.glMultMatrixf(pmvMatL.glGetMvMatrixf());
		pmvMat.glMultMatrixf(cameraPMV.glGetPMatrixf());
		
		setPMVMatrix(pmvMat);
	}
	
	/**
	 * prepare and use PMVMatrix for rendering shadows from global lighting perspective
	 * @param lighting
	 */
	public void preparePMVMatrix(GlobalLightingParameters lighting, PMVMatrix cameraPMV, double[] primitivesBoundingBox) {
		
		// set view and projection matrices to light source

		
		pmvMat.glMatrixMode(GL_MODELVIEW);
		pmvMat.glLoadIdentity();
		pmvMat.gluLookAt((float)lighting.lightFromDirection.x, (float)lighting.lightFromDirection.y, -(float)lighting.lightFromDirection.z,
				0f, 0f, 0f,
				0f, 1f, 0f);
		
		pmvMat.glMatrixMode(GL_PROJECTION);
		pmvMat.glLoadIdentity();
		/*Projection projection = Defaults.PERSPECTIVE_PROJECTION;
		pmvMat.gluPerspective(
				(float)(projection.getVertAngle()),
				(float)(projection.getAspectRatio()),
				(float)(projection.getNearClippingDistance()),
				(float)(projection.getFarClippingDistance()));*/
		//pmvMat.glOrthof(-1000,1000,-1000,1000,-1000,1500);
		
		float[] frustum;
		//frustum = intersectFrustum(calculateCameraLightFrustum(pmvMat, cameraPMV),
		//		calculatePrimitivesLightFrustum(pmvMat, primitivesBoundingBox));
		frustum = calculatePrimitivesLightFrustum(pmvMat, primitivesBoundingBox);
		//System.out.println("shadow map frustum: " + Arrays.toString(frustum));
		pmvMat.glOrthof(frustum[0], frustum[1], frustum[2], frustum[3], frustum[4], frustum[5]);
		
		setPMVMatrix(pmvMat);
	}
	
	private float[] intersectFrustum(float[] frustum1, float[] frustum2) {
		float[] result = new float[frustum1.length];
		for (int i=0; i<frustum1.length; i++) {
			if (i%2 == 0) {
				result[i] = Math.max(frustum1[i], frustum2[i]);
			} else {
				result[i] = Math.min(frustum1[i], frustum2[i]);
			}
		}
		return result;
	}
	
	/**
	 * the frustum for the light projection matrix based on the bounding box of all primitives.
	 * transforms the bounding box into lightspace and draws an axis aligned bounding box around it.
	 * @param lightPMV
	 * @param primitivesBoundingBox
	 * @return
	 */
	private float[] calculatePrimitivesLightFrustum(PMVMatrix lightPMV, double[] primitivesBoundingBox) {
		float[] frustum = null;
		for (double x : new double[]{primitivesBoundingBox[0], primitivesBoundingBox[1]}) {
			for (double y : new double[]{primitivesBoundingBox[2], primitivesBoundingBox[3]}) {
				for (double z : new double[]{primitivesBoundingBox[4], primitivesBoundingBox[5]}) {
					float[] corner = {(float)x, (float)y, (float)z, 1};
					float[] result = new float[4];
					FloatUtil.multMatrixVecf(lightPMV.glGetMvMatrixf(), corner, result);
					float frustumCornerX = result[0]/result[3];
					float frustumCornerY = result[1]/result[3];
					float frustumCornerZ = result[2]/result[3];
					if (frustum == null) {
						frustum = new float[] {frustumCornerX, frustumCornerX, frustumCornerY, frustumCornerY, frustumCornerZ, frustumCornerZ};
					} else {
						addToBoundingBox(frustum, frustumCornerX, frustumCornerY, frustumCornerZ);
					}
				}
			}
		}
		return frustum;
	}
	
	/**
	 * Calculate the frustum for the light projection matrix based on the frustum of the camera
	 * @param lightPMV
	 * @param cameraPMV
	 * @param frustum
	 */
	private float[] calculateCameraLightFrustum(PMVMatrix lightPMV, PMVMatrix cameraPMV) {
		/*
		 * calculate transform from screen space bounding box to light space:
		 * inverse projection -> inverse modelview -> modelview of light
		 */
		FloatBuffer cameraP_inverse = FloatBuffer.allocate(16);
		FloatBuffer cameraPMV_inverse = FloatBuffer.allocate(16);
		ProjectFloat p = new ProjectFloat();
		p.gluInvertMatrixf(cameraPMV.glGetPMatrixf(), cameraP_inverse);
		FloatUtil.multMatrixf(cameraPMV.glGetMviMatrixf(), cameraP_inverse, cameraPMV_inverse);
		FloatBuffer NDC2light = FloatBuffer.allocate(16);
		FloatUtil.multMatrixf(lightPMV.glGetMvMatrixf(), cameraPMV_inverse, NDC2light);
		
		/*
		 * transform screen space bounding box to light space
		 * and calculate axis aligned bounding box
		 */
		float[] frustum = null;
		for (int x = -1; x<=1; x+=2) {
			for (int y = -1; y<=1; y+=2) {
				for (int z = -1; z<=1; z+=2) {
					float[] NDCcorner = {x, y, z, 1};
					float[] result = new float[4];
					FloatUtil.multMatrixVecf(NDC2light, NDCcorner, result);
					float frustumCornerX = result[0]/result[3];
					float frustumCornerY = result[1]/result[3];
					float frustumCornerZ = result[2]/result[3];
					if (frustum == null) {
						frustum = new float[] {frustumCornerX, frustumCornerX, frustumCornerY, frustumCornerY, frustumCornerZ, frustumCornerZ};
					} else {
						addToBoundingBox(frustum, frustumCornerX, frustumCornerY, frustumCornerZ);
					}
				}
			}
		}
		return frustum;
	}
	
	private void addToBoundingBox(float[] boundingBox, float x, float y, float z) {
		if (x < boundingBox[0]) { boundingBox[0] = x; }
		if (x > boundingBox[1]) { boundingBox[1] = x; }
		if (y < boundingBox[2]) { boundingBox[2] = y; }
		if (y > boundingBox[3]) { boundingBox[3] = y; }
		if (z < boundingBox[4]) { boundingBox[4] = z; }
		if (z > boundingBox[5]) { boundingBox[5] = z; }
	}
	
	public int getShadowMapHandle() {
		return depthBufferHandle;
	}
	
	public int getVertexPositionID() {
		return vertexPositionID;
	}
	
	public int getModelViewProjectionMatrixID() {
		return modelViewProjectionMatrixID;
	}
	
	@Override
	public void useShader() {
		super.useShader();
		prepareShadowMapGeneration();
	}
	
	@Override
	public void disableShader() {
		
		// reset viewport
		gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
		super.disableShader();
	}

	@Override
	public void setMaterial(Material material, JOGLTextureManager textureManager) {
		// nothing to do, material doesn't affect shadows
		// TODO: transparency???
	}

	@Override
	public int getVertexNormalID() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getVertexTexCoordID(int i) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getVertexBumpMapCoordID() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getVertexTangentID() {
		// TODO Auto-generated method stub
		return -1;
	}
}