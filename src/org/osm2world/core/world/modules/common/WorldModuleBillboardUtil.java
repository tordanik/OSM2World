package org.osm2world.core.world.modules.common;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;

/**
 * utility class for drawing billboards, particularly "cross tree" shapes
 */
public final class WorldModuleBillboardUtil {

	private WorldModuleBillboardUtil() { }
	
	/**
	 * renders a "cross tree" shape.
	 * 
	 * This shape is composed from two rectangular billboards, each with front
	 * and back side, intersecting orthogonally. Each billboard consists of
	 * two halves, separated at the billboard's intersection axis,
	 * to allow sorting of primitives for transparent rendering.
	 */
	public static final void renderCrosstree(Target<?> target,
			Material material, VectorXYZ pos,
			double width, double height,
			boolean mirroredTextures) {

		double halfWidth = 0.5 * width;
		
		VectorXYZ xPosBottom = pos.add(halfWidth, 0, 0);
		VectorXYZ xNegBottom = pos.add(-halfWidth, 0, 0);
		VectorXYZ zPosBottom = pos.add(0, 0, halfWidth);
		VectorXYZ zNegBottom = pos.add(0, 0, -halfWidth);

		VectorXYZ xPosTop = xPosBottom.add(0, height, 0);
		VectorXYZ xNegTop = xNegBottom.add(0, height, 0);
		VectorXYZ zPosTop = zPosBottom.add(0, height, 0);
		VectorXYZ zNegTop = zNegBottom.add(0, height, 0);
		VectorXYZ posTop = pos.add(0, height, 0);
		
		target.drawTriangleStrip(material, asList(
				xNegTop, xNegBottom, posTop, pos),
				buildBillboardTexCoordLists(false, mirroredTextures,
						material.getTextureDataList().size()));
		
		target.drawTriangleStrip(material, asList(
				xPosBottom, xPosTop, pos, posTop),
				buildBillboardTexCoordLists(true, mirroredTextures,
						material.getTextureDataList().size()));

		target.drawTriangleStrip(material, asList(
				zNegTop, zNegBottom, posTop, pos),
				buildBillboardTexCoordLists(false, mirroredTextures,
						material.getTextureDataList().size()));
		
		target.drawTriangleStrip(material, asList(
				zPosBottom, zPosTop, pos, posTop),
				buildBillboardTexCoordLists(true, mirroredTextures,
						material.getTextureDataList().size()));

		target.drawTriangleStrip(material, asList(
				xPosTop, xPosBottom, posTop, pos),
				buildBillboardTexCoordLists(false, mirroredTextures,
						material.getTextureDataList().size()));
		
		target.drawTriangleStrip(material, asList(
				xNegBottom, xNegTop, pos, posTop),
				buildBillboardTexCoordLists(true, mirroredTextures,
						material.getTextureDataList().size()));

		target.drawTriangleStrip(material, asList(
				zPosTop, zPosBottom, posTop, pos),
				buildBillboardTexCoordLists(false, mirroredTextures,
						material.getTextureDataList().size()));
		
		target.drawTriangleStrip(material, asList(
				zNegBottom, zNegTop, pos, posTop),
				buildBillboardTexCoordLists(true, mirroredTextures,
						material.getTextureDataList().size()));
		
	}
	
	private static final List<List<VectorXZ>> buildBillboardTexCoordLists(
			boolean right, boolean mirrored, int copies) {
		
		if (right) {
			
			if (mirrored) {
				if (copies <= 3) { return BILLBOARD_RIGHT_TEX_COORDS_MIRRORED_COPIES[copies]; }
				else { return nCopies(copies, BILLBOARD_RIGHT_TEX_COORDS_MIRRORED); }
			} else {
				if (copies <= 3) { return BILLBOARD_RIGHT_TEX_COORDS_COPIES[copies]; }
				else { return nCopies(copies, BILLBOARD_RIGHT_TEX_COORDS); }
			}
			
		} else {
			
			if (mirrored) {
				if (copies <= 3) { return BILLBOARD_LEFT_TEX_COORDS_MIRRORED_COPIES[copies]; }
				else { return nCopies(copies, BILLBOARD_LEFT_TEX_COORDS_MIRRORED); }
			} else {
				if (copies <= 3) { return BILLBOARD_LEFT_TEX_COORDS_COPIES[copies]; }
				else { return nCopies(copies, BILLBOARD_LEFT_TEX_COORDS); }
			}
			
		}
		
	}
	
