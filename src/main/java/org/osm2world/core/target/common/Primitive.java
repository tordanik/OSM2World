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

	public final List<VectorXYZ> vertices;

	public final List<VectorXYZ> normals;

	public final List<List<VectorXZ>> texCoordLists;

	public Primitive(Type type, List<VectorXYZ> vertices,
			List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists) {
		this.type = type;
		this.vertices = vertices;
		this.normals = normals;
		this.texCoordLists = texCoordLists;
	}

	@Override
	public String toString() {
		return "{" + type + ", " + vertices + "}";
	}

}
