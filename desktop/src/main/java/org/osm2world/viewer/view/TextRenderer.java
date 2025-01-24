package org.osm2world.viewer.view;

import java.awt.Color;

public interface TextRenderer {

	//public abstract void drawText(String string, Vector3D pos, Color color);

	/**
	 * Draw text beginning at the topleft corner of the window with offset <code>x</code> and <code>y</code>.
	 * @param string the text to draw
	 * @param x left offset
	 * @param y top offset
	 * @param color text color
	 */
	public abstract void drawTextTop(String string, float x, float y, Color color);

	/**
	 * Draw text beginning at the bottomleft corner of the window with offset <code>x</code> and <code>y</code>.
	 * @param string the text to draw
	 * @param x left offset
	 * @param y bottom offset
	 * @param color text color
	 */
	public abstract void drawTextBottom(String string, float x, float y, Color color);

	/**
	 * Reshape the rendering region of the text renderer. Has to be called when the canvas size changes.
	 */
	public abstract void reshape(int width, int height);

	/**
	 * Set the scale factor to apply to the whole textrendering. Scales offsets and fontsize. Default is 1
	 */
	public abstract void setScale(float scale);

	/**
	 * Free all resources.
	 */
	public abstract void destroy();
}
