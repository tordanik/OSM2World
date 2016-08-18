package org.osm2world.core.target.jogl;

import org.osm2world.core.target.common.material.Material;

// Class has to be public because WorldObjectNormalsDebugView needs to use it
public class JOGLMaterial {
	private Material baseMaterial;
	private Cubemap reflectionMap;
	private boolean enabled;
	//private Texture reflectionTexture;

	public JOGLMaterial(Material base) {
		baseMaterial = base;
	}

	public Material getBaseMaterial() {
		return baseMaterial;
	}

	public boolean hasReflection() {
		return reflectionMap != null;
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

//	public void setReflTex(Texture refel) {
//		reflectionTexture = refel;
//	}

//	public Texture getReflTex() {
//		return reflectionTexture;
//	}
}

