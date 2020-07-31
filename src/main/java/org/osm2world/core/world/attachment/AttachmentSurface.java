package org.osm2world.core.world.attachment;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bboxUnion;
import static org.osm2world.core.math.GeometryUtil.closeLoop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.FaceXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.FaceTarget;
import org.osm2world.core.target.common.material.Material;

/**
 * TODO describe the concept
 */
public class AttachmentSurface implements BoundedObject {

	/** the surface categories this surface belongs to, e.g. "pole" or "wall" */
	private final Collection<String> types;

	/** the faces (flat polygons) that define the surface. All faces are counterclockwise */
	private final Collection<FaceXYZ> faces;

	/**
	 * the "bottom" elevation of the surface, e.g. the ground elevation for an outdoor feature,
	 * the floor elevation for an indoor wall, or the elevation of a bridge for a feature on the bridge.
	 * This allows objects to attach at a sensible height.
	 */
	private final double baseEle;

	private final Collection<AttachmentConnector> attachedConnectors = new ArrayList<>();

	public AttachmentSurface(Collection<String> types, Collection<FaceXYZ> faces, double baseEle) {
		if (types.isEmpty() || faces.isEmpty()) throw new IllegalArgumentException();
		this.types = types;
		this.faces = faces;
		this.baseEle = baseEle;
	}

	public AttachmentSurface(Collection<String> types, Collection<FaceXYZ> faces) {
		this(types, faces, faces.stream().flatMap(f -> f.getVertices().stream()).mapToDouble(v -> v.y).min().orElseGet(() -> 0));
	}

	public Collection<String> getTypes() {
		return types;
	}

	public Collection<FaceXYZ> getFaces() {
		return faces;
	}

	public double getBaseEle() {
		return baseEle;
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

	public static class Builder extends FaceTarget {

		private final Collection<String> types;
		private final Collection<FaceXYZ> faces = new ArrayList<>();

		public Builder(String... types) {
			this.types = asList(types);
		}

		public AttachmentSurface build() {
			return new AttachmentSurface(types, faces);
		}

		@Override
		public void drawFace(Material material, List<VectorXYZ> vs, List<VectorXYZ> normals,
				List<List<VectorXZ>> texCoordLists) {
			vs = closeLoop(vs);
			faces.add(new FaceXYZ(vs));
		}

		@Override
		public boolean reconstructFaces() {
			return false;
		}

	}

}
