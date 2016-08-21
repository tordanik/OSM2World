package org.osm2world.core.target.jogl;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;

import javax.media.opengl.GL3;

public class Framebuffer {
	private final int target;
	private final boolean depthBuffer;

	private GL3 gl;

	private int framebufferID;
	private int textureID;
	private int depthBufferID;

	private int viewWidth;
	private int viewHeight;

	private int prevWidth;
	private int prevHeight;

	private Cubemap cubemap;

	private ByteBuffer[] buffers = new ByteBuffer[6];
	private int[] images;

	public Framebuffer(int target, int width, int height, boolean depthBuffer) {
		this.target = target;
		if(target == GL3.GL_TEXTURE_CUBE_MAP && width != height)
			System.err.println("Cubemaps must be square");

		this.viewWidth = width;
		this.viewHeight = height;
		this.depthBuffer = depthBuffer;
	}

	/**
	 * Gets the finished texture that has been rendered to
	 **/
	public int getTextureID() {
		return textureID;
	}

	public Cubemap getCubemap() {
		if(this.target != GL3.GL_TEXTURE_CUBE_MAP) {
			System.err.println("Framebuffer not bound to a cubemap");
			return null;
		}
		if(cubemap == null)
			cubemap = new Cubemap(textureID);
		return cubemap;
	}

	public int getTexture() {
		if(this.target != GL3.GL_TEXTURE_2D) {
			System.err.println("Framebuffer not bound to a texture");
			return -1;
		}
		return textureID;
	}
	
	public void init(GL3 gl, boolean useAlpha) {
		this.gl = gl;

		// Generate the framebuffer
		IntBuffer framebuffer = IntBuffer.allocate(1);
		gl.glGenFramebuffers(1, framebuffer);

		framebufferID = framebuffer.get();
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, framebufferID);

		if(depthBuffer) {
			IntBuffer t = IntBuffer.allocate(1);
			gl.glGenRenderbuffers(1, t);
			depthBufferID = t.get();

			gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, depthBufferID);
			gl.glRenderbufferStorage(GL3.GL_RENDERBUFFER, GL3.GL_DEPTH24_STENCIL8, viewWidth, viewHeight);
			gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_STENCIL_ATTACHMENT, GL3.GL_RENDERBUFFER, depthBufferID);  

		}

		IntBuffer texture = IntBuffer.allocate(1);
		gl.glGenTextures(1, texture);
		textureID = texture.get();

		gl.glBindTexture(target, textureID);

		int colorspace = useAlpha ? GL3.GL_RGBA : GL3.GL_RGB;

		// Prepare the texture that we will render to
		if(target == GL3.GL_TEXTURE_CUBE_MAP) {
			// Cubemap size
			int s = viewWidth;

			for(int i = 0; i < 6; i++) {

				// Allocate room for faces
				gl.glTexImage2D(
					GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 
					0, colorspace, s, s, 0, colorspace, GL3.GL_UNSIGNED_BYTE, null
				);
			}

			gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_R, GL3.GL_CLAMP_TO_EDGE);

		} else {
			gl.glTexImage2D(
				GL3.GL_TEXTURE_2D, 
				0, colorspace, viewWidth, viewHeight, 0, colorspace, GL3.GL_UNSIGNED_BYTE, null
			);
		}

		gl.glTexParameteri(target, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
		gl.glTexParameteri(target, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
		gl.glTexParameteri(target, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(target, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);

		gl.glBindTexture(target, 0);
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	}

	@Override
	public void finalize() {
		// TODO Cleanup gl resources
	}

	public void bind() {
		if(target == GL3.GL_TEXTURE_CUBE_MAP) {
			System.err.println("Must specify face to bind cubemap");
			return;
		}

		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, framebufferID);
		gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0
				, GL3.GL_TEXTURE_2D, textureID, 0);

		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE) {
			System.err.println("Framebuffer not ready");
			return;
		}

		// TODO get these from somewhere
		prevWidth = 800;
		prevHeight = 600;
		gl.glViewport(0, 0, viewWidth, viewHeight);
	}

	public void bind(int face) {
		if(target != GL3.GL_TEXTURE_CUBE_MAP) {
			System.err.println("Can only bind face on cubemap");
			return;
		}

		if(depthBuffer) {
			gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, depthBufferID);
		}

		switch(face) {
			case GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X:
			case GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
			case GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
			case GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
			case GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
			case GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
				gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, framebufferID);
				gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0
						, face, textureID, 0);

				if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE) {
					System.err.println("Framebuffer not ready");
					System.err.println(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
					return;
				}

				// TODO get these from somewhere
				prevWidth = 800;
				prevHeight = 600;
				gl.glViewport(0, 0, viewWidth, viewHeight);

				break;

			default:
				System.err.println(face + " is not a valid cubemap face");
				break;
		}
	}

	public void unbind() {
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, prevWidth, prevHeight);
		if(depthBuffer)
			gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, 0);
	}

}
