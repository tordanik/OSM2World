package org.osm2world.output.jogl;

import static com.jogamp.opengl.GL.*;

import org.osm2world.output.common.material.Material;
import org.osm2world.output.common.material.Material.AmbientOcclusion;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

/**
 * Shader to render the depth buffer into a texture that can be used to implement SSAO later.
 */
public class SSAOShader extends DepthBufferShader {

	private int depthBufferHandle;
	private int frameBufferHandle;
	private int width;
	private int height;

	public SSAOShader(GL3 gl) {
		super(gl);
		initialize();
	}

	/**
	 * Setup the framebuffer and texture.
	 */
	private void initialize() {
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		width = viewport[2];
		height = viewport[3];

		// create the shadow map texture / depth buffer
		int[] tmp = new int[1];
		gl.glGenTextures(1,tmp,0);
		depthBufferHandle = tmp[0];
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, depthBufferHandle);

		gl.glTexImage2D(GL.GL_TEXTURE_2D,          // target texture type
		        0,                                  // mipmap LOD level
		        GL3.GL_DEPTH_COMPONENT,         // internal pixel format
		                                            //GL_DEPTH_COMPONENT
		        width,                     // width of generated image
		        height,                    // height of generated image
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
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		//float [] border = {1.0f, 0.0f, 0.0f, 0.0f};
		//gl.glTexParameterfv(GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_BORDER_COLOR, border, 0);

		/* special for depth textures: do not retrieve the texture values, but the result of a comparison.
		 * compare the third value (r) of the texture coordinate against the depth value stored at the texture coordinate (s,t)
		 * result will be 1.0 if r is less than the texture value (which means the fragment is nearer) and 0.0 otherwise
		 */
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_COMPARE_MODE, GL3.GL_COMPARE_REF_TO_TEXTURE);
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL3.GL_TEXTURE_COMPARE_FUNC, GL.GL_LESS);

		gl.glActiveTexture(GL.GL_TEXTURE1);
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
	 * Resize the framebuffer backing texture, if size doesn't match.
	 */
	private void resizeBuffer(int width, int height) {
		if (width != this.width || height != this.height) {
			this.width = width;
			this.height = height;
			gl.glBindTexture(GL.GL_TEXTURE_2D, depthBufferHandle);

			gl.glTexImage2D(GL.GL_TEXTURE_2D,          // target texture type
			        0,                                  // mipmap LOD level
			        GL3.GL_DEPTH_COMPONENT,         // internal pixel format
			                                            //GL_DEPTH_COMPONENT
			        width,                     // width of generated image
			        height,                    // height of generated image
			        0,                          // border of image
			        GL3.GL_DEPTH_COMPONENT,     // external pixel format
			        GL.GL_UNSIGNED_BYTE,        // datatype for each value
			        null);  // buffer to store the texture in memory
		}
	}

	/**
	 * Prepares rendering of the depth map. Binds the framebuffer and clears it.
	 * The size of the framebuffer is automatically adjusted to match the current viewport.
	 */
	private void prepareDepthMapGeneration() {
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		resizeBuffer(viewport[2], viewport[3]);

		// bind FBO
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, frameBufferHandle);

		// clear shadow map
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glEnable(GL_DEPTH_TEST);
	}

	/**
	 * {@inheritDoc}
	 * Only primitives that support ambient occlusion will get rendered.
	 */
	@Override
	public boolean setMaterial(Material material, JOGLTextureManager textureManager) {

		if (material.getAmbientOcclusion() == AmbientOcclusion.FALSE) {
			return false;
		}

		return super.setMaterial(material, textureManager);
	}

	/**
	 * Returns the handle of the texture containing the rendered depth map.
	 */
	public int getDepthBuferHandle() {
		return depthBufferHandle;
	}

	/**
	 * Prepares rendering of the depth map. This changes the current framebuffer.
	 * {@link #disableShader()} should be called after the rendering is complete to bind the default framebuffer again.
	 */
	@Override
	public void useShader() {
		super.useShader();
		prepareDepthMapGeneration();
	}

	/**
	 * Completes the rendering of the depth map. The default framebuffer gets restored.
	 */
	@Override
	public void disableShader() {

		//ShaderManager.saveDepthBuffer(new File("/home/sebastian/ssao_depth.png"), depthBufferHandle, width, height, gl);

		// bind default framebuffer
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		super.disableShader();
	}

}