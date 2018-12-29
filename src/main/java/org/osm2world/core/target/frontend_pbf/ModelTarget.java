package org.osm2world.core.target.frontend_pbf;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.model.Model;

/**
 * a target that allows rendering instanced models
 *
 * TODO: delete this after merging universal model support
 */
public interface ModelTarget<R extends Renderable> extends Target<R> {

	/** draw an instanced model */
	public void drawModel(Model model, VectorXYZ position,
			double direction, Double height, Double width, Double length);

}
