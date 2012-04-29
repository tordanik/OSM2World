package org.osm2world.core.target.jogl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

/**
 * loads textures from files to JOGL and keeps them available for future use
 */
public class JOGLTextureManager {

	private final GL gl;
	
	private final Map<File, Texture> availableTextures = new HashMap<File, Texture>();
	
	public JOGLTextureManager(GL gl) {
		this.gl = gl;
	}
	
	public Texture getTextureForFile(File file) {
		
		Texture result = availableTextures.get(file);
		
		if (result == null) {
			
			String fileType;
			if (file.getName().endsWith(".jpg")) {
				fileType = "jpg";
			} else {
				fileType = "png";
			}
			
			try {
								
				InputStream stream = new FileInputStream(file);
				TextureData data = TextureIO.newTextureData(stream, true, fileType);
				result = TextureIO.newTexture(data);
				
				availableTextures.put(file, result);
				
			} catch (IOException exc) {
				
				exc.printStackTrace();
				System.exit(2);
				//TODO error handling
				
			}
			
		}
		
		return result;
		
	}
	
	
	public void releaseAll() {
				
		for (Texture texture : availableTextures.values()) {
			texture.dispose();
		}
		
		availableTextures.clear();
		
	}
		
}
