package org.osm2world.viewer.view;

import java.awt.Color;

import org.osm2world.core.math.Vector3D;

public interface TextRenderer {
	
	//public abstract void drawText(String string, Vector3D pos, Color color);

	public abstract void drawText(String string, int x, int y,
			int screenWidth, int screenHeight, Color color);

	public abstract void destroy();
}
