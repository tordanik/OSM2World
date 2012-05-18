package org.osm2world.core.target.jogl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * loads textures from files to JOGL and keeps them available for future use
 */
public class JOGLTextureManager {

	private final GL2 gl;
	
	private final Map<File, Texture> availableTextures = new HashMap<File, Texture>();
	
	public JOGLTextureManager(GL2 gl) {
		this.gl = gl;
	}
	
	public Texture getTextureForFile(File file) {
		
		Texture result = availableTextures.get(file);
		
		if (result == null) {
			
			synchronized (this) {
				
				//try again
				
				if (availableTextures.containsKey(file)) {
					return availableTextures.get(file);
				}
				
				try {
					
					result = TextureIO.newTexture(file, true);
					
					availableTextures.put(file, result);
					
				} catch (IOException exc) {
					
					exc.printStackTrace();
					System.exit(2);
					//TODO error handling
					
				}
			}
			
		}
		
		return result;
		
	}
	
	
	public void releaseAll() {
				
		for (Texture texture : availableTextures.values()) {
			texture.destroy(gl);
		}
		
		availableTextures.clear();
		
	}
		
}
