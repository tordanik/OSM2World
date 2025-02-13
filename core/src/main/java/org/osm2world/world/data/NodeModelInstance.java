package org.osm2world.world.data;

import static java.lang.Math.PI;

import java.util.List;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.output.common.mesh.Mesh;
import org.osm2world.output.common.model.InstanceParameters;
import org.osm2world.output.common.model.Model;
import org.osm2world.output.common.model.ModelInstance;

public class NodeModelInstance extends NoOutlineNodeWorldObject {

	public final Model model;
	public final double direction;
	public final double scale;

	public NodeModelInstance(MapNode node, Model model, double direction, double scale) {
		super(node);
		this.model = model;
		this.direction = direction;
		this.scale = scale;
	}

	public NodeModelInstance(MapNode node, Model model, double direction) {
		this(node, model, direction, 1.0);
	}

	public NodeModelInstance(MapNode node, Model model) {
		this(node, model, PI);
	}

	@Override
	public List<Mesh> buildMeshes() {
		return new ModelInstance(model, new InstanceParameters(getBase(), direction)).getMeshes();
	}

	@Override
	public String toString() {
		return model.getClass().getSimpleName() + "(" + node + ")";
	}

}
