package org.osm2world.viewer.view;

import org.osm2world.scene.color.Color;

import com.jogamp.opengl.GL2ES2;

public class TextRendererShader implements org.osm2world.viewer.view.TextRenderer {

	// TODO: this is currently unimplemented after the incompatible upgrade to JOGL 1.4

	public TextRendererShader(GL2ES2 gl) {}

	@Override
	public void destroy() {}

	@Override
	public void drawTextTop(String string, float x, float y, Color color) {}

	@Override
	public void drawTextBottom(String string, float x, float y, Color color) {}

	@Override
	public void reshape(int width, int height) {}

	@Override
	public void setScale(float scale) {}

}
