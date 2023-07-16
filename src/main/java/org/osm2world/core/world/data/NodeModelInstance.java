package org.osm2world.core.world.data;

import static java.lang.Math.PI;

import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;

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
		return model.buildMeshes(new InstanceParameters(getBase(), direction, null, null, null));
	}

	@Override
	public String toString() {
		return model.getClass().getSimpleName() + "(" + node + ")";
	}

}
