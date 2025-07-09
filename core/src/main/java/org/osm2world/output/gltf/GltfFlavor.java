package org.osm2world.output.gltf;

public enum GltfFlavor {

	GLTF, GLB;

	public String extension() {
		return "." + name().toLowerCase();
	}

}
