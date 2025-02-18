package org.osm2world.world.attachment;

import static java.util.Collections.singletonList;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bboxUnion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.scene.mesh.Geometry;
import org.osm2world.scene.mesh.Mesh;

/**
 * a surface (consisting of one or more {@link FaceXYZ}s) that {@link AttachmentConnector} can attach to
 */
public class AttachmentSurface implements BoundedObject {

	/** the surface categories this surface belongs to, e.g. "pole" or "wall" */
	private final Collection<String> types;

	/** the faces (flat polygons) that define the surface. All faces are counterclockwise */
	private final Collection<? extends FlatSimplePolygonShapeXYZ> faces;

	/**
	 * the "bottom" elevation of the surface, e.g. the ground elevation for an outdoor feature,
	 * the floor elevation for an indoor wall, or the elevation of a bridge for a feature on the bridge.
	 * This allows objects to attach at a sensible height.
	 * The value will often just be constant for the entire surface,
	 * but it can be dependent on horizontal location (e.g. for a wall running up a hill).
	 */
	private final Function<VectorXZ, Double> baseEleFunction;

	private final Collection<AttachmentConnector> attachedConnectors = new ArrayList<>();

	public AttachmentSurface(Collection<String> types, Collection<? extends FlatSimplePolygonShapeXYZ> faces,
			Function<VectorXZ, Double> baseEleFunction) {
		if (types.isEmpty() || faces.isEmpty()) throw new IllegalArgumentException();
		this.types = types;
		this.faces = faces;
		this.baseEleFunction = baseEleFunction;
	}

	public AttachmentSurface(Collection<String> types, Collection<? extends FlatSimplePolygonShapeXYZ> faces) {
		this(types, faces, pos ->
				faces.stream().flatMap(f -> f.verticesNoDup().stream()).mapToDouble(v -> v.y).min().orElseGet(() -> 0));
	}

	public Collection<String> getTypes() {
		return types;
	}

	public Collection<? extends FlatSimplePolygonShapeXYZ> getFaces() {
		return faces;
	}

	public double getBaseEleAt(VectorXZ position) {
		return baseEleFunction.apply(position);
	}

	public Collection<AttachmentConnector> getAttachedConnectors() {
		return attachedConnectors;
	}

	public void addAttachedConnector(AttachmentConnector connector) {
		this.attachedConnectors.add(connector);
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return bboxUnion(faces);
	}

	public double distanceTo(VectorXYZ v) {
		return getFaces().stream().mapToDouble(f -> f.distanceTo(v)).min().getAsDouble();
	}

	@Override
	public String toString() {
		return "{" + types + ", " + faces + "}";
	}

	public static AttachmentSurface fromGeometry(String type, Geometry... geometries) {
		return fromGeometry(singletonList(type), geometries);
	}

	public static AttachmentSurface fromGeometry(Collection<String> types, Geometry... geometries) {
		List<FaceXYZ> faces = new ArrayList<>();
		for (Geometry g : geometries) {
			g.asTriangles().triangles.forEach(t -> faces.add(new FaceXYZ(t.vertices())));
		}
		return new AttachmentSurface(types, faces);
	}

	public static AttachmentSurface fromMeshes(String type, Iterable<Mesh> meshes) {
		return fromMeshes(singletonList(type), meshes);
	}

	public static AttachmentSurface fromMeshes(Collection<String> types, Iterable<Mesh> meshes) {
		return fromMeshes(types, meshes, null);
	}

	public static AttachmentSurface fromMeshes(Collection<String> types, Iterable<Mesh> meshes,
			@Nullable Function<VectorXZ, Double> baseEleFunction) {

		List<FlatSimplePolygonShapeXYZ> faces = new ArrayList<>();
		for (Mesh mesh : meshes) {
			faces.addAll(mesh.geometry.asTriangles().triangles);
		}

		if (baseEleFunction != null) {
			return new AttachmentSurface(types, faces, baseEleFunction);
		} else {
			return new AttachmentSurface(types, faces);
		}

	}

}
