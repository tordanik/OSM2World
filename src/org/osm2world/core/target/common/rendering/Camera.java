package org.osm2world.core.target.common.rendering;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

public class Camera {
	
	private static final VectorXYZ UP = new VectorXYZ(0, 1, 0);
		
	VectorXYZ pos;
	VectorXYZ lookAt;
		
	/** returns the view direction vector with length 1 */
	public VectorXYZ getViewDirection() {
		//TODO: (performance)? cache viewDirection
		return lookAt.subtract(pos).normalize();
	}
	
	/**
	 * returns the vector that is orthogonal to the connection
	 * between pos and lookAt and points to the right of it.
	 * The result has length 1 and is parallel to the XZ plane.
	 */
	public VectorXYZ getRight() {
		return getViewDirection().crossNormalized(UP);
	}
	
	public VectorXYZ getPos() {
		return pos;
	}
	
	public VectorXYZ getLookAt() {
		return lookAt;
	}
	
	public void setPos(VectorXYZ pos) {
		this.pos = pos;
	}
	
	public void setPos(double x, double y, double z) {
		this.setPos(new VectorXYZ(x, y, z));
	}
	
	public void setLookAt(VectorXYZ lookAt) {
		this.lookAt = lookAt;
	}

	public void setLookAt(double x, double y, double z) {
		this.setLookAt(new VectorXYZ(x, y, z));
	}

	/**
	 * moves pos and lookAt in the view direction
	 * @param step  units to move forward
	 */
	public void moveForward(double step) {
		
		VectorXYZ d = getViewDirection();
		VectorXZ flatD = new VectorXZ(d.x, d.z).normalize();
		
		move(flatD.x * step, 0, flatD.z * step);
	}

	/**
	 * moves pos and lookAt to the right, orthogonally to the view direction
	 * 
	 * @param step  units to move right, negative units move to the left
	 */
	public void moveRight(double step) {
		
		VectorXYZ right = getRight();
		
		move(right.x * step, 0, right.z * step);
	}
	
	/** moves both pos and lookAt by the given vector */
	public void move(VectorXYZ move) {
		pos = pos.add(move);
		lookAt = lookAt.add(move);
	}

	/** moves both pos and lookAt by the given vector */
	public void move(double moveX, double moveY, double moveZ) {
		move(new VectorXYZ(moveX, moveY, moveZ));
	}
	
	/**
	 * moves lookAt to represent a rotation counterclockwise
	 * around the y axis on pos
	 * 
	 * @param d  angle in radians
	 */
	public void rotateY(double d) {
		
		VectorXYZ toOldLookAt = lookAt.subtract(pos);
		VectorXYZ toNewLookAt = toOldLookAt.rotateY(d);
		
		lookAt = pos.add(toNewLookAt);
		
	}

	
	/**
	 * moves lookAt to represent a rotation counterclockwise
	 * around the direction returned by {@link #getRight()}
	 * 
	 * @param d  angle in radians
	 */
	public void rotateAroundRight(double d) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	@Override
	public String toString() {
		return "{pos=" + pos + ", lookAt=" + lookAt + "}";
	}
		
}
