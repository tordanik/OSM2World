package org.osm2world.world.connectors;

import static java.lang.Math.abs;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bboxUnion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.math.BoundedObject;
import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.scene.mesh.Geometry;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.world.connectors.Connector.AttachmentProperties;
import org.osm2world.world.data.WorldObject;

/**
 * a surface (consisting of one or more {@link FaceXYZ}s) that {@link Connector}s can attach to.
 * <p>
 * The surface's geometry can be replaced using {@link #updateGeometryFrom(AttachmentSurface)},
 * e.g. when the {@link WorldObject} it belongs to has been moved.
 * The instance itself is kept because attached {@link Connector}s hold references to it.
 */
public class AttachmentSurface implements BoundedObject {

	/** the surface categories this surface belongs to, e.g. "pole" or "wall" */
	private final Collection<String> types;

	/** the object this surface belongs to */
	private final @Nullable WorldObject worldObject;

	/** the faces (flat polygons) that define the surface. All faces are counterclockwise */
	private Collection<? extends FlatSimplePolygonShapeXYZ> faces;

	/**
	 * the "bottom" elevation of the surface, e.g. the ground elevation for an outdoor feature,
	 * the floor elevation for an indoor wall, or the elevation of a bridge for a feature on the bridge.
	 * This allows objects to attach at a sensible height.
	 * The value will often just be constant for the entire surface,
	 * but it can be dependent on horizontal location (e.g. for a wall running up a hill).
	 * If this is null, the elevation of the surface's lowest point is used.
	 */
	private @Nullable Function<VectorXZ, Double> baseEleFunction;

	/** the elevation of the surface's lowest point, used if {@link #baseEleFunction} is null */
	private double minEle;

	private final Collection<Connector> attachedConnectors = new ArrayList<>();

	/**
	 * @param baseEleFunction  function for {@link #getBaseEleAt(VectorXZ)},
	 *                         null to use the elevation of the surface's lowest point
	 */
	public AttachmentSurface(Collection<String> types, @Nullable WorldObject worldObject,
			Collection<? extends FlatSimplePolygonShapeXYZ> faces,
			@Nullable Function<VectorXZ, Double> baseEleFunction) {
		if (types.isEmpty()) throw new IllegalArgumentException();
		this.worldObject = worldObject;
		this.types = types;
		setGeometry(faces, baseEleFunction);
	}

	public AttachmentSurface(Collection<String> types, @Nullable WorldObject worldObject,
			Collection<? extends FlatSimplePolygonShapeXYZ> faces) {
		this(types, worldObject, faces, null);
	}

	private void setGeometry(Collection<? extends FlatSimplePolygonShapeXYZ> faces,
			@Nullable Function<VectorXZ, Double> baseEleFunction) {
		if (faces.isEmpty()) throw new IllegalArgumentException();
		this.faces = faces;
		this.baseEleFunction = baseEleFunction;
		this.minEle = faces.stream().flatMap(f -> f.verticesNoDup().stream()).mapToDouble(v -> v.y).min().orElse(0);
	}

	/**
	 * replaces this surface's geometry (faces and base elevation) with that of another surface.
	 * Attached connectors are kept, but not moved.
	 *
	 * @param newSurface  surface with the new geometry, must have the same types as this one
	 */
	public void updateGeometryFrom(AttachmentSurface newSurface) {
		if (!types.equals(newSurface.types)) {
			throw new IllegalArgumentException("surface types differ: " + types + " vs. " + newSurface.types);
		}
		setGeometry(newSurface.faces, newSurface.baseEleFunction);
	}

	public Collection<String> getTypes() {
		return types;
	}

	public @Nullable WorldObject getWorldObject() {
		return worldObject;
	}

	public Collection<? extends FlatSimplePolygonShapeXYZ> getFaces() {
		return faces;
	}

	public double getBaseEleAt(VectorXZ position) {
		return baseEleFunction != null ? baseEleFunction.apply(position) : minEle;
	}

	public Collection<Connector> getAttachedConnectors() {
		return attachedConnectors;
	}

	public void addAttachedConnector(Connector connector) {
		this.attachedConnectors.add(connector);
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return bboxUnion(faces);
	}

	public double distanceTo(VectorXYZ v) {
		return getFaces().stream().mapToDouble(f -> f.distanceTo(v)).min().getAsDouble();
	}

