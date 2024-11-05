package org.osm2world.core.target.common.mesh;

/** a range between two {@link LevelOfDetail} values (inclusive) */
public record LODRange(LevelOfDetail min, LevelOfDetail max) {

	public LODRange {
		if (min.ordinal() > max.ordinal()) {
			throw new IllegalArgumentException(min + " is larger than " + max);
		}
	}

	public LODRange(LevelOfDetail lod) {
		this(lod, lod);
	}

	@Override
	public String toString() {
		return "LOD " + min.ordinal() + "-" + max.ordinal();
	}

	public boolean contains(LevelOfDetail lod) {
		return min.ordinal() <= lod.ordinal() && lod.ordinal() <= max.ordinal();
	}

}
