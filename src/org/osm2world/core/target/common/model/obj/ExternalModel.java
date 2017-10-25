package org.osm2world.core.target.common.model.obj;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.obj.parser.ModelLinksProxy;
import org.osm2world.core.target.common.model.obj.parser.ObjModel;
import org.osm2world.core.target.common.model.obj.parser.ObjModel.ObjFace;


public class ExternalModel implements Model {
	
	private ObjModel model;
	
	private VectorXZ originT = new VectorXZ(0.0, 0.0);
	private double scale = 1.0;
	private boolean zAxisUp = false;

	private ModelLinksProxy proxy;
	
	public ExternalModel(String link) {
		this.proxy = new ModelLinksProxy("/opt/osm/3dlib");
		this.model = new ObjModel(link, proxy);
		
		originT = this.model.getBBOX().center().xz().invert();
	}

	@Override
	public void render(Target<?> target, VectorXYZ position, 
			double direction, Double height, Double width,
			Double length) {
		
		VectorXYZ translate = position.add(originT);
		
		for(ObjFace f : this.model.listFaces()) {
			List<VectorXYZ> vs = new ArrayList<>(f.vs.size());
			
			for (VectorXYZ src : f.vs) {
				src = src.mult(scale);

				if (zAxisUp) {
					src = src.rotateX(Math.toRadians(-90.0));
				}
				
				src = src.rotateY(direction);
				src = src.add(translate);

				vs.add(src);
			}
			
			if (f.material != null) {
				f.material = f.material.withAmbientFactor(0.9f);
				target.drawTriangleFan(f.material, vs, f.texCoordLists);
			}
		}
	}

}
