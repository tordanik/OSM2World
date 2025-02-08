package org.osm2world.world.attachment;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.world.data.WorldObject;

/**
 * a connector that can be attached to an {@link AttachmentSurface}.
 * This concept is used to model many possible connections, such as traffic signs or waste baskets attaching to a pole,
 * parking lots or roads attaching to a roof, furniture attaching to indoor floors, and many others.
 */
public class AttachmentConnector {

	/** default value for {@link #maxDistanceXZ()} */
	private static final double DEFAULT_MAX_DISTANCE_XZ = 10;

	/**
	 * default value for {@link #maxDistanceXZ()} on connectors that are not supposed to be moved horizontally.
	 * Still allows for small horizontal movement in case of slight position errors.
	 */
	private static final double DEFAULT_IMMOBILE_MAX_DISTANCE_XZ = 0.01;

	public final List<String> compatibleSurfaceTypes;

	public final VectorXYZ originalPos;

	/** the object that has created this connector, and which is looking for a surface */
	public final @Nullable WorldObject object;

	/**
	 * whether the connector is allowed to be moved horizontally.
	 * This is sometimes desired (e.g. when attaching a traffic sign to a pole, where the two nodes tend to be some
	 * distance apart), and sometimes not (e.g. with indoor mapping, where connectors are only used to find the
	 * desired floor, i.e. vertical elevation).
	 */
	public final boolean changeXZ;

	/** preferred height above the surface's {@link AttachmentSurface#getBaseEleAt(VectorXZ)} */
	public final double preferredHeight;

	public final Predicate<VectorXYZ> isAcceptableNormal;

	private boolean isAttached = false;
	private AttachmentSurface attachedSurface = null;
	private VectorXYZ attachedPos = null;
	private VectorXYZ attachedSurfaceNormal = null;

	// TODO: add layer; necessary to support multiple crossing bridges

	/**
	 *
	 * @param compatibleSurfaceTypes  list of surface types this can attach to in order of preference;
	 *   see {@link AttachmentSurface#getTypes()}
	 * @param originalPos  the original location of the connector (before snapping to an {@link AttachmentSurface})
	 * @param changeXZ  whether the horizontal position may be changed
	 */
	public AttachmentConnector(List<String> compatibleSurfaceTypes, VectorXYZ originalPos, WorldObject object,
			double preferredHeight, boolean changeXZ, @Nullable Predicate<VectorXYZ> isAcceptableNormal) {
		this.compatibleSurfaceTypes = compatibleSurfaceTypes;
		this.originalPos = originalPos;
		this.object = object;
		this.changeXZ = changeXZ;
		this.preferredHeight = preferredHeight;
		this.isAcceptableNormal = (isAcceptableNormal != null) ? isAcceptableNormal : (v -> true);
	}

	public AttachmentConnector(List<String> compatibleSurfaceTypes, VectorXYZ originalPos, WorldObject object,
			double preferredHeight, boolean changeXZ) {
		this(compatibleSurfaceTypes, originalPos, object, preferredHeight, changeXZ, null);
	}

	/**
	 * the maximum horizontal distance a surface can be from the initial position and still allow attachment.
	 * May be set to 0 (or close to it) when only vertical movement is desired.
	 */
	public double maxDistanceXZ() {
		return changeXZ ? DEFAULT_MAX_DISTANCE_XZ : DEFAULT_IMMOBILE_MAX_DISTANCE_XZ;
	}

	/**
	 * attach the connector to a surface. Must only be called once.
	 */
	public void attach(AttachmentSurface surface, VectorXYZ attachedPos, VectorXYZ attachedSurfaceNormal) {

		if (isAttached) {
			throw new IllegalStateException("this connector has already been attached");
		}

		this.isAttached = true;
		this.attachedSurface = surface;
		this.attachedPos = attachedPos;
		this.attachedSurfaceNormal = attachedSurfaceNormal;

		if (attachedPos.xz().distanceTo(originalPos.xz()) > maxDistanceXZ() + 0.001) {
			throw new IllegalArgumentException("this connector must not be moved horizontally more than " + maxDistanceXZ());
		}

		attachedSurface.addAttachedConnector(this);

	}

	public boolean isAttached() {
		return isAttached;
	}

	public AttachmentSurface getAttachedSurface() {
		if (!isAttached) throw new IllegalStateException("this connector is not attached to a surface");
		return attachedSurface;
	}

	public VectorXYZ getAttachedPos() {
		if (!isAttached) throw new IllegalStateException("this connector is not attached to a surface");
		return attachedPos;
	}

	public VectorXYZ getAttachedSurfaceNormal() {
		if (!isAttached) throw new IllegalStateException("this connector is not attached to a surface");
		return attachedSurfaceNormal;
	}

}
