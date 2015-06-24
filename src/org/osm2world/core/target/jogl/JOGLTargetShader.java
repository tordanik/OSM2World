package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_BACK;
import static javax.media.opengl.GL.GL_CCW;
import static javax.media.opengl.GL.GL_CULL_FACE;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_FRONT_AND_BACK;
import static javax.media.opengl.GL2ES1.GL_LIGHT_MODEL_AMBIENT;
import static javax.media.opengl.GL2GL3.GL_FILL;
import static javax.media.opengl.GL2GL3.GL_LINE;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_AMBIENT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_DIFFUSE;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHT0;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_POSITION;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_SPECULAR;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.Color;
import java.io.File;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

import com.jogamp.opengl.util.PMVMatrix;

public class JOGLTargetShader extends AbstractJOGLTarget implements JOGLTarget {
	private DefaultShader defaultShader;
	private NonAreaShader nonAreaShader;
	private GL3 gl;
	private PMVMatrix pmvMatrix;
	private JOGLRendererVBONonAreaShader nonAreaRenderer;
	
	public JOGLTargetShader(GL3 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		super(gl, renderingParameters, globalLightingParameters);
		defaultShader = new DefaultShader(gl);
		nonAreaShader = new NonAreaShader(gl);
		this.gl = gl;
		pmvMatrix = new PMVMatrix();
		reset();
	}

	@Override
	public void drawBackgoundImage(File backgroundImage,
			int startPixelX, int startPixelY, int pixelWidth, int pixelHeight,
			JOGLTextureManager textureManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renderPart(Camera camera, Projection projection, double xStart,
			double xEnd, double yStart, double yEnd) {
		if (renderer == null) {
			throw new IllegalStateException("finish must be called first");
		}
		
		/* apply camera and projection information */
		defaultShader.useShader();
		defaultShader.loadDefaults();
		
		applyProjectionMatricesForPart(pmvMatrix, projection,
				xStart, xEnd, yStart, yEnd);
		
		applyCameraMatrices(pmvMatrix, camera);
		
		defaultShader.setPMVMatrix(pmvMatrix);
		
		/* apply global rendering parameters */
		
		applyRenderingParameters(gl, renderingParameters);
		applyLightingParameters(defaultShader, globalLightingParameters);
		
		/* render primitives */
		
		renderer.render(camera, projection);
		
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
	
	static final void applyLightingParameters(DefaultShader shader,
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
		
		renderer = new JOGLRendererVBOShader(gl, defaultShader, textureManager, primitiveBuffer);
		nonAreaRenderer = new JOGLRendererVBONonAreaShader(gl, nonAreaShader, nonAreaPrimitives);
	}
}