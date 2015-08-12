package org.osm2world.core.target.jogl;

import static javax.media.opengl.GL.*;

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
	final boolean useShadowVolumes;
	final boolean useShadowMaps;
	final boolean useSSAO;
	final boolean overwriteProjectionClippingPlanes;
	
	/**
	 * @param frontFace   winding of the front face for backface culling;
	 *                     null disables backface culling
	 * @param wireframe   renders just a wireframe instead of filled surfaces
	 * @param useZBuffer  enables the z buffer, should usually be true
	 */
	public JOGLRenderingParameters(
			Winding frontFace, boolean wireframe, boolean useZBuffer, boolean useShadowVolumes,
			boolean useShadowMaps, boolean useSSAO, boolean overwriteProjectionClippingPlanes) {
		
		this.frontFace = frontFace;
		this.wireframe = wireframe;
		this.useZBuffer = useZBuffer;
		this.useShadowVolumes = useShadowVolumes;
		this.useShadowMaps = useShadowMaps;
		this.useSSAO = useSSAO;
		this.overwriteProjectionClippingPlanes = overwriteProjectionClippingPlanes;
	}
	
}
