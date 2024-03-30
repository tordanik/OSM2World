package org.osm2world.core.math;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class PolygonUtils {

    /**
     * Calculates the signed area of a polygon defined by a list of vertices.
     *
     * @param vertexLoop The list of vertices in the polygon.
     * @return The signed area of the polygon.
     */
    public static double calculateSignedArea(List<VectorXZ> vertexLoop) {
        double sum = 0.0;
        int n = vertexLoop.size();
        for (int i = 0; i < n - 1; i++) {
            VectorXZ current = vertexLoop.get(i);
            VectorXZ next = vertexLoop.get((i + 1) % n);
            sum += current.x * next.z - next.x * current.z;
        }
        return sum / 2.0;
    }

    /**
     * Determines if the polygon defined by the given vertex loop is clockwise.
     *
     * @param vertexLoop The list of vertices in the polygon.
     * @return true if the polygon is clockwise, false otherwise.
     */
    public static boolean isClockwise(List<VectorXZ> vertexLoop) {
        return calculateSignedArea(vertexLoop) < 0;
    }

    /**
     * Calculates the centroid of a polygon defined by a list of vertices.
     *
     * @param vertexLoop The list of vertices in the polygon.
     * @return The centroid of the polygon.
     */
    public static VectorXZ calculateCentroid(List<VectorXZ> vertexLoop) {
        double signedArea = calculateSignedArea(vertexLoop);
        double x = 0.0;
        double z = 0.0;
        int n = vertexLoop.size();

        for (int i = 0; i < n - 1; i++) {
            VectorXZ current = vertexLoop.get(i);
            VectorXZ next = vertexLoop.get((i + 1) % n);
            double cross = (current.x * next.z - next.x * current.z);
            x += (current.x + next.x) * cross;
            z += (current.z + next.z) * cross;
        }

        if (signedArea == 0) return new VectorXZ(0, 0);

        return new VectorXZ(x / (6 * signedArea), z / (6 * signedArea));
    }

    /**
     * Calculates the convex hull of a set of points using the Graham scan algorithm.
     *
     * @param points The list of points.
     * @return The convex hull as a list of points.
     */
    public static List<VectorXZ> calculateConvexHull(List<VectorXZ> points) {
        // Implement the Graham scan algorithm or another convex hull algorithm
        // Placeholder for actual implementation
        return points; // Replace with actual convex hull calculation
    }

    /**
     * Checks if a point is inside a polygon defined by a list of vertices.
     *
     * @param point      The point to check.
     * @param vertexLoop The list of vertices in the polygon.
     * @return true if the point is inside the polygon, false otherwise.
     */
    public static boolean isPointInsidePolygon(VectorXZ point, List<VectorXZ> vertexLoop) {
        boolean result = false;
        int n = vertexLoop.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((vertexLoop.get(i).z > point.z) != (vertexLoop.get(j).z > point.z) &&
                    (point.x < (vertexLoop.get(j).x - vertexLoop.get(i).x) * (point.z - vertexLoop.get(i).z) /
                            (vertexLoop.get(j).z - vertexLoop.get(i).z) + vertexLoop.get(i).x)) {
                result = !result;
            }
        }
        return result;
    }

    public static boolean isSelfIntersecting(List<VectorXZ> polygonVertexLoop) {

        final class Event {
            boolean start;
            LineSegmentXZ line;

            Event(LineSegmentXZ l, boolean s) {
                this.line = l;
                this.start = s;
            }
        }


        // we have n-1 vertices as the first and last vertex are the same
        final int segments = polygonVertexLoop.size() - 1;

        // generate an array of input events associated with their line segments
        Event[] events = new Event[segments * 2];
        for (int i = 0; i < segments; i++) {
            VectorXZ v1 = polygonVertexLoop.get(i);
            VectorXZ v2 = polygonVertexLoop.get(i + 1);

            // Create a line where the first vertex is left (or above) the second vertex
            LineSegmentXZ line;
            if ((v1.x < v2.x) || ((v1.x == v2.x) && (v1.z < v2.z))) {
                line = new LineSegmentXZ(v1, v2);
            } else {
                line = new LineSegmentXZ(v2, v1);
            }

            events[2 * i] = new Event(line, true);
            events[2 * i + 1] = new Event(line, false);
        }

        // sort the input events according to the x-coordinate, then z-coordinate
        Arrays.sort(events, (Event e1, Event e2) -> {

            VectorXZ v1 = e1.start ? e1.line.p1 : e1.line.p2;
            VectorXZ v2 = e2.start ? e2.line.p1 : e2.line.p2;

            if (v1.x < v2.x) return -1;
            else if (v1.x == v2.x) {
                if (v1.z < v2.z) return -1;
                else if (v1.z == v2.z) return 0;
            }
            return 1;
        });

        // A TreeSet, used for the sweepline algorithm
        TreeSet<LineSegmentXZ> sweepLine = new TreeSet<LineSegmentXZ>((LineSegmentXZ l1, LineSegmentXZ l2) -> {

            VectorXZ v1 = l1.p1;
            VectorXZ v2 = l2.p1;

            if (v1.z < v2.z) return -1;
            else if (v1.z == v2.z) {
                if (v1.x < v2.x) return -1;
                else if (v1.x == v2.x) {
                    if (l1.p2.z < l2.p2.z) return -1;
                    else if (l1.p2.z == l2.p2.z) {
                        if (l1.p2.x < l2.p2.x) return -1;
                        else if (l1.p2.x == l2.p2.x) return 0;
                    }
                }
            }
            return 1;
        });

        // start the algorithm by visiting every event
        for (Event event : events) {
            LineSegmentXZ line = event.line;
            if (event.start) {
                handleStartEvent(sweepLine, line);
            } else {
                handleEndEvent(sweepLine, line);
            }
        }
        return false;
    }

    private static boolean handleStartEvent(TreeSet<LineSegmentXZ> sweepLine, LineSegmentXZ line) {
        LineSegmentXZ lower = sweepLine.lower(line);
        LineSegmentXZ higher = sweepLine.higher(line);

        sweepLine.add(line);

        if (lower != null && lower.intersects(line.p1, line.p2)) {
            return true;
        }

        if (higher != null && higher.intersects(line.p1, line.p2)) {
            return true;
        }
        return false;
    }

    private static boolean handleEndEvent(TreeSet<LineSegmentXZ> sweepLine, LineSegmentXZ line) {
        LineSegmentXZ lower = sweepLine.lower(line);
        LineSegmentXZ higher = sweepLine.higher(line);

        sweepLine.remove(line);

        if ((lower == null) || (higher == null)) {
            return false;
        }

        if (lower.intersects(higher.p1, higher.p2)) {
            return true;
        }
        return false;
    }
}
