package org.osm2world.map_elevation.data;

import static org.osm2world.map_elevation.data.GroundState.ON;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.creation.EleCalculator;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.world.data.WorldObject;

/**
 * a point referenced by one or more {@link WorldObject}s, with known xz coords
 * and an elevation to be assigned by an {@link EleCalculator}.
 *
 * This class is a core concept of elevation calculation:
 * Because there is no injective mapping from xz coords to elevation (that
 * would not allow for bridges etc.), we need to represent whether points
 * are supposed to be "the same" with regards to elevation in some other manner.
 * Moreover, not all such points correspond to {@link MapNode}s.
 * Thus, this purpose is served by EleConnectors.
 */
public class EleConnector {

	public final VectorXZ pos;

	/** TODO document - MapNode or Intersection object, for example */
	public final Object reference;

	/**
	 * indicates whether this connector should be connected to the terrain,
	 * or is instead above or below the terrain
	 */
	public final GroundState groundState;

	private VectorXYZ posXYZ;

	/**
	 * creates an EleConnector at the given xz coordinates.
	 * @param pos          final value for {@link #pos}; != null
	 * @param reference    final value for {@link #reference}; may be null
	 * @param groundState  final value for {@link #groundState}
	 */
	public EleConnector(VectorXZ pos, Object reference, GroundState groundState) {
		assert pos != null;
		this.pos = pos;
		this.reference = reference;
		this.groundState = groundState;
	}

	/**
	 * assigns the elevation that has been calculated for this connector.
	 * Only for use by an {@link EleCalculator}.
	 *
	 * TODO make package-visible
	 */
	public void setPosXYZ(VectorXYZ posXYZ) {

		assert posXYZ.xz().equals(this.pos);

		this.posXYZ = posXYZ;

	}

	/**
	 * returns the 3d position after it has been calculated.
	 *
	 * The elevation, and therefore this {@link VectorXYZ}, is the only
	 * property which changes (exactly once) over the lifetime of an
	 * {@link EleConnector}: It is null before elevation calculation,
	 * and assigned its ultimate value afterwards.
	 */
	public VectorXYZ getPosXYZ() {
		return posXYZ;
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
		return String.format("(%s, %s, %s)", pos, reference, groundState);
	}

}
