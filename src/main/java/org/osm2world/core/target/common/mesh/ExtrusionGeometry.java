package org.osm2world.core.target.common.mesh;

import static java.lang.Math.PI;
import static java.lang.Math.ceil;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.triangleVertexListFromTriangleStrip;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;
import static org.osm2world.core.target.common.ExtrudeOption.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.transformShape;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.*;
import org.osm2world.core.target.CommonTarget;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.common.texcoord.MapBasedTexCoordFunction;

/**
 * geometry defined by extruding a 2d shape along a path
 */
public class ExtrusionGeometry implements Geometry {

	private static final Double DEFAULT_SCALE_FACTOR = Double.valueOf(1.0);
	private static final EnumSet<ExtrudeOption> DEFAULT_EXTRUDE_OPTIONS = EnumSet.noneOf(ExtrudeOption.class);

	/** the shape to be extruded; != null */
	public final ShapeXZ shape;

	/**
	 * the path along which the shape is extruded.
	 * Implicitly, this also defines a rotation for the shape at each point.
	 * Must have at least two points; != null.
	 */
	public final List<VectorXYZ> path;

	/**
	 * defines the rotation (along with the path) at each point.
	 * Must have the same number of elements as path.
	 * You can use {@link Collections#nCopies(int, Object)} if you want the same up vector for all points of the path.
	 * Can be null if the path is vertical (defaults to z unit vector).
	 */
	public final @Nullable List<VectorXYZ> upVectors;

	/**
	 * optionally allows the shape to be scaled at each point. Must have the same number of elements as path.
	 * Can be set to null for a constant scale factor of 1.
	 */
	public final @Nullable List<Double> scaleFactors;

	/** vertex color to use */
	public final @Nullable Color color;

	/** flags setting additional options; can be null for no options. Usually an {@link EnumSet}. */
	public final @Nullable Set<ExtrudeOption> options;

	/** the dimensions of each texture layer */
	public final List<TextureDataDimensions> textureDimensions;

	public ExtrusionGeometry(ShapeXZ shape, List<VectorXYZ> path, List<VectorXYZ> upVectors, List<Double> scaleFactors,
			@Nullable Color color, Set<ExtrudeOption> options, List<TextureDataDimensions> textureDimensions) {

		this.shape = shape;
		this.path = path;
		this.upVectors = upVectors;
		this.scaleFactors = scaleFactors;
		this.options = options;
		this.color = color;
		this.textureDimensions = textureDimensions;

		if (path.size() < 2) {
			throw new IllegalArgumentException("path needs at least 2 nodes");
		} else if (upVectors != null && path.size() != upVectors.size()) {
			throw new IllegalArgumentException("path and upVectors must have same size");
		} else if (scaleFactors != null && path.size() != scaleFactors.size()) {
			throw new IllegalArgumentException("path and scaleFactors must have same size");
		}

		if (upVectors == null && !IntStream.range(0, path.size() - 1).allMatch(
				i -> path.get(i + 1).distanceToXZ(path.get(i)) < 1e-3)) {
			throw new NullPointerException("upVectors must not be null for non-vertical paths");
		}

		// convert to triangles; this ensures that any exceptions related to illegal geometry are thrown now
		this.asTriangles();

	}

	/**
	 * creates geometry for a column with outward-facing polygons around a point.
	 * A column is a polygon with 3 or more corners extruded upwards.
	 *
	 * The implementation may decide to reduce the number of corners in order to improve performance
	 * (or make rendering possible when a perfect cylinder isn't supported).
	 *
	 * @param corners  number of corners; null uses a circle shape to create a cylinder or (truncated) cone
	 */
	public static ExtrusionGeometry createColumn(Integer corners, VectorXYZ base, double height,
			double radiusBottom, double radiusTop, boolean drawBottom, boolean drawTop, @Nullable Color color,
			List<TextureDataDimensions> textureDimensions) {

		CircleXZ bottomCircle = new CircleXZ(VectorXZ.NULL_VECTOR, radiusBottom);
		ShapeXZ bottomShape;

		if (corners == null) {
			bottomShape = bottomCircle;
		} else {
			bottomShape = new SimplePolygonXZ(bottomCircle.vertices(corners));
		}

		EnumSet<ExtrudeOption> options = EnumSet.noneOf(ExtrudeOption.class);

		if (drawBottom) { options.add(START_CAP); }
		if (drawTop) { options.add(END_CAP); }

		return new ExtrusionGeometry(bottomShape, asList(base, base.addY(height)),
				null, asList(1.0, radiusTop/radiusBottom), color, options, textureDimensions);

	}

