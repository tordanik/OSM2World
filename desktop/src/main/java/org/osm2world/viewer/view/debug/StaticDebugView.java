package org.osm2world.viewer.view.debug;

import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;

/**
 * a {@link DebugView} which does not change depending on the camera or projection
 */
public abstract class StaticDebugView extends DebugView {

	StaticDebugView(String label, String description) {
		super(label, description);
	}

	/**
	 * Lets the subclass add all content and settings for rendering.
	 * Will only be called if {@link #canBeUsed()} is true, and will not be called again even if the camera moves.
	 */
	protected abstract void fillOutput(JOGLOutput output);

	@Override
	protected void updateOutput(JOGLOutput output, boolean viewChanged, Camera camera, Projection projection) {
		if (!output.isFinished()) {
			fillOutput(output);
		}
	}
}
