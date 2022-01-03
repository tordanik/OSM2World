package org.osm2world.core.target.common.model;

/** one instance of a {@link Model} */
public class ModelInstance {

	public final Model model;
	public final InstanceParameters params;

	public ModelInstance(Model model, InstanceParameters params) {
		this.model = model;
		this.params = params;
	}

}
