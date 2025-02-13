package org.osm2world.output.common.model;

import java.util.List;

import org.osm2world.output.common.mesh.Mesh;

/**
 * A model referencing an external resource, such as a URI
 */
public record ExternalResourceModel(String resourceIdentifier) implements Model {

	@Override
	public List<Mesh> buildMeshes(InstanceParameters params) {
		// TODO implement - right now this only works for the FrontendPbfTarget, others will just show nothing
		return List.of();
	}

}
