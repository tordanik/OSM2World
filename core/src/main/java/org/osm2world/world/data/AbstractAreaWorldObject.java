package org.osm2world.world.data;

import static org.osm2world.math.algorithms.GeometryUtil.interpolateOnTriangle;
import static org.osm2world.world.connectors.Connector.AttachmentProperties;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.world.connectors.AttachmentUtil;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.connectors.ConnectorGroup;
import org.osm2world.world.connectors.GroundState;


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

	private ConnectorGroup connectors;

	/** cached result of {@link #getGroundFootprint()} */
	private @Nullable Collection<PolygonShapeXZ> groundFootprint = null;

	protected AbstractAreaWorldObject(MapArea area) {
		this.area = area;
	}

	@Override
	public GroundState getGroundState() {
		if (connectors != null && connectors.stream().allMatch(Connector::isAttached)) {
			return GroundState.ATTACHED;
		} else {
			return GroundState.ON;
		}
	}

	@Override
	public ConnectorGroup getConnectors() {

		if (connectors == null) {

			connectors = new ConnectorGroup();

			List<String> attachmentTypes = AttachmentUtil.getCompatibleSurfaceTypes(area);
			AttachmentProperties ap = attachmentTypes.isEmpty() ? null
					: new AttachmentProperties(attachmentTypes, 0, false);

			connectors.addConnectorsForTriangulation(
					getTriangulationXZ(), null, getGroundState(), ap, this);

		}

		return connectors;

	}

	@Override
	public @Nonnull PolygonWithHolesXZ getOutlinePolygonXZ() {
		// cache the otherwise unchanged result
		if (outlinePolygonXZ == null) {
			this.outlinePolygonXZ = (PolygonWithHolesXZ) AreaWorldObject.super.getOutlinePolygonXZ();
		}
		return outlinePolygonXZ;
	}

	public PolygonWithHolesXYZ getOutlinePolygon() {
		return connectors.getPosXYZ(outlinePolygonXZ);
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
		return TriangulationUtil.triangulationXZtoXYZ(getTriangulationXZ(), connectors::getPosXYZ);
	}

	/**
	 * returns elevation at any point within the triangulation of this area
	 */
	public double getEleAt(VectorXZ pos) {

		if (getConnectors().stream().allMatch(it -> it.getPosXYZ().y == 0.0)) {
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
