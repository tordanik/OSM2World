package org.osm2world.output.tileset;

import static org.osm2world.scene.mesh.MeshStore.MeshWithMetadata;

import java.util.Comparator;

import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

final class MeshHeightAndSizeComparator implements Comparator<MeshWithMetadata> {

	private final Comparator<MeshWithMetadata> IMPLEMENTATION = Comparator.comparingInt((MeshWithMetadata m) -> {
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