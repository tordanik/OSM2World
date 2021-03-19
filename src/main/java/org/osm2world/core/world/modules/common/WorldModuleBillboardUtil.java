package org.osm2world.core.world.modules.common;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.core.world.modules.common.WorldModuleBillboardUtil.LeftRightBoth.*;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;

/**
 * utility class for drawing billboards, particularly "cross tree" shapes
 */
public final class WorldModuleBillboardUtil {

	private WorldModuleBillboardUtil() { }

	enum LeftRightBoth {
		LEFT, RIGHT, BOTH
	}

	/**
	 * renders a "cross tree" shape.
	 *
	 * This shape is composed from two rectangular billboards, each with front
	 * and back side, intersecting orthogonally. Each billboard consists of
	 * two halves, separated at the billboard's intersection axis,
	 * to allow sorting of primitives for transparent rendering.
	 */
	public static final void renderCrosstree(Target target, Material material, VectorXYZ pos,
			double width, double height, boolean mirroredTextures) {

		/* With alpha blending, each billboard needs to consist of two halves,
		 * separated at the billboards' line of intersection, to allow sorting of primitives. */
		boolean centerSplit = material.getTransparency() == Transparency.TRUE;

		double halfWidth = 0.5 * width;

		VectorXYZ xPosBottom = pos.add(halfWidth, 0, 0);
		VectorXYZ xNegBottom = pos.add(-halfWidth, 0, 0);
		VectorXYZ zPosBottom = pos.add(0, 0, halfWidth);
		VectorXYZ zNegBottom = pos.add(0, 0, -halfWidth);

		VectorXYZ xPosTop = xPosBottom.addY(height);
		VectorXYZ xNegTop = xNegBottom.addY(height);
		VectorXYZ zPosTop = zPosBottom.addY(height);
		VectorXYZ zNegTop = zNegBottom.addY(height);

		if (centerSplit) {

			VectorXYZ posTop = pos.addY(height);

			target.drawTriangleStrip(material, asList(xNegTop, xNegBottom, posTop, pos),
					buildBillboardTexCoordLists(LEFT, mirroredTextures, material.getTextureLayers().size()));

			target.drawTriangleStrip(material, asList(xPosBottom, xPosTop, pos, posTop),
					buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.getTextureLayers().size()));

			target.drawTriangleStrip(material, asList(zNegTop, zNegBottom, posTop, pos),
					buildBillboardTexCoordLists(LEFT, mirroredTextures, material.getTextureLayers().size()));

			target.drawTriangleStrip(material, asList(zPosBottom, zPosTop, pos, posTop),
					buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.getTextureLayers().size()));

			if (!material.isDoubleSided()) {

				target.drawTriangleStrip(material, asList(xPosTop, xPosBottom, posTop, pos),
						buildBillboardTexCoordLists(LEFT, mirroredTextures, material.getTextureLayers().size()));

				target.drawTriangleStrip(material, asList(xNegBottom, xNegTop, pos, posTop),
						buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.getTextureLayers().size()));

				target.drawTriangleStrip(material, asList(zPosTop, zPosBottom, posTop, pos),
						buildBillboardTexCoordLists(LEFT, mirroredTextures, material.getTextureLayers().size()));

				target.drawTriangleStrip(material, asList(zNegBottom, zNegTop, pos, posTop),
						buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.getTextureLayers().size()));

			}

		} else {

			target.drawTriangleStrip(material, asList(xNegTop, xNegBottom, xPosTop, xPosBottom),
					buildBillboardTexCoordLists(BOTH, mirroredTextures, material.getTextureLayers().size()));

			target.drawTriangleStrip(material, asList(zNegTop, zNegBottom, zPosTop, zPosBottom),
					buildBillboardTexCoordLists(BOTH, mirroredTextures, material.getTextureLayers().size()));

			if (!material.isDoubleSided()) {

				target.drawTriangleStrip(material, asList(xPosTop, xPosBottom, xNegTop, xNegBottom),
						buildBillboardTexCoordLists(BOTH, mirroredTextures, material.getTextureLayers().size()));

				target.drawTriangleStrip(material, asList(zPosTop, zPosBottom, zNegTop, zNegBottom),
						buildBillboardTexCoordLists(BOTH, mirroredTextures, material.getTextureLayers().size()));
			}

		}

	}

	private static final List<List<VectorXZ>> buildBillboardTexCoordLists(
			LeftRightBoth side, boolean mirrored, int copies) {

		List<List<VectorXZ>> texCoordConstant;

		if (side == RIGHT) {
			if (mirrored) {
				texCoordConstant = BILLBOARD_RIGHT_TEX_COORDS_MIRRORED;
			} else {
				texCoordConstant = BILLBOARD_RIGHT_TEX_COORDS;
			}
		} else if (side == LEFT) {
			if (mirrored) {
				texCoordConstant = BILLBOARD_LEFT_TEX_COORDS_MIRRORED;
			} else {
				texCoordConstant = BILLBOARD_LEFT_TEX_COORDS;
			}
		} else {
			if (mirrored) {
				texCoordConstant = BILLBOARD_BOTH_TEX_COORDS_MIRRORED;
			} else {
				texCoordConstant = BILLBOARD_BOTH_TEX_COORDS;
			}
		}

		switch (copies) {
		case 0: return emptyList();
		case 1: return texCoordConstant;
		default: return nCopies(copies, texCoordConstant.get(0));
		}

	}

	private static final List<List<VectorXZ>> BILLBOARD_LEFT_TEX_COORDS = asList(asList(
			VectorXZ.Z_UNIT, VectorXZ.NULL_VECTOR,
			new VectorXZ(0.5, 1), new VectorXZ(0.5, 0)));

	private static final List<List<VectorXZ>> BILLBOARD_LEFT_TEX_COORDS_MIRRORED = asList(asList(
			new VectorXZ(1, 1), VectorXZ.X_UNIT,
			new VectorXZ(0.5, 1), new VectorXZ(0.5, 0)));

	private static final List<List<VectorXZ>> BILLBOARD_RIGHT_TEX_COORDS = asList(asList(
			VectorXZ.X_UNIT, new VectorXZ(1, 1),
			new VectorXZ(0.5, 0), new VectorXZ(0.5, 1)));

	private static final List<List<VectorXZ>> BILLBOARD_RIGHT_TEX_COORDS_MIRRORED = asList(asList(
			VectorXZ.NULL_VECTOR, VectorXZ.Z_UNIT,
			new VectorXZ(0.5, 0), new VectorXZ(0.5, 1)));

	private static final List<List<VectorXZ>> BILLBOARD_BOTH_TEX_COORDS = asList(asList(
			VectorXZ.Z_UNIT, VectorXZ.NULL_VECTOR,
			new VectorXZ(1, 1), new VectorXZ(1, 0)));

	private static final List<List<VectorXZ>> BILLBOARD_BOTH_TEX_COORDS_MIRRORED = asList(asList(
			new VectorXZ(1, 1), VectorXZ.X_UNIT,
			new VectorXZ(1, 1), new VectorXZ(1, 0)));

}
