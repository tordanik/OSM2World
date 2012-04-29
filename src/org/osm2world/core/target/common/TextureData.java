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
	
}
