package org.osm2world.core.target.common.mesh;

import org.osm2world.core.target.common.material.Material;

public class Mesh {

	public final Geometry geometry;
	public final Material material;
	public final LevelOfDetail lodRangeMin;
	public final LevelOfDetail lodRangeMax;

	public Mesh(Geometry geometry, Material material) {
		this(geometry, material, LevelOfDetail.LOD0, LevelOfDetail.LOD4);
	}

	public Mesh(Geometry geometry, Material material, LevelOfDetail lod) {
		this(geometry, material, lod, lod);
	}

	public Mesh(Geometry geometry, Material material, LevelOfDetail lodRangeMin, LevelOfDetail lodRangeMax) {

		this.geometry = geometry;
		this.material = material;
		this.lodRangeMin = lodRangeMin;
		this.lodRangeMax = lodRangeMax;

		if (lodRangeMin.compareTo(lodRangeMax) > 0) {
			throw new IllegalArgumentException("invalid LOD range: " + lodRangeMin + "-" + lodRangeMax);
		}
		if (geometry instanceof TriangleGeometry tg && tg.texCoords.size() != material.getNumTextureLayers()) {
			throw new IllegalArgumentException("incorrect number of texCoord layers");
		}

	}

	public boolean lodRangeContains(LevelOfDetail lod) {
		return lodRangeMin.ordinal() <= lod.ordinal() && lod.ordinal() <= lodRangeMax.ordinal();
	}

	@Override
	public String toString() {
		String lodString = "LOD " + lodRangeMin.ordinal() + "-" + lodRangeMax.ordinal() + ", ";
		return "Mesh(" + lodString + material + ", " + geometry.getClass().getSimpleName() + ")";
	}

}
