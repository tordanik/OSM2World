package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.GlobalValues;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.AbstractJOGLTarget;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.view.TextRenderer;
import org.osm2world.viewer.view.TextRendererFixedFunction;
import org.osm2world.viewer.view.TextRendererShader;

/**
 * view that prints an explanation how to open OSM data.
 * Only visible when no data is available.
 */
public class HelpView extends DebugView {
	
	TextRenderer textRenderer;

	@Override
	public boolean canBeUsed() {
		return map == null;
	}
	
	@Override
	public void renderTo(GL gl, Camera camera, Projection projection) {
		
		if (!canBeUsed()) { return; }
		if (textRenderer == null) {
			if (gl.isGL2ES2()) {
				textRenderer = new TextRendererShader(gl.getGL2ES2());
			} else {
				textRenderer = new TextRendererFixedFunction();
			}
		}
		
		//TODO: needs real panel measures; currently guesses 800x600
		textRenderer.drawText("Use \"File\" > \"Open OSM file\" "
				+ "to load a file containing OpenStreetMap data.",
				50, 550, 800, 600, Color.LIGHT_GRAY);
		
		textRenderer.drawText("This is OSM2World " + GlobalValues.VERSION_STRING,
				50, 100, 800, 600, Color.LIGHT_GRAY);
		textRenderer.drawText("Website: " + GlobalValues.OSM2WORLD_URI,
				50, 75, 800, 600, Color.LIGHT_GRAY);
		textRenderer.drawText("Usage instructions: " + GlobalValues.WIKI_URI,
				50, 50, 800, 600, Color.LIGHT_GRAY);
		
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		//do nothing, has its own renderTo implementation
	}
	
}
