package org.osm2world.world.data;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.*;
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

		List<String> attachmentTypes = getAttachmentTypes();

		if (connectors == null) {

			SimplePolygonXZ outlinePolygonXZ = getOutlinePolygonXZ();

			if (outlinePolygonXZ == null) {

				connectors = EleConnectorGroup.EMPTY;

			} else {

				var attachmentProperties = attachmentTypes.isEmpty() ? null
					: new EleConnector.AttachmentProperties(attachmentTypes, 0, false);

				connectors = new EleConnectorGroup();
				connectors.addConnectorsFor(outlinePolygonXZ.getVertices(),
						node, getGroundState(), attachmentProperties, this);

			}

		}

		return connectors;

	}

	/**
	 * Returns the possible attachment types for this object.
	 * Can be empty if this object should not attach to anything.
	 * Subclasses can override this to implement their own logic.
	 */
	protected List<String> getAttachmentTypes() {
		return AttachmentUtil.getCompatibleSurfaceTypes(node);
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
		if (outlinePolygonXZ == null) {
			return null;
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

		return TriangulationUtil.triangulationXZtoXYZ(trianglesXZ, connectors::getPosXYZ);

	}

}
