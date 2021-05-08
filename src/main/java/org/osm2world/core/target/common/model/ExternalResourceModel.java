package org.osm2world.core.target.common.model;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;

/**
 * A model referencing an external resource, such as an URI
 */
public class ExternalResourceModel implements Model {

	private final String resourceIdentifier;

	public ExternalResourceModel(String resourceIdentifier) {
		this.resourceIdentifier = resourceIdentifier;
	}

	public String getResourceIdentifier() {
		return resourceIdentifier;
	}

	@Override
	public void render(Target target, VectorXYZ position, double direction,
			Double height, Double width, Double length) {
		// TODO implement - right now this only works for the FrontendPbfTarget, others will just show nothing
	}

}