	public double distanceToXZ(Vector3D v) {
		return getFaces().stream().mapToDouble(f -> f.distanceToXZ(v)).min().getAsDouble();
	}

	public VectorXYZ closestPoint(VectorXYZ v) {
		return getFaces().stream().map(f -> f.closestPoint(v))
				.min(Comparator.comparingDouble(f -> f.distanceTo(v))).orElseThrow();
	}

	/** a position on an {@link AttachmentSurface} and the surface normal at that position */
	public record AttachmentPoint(VectorXYZ pos, VectorXYZ normal) {}

	/**
	 * finds the position on this surface where a connector with the given properties would be placed.
	 * Does not check whether the surface types are compatible.
	 *
	 * @param ap           the connector's attachment properties
	 * @param originalPos  the connector's original position
	 * @return  the attachment point, or null if the connector cannot be placed on this surface
	 */
	public @Nullable AttachmentPoint findAttachmentPoint(AttachmentProperties ap, VectorXYZ originalPos) {

		double ele = getBaseEleAt(originalPos.xz()) + ap.preferredHeight();
		VectorXYZ posAtEle = originalPos.y(ele);

		for (boolean requirePreferredHeight : List.of(true, false)) {

			Predicate<FlatSimplePolygonShapeXYZ> matchesPreferredHeight = f -> {
				if (!requirePreferredHeight) {
					return true;
				} else {
					VectorXYZ closestPoint = f.closestPoint(posAtEle);
					double height = closestPoint.y - getBaseEleAt(closestPoint.xz());
					return abs(height - ap.preferredHeight()) < 0.001;
				}
			};

			Optional<? extends FlatSimplePolygonShapeXYZ> closestFace = faces.stream()
					.filter(matchesPreferredHeight)
					.filter(f -> ap.isAcceptableNormal().test(f.getNormal()))
					.min(comparingDouble(f -> ap.changeXZ() ? f.distanceTo(posAtEle) : f.distanceToXZ(posAtEle)));

			if (closestFace.isEmpty()) continue; // try again without enforcing the preferred height

			VectorXYZ closestPoint;

			if (!ap.changeXZ() && closestFace.get().getNormal().y >= 0.001) {
				// no XZ movement is desired, obtain the face point directly above/below the connector
				VectorXYZ pointInFacePlane = posAtEle.y(closestFace.get().getYAt(posAtEle.xz()));
				closestPoint = closestFace.get().closestPoint(pointInFacePlane);
			} else {
				closestPoint = closestFace.get().closestPoint(posAtEle);
			}

			if (closestPoint.xz().distanceTo(originalPos.xz()) > ap.maxDistanceXZ() + 0.001) {
				continue;
			}

			return new AttachmentPoint(closestPoint, closestFace.get().getNormal());

		}

		return null;

	}

	@Override
	public String toString() {
		return "{" + types + ", " + faces + "}";
	}

	public static AttachmentSurface fromGeometry(String type, WorldObject worldObject, Geometry... geometries) {
		return fromGeometry(singletonList(type), worldObject, geometries);
	}

	public static AttachmentSurface fromGeometry(Collection<String> types, WorldObject worldObject, Geometry... geometries) {
		List<FaceXYZ> faces = new ArrayList<>();
		for (Geometry g : geometries) {
			g.asTriangles().triangles.forEach(t -> faces.add(new FaceXYZ(t.vertices())));
		}
		return new AttachmentSurface(types, worldObject, faces);
	}

	public static AttachmentSurface fromMeshes(String type, WorldObject worldObject, Iterable<Mesh> meshes) {
		return fromMeshes(singletonList(type), worldObject, meshes);
	}

	public static AttachmentSurface fromMeshes(Collection<String> types, WorldObject worldObject, Iterable<Mesh> meshes) {
		return fromMeshes(types, worldObject, meshes, null);
	}

	public static AttachmentSurface fromMeshes(Collection<String> types, WorldObject worldObject, Iterable<Mesh> meshes,
			@Nullable Function<VectorXZ, Double> baseEleFunction) {

		List<FlatSimplePolygonShapeXYZ> faces = new ArrayList<>();
		for (Mesh mesh : meshes) {
			faces.addAll(mesh.geometry.asTriangles().triangles);
		}

		return new AttachmentSurface(types, worldObject, faces, baseEleFunction);

	}

}
