package org.osm2world.core.target.common;

import java.io.File;

import org.osm2world.core.target.common.material.TexCoordFunction;

public class ImageTextureData extends TextureData {

	/** 
	 * Path to the texture file.
	 * Represents a permanent, already saved
	 * image file in contrast to the file 
	 * field in {@link TextTextureData} 
	 */
	private final File file;
	
	public ImageTextureData(File file, double width, double height, Wrap wrap, TexCoordFunction texCoordFunction,
			boolean colorable, boolean isBumpMap) {
		
		super(width, height, wrap, texCoordFunction, colorable, isBumpMap);
		
		this.file = file;
	}
	
	public File getFile() {
		return this.file;
	}
	
	@Override
	public String toString() {
		return "ImageTextureData [file=" + file + ", width=" + width + ", height=" + height + ", wrap=" + wrap
				+ ", texCoordFunction=" + coordFunction + ", colorable=" + colorable + ", isBumpMap=" + isBumpMap + "]";
	}

	//auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	//auto-generated
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageTextureData other = (ImageTextureData) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
	
	

}
