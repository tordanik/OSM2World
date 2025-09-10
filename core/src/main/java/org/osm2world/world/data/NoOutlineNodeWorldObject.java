package org.osm2world.world.data;

import static java.lang.Math.abs;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.osm2world.util.ValueParseUtil.parseMeasure;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.List;

import org.osm2world.conversion.ConversionLog;
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

	protected final EleConnector eleConnector;
	protected final AttachmentConnector attachmentConnector;

	protected final boolean supportMinHeight;

	public NoOutlineNodeWorldObject(MapNode node) {
		this(node, false);
	}

	public NoOutlineNodeWorldObject(MapNode node, boolean supportMinHeight) {

		this.node = node;
		this.supportMinHeight = supportMinHeight;

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
	 * Provides subclasses with the 3d position of the {@link MapNode}.
	 * Only works during rendering (i.e. after elevation calculation).
	 * Will consider attachment, and will also consider min_height tags if {@link #supportMinHeight} is true.
	 */
	protected VectorXYZ getBase() {

		VectorXYZ base = (attachmentConnector != null && attachmentConnector.isAttached())
				? attachmentConnector.getAttachedPos()
				: eleConnector.getPosXYZ();

		double minHeight = getMinHeight();

		if (minHeight <= 0) {
			return base;
		} else if (attachmentConnector != null && attachmentConnector.isAttached()
				&& abs((attachmentConnector.getAttachedPos().y - eleConnector.getPosXYZ().y) - minHeight) < 0.5) {
			// ignore redundant min_height, e.g. when both minHeight and location=roof are used
			return base;
		} else {
			return base.addY(minHeight);
		}

	}

	/**
	 * Returns the object's height based on tags. Will subtract min_height if {@link #supportMinHeight} is true.
	 */
	protected double getHeight(double defaultHeight) {

		double height = parseHeight(node.getTags(), defaultHeight);
		double minHeight = getMinHeight();

		if (minHeight > height) {
			ConversionLog.warn("Invalid min_height/height values: " + minHeight + "/" + height, node);
			return height;
		} else {
			return height - minHeight;
		}

	}

	private double getMinHeight() {
		if (!supportMinHeight) {
			return 0;
		} else {
			double minHeight = parseMeasure(node.getTags().getValue("min_height"), 0.0);
			if (minHeight < 0) {
				ConversionLog.warn("Negative min_height value: " + minHeight, node);
				return 0;
			} else {
				return minHeight;
			}
		}
	}

}
