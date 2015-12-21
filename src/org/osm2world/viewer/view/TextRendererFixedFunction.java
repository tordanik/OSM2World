package org.osm2world.viewer.view;

import java.awt.Color;
import java.awt.Font;

import com.jogamp.opengl.util.awt.TextRenderer;

public class TextRendererFixedFunction implements org.osm2world.viewer.view.TextRenderer {
	
	private TextRenderer textRenderer = new TextRenderer(
			new Font("SansSerif", Font.PLAIN, 12), true, false);
	//needs quite a bit of memory, so it must not create an instance for each use!
	
	private int screenWidth = 0, screenHeight = 0;
	private float scale = 1;
	
//	@Override
//	public final void drawText(String string, Vector3D pos, Color color) {
//		textRenderer.setColor(color);
//		textRenderer.begin3DRendering();
//		textRenderer.draw3D(string,
//				(float)pos.getX(), (float)pos.getY(), -(float)pos.getZ(),
//				0.05f);
//	}

	protected void drawText(String string, float x, float y, Color color) {
		textRenderer.setColor(color);
		textRenderer.beginRendering((int) (screenWidth / scale), (int) (screenHeight / scale));
		textRenderer.draw(string, (int) x, (int) y);
		textRenderer.endRendering();
	}
	
	@Override
	public void destroy() {
		textRenderer.dispose();
		textRenderer = null;
	}

	@Override
	public void drawTextTop(String string, float x, float y, Color color) {
		this.drawText(string, x, (int) (screenHeight / scale) - y, color);
	}

	@Override
	public void drawTextBottom(String string, float x, float y, Color color) {
		this.drawText(string, x, y, color);
	}

	@Override
	public void reshape(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
	}

	@Override
	public void setScale(float scale) {
		this.scale = scale;
	}

}
