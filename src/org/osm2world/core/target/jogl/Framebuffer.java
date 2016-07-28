package org.osm2world.core.target.jogl;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;

import javax.media.opengl.GL3;

public class Framebuffer {
	private final int target;

	private GL3 gl;

	private int framebufferID;
	private int textureID;

	private Cubemap cubemap;

	private ByteBuffer[] buffers = new ByteBuffer[6];
	private int[] images;

	public Framebuffer(int target) {
		this.target = target;
	}

	public Framebuffer(int target, GL3 gl) {
		this(target);
		init(gl);
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
	
	public void init(GL3 gl) {
		this.gl = gl;

		// Generate the framebuffer
		IntBuffer framebuffer = IntBuffer.allocate(1);
		gl.glGenFramebuffers(1, framebuffer);

		framebufferID = framebuffer.get();
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, framebufferID);

		IntBuffer cubemap = IntBuffer.allocate(1);
		gl.glGenTextures(1, cubemap);
		textureID = cubemap.get();

		gl.glBindTexture(GL3.GL_TEXTURE_CUBE_MAP, textureID);

		// Prepare the texture that we will render to
		if(target == GL3.GL_TEXTURE_CUBE_MAP) {
			// Cubemap size
			int s = 800;

			for(int i = 0; i < 6; i++) {

				// Allocate room for faces
				gl.glTexImage2D(
					GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 
					0, GL3.GL_RGB, s, s, 0, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, null
				);
			}

			gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
			gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
			gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_R, GL3.GL_CLAMP_TO_EDGE);

			gl.glBindTexture(GL3.GL_TEXTURE_CUBE_MAP, 0);
			gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
		} else {
			// TODO Allocate room for TEXTURE_2D
		}
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
		gl.glViewport(0,0,800,800);
		gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0
				, GL3.GL_TEXTURE_2D, textureID, 0);

		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE) {
			System.err.println("Framebuffer not ready");
			return;
		}
	}

	public void bind(int face) {
		if(target != GL3.GL_TEXTURE_CUBE_MAP) {
			System.err.println("Can only bind face on cubemap");
			return;
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

				break;

			default:
				System.err.println(face + " is not a valid cubemap face");
				break;
		}
	}

	public void unbind() {
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	}

}
