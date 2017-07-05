package org.osm2world.core.external_models;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.external_models.objparser.ObjParser;
import org.osm2world.core.external_models.objparser.ObjParser.ObjFace;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.data.WorldObject;


public class ExternalModel implements RenderableToAllTargets {
	
	private ObjParser model;
	
	private double rot = Math.toRadians(0.0);
	private VectorXZ origin = new VectorXZ(0.0, 0.0);
	private double scale = 1.0;
	
	public ExternalModel(WorldObject object, File model) {
		this.model = new ObjParser(model);

		this.scale = 1.0 / 100.0;
		
		// Just for test
		this.rot = Math.toRadians(30.0);
		origin = object.getPrimaryMapElement().getAxisAlignedBoundingBoxXZ().center();
	}

	@Override
	public void renderTo(Target<?> target) {
		for(ObjFace f : this.model.listFaces()) {
			List<VectorXYZ> vs = new ArrayList<>(f.vs.size());
			for (VectorXYZ src : f.vs) {
				VectorXYZ t = src.rotateY(rot);
				t = t.add(origin);
				t = t.mult(scale);
				vs.add(t);
			}
			if (f.material != null) {
				target.drawTriangleFan(f.material, vs, f.texCoordLists);
			}
		}
	}

	
}