	private static final List<VectorXZ> BILLBOARD_LEFT_TEX_COORDS = asList(
			VectorXZ.Z_UNIT, VectorXZ.NULL_VECTOR,
			new VectorXZ(0.5, 1), new VectorXZ(0.5, 0));
	
	private static final List<VectorXZ> BILLBOARD_LEFT_TEX_COORDS_MIRRORED = asList(
			new VectorXZ(1, 1), VectorXZ.X_UNIT,
			new VectorXZ(0.5, 1), new VectorXZ(0.5, 0));
		
	private static final List<VectorXZ> BILLBOARD_RIGHT_TEX_COORDS = asList(
			VectorXZ.X_UNIT, new VectorXZ(1, 1),
			new VectorXZ(0.5, 0), new VectorXZ(0.5, 1));
	
	private static final List<VectorXZ> BILLBOARD_RIGHT_TEX_COORDS_MIRRORED = asList(
			VectorXZ.NULL_VECTOR, VectorXZ.Z_UNIT,
			new VectorXZ(0.5, 0), new VectorXZ(0.5, 1));
	
	@SuppressWarnings("unchecked")
	private static final List<List<VectorXZ>>[] BILLBOARD_LEFT_TEX_COORDS_COPIES = new List[]{
		nCopies(0, BILLBOARD_LEFT_TEX_COORDS),
		nCopies(1, BILLBOARD_LEFT_TEX_COORDS),
		nCopies(2, BILLBOARD_LEFT_TEX_COORDS),
		nCopies(3, BILLBOARD_LEFT_TEX_COORDS)
	};
	
	@SuppressWarnings("unchecked")
	private static final List<List<VectorXZ>>[] BILLBOARD_LEFT_TEX_COORDS_MIRRORED_COPIES = new List[]{
		nCopies(0, BILLBOARD_LEFT_TEX_COORDS_MIRRORED),
		nCopies(1, BILLBOARD_LEFT_TEX_COORDS_MIRRORED),
		nCopies(2, BILLBOARD_LEFT_TEX_COORDS_MIRRORED),
		nCopies(3, BILLBOARD_LEFT_TEX_COORDS_MIRRORED)
	};
	
	@SuppressWarnings("unchecked")
	private static final List<List<VectorXZ>>[] BILLBOARD_RIGHT_TEX_COORDS_COPIES = new List[]{
		nCopies(0, BILLBOARD_RIGHT_TEX_COORDS),
		nCopies(1, BILLBOARD_RIGHT_TEX_COORDS),
		nCopies(2, BILLBOARD_RIGHT_TEX_COORDS),
		nCopies(3, BILLBOARD_RIGHT_TEX_COORDS)
	};
	
	@SuppressWarnings("unchecked")
	private static final List<List<VectorXZ>>[] BILLBOARD_RIGHT_TEX_COORDS_MIRRORED_COPIES = new List[]{
		nCopies(0, BILLBOARD_RIGHT_TEX_COORDS_MIRRORED),
		nCopies(1, BILLBOARD_RIGHT_TEX_COORDS_MIRRORED),
		nCopies(2, BILLBOARD_RIGHT_TEX_COORDS_MIRRORED),
		nCopies(3, BILLBOARD_RIGHT_TEX_COORDS_MIRRORED)
	};
	
}
