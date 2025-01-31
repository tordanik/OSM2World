package org.osm2world.core.world.data;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.osm2world.core.math.algorithms.GeometryUtil.interpolateOnTriangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.*;
import org.osm2world.core.util.ValueParseUtil;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;


/**
 * implementation of {@link AreaWorldObject} that offers some basic features:
 * <ul><li> providing the object outline based on the {@link MapArea}
 * </li><li> providing bounding geometry for intersection tests
 * </li><li> calculating a triangulation of the surface for rendering
 * </ul>
 */
public abstract class AbstractAreaWorldObject implements AreaWorldObject, BoundedObject {

	protected final MapArea area;

	private PolygonWithHolesXZ outlinePolygonXZ;

	private final @Nullable AttachmentConnector attachmentConnector;

	private EleConnectorGroup connectors;

	/** cached result of {@link #getGroundFootprint()} */
	private @Nullable Collection<PolygonShapeXZ> groundFootprint = null;

	protected AbstractAreaWorldObject(MapArea area) {

		this.area = area;

		List<String> types = new ArrayList<>();

		if (area.getTags().contains("location", "roof") || area.getTags().contains("parking", "rooftop")) {
			if (area.getTags().containsKey("level")) {
				List<Integer> levels = ValueParseUtil.parseLevels(area.getTags().getValue("level"));
				if (levels != null) {
					types.add("roof" + levels.get(0));
				}
			}
			types.add("roof");
		} else if (area.getTags().containsKey("level")) {
			List<Integer> levels = ValueParseUtil.parseLevels(area.getTags().getValue("level"));
			if (levels != null) {
				types.add("floor" + levels.get(0));
			}
		}


		if (!types.isEmpty()) {
			VectorXYZ pos;
			if (area.getPolygon().contains(area.getOuterPolygon().getCentroid())) {
				pos = area.getOuterPolygon().getCentroid().xyz(0);
			} else {
				pos = area.getOuterPolygon().getClosestSegment(area.getOuterPolygon().getCentroid()).getCenter().xyz(0);
			}
			attachmentConnector = new AttachmentConnector(
					types,
					pos,
					this,
					0,
					false);
		} else {
			attachmentConnector = null;
		}

	}

	@Override
	public Iterable<AttachmentConnector> getAttachmentConnectors() {
		if (attachmentConnector == null) {
			return emptyList();
		} else {
			return singleton(attachmentConnector);
		}
	}

	/**
	 * returns the {@link AttachmentConnector} for this area if it exists and
	 * has successfully attached to an {@link AttachmentSurface}, null otherwise.
	 */
	protected @Nullable AttachmentConnector getConnectorIfAttached() {
		if (attachmentConnector != null && attachmentConnector.isAttached()) {
			return attachmentConnector;
		} else {
			return null;
		}
	}

	@Override
	public GroundState getGroundState() {
		if (attachmentConnector != null) {
			return GroundState.ATTACHED;
		} else {
			return GroundState.ON;
		}
	}

	@Override
	public EleConnectorGroup getEleConnectors() {

		if (connectors == null) {

			connectors = new EleConnectorGroup();

			connectors.addConnectorsForTriangulation(
					getTriangulationXZ(), null, getGroundState());

		}

		return connectors;

	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	@Override
	public @Nonnull PolygonWithHolesXZ getOutlinePolygonXZ() {
		// cache the otherwise unchanged result
		if (outlinePolygonXZ == null) {
			this.outlinePolygonXZ = (PolygonWithHolesXZ) AreaWorldObject.super.getOutlinePolygonXZ();
		}
		return outlinePolygonXZ;
	}

	public PolygonWithHolesXYZ getOutlinePolygon() {
		if (getConnectorIfAttached() != null) {
			return outlinePolygonXZ.xyz(attachmentConnector.getAttachedPos().getY());
		} else {
			return connectors.getPosXYZ(outlinePolygonXZ);
		}
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return area.getOuterPolygon().boundingBox();
	}

	@Override
	public final MapArea getPrimaryMapElement() {
		return area;
	}

	/**
	 * decompose this area into counterclockwise triangles.
	 */
	protected List<TriangleXZ> getTriangulationXZ() {
		return TriangulationUtil.triangulate(getGroundFootprint(), List.of());
	}

	/**
	 * decompose this area into counterclockwise 3d triangles.
	 * Only available after elevation calculation.
	 */
	protected List<TriangleXYZ> getTriangulation() {

		Function<VectorXZ, VectorXYZ> xzToXYZ = getConnectorIfAttached() != null
				? v -> v.xyz(getConnectorIfAttached().getAttachedPos().getY())
				: connectors::getPosXYZ;

		return TriangulationUtil.triangulationXZtoXYZ(getTriangulationXZ(), xzToXYZ);

	}

	/**
	 * returns elevation at any point within the triangulation of this area
	 */
	public double getEleAt(VectorXZ pos) {

		if (getConnectorIfAttached() != null) {
			return getConnectorIfAttached().getAttachedPos().getY();
		} else if (getEleConnectors().eleConnectors.stream().allMatch(it -> it.getPosXYZ().y == 0.0)) {
			// fast case for disabled elevation
			return 0.0;
		}

		var containingTriangle = getTriangulation().stream().filter(t -> t.xz().contains(pos)).findFirst();

		if (containingTriangle.isPresent()) {
			TriangleXYZ t = containingTriangle.get();
			return interpolateOnTriangle(pos, t.xz(), t.v1.y, t.v2.y, t.v3.y);
		} else {
			throw new IllegalArgumentException(pos + " is not within the triangulation of " + this);
		}

	}

	@Override
	public Collection<PolygonShapeXZ> getGroundFootprint() {
		// cache the otherwise unchanged result
		if (groundFootprint == null) {
			groundFootprint = AreaWorldObject.super.getGroundFootprint();
		}
		return groundFootprint;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + area + ")";
	}

}
