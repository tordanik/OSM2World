package org.osm2world.core.target.common;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.osm2world.core.target.common.ResourceOutputSettings.ResourceOutputMode.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.target.common.material.ImageFileTexture;
import org.osm2world.core.target.common.material.RasterImageFileTexture;
import org.osm2world.core.target.common.material.TextureData;

/** options for how to handle textures and other resources when exporting generated models/scenes as files */
public record ResourceOutputSettings(
		ResourceOutputMode modeForStaticResources,
		ResourceOutputMode modeForGeneratedResources,
		URI textureDirectory
) {

	public enum ResourceOutputMode {
		/** store the resource inside the model file */
		EMBED,
		/** store the resource as a separate file, and reference the location of that file */
		STORE_SEPARATELY_AND_REFERENCE,
		/** reference the location of an already-existing file */
		REFERENCE
	}

	public ResourceOutputMode modeForTexture(TextureData texture) {
		return texture instanceof RasterImageFileTexture
				? modeForStaticResources()
				: modeForGeneratedResources();
	}

	/**
	 * Stores a texture and returns the path to it.
	 * This can be used to implement {@link ResourceOutputMode#STORE_SEPARATELY_AND_REFERENCE})
	 */
	public String storeTexture(TextureData texture, @Nullable URI baseForRelativePaths) throws IOException {

		File textureDir = new File(textureDirectory);
		boolean textureDirCreated = textureDir.mkdir();
		if (!textureDirCreated && !textureDir.exists()) {
			throw new IOException("Could not create texture directory at " + textureDir);
		}

		String prefix = "tex-" + ((texture instanceof ImageFileTexture)
				? getBaseName(((ImageFileTexture)texture).getFile().getName()) + "-" : "");
		File textureFile = File.createTempFile(prefix, ".png", textureDir);

		ImageIO.write(texture.getBufferedImage(), "png", textureFile);

		if (baseForRelativePaths == null) {
			return textureFile.getAbsolutePath();
		} else {
			return baseForRelativePaths.relativize(textureFile.toURI()).getPath();
		}

	}

	public static ResourceOutputSettings fromConfig(Configuration config, URI defaultTextureDirectory, boolean canEmbed) {

		ResourceOutputMode modeForStaticResources = canEmbed ? EMBED : REFERENCE;
		ResourceOutputMode modeForGeneratedResources = canEmbed ? EMBED : STORE_SEPARATELY_AND_REFERENCE;

		try {
			modeForStaticResources = ResourceOutputMode.valueOf(config.getString("staticResourceOutputMode", ""));
		} catch (IllegalArgumentException ignored) { /* keep existing value */ }

		try {
			modeForGeneratedResources = ResourceOutputMode.valueOf(config.getString("generatedResourceOutputMode", ""));
		} catch (IllegalArgumentException ignored) { /* keep existing value */ }

		URI textureDirectory = defaultTextureDirectory;

		/* perform some validation */

		if (modeForGeneratedResources == REFERENCE) {
			throw new Error("Cannot reference resources which are only generated during runtime");
		} else if ((modeForStaticResources == STORE_SEPARATELY_AND_REFERENCE
				|| modeForGeneratedResources == STORE_SEPARATELY_AND_REFERENCE)
				&& !textureDirectory.getScheme().equals("file")) {
			throw new Error("Cannot store generated resources at a remote location, textureDirectory needs to be file");
		}

		/* build and return the result */

		return new ResourceOutputSettings(modeForStaticResources, modeForGeneratedResources, textureDirectory);

	}

}
