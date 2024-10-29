package org.osm2world.core.target.tileset;

import java.util.Comparator;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.target.common.MeshStore;
import org.osm2world.core.target.common.MeshStore.MeshWithMetadata;

final class MeshHeightAndSizeComparator implements Comparator<MeshStore.MeshWithMetadata> {

	private final Comparator<MeshStore.MeshWithMetadata> IMPLEMENTATION = Comparator.comparingInt((MeshStore.MeshWithMetadata m) -> {
		// TODO: does not work with terrain enabled, a small object in a high location will have large y values
		double max = m.mesh().geometry.asTriangles().vertices().stream().mapToDouble(v -> v.y).max().orElse(0);
		return (int) Math.round(max * 2) / 2;
	}).thenComparingInt(m -> {
		AxisAlignedRectangleXZ bbox = AxisAlignedRectangleXZ.bbox(m.mesh().geometry.asTriangles().vertices());
		return (int) Math.round(bbox.getDiameter() * 4) / 4;
	}).reversed();

	@Override
	public int compare(MeshWithMetadata m1, MeshWithMetadata m2) {
		return IMPLEMENTATION.compare(m1, m2);
	}
}