package org.osm2world.core.target.common;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Y;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.ClosedShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.Mesh;

/**
 * superclass for {@link Target} implementations that defines some
 * of the required methods using others. Extending it reduces the number of
 * methods that have to be provided by the implementation
 */
public abstract class AbstractTarget implements Target {

	protected Configuration config;

	@Override
	public void setConfiguration(Configuration config) {
		this.config = config;
	}

	@Override
	public void drawShape(Material material, ClosedShapeXZ shape, VectorXYZ point,
			VectorXYZ frontVector, VectorXYZ upVector, double scaleFactor) {

		for (TriangleXZ triangle : shape.getTriangulation()) {

			List<VectorXYZ> triangleVertices = new ArrayList<VectorXYZ>();

			for (VectorXZ v : triangle.vertices()) {
				triangleVertices.add(new VectorXYZ(-v.x, v.z, 0));
			}

			if (scaleFactor != 1.0) {
				triangleVertices = scaleShapeVectors(triangleVertices, scaleFactor);
			}

			triangleVertices = transformShape(
					triangleVertices, point, frontVector, upVector);

			//TODO better default texture coordinate function
			drawTriangleStrip(material, triangleVertices.subList(0, 3),
					texCoordLists(triangleVertices.subList(0, 3), material, GLOBAL_X_Y));

		}

	}

	/**
	 * draws an extruded shape using {@link #drawTriangleStrip(Material, List, List)} calls.
	 * See {@link Target#drawExtrudedShape(Material, ShapeXZ, List, List, List, List, Set)}
	 * for documentation of the implemented interface method.
	 */
	@Override
	public void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path,
			List<VectorXYZ> upVectors, List<Double> scaleFactors,
			List<List<VectorXZ>> texCoordLists, Set<ExtrudeOption> options) {

		drawMesh(new Mesh(new ExtrusionGeometry(shape, path, upVectors, scaleFactors, null, options,
				material.getTextureDimensions()), material));

	}

	public static final List<VectorXYZ> scaleShapeVectors(List<VectorXYZ> vs, double scale) {

		if (scale == 1) {

			return vs;

		} else if (scale == 0) {

			return nCopies(vs.size(), NULL_VECTOR);

		} else {

			List<VectorXYZ> result = new ArrayList<VectorXYZ>(vs.size());

			for (VectorXYZ v : vs) {
				result.add(v.mult(scale));
			}

			return result;

		}

	}

	@Override
	public void drawBox(Material material,
			VectorXYZ bottomCenter, VectorXZ faceDirection,
			double height, double width, double depth) {

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

		List<List<VectorXZ>> texCoords1 = null, texCoords2 = null;

		if (material.getTextureLayers() != null) {
			texCoords1 = nCopies(material.getTextureLayers().size(), BOX_TEX_COORDS_1);
			texCoords2 = nCopies(material.getTextureLayers().size(), BOX_TEX_COORDS_2);
		}

		drawTriangleStrip(material, vsStrip1, texCoords1);
		drawTriangleStrip(material, vsStrip2, texCoords2);

	}

	protected static final List<VectorXZ> BOX_TEX_COORDS_1 = asList(
		new VectorXZ(0,     0), new VectorXZ(0.25,     0),
		new VectorXZ(0, 1.0/3), new VectorXZ(0.25, 1.0/3),
		new VectorXZ(0, 2.0/3), new VectorXZ(0.25, 2.0/3),
		new VectorXZ(0,     1), new VectorXZ(0.25,     1)
	);

	protected static final List<VectorXZ> BOX_TEX_COORDS_2 = asList(
		new VectorXZ(0.25, 2.0/3), new VectorXZ(0.25, 1.0/3),
		new VectorXZ(0.50, 2.0/3), new VectorXZ(0.50, 1.0/3),
		new VectorXZ(0.75, 2.0/3), new VectorXZ(0.75, 1.0/3),
		new VectorXZ(1.00, 2.0/3), new VectorXZ(1.00, 1.0/3)
	);

	/**
	 * See {@link Target#drawColumn(Material, Integer, VectorXYZ, double, double, double, boolean, boolean)}.
	 * Implemented using {@link #drawExtrudedShape(Material, ShapeXZ, List, List, List, List, Set)}.
	 */
	@Override
	public void drawColumn(Material material, Integer corners, VectorXYZ base,
			double height, double radiusBottom, double radiusTop,
			boolean drawBottom, boolean drawTop) {

		drawMesh(new Mesh(ExtrusionGeometry.createColumn(corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop,
				null, material.getTextureDimensions()), material));

	}

	@Override
	public void drawTriangleStrip(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {

		List<List<VectorXZ>> newTexCoordLists = emptyList();
		if (texCoordLists != null && !texCoordLists.isEmpty()) {
			newTexCoordLists = new ArrayList<List<VectorXZ>>(texCoordLists.size());
			for (List<VectorXZ> texCoordList : texCoordLists) {
				newTexCoordLists.add(
						triangleVertexListFromTriangleStrip(texCoordList));
			}
		}

		drawTriangles(material, trianglesFromTriangleStrip(vs), newTexCoordLists);
	}

	@Override
	public void drawTriangleFan(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {

		List<List<VectorXZ>> newTexCoordLists = emptyList();
		if (texCoordLists != null && !texCoordLists.isEmpty()) {
			newTexCoordLists = new ArrayList<List<VectorXZ>>(texCoordLists.size());
			for (List<VectorXZ> texCoordList : texCoordLists) {
				newTexCoordLists.add(
						triangleVertexListFromTriangleFan(texCoordList));
			}
		}

		drawTriangles(material, trianglesFromTriangleFan(vs), newTexCoordLists);

	}

	@Override
	public void drawConvexPolygon(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		drawTriangleFan(material, vs, texCoordLists);
	}

	@Override
	public void finish() {}

}
