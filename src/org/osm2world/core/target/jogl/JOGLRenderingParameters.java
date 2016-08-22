package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.GL_CCW;
import static javax.media.opengl.GL.GL_CW;

/**
 * global parameters for rendering a JOGL scene
 */
public class JOGLRenderingParameters {
	
	public static enum Winding {
		
		CW(GL_CW), CCW(GL_CCW);
		
		int glConstant;
		
		private Winding(int glConstant) {
			this.glConstant = glConstant;
		}
		
	};
	
	final Winding frontFace;
	final boolean wireframe;
	final boolean useZBuffer;
	final boolean drawBoundingBox;
	final boolean useShadowVolumes;
	final boolean useShadowMaps;
	final int shadowMapWidth;
	final int shadowMapHeight;
	final int shadowMapCameraFrustumPadding;
	final boolean useSSAO;
	final int SSAOkernelSize;
	final float SSAOradius;
	final boolean overwriteProjectionClippingPlanes;
	final boolean showSkyReflections;
	final boolean showGroundReflections;
	final int geomReflType;
	
	/**
	 * @param frontFace
	 *            winding of the front face for backface culling; null disables
	 *            backface culling
	 * @param wireframe
	 *            renders just a wireframe instead of filled surfaces
	 * @param useZBuffer
	 *            enables the z buffer, should usually be true
	 * @param drawBoundingBox
	 *            draw the bounding box used when rendering to determine all
	 *            relevant primitives
	 * @param useShadowVolumes
	 *            renders only shadows casted by non-transparent objects with
	 *            shadow volumes
	 * @param useShadowMaps
	 *            renders the shadows of all objects with a shadow map, but only
	 *            the back faces (from light source view) cast a shadow. if
	 *            shadow volumes are activated too, the shadow map is only used
	 *            for non opaque objects.
	 * @param shadowMapWidth
	 *            resolution of the shadow map
	 * @param shadowMapHeight
	 *            resolution of the shadow map
	 * @param shadowMapCameraFrustumPadding
	 *            padding in meter for the camera frustum to use for the shadow
	 *            map camera. Increase here if objects outside the current
	 *            camera view frustum, that should throw a shadow won't do so.
	 * @param useSSAO
	 *            use screen space ambient occlusion
	 * @param SSAOkernelSize
	 *            size of the sampling kernel (number of samples)
	 * @param SSAOradius
	 *            sampling radius in meter
	 * @param overwriteProjectionClippingPlanes
	 *            optimize the clipping planes of the camera: reduce them to
	 *            match the world bounding box
	 */
	public JOGLRenderingParameters(
			Winding frontFace, boolean wireframe, boolean useZBuffer, boolean drawBoundingBox, 
			boolean useShadowVolumes, boolean useShadowMaps, int shadowMapWidth, int shadowMapHeight, 
			int shadowMapCameraFrustumPadding, boolean useSSAO, int SSAOkernelSize, 
			float SSAOradius, boolean overwriteProjectionClippingPlanes, boolean showSkyReflections,
			boolean showGroundReflections, int geomReflType
			) {
		
		this.frontFace = frontFace;
		this.wireframe = wireframe;
		this.useZBuffer = useZBuffer;
		this.drawBoundingBox = drawBoundingBox;
		this.useShadowVolumes = useShadowVolumes;
		this.useShadowMaps = useShadowMaps;
		this.shadowMapWidth = shadowMapWidth;
		this.shadowMapHeight = shadowMapHeight;
		this.shadowMapCameraFrustumPadding = shadowMapCameraFrustumPadding;
		this.useSSAO = useSSAO;
		this.SSAOkernelSize = SSAOkernelSize;
		this.SSAOradius = SSAOradius;
		this.overwriteProjectionClippingPlanes = overwriteProjectionClippingPlanes;
		this.showSkyReflections = showSkyReflections;
		this.showGroundReflections = showGroundReflections;
		this.geomReflType = geomReflType;
	}
	
	/**
	 * @param frontFace   winding of the front face for backface culling;
	 *                     null disables backface culling
	 * @param wireframe   renders just a wireframe instead of filled surfaces
	 * @param useZBuffer  enables the z buffer, should usually be true
	 */
	public JOGLRenderingParameters(
			Winding frontFace, boolean wireframe, boolean useZBuffer) {
		
		this.frontFace = frontFace;
		this.wireframe = wireframe;
		this.useZBuffer = useZBuffer;
		this.drawBoundingBox = false;
		this.useShadowVolumes = false;
		this.useShadowMaps = false;
		this.shadowMapWidth = 0;
		this.shadowMapHeight = 0;
		this.shadowMapCameraFrustumPadding = 0;
		this.useSSAO = false;
		this.SSAOkernelSize = 0;
		this.SSAOradius = 0;
		this.overwriteProjectionClippingPlanes = false;
		this.showSkyReflections = false;
		this.showGroundReflections = false;
		this.geomReflType = 0;
	}
	
	public JOGLRenderingParameters() {
		this.frontFace = null;
		this.wireframe = false;
		this.useZBuffer = true;
		this.drawBoundingBox = false;
		this.useShadowVolumes = false;
		this.useShadowMaps = false;
		this.shadowMapWidth = 0;
		this.shadowMapHeight = 0;
		this.shadowMapCameraFrustumPadding = 0;
		this.useSSAO = false;
		this.SSAOkernelSize = 0;
		this.SSAOradius = 0;
		this.overwriteProjectionClippingPlanes = false;
		this.showSkyReflections = false;
		this.showGroundReflections = false;
		this.geomReflType = 0;
	}
	
}
