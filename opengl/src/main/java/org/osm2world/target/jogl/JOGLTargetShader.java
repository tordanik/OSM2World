package org.osm2world.target.jogl;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2GL3.GL_FILL;
import static com.jogamp.opengl.GL2GL3.GL_LINE;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static java.util.Arrays.asList;

import java.awt.*;
import java.io.File;
import java.nio.FloatBuffer;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXYZW;
import org.osm2world.math.shapes.AxisAlignedBoundingBoxXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.target.common.lighting.GlobalLightingParameters;
import org.osm2world.target.common.material.ImageFileTexture;
import org.osm2world.target.common.material.TextureData.Wrap;
import org.osm2world.target.common.material.TextureDataDimensions;
import org.osm2world.target.common.rendering.Camera;
import org.osm2world.target.common.rendering.Projection;
import org.osm2world.target.common.texcoord.NamedTexCoordFunction;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;

public class JOGLTargetShader extends AbstractJOGLTarget implements JOGLTarget {
	private DefaultShader defaultShader;
	private ShadowMapShader shadowMapShader;
	private ShadowVolumeShader shadowVolumeShader;
	//private DepthBufferShader depthBufferShader;
	private SSAOShader ssaoShader;
	private NonAreaShader nonAreaShader;
	private BackgroundShader backgroundShader;
	private GL3 gl;

	/**
	 * PMVMatrix for rendering the world from camera perspective.
	 */
	private PMVMatrix pmvMatrix;
	private JOGLRendererVBONonAreaShader nonAreaRenderer;
	private JOGLRendererVBOShader rendererShader;
	private JOGLRendererVBOShadowVolume rendererShadowVolume;
	private AxisAlignedRectangleXZ xzBoundary;
	private boolean showShadowPerspective;

	public JOGLTargetShader(GL3 gl, JOGLRenderingParameters renderingParameters,
			GlobalLightingParameters globalLightingParameters) {
		super(gl, renderingParameters, globalLightingParameters);
		defaultShader = new DefaultShader(gl);
		shadowMapShader = new ShadowMapShader(gl);
		//depthBufferShader = new DepthBufferShader(gl);
		ssaoShader = new SSAOShader(gl);
		shadowVolumeShader = new ShadowVolumeShader(gl);
		nonAreaShader = new NonAreaShader(gl);
		backgroundShader = new BackgroundShader(gl);
		this.gl = gl;
		pmvMatrix = new PMVMatrix();
		reset();
	}

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

		Texture backgroundTexture = textureManager.getTextureForTextureData(ImageFileTexture.create(
				backgroundImage, new TextureDataDimensions(1, 1), Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z));

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

	/**
	 * Calculate tighter near and far planes for the boundingBox around the visible world objects.
	 * @param camera the current camera for which the planes are calculated
	 * @param projection the current projection
	 * @param boundingBox the bounding box around all visible world objects
	 * @return a new projection with near and far planes as tight as possible
	 */
	public static Projection updateClippingPlanesForCamera(Camera camera, Projection projection, AxisAlignedBoundingBoxXYZ boundingBox) {

		double nearPlane = Double.POSITIVE_INFINITY, farPlane = 0;

		PMVMatrix camMat = new PMVMatrix();
		VectorXYZ pos = camera.getPos();
		VectorXYZ lookAt = camera.getLookAt();
		VectorXYZ up = camera.getUp();
		camMat.gluLookAt(
				(float)pos.x, (float)pos.y, (float)-pos.z,
				(float)lookAt.x, (float)lookAt.y, (float)-lookAt.z,
				(float)up.x, (float)up.y, (float)-up.z);


		for (VectorXYZ corner : boundingBox.corners()) {
			float[] result = new float[4];
			FloatUtil.multMatrixVec(camMat.glGetMvMatrixf(),
					new float[]{(float)corner.x, (float)corner.y, (float)corner.z, 1}, result);
			VectorXYZ cornerCam = new VectorXYZ(result[0]/result[3], result[1]/result[3], result[2]/result[3]);
			double depth = -cornerCam.z;
			nearPlane = Math.min(depth, nearPlane);
			farPlane = Math.max(depth, farPlane);
		}

		if (nearPlane == Double.POSITIVE_INFINITY)
			nearPlane = projection.getNearClippingDistance();
		else
			nearPlane = Math.max(projection.getNearClippingDistance(), nearPlane);
		if (farPlane == 0)
			farPlane = projection.getFarClippingDistance();
		else
			farPlane = Math.min(projection.getFarClippingDistance(), farPlane);

		return new Projection(projection.isOrthographic(), projection.getAspectRatio(),
				projection.getVertAngle(), projection.getVolumeHeight(), nearPlane, farPlane);
	}

