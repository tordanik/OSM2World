package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL2;

import org.osm2world.core.GlobalValues;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * view that prints an explanation how to open OSM data.
 * Only visible when no data is available.
 */
public class HelpView extends DebugView {

	@Override
	public boolean canBeUsed() {
		return map == null && terrain == null && eleData == null;
	}
	
	@Override
	protected void renderToImpl(GL2 gl, Camera camera, Projection projection) {
		
		if (!canBeUsed()) { return; }
		
		JOGLTarget target = new JOGLTarget(gl, camera);

		//TODO: needs real panel measures; currently guesses 800x600

		target.drawText("Use \"File\" > \"Open OSM file\" "
				+ "to load a file containing OpenStreetMap data.",
				50, 550, 800, 600, Color.LIGHT_GRAY);
		
		target.drawText("This is OSM2World " + GlobalValues.VERSION_STRING,
				50, 100, 800, 600, Color.LIGHT_GRAY);
		target.drawText("Website: " + GlobalValues.OSM2WORLD_URI,
				50, 75, 800, 600, Color.LIGHT_GRAY);
		target.drawText("Usage instructions: " + GlobalValues.WIKI_URI,
				50, 50, 800, 600, Color.LIGHT_GRAY);
				
	}
	
}
