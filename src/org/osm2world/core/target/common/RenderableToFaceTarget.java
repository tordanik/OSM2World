package org.osm2world.core.target.common;

import org.osm2world.core.target.Renderable;

/**
 * a renderable with specialized output code for {@link FaceTarget}s
 */
public interface RenderableToFaceTarget extends Renderable {
	
	public void renderTo(FaceTarget<?> target);
	
}