	@Override
	public TriangleGeometry asTriangles() {

		/* provide defaults for optional parameters */

		List<Double> scaleFactors = this.scaleFactors;
		List<VectorXYZ> upVectors = this.upVectors;
		Set<ExtrudeOption> options = this.options;

		if (scaleFactors == null) {
			scaleFactors = nCopies(path.size(), DEFAULT_SCALE_FACTOR);
		}

		if (upVectors == null) {
			upVectors = nCopies(path.size(), Z_UNIT);
		}

		if (options == null) {
			options = DEFAULT_EXTRUDE_OPTIONS;
		}

		/* prepare the builder for the result */

		Interpolation normalMode = (shape instanceof CircleXZ || options.contains(SMOOTH_SIDES))
				? Interpolation.SMOOTH : Interpolation.FLAT;
		Map<VectorXYZ, VectorXZ> texCoordMap = new HashMap<>();
		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(textureDimensions.size(), color, normalMode);

		/* calculate the forward direction of the shape from the path.
		 * Special handling for the first and last point,
		 * where the calculation of the "forward" vector is different. */

		List<VectorXYZ> forwardVectors = new ArrayList<>(path.size());

		forwardVectors.add(path.get(1).subtract(path.get(0)).normalize());

		for (int pathI = 1; pathI < path.size() - 1; pathI ++) {

			VectorXYZ forwardVector = path.get(pathI+1).subtract(path.get(pathI-1));
			forwardVectors.add(forwardVector.normalize());

		}

		int last = path.size() - 1;
		forwardVectors.add(path.get(last).subtract(path.get(last-1)).normalize());

		/* if the shape is a circle, approximate it with a polygon */

		ShapeXZ shape = this.shape;

		if (shape instanceof CircleXZ) {

			double desiredMinDetail = 0.03;
			CircleXZ circle = (CircleXZ)shape;
			double maxCircumference = max(scaleFactors) * 2 * circle.getRadius() * PI;
			int numPoints = Integer.max(4, (int) ceil(maxCircumference / desiredMinDetail));

			if (!options.contains(START_CAP) && !options.contains(END_CAP)) {
				// if the ends aren't visible, it's a lot easier to fake roundness with smooth shading
				numPoints = Integer.max(4, numPoints / 2);
			}

			shape = new SimplePolygonXZ(circle.vertices(numPoints));

		}

		/* extrude each ring of the shape separately */

		Collection<ShapeXZ> rings = singleton(shape);

		if (shape instanceof ClosedShapeXZ) {

			rings = new ArrayList<>();
			rings.add(((ClosedShapeXZ) shape).getOuter());

			boolean outerIsClockwise = ((ClosedShapeXZ) shape).getOuter().isClockwise();

			for (SimpleClosedShapeXZ hole : ((ClosedShapeXZ) shape).getHoles()) {
				// inner rings need to be the opposite winding compared to the outer ring
				SimplePolygonXZ inner = asSimplePolygon(hole);
				inner = (SimplePolygonXZ) (outerIsClockwise ? inner.makeCounterclockwise() : inner.makeClockwise());
				rings.add(inner);
			}

		}

		for (ShapeXZ ring : rings) {

			List<VectorXYZ> shapeVertices = new ArrayList<>();

			for (VectorXZ v : ring.vertices()) {
				shapeVertices.add(new VectorXYZ(-v.x, v.z, 0));
			}

			/* create an instance of the ring at each point of the path. */

			@SuppressWarnings("unchecked")
			List<VectorXYZ>[] shapeVectors = new List[path.size()];

			for (int pathI = 0; pathI < path.size(); pathI ++) {

				shapeVectors[pathI] = transformShape(
						CommonTarget.scaleShapeVectors(shapeVertices, scaleFactors.get(pathI)),
						path.get(pathI),
						forwardVectors.get(pathI),
						upVectors.get(pathI));

			}

			/* calculate texture coordinates */

			//TODO implement texture snapping using totalPathLength and totalShapeLength

			double totalPathLength = new PolylineXYZ(path).length();
			double totalShapeLength = new PolylineXZ(shape.vertices()).getLength();

			for (int pathI = 0; pathI < path.size(); pathI ++) {
				for (int j = 0; j < shape.vertices().size(); j++) {

					VectorXZ texCoord;

					double lengthAlongPath = 0;
					if (pathI > 0) {
						lengthAlongPath = new PolylineXYZ(path.subList(0, pathI + 1)).length();
					}

					double lengthAlongShape = 0;
					if (j > 0) {
						lengthAlongShape = new PolylineXZ(shape.vertices().subList(0, j + 1)).getLength();
					}

					if (options.contains(TEX_HEIGHT_ALONG_PATH)) {
						texCoord = new VectorXZ(lengthAlongShape, lengthAlongPath);
					} else {
						texCoord = new VectorXZ(lengthAlongPath, lengthAlongShape);
					}

					texCoordMap.put(shapeVectors[pathI].get(j), texCoord);

				}
			}

			/* create the rings of triangles between each pair of successive points */

			for (int pathI = 0; pathI + 1 < path.size(); pathI ++) {

				double scaleA = scaleFactors.get(pathI);
				double scaleB = scaleFactors.get(pathI + 1);
				List<VectorXYZ> shapeA = shapeVectors[pathI];
				List<VectorXYZ> shapeB = shapeVectors[pathI + 1];

				List<VectorXYZ> triangleVs;

				if (scaleA != 0 && scaleB != 0) {
					triangleVs = triangleVertexListFromTriangleStrip(createTriangleStripBetween(shapeB, shapeA));
				} else if (scaleA != 0 && scaleB == 0) {
					triangleVs = new ArrayList<>();
					for (int i = 0; i + 1 < shapeA.size(); i++) {
						triangleVs.addAll(asList(shapeA.get(i), shapeA.get(i + 1), shapeB.get(0)));
					}
				} else if (scaleA == 0 && scaleB != 0) {
					triangleVs = new ArrayList<>();
					for (int i = 0; i + 1 < shapeB.size(); i++) {
						triangleVs.addAll(asList(shapeB.get(i + 1), shapeB.get(i), shapeA.get(0)));
					}
				} else {
					triangleVs = null;
				}

				if (triangleVs != null) {

					List<List<VectorXZ>> texCoords = textureDimensions.stream()
							.map(t -> new MapBasedTexCoordFunction(texCoordMap, t))
							.map(f -> f.apply(triangleVs))
							.collect(toList());

					builder.addTriangleVs(triangleVs, texCoords);

				}

			}

		}


		TriangleGeometry result = builder.build();

		/* draw caps (if requested in the options and possible for this shape) */

		if (shape instanceof ClosedShapeXZ) {

			List<Geometry> geometries = new ArrayList<>(3);
			geometries.add(result);

			Interpolation capNormalMode = options.contains(SMOOTH_CAPS) ? Interpolation.SMOOTH : Interpolation.FLAT;

			if (options.contains(START_CAP) && scaleFactors.get(0) > 0) {
				// triangulate the shape here because there's no method (yet) for mirroring or reversing a ClosedShape
				for (TriangleXZ triangle : ((ClosedShapeXZ)shape).getTriangulation()) {
					geometries.add(new ShapeGeometry(triangle.reverse(), // invert winding
							path.get(0), forwardVectors.get(0), upVectors.get(0), scaleFactors.get(0),
							color, capNormalMode, textureDimensions));
				}
			}

			if (options.contains(END_CAP) && scaleFactors.get(last) > 0) {
				geometries.add(new ShapeGeometry((ClosedShapeXZ)shape,
						path.get(last), forwardVectors.get(last), upVectors.get(last), scaleFactors.get(last),
						color, capNormalMode, textureDimensions));
			}

			result = Geometry.combine(geometries).asTriangles();

		}

		return result;

	}

}
