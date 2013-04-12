package org.osm2world.core.map_elevation.data;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.creation.ElevationCalculator;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.WorldObject;

/**
 * a point referenced by one or more {@link WorldObject}s, with known xz coords
 * and an elevation to be assigned by an {@link ElevationCalculator}.
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
	
	/** indicates whether this connector should be connected to the terrain */
	public final boolean terrain;
	
	private VectorXYZ posXYZ;
	
	/**
	 * creates an EleConnector at the given xz coordinates.
	 * @param pos        final value for {@link #pos}; != null
	 * @param reference  final value for {@link #reference}; may be null
	 * @param terrain    final value for {@link #terrain}
	 */
	public EleConnector(VectorXZ pos, Object reference, boolean terrain) {
		assert pos != null;
		this.pos = pos;
		this.reference = reference;
		this.terrain = terrain;
	}
	
	/**
	 * assigns the elevation that has been calculated for this connector.
	 * Only for use by an {@link ElevationCalculator}.
	 * 
	 * TODO make package-visible
	 */
	public void setPosXYZ(VectorXYZ posXYZ) {
				
		assert this.posXYZ.xz().equals(this.pos);
		
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
	 * This is supposed to be transitive and commutative.
	 */
	public boolean connectsTo(EleConnector other) {
		return pos.equals(other.pos)
				&& ((reference != null && reference == other.reference)
					|| (terrain && other.terrain));
	}
	
	@Override
	public String toString() {
		return String.format("(%s, %s, %s)", pos, reference, terrain);
	}
	
}
