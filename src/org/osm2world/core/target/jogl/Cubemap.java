package org.osm2world.core.target.jogl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;


public class Cubemap {
	// The vertices of a cube where all faces face inward with a CW winding direction
	public static final float[] VERTS = {
        -1.0f,  1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
  
        -1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,
  
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
   
        -1.0f, -1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,
  
        -1.0f,  1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f, -1.0f,
  
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f,  1.0f
    };
	public static final String[] NAMES = {"posx", "negx", "negy", "posy", "posz", "negz"};

	private int addr;
	private String[] filename;
	private Texture cubemap;


	public Cubemap(Texture cubemap) {
		this.cubemap = cubemap;
	}

	public Cubemap(String[] filename) {
		this.filename = filename;
	}

	public Cubemap(int textureID) {
		addr = textureID;
	}
	

	public void bind(GL3 gl, int textureNumber) {
		if(addr != 0) {
			gl.glActiveTexture(GL3.GL_TEXTURE0 + textureNumber);
			gl.glBindTexture(GL3.GL_TEXTURE_CUBE_MAP, addr);
		} else {
			// If this texture has not been bound before, generate it first
			if(cubemap == null) {
				cubemap = load(gl, filename);
			}
			cubemap.bind(gl);
		}
	}

	public void unbind(GL3 gl) {
		gl.glBindTexture(GL3.GL_TEXTURE_CUBE_MAP, 0);
	}


	public static Texture load(GL3 gl, String[] file) {
		System.out.println("Cubemap created from: "+ file[0]);

		Texture cubemap = TextureIO.newTexture(GL3.GL_TEXTURE_CUBE_MAP);

		for(int i = 0; i < 6; i ++) {
			String filename = file[i];

			try{
				BufferedImage face = ImageIO.read(new File(filename));
				ImageUtil.flipImageVertically(face);

				TextureData data = AWTTextureIO.newTextureData(
							GLContext.getCurrentGL().getGLProfile(), 
							face, false);


				if (data == null) {
					throw new IOException("Unable to load texture " + filename);
				}
			cubemap.updateImage(gl, data, GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		return cubemap;
	}

}

