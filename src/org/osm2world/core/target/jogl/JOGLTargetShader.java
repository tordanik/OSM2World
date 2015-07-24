package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_ARRAY_BUFFER;
import static javax.media.opengl.GL.GL_BACK;
import static javax.media.opengl.GL.GL_CCW;
import static javax.media.opengl.GL.GL_CULL_FACE;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_FRONT_AND_BACK;
import static javax.media.opengl.GL.GL_REPEAT;
import static javax.media.opengl.GL.GL_REPLACE;
import static javax.media.opengl.GL.GL_STATIC_DRAW;
import static javax.media.opengl.GL.GL_TEXTURE0;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL.GL_TEXTURE_WRAP_S;
import static javax.media.opengl.GL.GL_TEXTURE_WRAP_T;
import static javax.media.opengl.GL.GL_TRIANGLES;
import static javax.media.opengl.GL2ES1.GL_LIGHT_MODEL_AMBIENT;
import static javax.media.opengl.GL2ES1.GL_TEXTURE_ENV;
import static javax.media.opengl.GL2ES1.GL_TEXTURE_ENV_MODE;
import static javax.media.opengl.GL2GL3.GL_CLAMP_TO_BORDER;
import static javax.media.opengl.GL2GL3.GL_FILL;
import static javax.media.opengl.GL2GL3.GL_LINE;
import static javax.media.opengl.GL2GL3.GL_QUADS;
import static javax.media.opengl.GL2GL3.GL_TEXTURE_BORDER_COLOR;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_AMBIENT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_DIFFUSE;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHT0;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_POSITION;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_SPECULAR;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static org.osm2world.core.target.jogl.AbstractJOGLTarget.getFloatBuffer;

import java.awt.Color;
import java.io.File;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;

public class JOGLTargetShader extends AbstractJOGLTarget implements JOGLTarget {
	private BumpMapShader defaultShader;
	private ShadowMapShader shadowMapShader;
	private NonAreaShader nonAreaShader;
	private BackgroundShader backgroundShader;
	private GL3 gl;
	private PMVMatrix pmvMatrix;
	private JOGLRendererVBONonAreaShader nonAreaRenderer;
	private JOGLRendererVBOShader rendererShader;
	private AxisAlignedBoundingBoxXZ xzBoundary;
	
	private static final boolean USE_SHADOWMAPS = true;
	
	public JOGLTargetShader(GL3 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		super(gl, renderingParameters, globalLightingParameters);
		defaultShader = new BumpMapShader(gl);
		shadowMapShader = new ShadowMapShader(gl);
		nonAreaShader = new NonAreaShader(gl);
		backgroundShader = new BackgroundShader(gl);
		this.gl = gl;
		pmvMatrix = new PMVMatrix();
		reset();
	}
	
	@Override
	protected void drawPrimitive(org.osm2world.core.target.common.Primitive.Type type, org.osm2world.core.target.common.material.Material material, java.util.List<VectorXYZ> vertices, java.util.List<VectorXYZ> normals, java.util.List<java.util.List<VectorXZ>> texCoordLists) {
		super.drawPrimitive(type, material, vertices, normals, texCoordLists);
		
		// cache textures. they should not be loaded in the render function (see https://www.opengl.org/wiki/Common_Mistakes#glGenTextures_in_render_function)
		// in some situations even errors were encountered
		for (TextureData t : material.getTextureDataList()) {
			textureManager.getTextureForFile(t.file, true);
		}
	};