	@Override
	public void renderPart(Camera camera, Projection projection, double xStart,
			double xEnd, double yStart, double yEnd) {
		if (renderer == null) {
			throw new IllegalStateException("finish must be called first");
		}

		if (renderingParameters.overwriteProjectionClippingPlanes) {
			projection = updateClippingPlanesForCamera(camera, projection, rendererShader.getBoundingBox());
		}

		applyProjectionMatricesForPart(pmvMatrix, projection,
				xStart, xEnd, yStart, yEnd);

		applyCameraMatrices(pmvMatrix, camera);

		if (renderingParameters.useSSAO) {
			defaultShader.setSSAOkernelSize(renderingParameters.SSAOkernelSize);
			defaultShader.setSSAOradius(renderingParameters.SSAOradius);

			// based on http://john-chapman-graphics.blogspot.de/2013/01/ssao-tutorial.html
			// render depth buffer only
			ssaoShader.useShader();
			ssaoShader.setPMVMatrix(pmvMatrix);
			applyRenderingParameters(gl, renderingParameters);
			rendererShader.setShader(ssaoShader);
			rendererShader.render();
			ssaoShader.disableShader();
		}

		if (renderingParameters.useShadowMaps) {

			// TODO: render only part?
			shadowMapShader.setCameraFrustumPadding(renderingParameters.shadowMapCameraFrustumPadding);
			shadowMapShader.setShadowMapSize(renderingParameters.shadowMapWidth, renderingParameters.shadowMapHeight);
			shadowMapShader.useShader();
			shadowMapShader.preparePMVMatrix(globalLightingParameters, pmvMatrix, rendererShader.getBoundingBox());
			// render opaque shadow casters only when not using shadow volumes simultaneously, as those will be rendered there
			shadowMapShader.setRenderOpaque(!renderingParameters.useShadowVolumes);
			//shadowMapShader.setPMVMatrix(pmvMatrix);

			/* render primitives to shadow map*/
			rendererShader.setShader(shadowMapShader);
			rendererShader.render();

			//ShaderManager.saveDepthBuffer(new File("/home/sebastian/shadowmap"+xStart+"_"+yStart+".png"), shadowMapShader.getShadowMapHandle(), shadowMapShader.shadowMapWidth, shadowMapShader.shadowMapHeight, gl);
			//shadowMapShader.saveShadowMap(new File("/home/sebastian/shadowmap.bmp"));
			//shadowMapShader.saveColorBuffer(new File("/home/sebastian/shadowmap_color"+xStart+"_"+yStart+".png"));

			shadowMapShader.disableShader();
		}

		/* apply camera and projection information */
		defaultShader.useShader();
		defaultShader.loadDefaults();

		if (showShadowPerspective)
			defaultShader.setPMVMatrix(shadowMapShader.getPMVMatrix());
		else
			defaultShader.setPMVMatrix(pmvMatrix);

		/* apply global rendering parameters */

		applyRenderingParameters(gl, renderingParameters);
		applyLightingParameters(defaultShader, globalLightingParameters);

		if (renderingParameters.useShadowMaps) {
			defaultShader.bindShadowMap(shadowMapShader.getShadowMapHandle());
			defaultShader.setShadowMatrix(shadowMapShader.getPMVMatrix());
		}
		if (!showShadowPerspective && renderingParameters.useSSAO) {
			defaultShader.enableSSAOwithDepthMap(ssaoShader.getDepthBuferHandle());
		}

		// if using shadow volumes render semi-transparent objects later
		defaultShader.setRenderSemiTransparent(!renderingParameters.useShadowVolumes);

		/* render primitives */

		rendererShader.setShader(defaultShader);
		rendererShader.render(camera, projection);

		defaultShader.disableShader();

		/* non area primitives */
		nonAreaShader.useShader();
		nonAreaShader.loadDefaults();
		if (showShadowPerspective)
			nonAreaShader.setPMVMatrix(shadowMapShader.getPMVMatrix());
		else
			nonAreaShader.setPMVMatrix(pmvMatrix);

		nonAreaRenderer.render();

		nonAreaShader.disableShader();

		/* render shadows with shadow volumes on top of world with lighting */
		if (renderingParameters.useShadowVolumes) {

			/* Render shadow volumes with depth-fail algorithm. Uses the previously filled depth buffer */
			int[] drawbuffer = new int[1];
			gl.glGetIntegerv(GL2GL3.GL_DRAW_BUFFER, drawbuffer, 0);
			gl.glDrawBuffer(GL.GL_NONE);
			gl.glEnable(GL.GL_STENCIL_TEST);
			gl.glDepthMask(false);
		    gl.glEnable(GL3.GL_DEPTH_CLAMP); // used to clamp the infinity big shadow volumes
		    gl.glDisable(GL_CULL_FACE);

		    // We need the stencil test to be enabled but we want it
		    // to succeed always. Only the depth test matters.
		    gl.glStencilFunc(GL.GL_ALWAYS, 0, 0xff);

		    // Set the stencil test per the depth fail algorithm
		    gl.glStencilOpSeparate(GL.GL_BACK, GL.GL_KEEP, GL.GL_INCR_WRAP, GL.GL_KEEP);
		    gl.glStencilOpSeparate(GL.GL_FRONT, GL.GL_KEEP, GL.GL_DECR_WRAP, GL.GL_KEEP);

		    // relax depth test to prevent z-fighting with self shadowing
		    //gl.glDepthFunc(GL.GL_LEQUAL);

			// if using shadow volumes render semi-transparent objects later
		    shadowVolumeShader.setRenderSemiTransparent(!renderingParameters.useShadowVolumes);

		    shadowVolumeShader.useShader();
		    shadowVolumeShader.setPMVMatrix(pmvMatrix);
		    rendererShadowVolume.setShader(shadowVolumeShader);
		    rendererShadowVolume.render();
		    shadowVolumeShader.disableShader();

		    // Restore local stuff
		    gl.glDepthMask(true);
		    gl.glDisable(GL3.GL_DEPTH_CLAMP);
		    gl.glEnable(GL_CULL_FACE);

		    /* Render scene in shadow */
		    gl.glDrawBuffer(drawbuffer[0]);
		    // Draw only if the corresponding stencil value is NOT zero
		    gl.glStencilFunc(GL.GL_NOTEQUAL, 0x0, 0xFF);
		    // prevent update to the stencil buffer
		    gl.glStencilOpSeparate(GL.GL_BACK, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
		    gl.glStencilOpSeparate(GL.GL_FRONT, GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);
		    // Draw on top of the already rendered world with lighting
		    gl.glDepthFunc(GL.GL_LEQUAL);

		    applyRenderingParameters(gl, renderingParameters);
		    defaultShader.useShader();
			defaultShader.loadDefaults();
			defaultShader.setPMVMatrix(pmvMatrix);
			defaultShader.setShadowed(true);

			if (!showShadowPerspective && renderingParameters.useSSAO) {
				defaultShader.enableSSAOwithDepthMap(ssaoShader.getDepthBuferHandle());
			}

			// if using shadow volumes render semi-transparent objects later
			defaultShader.setRenderSemiTransparent(!renderingParameters.useShadowVolumes);

//			/* render primitives */
			rendererShader.setShader(defaultShader);
			rendererShader.render(camera, projection);
			defaultShader.setShadowed(false);
			defaultShader.disableShader();

			/* non area primitives */
			nonAreaShader.useShader();
			nonAreaShader.loadDefaults();

			nonAreaShader.setPMVMatrix(pmvMatrix);

			nonAreaRenderer.render();

			nonAreaShader.disableShader();

			// Render shadow volumes for debug
			/*gl.glDisable(GL.GL_STENCIL_TEST);
			//gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		    shadowVolumeShader.useShader();
		    shadowVolumeShader.setPMVMatrix(pmvMatrix);
		    rendererShadowVolume.setShader(shadowVolumeShader);
		    rendererShadowVolume.render(camera, projection);
		    shadowVolumeShader.disableShader();*/

			// reset
		    gl.glDisable(GL.GL_STENCIL_TEST);
		    gl.glDepthFunc(GL.GL_LESS);

			/* render semi-transparent objects now */
		    // NOTE: results could be improved slightly, if the depth-fail algorithm is executed here as well
		    //       the result would be that the topmost semi-transparent pixel would receive shadow volume shadows
		    //       (needs to be investigated, if the difference is noticeable in practice)
		    defaultShader.useShader();
			defaultShader.loadDefaults();
			defaultShader.setPMVMatrix(pmvMatrix);

			/* apply global rendering parameters */

			applyRenderingParameters(gl, renderingParameters);
			applyLightingParameters(defaultShader, globalLightingParameters);

			if (renderingParameters.useShadowMaps) {
				defaultShader.bindShadowMap(shadowMapShader.getShadowMapHandle());
				defaultShader.setShadowMatrix(shadowMapShader.getPMVMatrix());
			}
			if (!showShadowPerspective && renderingParameters.useSSAO) {
				defaultShader.enableSSAOwithDepthMap(ssaoShader.getDepthBuferHandle());
			}
			defaultShader.setRenderOnlySemiTransparent(true);

			/* render primitives */

			rendererShader.setShader(defaultShader);
			rendererShader.render(camera, projection);

			defaultShader.disableShader();
		}

		finishRendering(gl);
	}

	/**
	 * Disables culling and depth testing
	 */
	static final void finishRendering(GL gl) {
		gl.glDisable(GL_CULL_FACE);
		gl.glDisable(GL_DEPTH_TEST);
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
	 * similar to {@link #applyProjectionMatrices(PMVMatrix, Projection)},
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

	/**
	 * Draw the corners of a bounding box as colored lines.
	 * @param color the color of the lines
	 * @param bb the bounding box to draw
	 */
	protected final void drawBoundingBox(Color color, AxisAlignedBoundingBoxXYZ bb) {
		// bottom
		drawBox(color, new VectorXYZ(bb.minX, bb.minY, bb.minZ), new VectorXYZ(
				bb.minX, bb.minY, bb.maxZ), new VectorXYZ(bb.maxX, bb.minY,
				bb.maxZ), new VectorXYZ(bb.maxX, bb.minY, bb.minZ));
		// top
		drawBox(color, new VectorXYZ(bb.minX, bb.maxY, bb.minZ), new VectorXYZ(
				bb.minX, bb.maxY, bb.maxZ), new VectorXYZ(bb.maxX, bb.maxY,
				bb.maxZ), new VectorXYZ(bb.maxX, bb.maxY, bb.minZ));
		// bottom/top connections
		drawLineStrip(color, 1, new VectorXYZ(bb.minX, bb.minY, bb.minZ),
				new VectorXYZ(bb.minX, bb.maxY, bb.minZ));
		drawLineStrip(color, 1, new VectorXYZ(bb.minX, bb.minY, bb.maxZ),
				new VectorXYZ(bb.minX, bb.maxY, bb.maxZ));
		drawLineStrip(color, 1, new VectorXYZ(bb.maxX, bb.minY, bb.maxZ),
				new VectorXYZ(bb.maxX, bb.maxY, bb.maxZ));
		drawLineStrip(color, 1, new VectorXYZ(bb.maxX, bb.minY, bb.minZ),
				new VectorXYZ(bb.maxX, bb.maxY, bb.minZ));
	}

	/**
	 * Draw a colored line between two points.
	 */
	protected final void drawLine(Color color,
			VectorXYZ v1, VectorXYZ v2) {
		drawLineLoop(color, 1, asList(v1, v2));
	}

	/**
	 * Draw a colored 2D box as line loop.
	 */
	protected final void drawBox(Color color,
			VectorXYZ v1, VectorXYZ v2, VectorXYZ v3, VectorXYZ v4) {
		drawLineLoop(color, 1, asList(v1, v2, v3, v4));
	}

	@Override
	public void finish() {
		if (isFinished()) return;

		//this.drawLineLoop(Color.WHITE, 1, Arrays.asList(new VectorXYZ[]{xzBoundary.topLeft().xyz(0.1), xzBoundary.topRight().xyz(0.1), xzBoundary.bottomRight().xyz(0.1), xzBoundary.bottomLeft().xyz(0.1)}));
		rendererShader = new JOGLRendererVBOShader(gl, textureManager, primitiveBuffer, xzBoundary);
		renderer = rendererShader;
		if (renderingParameters.drawBoundingBox) {
			this.drawBoundingBox(Color.RED, rendererShader.getBoundingBox());
		}
		nonAreaRenderer = new JOGLRendererVBONonAreaShader(gl, nonAreaShader, nonAreaPrimitives);
		if (renderingParameters.useShadowVolumes)
			rendererShadowVolume = new JOGLRendererVBOShadowVolume(gl, primitiveBuffer, new VectorXYZW(globalLightingParameters.lightFromDirection, 0));
	}

	@Override
	public void reset() {
		super.reset();
		if (rendererShadowVolume != null) {
			rendererShadowVolume.freeResources();
			rendererShadowVolume = null;
		}
	}

	@Override
	public void setXZBoundary(AxisAlignedRectangleXZ boundary) {
		this.xzBoundary = boundary;
	}

	/**
	 * Set whether to use the real camera PMVMatrix or the PMVMatrix normally
	 * used for drawing the shadow map when rendering the world.
	 */
	public void setShowShadowPerspective(boolean s) {
		this.showShadowPerspective = s;
	}
}
