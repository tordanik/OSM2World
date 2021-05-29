package org.osm2world.core.target.jogl;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.osm2world.core.target.common.material.ImageFileTexture;
import org.osm2world.core.target.common.material.TextureData;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * loads textures from files to JOGL and keeps them available for future use
 */
public class JOGLTextureManager {

	private final GL gl;

	private final Map<TextureData, Texture> availableTextures = new HashMap<>();

	public JOGLTextureManager(GL gl) {
		this.gl = gl;
	}

	public Texture getTextureForTextureData(TextureData textureData) {

		boolean createMipmaps = true;

		Texture result = availableTextures.get(textureData);

		if (result == null) {

			synchronized (this) {

				//try again

				if (availableTextures.containsKey(textureData)) {
					return availableTextures.get(textureData);
				}

				if (!(textureData instanceof ImageFileTexture)
						|| !((ImageFileTexture)textureData).getFile().getName().toLowerCase().endsWith("png")) {

					//flip to ensure consistent tex coords with png images
					BufferedImage bufferedImage = textureData.getBufferedImage();
					ImageUtil.flipImageVertically(bufferedImage);

					result = AWTTextureIO.newTexture(
							gl.getGLProfile(), bufferedImage, createMipmaps);

				} else {

					result = AWTTextureIO.newTexture(
							gl.getGLProfile(), textureData.getBufferedImage(), createMipmaps);

				}

				/* workaround for OpenGL 3: call to glGenerateMipmap is missing in [AWT]TextureIO.newTexture()
				 * May be fixed in new versions of JOGL.
				 */
				if (createMipmaps && gl.isGL3()) {
					gl.glGenerateMipmap(result.getTarget());
				}

				availableTextures.put(textureData, result);

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

