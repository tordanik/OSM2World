package org.osm2world.scene.mesh;

import java.util.Arrays;

import javax.annotation.Nullable;

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

	/** returns the intersection of the ranges, or null if the intersection is empty */
	public static @Nullable LODRange intersection(LODRange... ranges) {
		if (ranges.length == 0) throw new IllegalArgumentException();
		var min = Arrays.stream(ranges).map(it -> it.min).max(LevelOfDetail::compareTo).get();
		var max = Arrays.stream(ranges).map(it -> it.max).min(LevelOfDetail::compareTo).get();
		if (min.ordinal() > max.ordinal()) {
			return null;
		} else {
			return new LODRange(min, max);
		}
	}

}
