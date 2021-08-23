package org.osm2world.core.target.common.mesh;

/**
 * level of detail, from lowest (0) to highest (4).
 * Describes a point on the spectrum of trade-offs between performance and quality.
 * OSM2World's levels do not strictly conform to any particular standard.
 */
public enum LevelOfDetail implements Comparable<LevelOfDetail> {
	LOD0, LOD1, LOD2, LOD3, LOD4
}