	@Override
	public void drawBackgoundImage(File backgroundImage,
			int startPixelX, int startPixelY, int pixelWidth, int pixelHeight,
			JOGLTextureManager textureManager) {
		
		backgroundShader.useShader();
		
		PMVMatrix backgroundPMVMatrix = new PMVMatrix();
		backgroundPMVMatrix.glMatrixMode(GL_PROJECTION);
		backgroundPMVMatrix.glLoadIdentity();
		backgroundPMVMatrix.glOrthof(0, 1, 0, 1, 0, 1);
		
		backgroundPMVMatrix.glMatrixMode(GL_MODELVIEW);
		backgroundPMVMatrix.glLoadIdentity();
		
		backgroundShader.setPMVMatrix(backgroundPMVMatrix);
		
		gl.glDepthMask( false );

		/* texture binding */

		gl.glActiveTexture(GL_TEXTURE0);
		
		Texture backgroundTexture =
				textureManager.getTextureForFile(backgroundImage);
		
		backgroundTexture.bind(gl);
		
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        gl.glUniform1i(backgroundShader.getTextureID(), 0);
		
		int texWidth = backgroundTexture.getImageWidth();
		int texHeight = backgroundTexture.getImageHeight();
		
		/* draw quad */
		
		/* create the buffer */
		
		int[] id = new int[1];
		gl.glGenBuffers(1, id, 0);
		
		/* collect the data for the buffer */
		int verticeCount = 4;
		int valueCount = 2;
		
		FloatBuffer valueBuffer = Buffers.newDirectFloatBuffer(verticeCount*(2*valueCount));
		valueBuffer.put(0);
		valueBuffer.put(0);
		
		valueBuffer.put(1f);
		valueBuffer.put(0);
		
		valueBuffer.put(0);
		valueBuffer.put(1f);
		
		valueBuffer.put(1f);
		valueBuffer.put(1f);
		
		valueBuffer.put((float) startPixelX / texWidth);
		valueBuffer.put((float) startPixelY / texHeight);
		
		valueBuffer.put((float) (startPixelX + pixelWidth) / texWidth);
		valueBuffer.put((float) startPixelY / texHeight);
		
		valueBuffer.put((float) startPixelX / texWidth);
		valueBuffer.put((float) (startPixelY + pixelHeight) / texHeight);
		
		valueBuffer.put((float) (startPixelX + pixelWidth) / texWidth);
		valueBuffer.put((float) (startPixelY + pixelHeight) / texHeight);
		
		valueBuffer.rewind();
		
		/* write the data into the buffer */
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, id[0]);
		
		gl.glBufferData(
				GL_ARRAY_BUFFER,
				valueBuffer.capacity() * Buffers.SIZEOF_FLOAT,
				valueBuffer,
				GL_STATIC_DRAW);
		
		gl.glEnableVertexAttribArray(backgroundShader.getVertexPositionID());
		gl.glEnableVertexAttribArray(backgroundShader.getVertexTexCoordID());
		
		int stride = 0;
		gl.glVertexAttribPointer(backgroundShader.getVertexPositionID(), valueCount, GL.GL_FLOAT, false, stride, 0);
		gl.glVertexAttribPointer(backgroundShader.getVertexTexCoordID(), valueCount, GL.GL_FLOAT, false, stride, Buffers.SIZEOF_FLOAT * valueCount * verticeCount);
		
		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		
		gl.glDisableVertexAttribArray(backgroundShader.getVertexPositionID());
		gl.glDisableVertexAttribArray(backgroundShader.getVertexTexCoordID());
		
		/* restore some settings */
		
		gl.glDepthMask( true );
		
