package org.osm2world.core.target.common;

import org.osm2world.core.math.VectorXYZ;

public class Primitive {

	public static enum Type {
		CONVEX_POLYGON,
		TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN
	}
	
	public final Type type;
	
	public final int[] indices;
		
	public final VectorXYZ[] normals; //TODO: why have indexed vertices, but direct refs to normals?
	
	public Primitive(Type type, int[] indices, VectorXYZ[] normals) {
		this.type = type;
		this.indices = indices;
		this.normals = normals;
	}
	
	@Override
	public String toString() {
		return "{" + type + ", " + indices + "}";
	}
	
}
