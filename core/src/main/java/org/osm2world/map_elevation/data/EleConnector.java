package org.osm2world.map_elevation.data;

import static org.osm2world.map_elevation.data.GroundState.ON;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.map_elevation.creation.EleCalculator;
import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.WorldObject;

/**
 * A point which anchors a {@link WorldObject} in the world.
 * Connectors may connect to other connectors in the same location,
 * attach themselves to an {@link AttachmentSurface},
 * or simply be located on/above/below the ground.
 * <p>
 * Connectors start out with an initial location,
 * but may be moved both horizontally and vertically to properly attach to nearby objects,
 * and will often be moved again vertically to reflect terrain elevation.
 */
public class EleConnector {

	/**
	 * Parameters for connectors which try to attach to an {@link AttachmentSurface}.
	 * This mechanism is used for many possible connections, such as traffic signs or waste baskets attaching to a pole,
	 * parking lots or roads attaching to a roof, furniture attaching to indoor floors, and many others.
	 *
	 * @param compatibleSurfaceTypes  acceptable surface types in order of preference
	 * @param changeXZ  whether the connector is allowed to be moved horizontally.
	 * This is sometimes desired (e.g. when attaching a traffic sign to a pole, where the two nodes tend to be some
	 * distance apart), and sometimes not (e.g. with indoor mapping, where connectors are only used to find the
	 * desired floor, i.e. vertical elevation).
	 * @param preferredHeight  preferred height above the surface's {@link AttachmentSurface#getBaseEleAt(VectorXZ)}
	 */
	public record AttachmentProperties(
			List<String> compatibleSurfaceTypes,
			double preferredHeight,
			boolean changeXZ,
			Predicate<VectorXYZ> isAcceptableNormal
	) {

		public AttachmentProperties(List<String> compatibleSurfaceTypes, double preferredHeight, boolean changeXZ) {
			this(compatibleSurfaceTypes, preferredHeight, changeXZ, (v -> true));
		}

		/** default value for {@link #maxDistanceXZ()} */
		private static final double DEFAULT_MAX_DISTANCE_XZ = 10;

		/**
		 * default value for {@link #maxDistanceXZ()} on connectors that are not supposed to be moved horizontally.
		 * Still allows for small horizontal movement in case of slight position errors.
		 */
		private static final double DEFAULT_IMMOBILE_MAX_DISTANCE_XZ = 0.01;

		/**
		 * the maximum horizontal distance a surface can be from the initial position and still allow attachment.
		 * May be set to 0 (or close to it) when only vertical movement is desired.
		 */
		public double maxDistanceXZ() {
			return changeXZ ? DEFAULT_MAX_DISTANCE_XZ : DEFAULT_IMMOBILE_MAX_DISTANCE_XZ;
		}

	}

	public final VectorXZ pos;
	public final VectorXYZ originalPos;

	/**
	 * indicates whether this connector should be connected to the terrain,
	 * or is instead above or below the terrain
	 */
	public final GroundState groundState;

	/** attachment information, null if the connector should not try to attach to {@link AttachmentSurface}s */
	public final @Nullable AttachmentProperties attachmentProperties;

	/** the object that has created this connector */
	public final @Nullable WorldObject object;

	/** TODO document - MapNode or Intersection object, for example */
	public final Object reference;

	/** the current position of this connector, which is changed from {@link #originalPos} by various calculations */
	private VectorXYZ posXYZ;

	private AttachmentSurface attachedSurface = null;
	private VectorXYZ attachedSurfaceNormal = null;

	/**
	 * creates an EleConnector at the given coordinates.
	 * @param pos          final value for {@link #pos}; != null
	 * @param reference    final value for {@link #reference}; may be null
	 * @param groundState  final value for {@link #groundState}
	 */
	public EleConnector(VectorXZ pos, @Nullable Object reference, GroundState groundState) {
		assert pos != null;
		this.pos = pos;
		this.originalPos = pos.xyz(0);
		this.reference = reference;
		this.groundState = groundState;
		this.attachmentProperties = null;
		this.object = null;
	}

	/**
	 * creates an EleConnector with attachment information.
	 */
	public EleConnector(Vector3D originalPos, @Nullable Object reference, GroundState groundState,
			@Nullable AttachmentProperties attachmentProperties, @Nullable WorldObject object) {
		this.pos = originalPos.xz();
		this.originalPos = originalPos instanceof VectorXYZ vXYZ ? vXYZ : originalPos.xz().xyz(0);
		this.groundState = groundState;
		this.attachmentProperties = attachmentProperties;
		this.object = object;
		this.reference = reference;
	}

	/**
	 * assigns the elevation that has been calculated for this connector.
	 * Only for use by an {@link EleCalculator} and related classes.
	 */
	public void setPosXYZ(VectorXYZ posXYZ) {
		this.posXYZ = posXYZ;
	}

	/**
	 * returns the 3d position after it has been calculated.
	 * This {@link VectorXYZ}, changes over the lifetime of an {@link EleConnector}:
	 * It is null before elevation calculation, may contain some intermediate results,
	 * and is eventually assigned its ultimate value.
	 */
	public VectorXYZ getPosXYZ() {
		return posXYZ;
	}

	/**
	 * Attach this connector to a surface. Must only be called once.
	 */
	public void attach(AttachmentSurface surface, VectorXYZ attachedPos, VectorXYZ attachedSurfaceNormal) {

		if (attachmentProperties == null) {
			throw new IllegalStateException("this connector is not intended for attachment");
		} else if (isAttached()) {
			throw new IllegalStateException("this connector has already been attached");
		} else if (attachedPos.distanceToXZ(originalPos) > attachmentProperties.maxDistanceXZ() + 0.001) {
			throw new IllegalArgumentException("this connector must not be moved horizontally more than "
					+ attachmentProperties.maxDistanceXZ());
		}

		this.setPosXYZ(attachedPos);

		this.attachedSurface = surface;
		this.attachedSurfaceNormal = attachedSurfaceNormal;

		attachedSurface.addAttachedConnector(this);

	}

	public boolean isAttached() {
		return attachedSurface != null;
	}

	public AttachmentSurface getAttachedSurface() {
		if (!isAttached()) throw new IllegalStateException("this connector is not attached to a surface");
		return attachedSurface;
	}

	public VectorXYZ getAttachedSurfaceNormal() {
		if (!isAttached()) throw new IllegalStateException("this connector is not attached to a surface");
		return attachedSurfaceNormal;
	}

	/**
	 * returns true if this connector is to be joined with the other one.
	 * It is possible that connectors are joined even if this method returns
	 * false - that can happen when they are both joined to a third connector.
	 */
	public boolean connectsTo(EleConnector other) {
		return pos.equals(other.pos)
				&& ((reference != null && reference == other.reference)
					|| (groundState == ON && other.groundState == ON));
	}

	@Override
	public String toString() {
		return String.format("(%s, %s, %s, %s)", originalPos, reference, groundState, attachmentProperties);
	}

}
