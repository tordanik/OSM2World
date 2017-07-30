package org.osm2world.core.target.common.model;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;

/**
 * a single 3D model, typically loaded from a file or other resource
 */
public interface Model {
	
	/**
	 * draws an instance of the model to any {@link Target}
	 * 
	 * @param target     target for the model; != null
	 * @param direction  rotation of the model in the XZ plane, as an angle in radians
	 * @param height     height of the model; null for default (unspecified) height
	 * @param width      width of the model; null for default (unspecified) width
	 * @param length     length of the model; null for default (unspecified) length
	 */
	public void render(Target<?> target, VectorXYZ position,
			double direction, Double height, Double width, Double length);
	
}
