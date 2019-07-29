package org.osm2world.core.target.jogl;

import org.osm2world.core.target.common.material.Material;

// Package private because this class is an abomination
class JOGLMaterial extends Material {
	private Cubemap reflectionMap;
	private boolean enabled;
	private boolean useGroundRefl;

	public JOGLMaterial(Material base) {
		super(base);
	}

	public void setRefl(Cubemap refl) {
		reflectionMap = refl;
	}

	public Cubemap getReflectionMap() {
		return reflectionMap;
	}

	public void enable() {
		enabled = true;
	}

	public void disable() {
		enabled = false;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setUseGround(boolean g) {
		useGroundRefl = g;
	}

	public boolean useGround() {
		return useGroundRefl;
	}

}

