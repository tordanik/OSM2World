package org.osm2world.target.common.mesh;

import javax.annotation.Nullable;

/**
 * level of detail, from lowest (0) to highest (4).
 * Describes a point on the spectrum of trade-offs between performance and quality.
 * OSM2World's levels do not strictly conform to any particular standard.
 */
public enum LevelOfDetail implements Comparable<LevelOfDetail> {

	LOD0, LOD1, LOD2, LOD3, LOD4;

	public static @Nullable LevelOfDetail fromInt(@Nullable Integer lod) {
		if (lod != null) {
			return switch (lod) {
				case 0, 1, 2, 3, 4 -> values()[lod];
				default -> null;
			};
		}
		return null;
	}

}
