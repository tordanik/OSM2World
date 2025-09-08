package org.osm2world.world.data;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

import java.util.List;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentUtil;

/**
 * superclass for {@link NodeWorldObject}s that don't have an outline,
 * and are not part of a network.
 * Instead, they are located at a single point on the terrain or other areas
 * and not connected to other features.
 *
 * @see OutlineNodeWorldObject
 */
public abstract class NoOutlineNodeWorldObject implements NodeWorldObject, BoundedObject {

	protected final MapNode node;

	private final EleConnector eleConnector;
	protected final AttachmentConnector attachmentConnector;

	public NoOutlineNodeWorldObject(MapNode node) {

		this.node = node;
		this.eleConnector = new EleConnector(node.getPos(), node,
				getGroundState());

		List<String> attachmentTypes = getAttachmentTypes();

		if (attachmentTypes.isEmpty()) {
			this.attachmentConnector = null;
		} else if (AttachmentUtil.hasVerticalSurfaceTypes(attachmentTypes)) {
			this.attachmentConnector = new AttachmentConnector(attachmentTypes,
					node.getPos().xyz(0), this, getPreferredVerticalAttachmentHeight(), true);
		} else {
			this.attachmentConnector = new AttachmentConnector(attachmentTypes,
					node.getPos().xyz(0), this, 0, false);
		}

	}

	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return new AxisAlignedRectangleXZ(
				node.getPos().x, node.getPos().z,
				node.getPos().x, node.getPos().z);
	}

	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return singleton(eleConnector);
	}

	@Override
	public GroundState getGroundState() {
		if (attachmentConnector != null && attachmentConnector.isAttached()) {
			return GroundState.ATTACHED;
		} else {
			return GroundState.ON;
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
	 * Returns the possible attachment types for this object.
	 * Can be empty if this object should not attach to anything.
	 * Subclasses can override this to implement their own logic.
	 */
	protected List<String> getAttachmentTypes() {
		return AttachmentUtil.getCompatibleSurfaceTypes(node);
	}

	protected double getPreferredVerticalAttachmentHeight() {
		return 0;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + node + ")";
	}

	/**
	 * provides subclasses with the 3d position of the {@link MapNode}.
	 * Only works during rendering (i.e. after elevation calculation).
	 */
	protected VectorXYZ getBase() {
		if (attachmentConnector != null && attachmentConnector.isAttached()) {
			return attachmentConnector.getAttachedPos();
		} else {
			return eleConnector.getPosXYZ();
		}
	}

}
