package org.osm2world.core.target.jogl;

import org.osm2world.core.target.common.material.Material;

// Package private because this class is an abomination
class JOGLMaterial extends Material {
	private Cubemap reflectionMap;

	public JOGLMaterial(Material base) {
		super(base);
	}

	public void setRefl(Cubemap refl) {
		reflectionMap = refl;
	}

	public Cubemap getReflectionMap() {
		return reflectionMap;
	}

}
