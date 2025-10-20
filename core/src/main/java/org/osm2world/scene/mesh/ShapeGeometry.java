package org.osm2world.scene.mesh;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.ClosedShapeXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.TextureDataDimensions;
import org.osm2world.scene.texcoord.GlobalXZTexCoordFunction;
import org.osm2world.scene.texcoord.TexCoordFunction;

/** a geometry defined by placing a 2D shape ({@link ClosedShapeXZ}) in 3D space at some location, rotation and scale */
public class ShapeGeometry implements Geometry {

	/** the shape to be drawn; != null */
	public final ClosedShapeXZ shape;

	/** position where the shape is drawn; != null */
	public final VectorXYZ point;

	/** direction the shape is facing. Defines the shape's rotation along with {@link #upVector}; != null */
	public final VectorXYZ frontVector;

	/** up direction of the shape. Defines the shape's rotation along with {@link #frontVector}; != null */
	public final VectorXYZ upVector;

	/** a factor to scale the shape by, 1.0 leaves the shape unscaled. Must be positive. */
	public final double scaleFactor;

	/** vertex color to use */
	public final @Nullable Color color;

	public final Interpolation normalMode;

	/** the dimensions of each texture layer */
	public final List<TextureDataDimensions> textureDimensions;


	public ShapeGeometry(ClosedShapeXZ shape, VectorXYZ point, VectorXYZ frontVector, VectorXYZ upVector,
			double scaleFactor, @Nullable Color color, Interpolation normalMode,
			List<TextureDataDimensions> textureDimensions) {

		this.shape = shape;
		this.point = point;
		this.frontVector = frontVector;
		this.upVector = upVector;
		this.scaleFactor = scaleFactor;
		this.color = color;
		this.normalMode = normalMode;
		this.textureDimensions = textureDimensions;

		if (scaleFactor <= 0) {
			throw new IllegalArgumentException("scale factor must be positive, was " + scaleFactor);
		}

	}

	public ShapeGeometry(ClosedShapeXZ shape, VectorXYZ point, VectorXYZ frontVector, VectorXYZ upVector,
			double scaleFactor, @Nullable Color color, List<TextureDataDimensions> textureDimensions) {
		this(shape, point, frontVector, upVector, scaleFactor, color, Interpolation.FLAT, textureDimensions);
	}

	@Override
	public TriangleGeometry asTriangles() {

		// TODO better default texture coordinate function
		List<TexCoordFunction> defaultTexCoordFunction = textureDimensions.stream()
						.map(t -> new GlobalXZTexCoordFunction(t))
						.collect(toList());

		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(defaultTexCoordFunction, color, normalMode);

		for (TriangleXZ triangle : shape.getTriangulation()) {

			List<VectorXYZ> triangleVertices = new ArrayList<>();

			for (VectorXZ v : triangle.vertices()) {
				triangleVertices.add(new VectorXYZ(-v.x, v.z, 0));
			}

			if (scaleFactor != 1.0) {
				triangleVertices = CommonTarget.scaleShapeVectors(triangleVertices, scaleFactor);
			}

			triangleVertices = transformShape(triangleVertices, point, frontVector, upVector);

			TriangleXYZ tXYZ = new TriangleXYZ(triangleVertices.get(0), triangleVertices.get(1), triangleVertices.get(2));
			builder.addTriangles(singletonList(tXYZ));

		}

		return builder.build();

	}

}
