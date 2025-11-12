package org.osm2world.world.modules.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.osm2world.util.enums.LeftRightBoth.*;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Transparency;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.enums.LeftRightBoth;

/**
 * utility class for drawing billboards, particularly "cross tree" shapes
 */
public final class WorldModuleBillboardUtil {

	private WorldModuleBillboardUtil() { }

	/**
	 * renders a "cross tree" shape.
	 *
	 * This shape is composed of two rectangular billboards, each with front
	 * and back side, intersecting orthogonally. Each billboard consists of
	 * two halves, separated at the billboard's intersection axis,
	 * to allow sorting of primitives for transparent rendering.
	 */
	public static List<Mesh> buildCrosstree(Material material, VectorXYZ pos,
			double width, double height, boolean mirroredTextures) {

		var builder = new TriangleGeometry.Builder(material.textureLayers().size(), null, Material.Interpolation.FLAT);

		/* With alpha blending, each billboard needs to consist of two halves,
		 * separated at the billboards' line of intersection, to allow sorting of primitives. */
		boolean centerSplit = material.transparency() == Transparency.TRUE;

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

			builder.addTriangleStrip(List.of(xNegTop, xNegBottom, posTop, pos),
					buildBillboardTexCoordLists(LEFT, mirroredTextures, material.textureLayers().size()));

			builder.addTriangleStrip(List.of(xPosBottom, xPosTop, pos, posTop),
					buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.textureLayers().size()));

			builder.addTriangleStrip(List.of(zNegTop, zNegBottom, posTop, pos),
					buildBillboardTexCoordLists(LEFT, mirroredTextures, material.textureLayers().size()));

			builder.addTriangleStrip(List.of(zPosBottom, zPosTop, pos, posTop),
					buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.textureLayers().size()));

			if (!material.doubleSided()) {

				builder.addTriangleStrip(List.of(xPosTop, xPosBottom, posTop, pos),
						buildBillboardTexCoordLists(LEFT, mirroredTextures, material.textureLayers().size()));

				builder.addTriangleStrip(List.of(xNegBottom, xNegTop, pos, posTop),
						buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.textureLayers().size()));

				builder.addTriangleStrip(List.of(zPosTop, zPosBottom, posTop, pos),
						buildBillboardTexCoordLists(LEFT, mirroredTextures, material.textureLayers().size()));

				builder.addTriangleStrip(List.of(zNegBottom, zNegTop, pos, posTop),
						buildBillboardTexCoordLists(RIGHT, mirroredTextures, material.textureLayers().size()));

			}

		} else {

			builder.addTriangleStrip(List.of(xNegTop, xNegBottom, xPosTop, xPosBottom),
					buildBillboardTexCoordLists(BOTH, mirroredTextures, material.textureLayers().size()));

			builder.addTriangleStrip(List.of(zNegTop, zNegBottom, zPosTop, zPosBottom),
					buildBillboardTexCoordLists(BOTH, mirroredTextures, material.textureLayers().size()));

			if (!material.doubleSided()) {

				builder.addTriangleStrip(List.of(xPosTop, xPosBottom, xNegTop, xNegBottom),
						buildBillboardTexCoordLists(BOTH, mirroredTextures, material.textureLayers().size()));

				builder.addTriangleStrip(List.of(zPosTop, zPosBottom, zNegTop, zNegBottom),
						buildBillboardTexCoordLists(BOTH, mirroredTextures, material.textureLayers().size()));
			}

		}

		return List.of(new Mesh(builder.build(), material));

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

		return switch (copies) {
			case 0 -> emptyList();
			case 1 -> texCoordConstant;
			default -> nCopies(copies, texCoordConstant.get(0));
		};

	}

	private static final List<List<VectorXZ>> BILLBOARD_LEFT_TEX_COORDS = List.of(List.of(
			VectorXZ.Z_UNIT, VectorXZ.NULL_VECTOR,
			new VectorXZ(0.5, 1), new VectorXZ(0.5, 0)));

	private static final List<List<VectorXZ>> BILLBOARD_LEFT_TEX_COORDS_MIRRORED = List.of(List.of(
			new VectorXZ(1, 1), VectorXZ.X_UNIT,
			new VectorXZ(0.5, 1), new VectorXZ(0.5, 0)));

	private static final List<List<VectorXZ>> BILLBOARD_RIGHT_TEX_COORDS = List.of(List.of(
			VectorXZ.X_UNIT, new VectorXZ(1, 1),
			new VectorXZ(0.5, 0), new VectorXZ(0.5, 1)));

	private static final List<List<VectorXZ>> BILLBOARD_RIGHT_TEX_COORDS_MIRRORED = List.of(List.of(
			VectorXZ.NULL_VECTOR, VectorXZ.Z_UNIT,
			new VectorXZ(0.5, 0), new VectorXZ(0.5, 1)));

	private static final List<List<VectorXZ>> BILLBOARD_BOTH_TEX_COORDS = List.of(List.of(
			new VectorXZ(0, 1), new VectorXZ(0, 0),
			new VectorXZ(1, 1), new VectorXZ(1, 0)));

	private static final List<List<VectorXZ>> BILLBOARD_BOTH_TEX_COORDS_MIRRORED = List.of(List.of(
			new VectorXZ(1, 1), new VectorXZ(1, 0),
			new VectorXZ(0, 1), new VectorXZ(0, 0)));

}
