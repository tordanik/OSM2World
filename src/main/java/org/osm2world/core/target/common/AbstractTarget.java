package org.osm2world.core.target.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Y;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.ClosedShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.MeshUtil;

/**
 * superclass for {@link Target} implementations that defines some
 * of the required methods using others. Extending it reduces the number of
 * methods that have to be provided by the implementation
 */
public abstract class AbstractTarget implements Target {

	protected @Nonnull Configuration config = new PropertiesConfiguration();

	@Override
	public Configuration getConfiguration() {
		return config;
	}

	@Override
	public void setConfiguration(Configuration config) {
		if (config != null) {
			this.config = config;
		} else {
			this.config = new PropertiesConfiguration();
		}
	}

	@Override
	public void drawShape(@Nonnull Material material, @Nonnull ClosedShapeXZ shape, @Nonnull VectorXYZ point,
						  @Nonnull VectorXYZ frontVector, @Nonnull VectorXYZ upVector, double scaleFactor) {

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
	public void drawExtrudedShape(@Nonnull Material material, @Nonnull ShapeXZ shape, @Nonnull List<VectorXYZ> path,
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
	public void drawBox(@Nonnull Material material, @Nonnull VectorXYZ bottomCenter, @Nonnull VectorXZ faceDirection,
						double height, double width, double depth) {

		drawMesh(new Mesh(MeshUtil.createBox(bottomCenter, faceDirection, height, width, depth, null,
				material.getTextureDimensions()), material));

	}

	@Override
	public void drawColumn(@Nonnull Material material, Integer corners, @Nonnull VectorXYZ base,
						   double height, double radiusBottom, double radiusTop,
						   boolean drawBottom, boolean drawTop) {

		drawMesh(new Mesh(ExtrusionGeometry.createColumn(corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop,
				null, material.getTextureDimensions()), material));

	}

	@Override
	public void drawTriangleStrip(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
								  @Nonnull List<List<VectorXZ>> texCoordLists) {

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
	public void drawTriangleFan(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
								@Nonnull List<List<VectorXZ>> texCoordLists) {

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
	public void drawConvexPolygon(@Nonnull Material material, @Nonnull List<VectorXYZ> vs,
								  @Nonnull List<List<VectorXZ>> texCoordLists) {
		drawTriangleFan(material, vs, texCoordLists);
	}

	@Override
	public void finish() {}

}
