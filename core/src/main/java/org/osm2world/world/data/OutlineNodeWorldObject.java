package org.osm2world.world.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.*;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.attachment.AttachmentUtil;

/**
 * superclass for {@link NodeWorldObject}s that do have an outline
 * and are not just treated as an infinitely small point.
 *
 * @see NoOutlineNodeWorldObject
 */
public abstract class OutlineNodeWorldObject implements NodeWorldObject, BoundedObject {

	protected final MapNode node;

	private EleConnectorGroup connectors = null;
	protected List<AttachmentConnector> attachmentConnectors;

	protected OutlineNodeWorldObject(MapNode node) {
		this.node = node;
	}

	@Override
	public abstract SimplePolygonXZ getOutlinePolygonXZ();

	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}

	@Override
	public EleConnectorGroup getEleConnectors() {

		if (connectors == null) {

			SimplePolygonXZ outlinePolygonXZ = getOutlinePolygonXZ();

			if (outlinePolygonXZ == null) {

				connectors = EleConnectorGroup.EMPTY;

			} else {

				connectors = new EleConnectorGroup();
				connectors.addConnectorsFor(outlinePolygonXZ.getVertices(),
						node, getGroundState());

			}

		}

		return connectors;

	}

	@Override
	public Iterable<AttachmentConnector> getAttachmentConnectors() {

		if (attachmentConnectors == null) {

			List<String> attachmentTypes = getAttachmentTypes();

			if (attachmentTypes.isEmpty()) {
				this.attachmentConnectors = List.of();
			} else {
				this.attachmentConnectors = List.of(new AttachmentConnector(attachmentTypes,
						node.getPos().xyz(0), this, 0, false));
			}

		}

		return attachmentConnectors;

	}

	/**
	 * Returns the possible attachment types for this object.
	 * Can be empty if this object should not attach to anything.
	 * Subclasses can override this to implement their own logic.
	 */
	protected List<String> getAttachmentTypes() {
		return AttachmentUtil.getCompatibleSurfaceTypes(node);
	}

	/**
	 * returns the {@link AttachmentConnector} for this area if it exists and
	 * has successfully attached to an {@link AttachmentSurface}, null otherwise.
	 */
	protected @Nullable AttachmentConnector getConnectorIfAttached() {
		if (attachmentConnectors != null && !attachmentConnectors.isEmpty()
				&& attachmentConnectors.get(0).isAttached()) {
			return attachmentConnectors.get(0);
		} else {
			return null;
		}
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		if (getOutlinePolygonXZ() != null) {
			return getOutlinePolygonXZ().boundingBox();
		} else {
			return node.getPos().boundingBox();
		}
	}

	public PolygonXYZ getOutlinePolygon() {
		SimplePolygonXZ outlinePolygonXZ = getOutlinePolygonXZ();
		AttachmentConnector attachmentConnector = getConnectorIfAttached();
		if (outlinePolygonXZ == null) {
			return null;
		} else if (attachmentConnector != null) {
			return outlinePolygonXZ.xyz(attachmentConnector.getAttachedPos().y);
		} else {
			return connectors.getPosXYZ(outlinePolygonXZ);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + node + ")";
	}

	/**
	 * @return  a triangulation of the area covered by this
	 */
	protected List<TriangleXYZ> getTriangulation() {

		if (getOutlinePolygonXZ() == null) return emptyList();

		List<TriangleXZ> trianglesXZ = new ArrayList<>(TriangulationUtil.triangulate(getOutlinePolygonXZ()));
		trianglesXZ.replaceAll(TriangleXZ::makeCounterclockwise);

		AttachmentConnector attachmentConnector = getConnectorIfAttached();
		if (attachmentConnector != null) {
			double ele = attachmentConnector.getAttachedPos().y;
			return TriangulationUtil.triangulationXZtoXYZ(trianglesXZ, t -> t.xyz(ele));
		} else {
			return TriangulationUtil.triangulationXZtoXYZ(trianglesXZ, connectors::getPosXYZ);
		}

	}

}
