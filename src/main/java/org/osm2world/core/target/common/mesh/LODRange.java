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

}
