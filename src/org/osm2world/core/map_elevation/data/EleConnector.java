package org.osm2world.core.map_elevation.data;

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
 * This purpose is served by EleConnectors.
 */
public class EleConnector {
	
	public final VectorXZ pos;
	
	//TODO object reference for determining whether two E.C. with the same xz are "the same"
	
	private VectorXYZ posXYZ;
	
	/**
	 * creates an EleConnector at the given xz coordinates.
	 * @param pos  final value for {@link #pos}; != null
	 */
	public EleConnector(VectorXZ pos) {
		assert pos != null;
		this.pos = pos;
	}
	
	/**
	 * assigns the elevation that has been calculated for this connector.
	 * Only for one-time use by an {@link ElevationCalculator}.
	 */
	public void setPosXYZ(VectorXYZ posXYZ) {
		
		if (this.posXYZ != null) {
			throw new IllegalStateException("ele has already been set");
		}
		
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
	
}
