package org.osm2world.core.target.common.mesh;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleStrip;

import java.awt.Color;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.TextureDataDimensions;

public class MeshUtil {

	/** prevents instantiation */
	private MeshUtil() {}

	private static final List<VectorXZ> BOX_TEX_COORDS_1 = triangleVertexListFromTriangleStrip(asList(
		new VectorXZ(0,     0), new VectorXZ(0.25,     0),
		new VectorXZ(0, 1.0/3), new VectorXZ(0.25, 1.0/3),
		new VectorXZ(0, 2.0/3), new VectorXZ(0.25, 2.0/3),
		new VectorXZ(0,     1), new VectorXZ(0.25,     1)
	));

	private static final List<VectorXZ> BOX_TEX_COORDS_2 = triangleVertexListFromTriangleStrip(asList(
		new VectorXZ(0.25, 2.0/3), new VectorXZ(0.25, 1.0/3),
		new VectorXZ(0.50, 2.0/3), new VectorXZ(0.50, 1.0/3),
		new VectorXZ(0.75, 2.0/3), new VectorXZ(0.75, 1.0/3),
		new VectorXZ(1.00, 2.0/3), new VectorXZ(1.00, 1.0/3)
	));

	/**
	 * draws a box with outward-facing polygons.
	 *
	 * @param faceDirection  direction for the "front" of the box
	 */
	public static Geometry createBox(VectorXYZ bottomCenter, VectorXZ faceDirection, double height, double width,
			double depth, @Nullable Color color, List<TextureDataDimensions> textureDimensions) {

		final VectorXYZ backVector = faceDirection.mult(-depth).xyz(0);
		final VectorXYZ rightVector = faceDirection.rightNormal().mult(-width).xyz(0);
		final VectorXYZ upVector = new VectorXYZ(0, height, 0);

		final VectorXYZ frontLowerLeft = bottomCenter
				.add(rightVector.mult(-0.5))
				.add(backVector.mult(-0.5));

		final VectorXYZ frontLowerRight = frontLowerLeft.add(rightVector);
		final VectorXYZ frontUpperLeft  = frontLowerLeft.add(upVector);
		final VectorXYZ frontUpperRight = frontLowerRight.add(upVector);

		final VectorXYZ backLowerLeft   = frontLowerLeft.add(backVector);
		final VectorXYZ backLowerRight  = frontLowerRight.add(backVector);
		final VectorXYZ backUpperLeft   = frontUpperLeft.add(backVector);
		final VectorXYZ backUpperRight  = frontUpperRight.add(backVector);

		List<VectorXYZ> vsStrip1 = asList(
				backLowerLeft, backLowerRight,
				frontLowerLeft, frontLowerRight,
				frontUpperLeft, frontUpperRight,
				backUpperLeft, backUpperRight
		);

		List<VectorXYZ> vsStrip2 = asList(
				frontUpperRight, frontLowerRight,
				backUpperRight, backLowerRight,
				backUpperLeft, backLowerLeft,
				frontUpperLeft, frontLowerLeft
		);

		int numLayers = textureDimensions == null ? 0 : textureDimensions.size();

		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(numLayers, color, Interpolation.FLAT);
		builder.addTriangleStrip(vsStrip1, nCopies(numLayers, BOX_TEX_COORDS_1));
		builder.addTriangleStrip(vsStrip2, nCopies(numLayers, BOX_TEX_COORDS_2));
		return builder.build();

	}

}
