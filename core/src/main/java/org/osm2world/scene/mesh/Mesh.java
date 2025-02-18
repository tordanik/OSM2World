package org.osm2world.scene.mesh;

import org.osm2world.scene.material.Material;

public class Mesh {

	public final Geometry geometry;
	public final Material material;
	public final LODRange lodRange;

	public Mesh(Geometry geometry, Material material) {
		this(geometry, material, LevelOfDetail.LOD0, LevelOfDetail.LOD4);
	}

	public Mesh(Geometry geometry, Material material, LevelOfDetail lod) {
		this(geometry, material, lod, lod);
	}

	public Mesh(Geometry geometry, Material material, LevelOfDetail lodRangeMin, LevelOfDetail lodRangeMax) {
		this(geometry, material, new LODRange(lodRangeMin, lodRangeMax));
	}

	public Mesh(Geometry geometry, Material material, LODRange lodRange) {

		this.geometry = geometry;
		this.material = material;
		this.lodRange = lodRange;

		if (geometry instanceof TriangleGeometry tg && tg.texCoords.size() != material.getNumTextureLayers()) {
			throw new IllegalArgumentException("incorrect number of texCoord layers");
		}

	}

	@Override
	public String toString() {
		return "Mesh(" + lodRange + ", " + geometry.getClass().getSimpleName() + ")";
	}

}