		backgroundShader.disableShader();
	}

	@Override
	public void renderPart(Camera camera, Projection projection, double xStart,
			double xEnd, double yStart, double yEnd) {
		if (renderer == null) {
			throw new IllegalStateException("finish must be called first");
		}
		
		applyProjectionMatricesForPart(pmvMatrix, projection,
				xStart, xEnd, yStart, yEnd);
		
		applyCameraMatrices(pmvMatrix, camera);
		
		if (USE_SHADOWMAPS) {
			// TODO: render only part?
			shadowMapShader.useShader();
			shadowMapShader.preparePMVMatrix(globalLightingParameters, pmvMatrix, rendererShader.getBoundingBox());
			//shadowMapShader.setPMVMatrix(pmvMatrix);
			
			/* render primitives to shadow map*/
			rendererShader.setShader(shadowMapShader);
			rendererShader.render(camera, projection);
			//shadowMapShader.saveShadowMap(new File("/home/sebastian/shadowmap.bmp"));
			//shadowMapShader.saveColorBuffer(new File("/home/sebastian/shadowmap_color.bmp"));
			
			shadowMapShader.disableShader();
		}
		
		/* apply camera and projection information */
		defaultShader.useShader();
		defaultShader.loadDefaults();
		
		defaultShader.setPMVMatrix(pmvMatrix);
		//defaultShader.setPMVMatrix(shadowMapShader.getPMVMatrix());
		
		/* apply global rendering parameters */
		
		applyRenderingParameters(gl, renderingParameters);
		applyLightingParameters(defaultShader, globalLightingParameters);
		
		if (USE_SHADOWMAPS) {
			defaultShader.bindShadowMap(shadowMapShader.getShadowMapHandle());
			defaultShader.setShadowMatrix(shadowMapShader.getPMVMatrix());
		}
		
		/* render primitives */

		rendererShader.setShader(defaultShader);
		rendererShader.render(camera, projection);
		
		defaultShader.disableShader();
		
		/* non area primitives */
		nonAreaShader.useShader();
		nonAreaShader.loadDefaults();
		
		nonAreaShader.setPMVMatrix(pmvMatrix);
		
		nonAreaRenderer.render();
		
		nonAreaShader.disableShader();
	}
	
	static final void applyRenderingParameters(GL3 gl,
			JOGLRenderingParameters parameters) {
		
		/* backface culling */
		
		if (parameters.frontFace == null) {
			gl.glDisable(GL_CULL_FACE);
		} else {
			gl.glFrontFace(GL_CCW);
			gl.glCullFace(GL_BACK);
			gl.glEnable (GL_CULL_FACE);
		}
		
		/* wireframe mode */
		
		if (parameters.wireframe) {
    		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    	} else {
    		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    	}
		
		/* z buffer */
		
		if (parameters.useZBuffer) {
			gl.glEnable(GL_DEPTH_TEST);
		} else {
			gl.glDisable(GL_DEPTH_TEST);
		}
				
	}
	
	static final void applyCameraMatrices(PMVMatrix pmvMatrix, Camera camera) {
		
		pmvMatrix.glMatrixMode(GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
		
		VectorXYZ pos = camera.getPos();
		VectorXYZ lookAt = camera.getLookAt();
		VectorXYZ up = camera.getUp();
		pmvMatrix.gluLookAt(
				(float)pos.x, (float)pos.y, (float)-pos.z,
				(float)lookAt.x, (float)lookAt.y, (float)-lookAt.z,
				(float)up.x, (float)up.y, (float)-up.z);
		
	}
	
	static final void applyProjectionMatrices(PMVMatrix pmvMatrix, Projection projection) {
		applyProjectionMatricesForPart(pmvMatrix, projection, 0, 1, 0, 1);
	}

	/**
	 * similar to {@link #applyProjectionMatrices(GL2, Projection)},
	 * but allows rendering only a part of the "normal" image.
	 */
	static final void applyProjectionMatricesForPart(PMVMatrix pmvMatrix, Projection projection,
			double xStart, double xEnd, double yStart, double yEnd) {
		
		if ((xStart != 0 || xEnd != 1 || yStart != 0 || yEnd != 1)
				&& !projection.isOrthographic()) {
			throw new IllegalArgumentException("section rendering only supported "
					+ "for orthographic projections");
		}
		
		pmvMatrix.glMatrixMode(GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		
		if (projection.isOrthographic()) {

			double volumeWidth = projection.getAspectRatio() * projection.getVolumeHeight();
			
			pmvMatrix.glOrthof(
					(float)((-0.5 + xStart) * volumeWidth),
					(float)((-0.5 + xEnd  ) * volumeWidth),
					(float)((-0.5 + yStart) * projection.getVolumeHeight()),
					(float)((-0.5 + yEnd  ) * projection.getVolumeHeight()),
					(float)(projection.getNearClippingDistance()),
					(float)(projection.getFarClippingDistance()));
			
		} else { //perspective

			pmvMatrix.gluPerspective(
					(float)(projection.getVertAngle()),
					(float)(projection.getAspectRatio()),
					(float)(projection.getNearClippingDistance()),
					(float)(projection.getFarClippingDistance()));
			
		}

		pmvMatrix.glMatrixMode(GL_MODELVIEW);
		
	}
	
	static final void applyLightingParameters(BumpMapShader shader,
			GlobalLightingParameters lighting) {
		
		shader.setGlobalLighting(lighting);
		
	}
	
//	public void drawPrimitive(GL3 gl, int glPrimitiveType,
//			List<VectorXYZ> vertices, List<VectorXYZ> normals,
//			List<List<VectorXZ>> texCoordLists) {
//		assert vertices.size() == normals.size();
//		
//		gl.glBegin(glPrimitiveType);
//		
//		for (int i = 0; i < vertices.size(); i++) {
//			
//			if (texCoordLists != null) {
//				for (int texLayer = 0; texLayer < texCoordLists.size(); texLayer++) {
//					VectorXZ textureCoord =	texCoordLists.get(texLayer).get(i);
//					if (i==0) {
//						gl.glVertexAttrib2d(shader.getVertexTexCoordID(), textureCoord.x, textureCoord.z);
//					}
//				}
//			}
//
//			VectorXYZ n = normals.get(i);
//			gl.glVertexAttrib3d(shader.getVertexNormalID(), n.x, n.y, -n.z);
//			
//			VectorXYZ v = vertices.get(i);
//			gl.glVertexAttrib3d(shader.getVertexPositionID(), v.x, v.y, -v.z);
//			
//		}
//		
//		gl.glEnd();
//	}
	
	@Override
	public void finish() {
		if (isFinished()) return;
		
		rendererShader = new JOGLRendererVBOShader(gl, textureManager, primitiveBuffer, xzBoundary);
		renderer = rendererShader;
		nonAreaRenderer = new JOGLRendererVBONonAreaShader(gl, nonAreaShader, nonAreaPrimitives);
	}

	@Override
	public void setXZBoundary(AxisAlignedBoundingBoxXZ boundary) {
		this.xzBoundary = boundary;
	}
}
