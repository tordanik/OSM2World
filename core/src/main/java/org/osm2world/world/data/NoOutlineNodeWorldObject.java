package org.osm2world.world.data;

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

	protected final boolean supportMinHeight;

	public NoOutlineNodeWorldObject(MapNode node) {
		this(node, false);
	}

	public NoOutlineNodeWorldObject(MapNode node, boolean supportMinHeight) {

		this.node = node;
		this.supportMinHeight = supportMinHeight;

		this.eleConnector = buildConnector();

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
	public final Iterable<EleConnector> getEleConnectors() {
		return singleton(eleConnector);
	}

	@Override
	public GroundState getGroundState() {
		if (eleConnector != null && eleConnector.isAttached()) {
			return GroundState.ATTACHED;
		} else {
			return GroundState.ON;
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

	/**
	 * Builds a connector based on {@link #getAttachmentTypes()}.
	 * Subclasses can override this to implement their own logic if just selecting custom types is not enough.
	 */
	protected EleConnector buildConnector() {

		List<String> attachmentTypes = getAttachmentTypes();

		if (attachmentTypes.isEmpty()) {
			return new EleConnector(node.getPos(), node, getGroundState());
		} else {
			EleConnector.AttachmentProperties ap = AttachmentUtil.hasVerticalSurfaceTypes(attachmentTypes)
					? new EleConnector.AttachmentProperties(attachmentTypes, getPreferredVerticalAttachmentHeight(), true)
					: new EleConnector.AttachmentProperties(attachmentTypes, 0, false);
			return new EleConnector(node.getPos(), node, getGroundState(), ap, this);
		}

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
	 */
	protected VectorXYZ getBase() {

		VectorXYZ base = eleConnector.getPosXYZ();

		double minHeight = getMinHeight();

		if (minHeight <= 0) {
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
