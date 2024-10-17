package org.osm2world.core.target.gltf;

import java.util.Comparator;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.target.common.MeshStore;
import org.osm2world.core.target.common.MeshStore.MeshWithMetadata;
import org.osm2world.core.target.gltf.GltfTarget.MinMax;

final class MeshHeightAndSizeComparator implements Comparator<MeshStore.MeshWithMetadata> {
	@Override
	public int compare(MeshWithMetadata m1, MeshWithMetadata m2) {
		MinMax ymm1 = new MinMax();
		MinMax ymm2 = new MinMax();

		m1.mesh().geometry.asTriangles().vertices().forEach(v -> {
			ymm1.max = Double.max(ymm1.max, v.y);
		});
		
		m2.mesh().geometry.asTriangles().vertices().forEach(v -> {
			ymm2.max = Double.max(ymm2.max, v.y);
		});

		int h1 = (int)Math.round(ymm1.max * 2) / 2;
		int h2 = (int)Math.round(ymm2.max * 2) / 2;

		if (h2 != h1) {
			return h2 - h1;
		}

		AxisAlignedRectangleXZ bbx1 = AxisAlignedRectangleXZ.bbox(m1.mesh().geometry.asTriangles().vertices());
		AxisAlignedRectangleXZ bbx2 = AxisAlignedRectangleXZ.bbox(m2.mesh().geometry.asTriangles().vertices());

		int d1 = (int)Math.round(bbx1.getDiameter() * 4) / 4;
		int d2 = (int)Math.round(bbx2.getDiameter() * 4) / 4;

		return d2 - d1;
	}
}