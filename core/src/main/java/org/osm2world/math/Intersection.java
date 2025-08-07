package org.osm2world.math;

import org.osm2world.math.shapes.LineSegmentXZ;

/**
 * the result of an intersection calculation,
 * providing both the point at which shapes intersect and one of the segments involved in the intersection
 */
public record Intersection(VectorXZ point, LineSegmentXZ segment) { }