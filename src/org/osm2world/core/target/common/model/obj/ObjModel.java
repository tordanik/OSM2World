package org.osm2world.core.target.common.model.obj;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.model.Model;

/**
 * a model loaded from Wavefront OBJ
 */
public class ObjModel implements Model {
	
	//TODO add meaningful constructor
	
	@Override
	public void render(Target<?> target, VectorXYZ position,
			double direction, Double height, Double width, Double length) {
		
		//TODO replace with actual implementation
		
		target.drawColumn(Materials.CONCRETE, 3, position,
				height != null ? height : 10, 1, 1, false, true);
		
	}
	
}
