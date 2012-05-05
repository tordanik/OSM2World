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
		
	public final List<VectorXYZ> normals; //TODO: why have indexed vertices, but direct refs to normals?
	
	public final List<List<VectorXZ>> texCoordLists;
	
	public Primitive(Type type, int[] indices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		this.type = type;
		this.indices = indices;
		this.normals = normals;
		this.texCoordLists = texCoordLists;
	}
	
	@Override
	public String toString() {
		return "{" + type + ", " + indices + "}";
	}
	
}
