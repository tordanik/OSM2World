package org.osm2world.core.math;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TriangleXYZWithNormals extends TriangleXYZ {

	public final VectorXYZ n1, n2, n3;

	public TriangleXYZWithNormals(TriangleXYZ t,
			VectorXYZ n1, VectorXYZ n2, VectorXYZ n3) {
		this(t.v1, t.v2, t.v3, n1, n2, n3);
	}

	public TriangleXYZWithNormals(VectorXYZ v1, VectorXYZ v2, VectorXYZ v3,
			VectorXYZ n1, VectorXYZ n2, VectorXYZ n3) {
		super(v1, v2, v3);
		this.n1 = n1;
		this.n2 = n2;
		this.n3 = n3;
	}

	public List<VectorXYZ> getNormals() {
		return ImmutableList.of(n1, n2, n3);
	}

}
