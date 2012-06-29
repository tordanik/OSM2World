package org.osm2world.core.target.common;

import java.io.File;

/**
 * a texture with all information necessary for applying it to an object
 * that has texture coordinates
 */
public class TextureData {
	
	public static enum Wrap { REPEAT, CLAMP };
	
	/** path to the texture file */
	public final File file;
	
	/** width of a single tile of the texture */
	public final double width;
	
	/** height of a single tile of the texture */
	public final double height;
	
	/** wrap style of the texture */
	public final Wrap wrap;
	
	/**
	 * whether the texture is modulated with the material color.
	 * Otherwise, a plain white base color is used, resulting in the texture's
	 * colors appearing unaltered (except for lighting)
	 */
	public final boolean colorable;

	public TextureData(File file, double width, double height, Wrap wrap,
			boolean colorable) {
		
		this.file = file;
		this.width = width;
		this.height = height;
		this.wrap = wrap;
		this.colorable = colorable;
		
	}

	//auto-generated
	@Override
	public String toString() {
		return "TextureData [file=" + file + ", width=" + width
				+ ", height=" + height + ", wrap=" + wrap
				+ ", colorable=" + colorable + "]";
	}

	//auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (colorable ? 1231 : 1237);
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		long temp;
		temp = Double.doubleToLongBits(height);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(width);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((wrap == null) ? 0 : wrap.hashCode());
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
		TextureData other = (TextureData) obj;
		if (colorable != other.colorable)
			return false;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
			return false;
		if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
			return false;
		if (wrap != other.wrap)
			return false;
		return true;
	}
	
}
