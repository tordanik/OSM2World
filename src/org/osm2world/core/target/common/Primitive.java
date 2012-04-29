package org.osm2world.core.target.common;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

public class Primitive {

	public static enum Type {
		CONVEX_POLYGON,
		TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN
	}
	
	public final Type type;
	
	public final int[] indices;
		
	public final VectorXYZ[] normals; //TODO: why have indexed vertices, but direct refs to normals?
	
	public final List<List<VectorXZ>> textureCoordLists;
	
	public Primitive(Type type, int[] indices, VectorXYZ[] normals,
			List<List<VectorXZ>> textureCoordLists) {
		this.type = type;
		this.indices = indices;
		this.normals = normals;
		this.textureCoordLists = textureCoordLists;
	}
	
	@Override
	public String toString() {
		return "{" + type + ", " + indices + "}";
	}
	
}
