package org.osm2world.viewer.view;

import java.awt.Color;
import java.awt.Font;

import org.osm2world.core.math.Vector3D;

import com.jogamp.opengl.util.awt.TextRenderer;

public class TextRendererFixedFunction implements org.osm2world.viewer.view.TextRenderer {
	
	private static final TextRenderer textRenderer = new TextRenderer(
			new Font("SansSerif", Font.PLAIN, 12), true, false);
	//needs quite a bit of memory, so it must not create an instance for each use!
	
//	@Override
//	public final void drawText(String string, Vector3D pos, Color color) {
//		textRenderer.setColor(color);
//		textRenderer.begin3DRendering();
//		textRenderer.draw3D(string,
//				(float)pos.getX(), (float)pos.getY(), -(float)pos.getZ(),
//				0.05f);
//	}

	@Override
	public final void drawText(String string, int x, int y,
			int screenWidth, int screenHeight, Color color) {
		textRenderer.beginRendering(screenWidth, screenHeight);
		textRenderer.setColor(color);
		textRenderer.draw(string, x, y);
		textRenderer.endRendering();
	}

}
