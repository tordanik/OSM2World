package org.osm2world.core.world.modules.building;

import static com.google.common.collect.Iterables.getLast;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Collections.*;
import static java.util.Collections.min;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.FaceDecompositionUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.target.common.texcoord.NamedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;

/**
 * a simplified representation of a wall as a 2D plane, with its origin in the bottom left corner.
 * This streamlines the placement of objects (windows, doors, and similar features) onto the wall.
 * Afterwards, positions are converted back into 3D space.
 *
 * Points on a wall are referenced with its own system of "wall surface coordinates":
 * The x coordinate is the position along the wall (starting with 0 for the first point of the wall's
 * lower boundary), the z coordinate refers to height.
 */
public class WallSurface {

	private final Material material;

	private final List<VectorXZ> lowerBoundary;
	private final SimplePolygonXZ wallOutline;

	private final List<VectorXYZ> lowerBoundaryXYZ;

	private final List<WallElement> elements = new ArrayList<>();

	/**
	 * Constructs a wall surface from a lower and upper wall boundary.
	 *
	 * @param lowerBoundaryXYZ  the lower boundary in global 3D space
	 * @param upperBoundaryXYZ  the upper boundary in global 3D space. Must be above the lower boundary.
	 *
	 * @throws InvalidGeometryException  if the lower and upper boundary do not represent a proper surface.
	 * This can happen, for example, because the wall has a zero or almost-zero height.
	 */
	public WallSurface(Material material, List<VectorXYZ> lowerBoundaryXYZ,
			List<VectorXYZ> upperBoundaryXYZ) throws IllegalArgumentException {

		this.material = material;
		this.lowerBoundaryXYZ = lowerBoundaryXYZ;

		if (lowerBoundaryXYZ.size() < 2)
			throw new IllegalArgumentException("need at least two points in the lower boundary");
		if (upperBoundaryXYZ.size() < 2)
			throw new IllegalArgumentException("need at least two ponts in the upper boundary");

		/* TODO: check for other problems, e.g. intersecting lower and upper boundary,
		   last points of the boundaries having different x values in wall surface coords, ... */

		/* convert the boundaries to wall surface coords */

		PolylineXZ lowerXZ = new PolylineXZ(lowerBoundaryXYZ.stream().map(p -> p.xz()).collect(toList()));

		Function<VectorXYZ, VectorXZ> toWallCoord = p -> new VectorXZ(
				lowerXZ.offsetOf(p.xz()),
				p.y - lowerBoundaryXYZ.get(0).y);

		Function<VectorXYZ, VectorXYZ> snapToLowerBoundary = p -> {
			VectorXZ xz = lowerXZ.closestPoint(p.xz());
			return xz.xyz(p.y);
		};

		lowerBoundary = lowerBoundaryXYZ.stream().map(toWallCoord).collect(toList());

		List<VectorXZ> upperBoundary = upperBoundaryXYZ.stream()
				.map(snapToLowerBoundary)
				.map(toWallCoord)
				.collect(toList());

		/* construct an outline polygon from the lower and upper boundary */

		List<VectorXZ> outerLoop = new ArrayList<>(upperBoundary);

		if (upperBoundary.get(0).distanceTo(lowerBoundary.get(0)) < 0.01) {
			outerLoop.remove(0);
		}
		if (getLast(upperBoundary).distanceTo(getLast(lowerBoundary)) < 0.01) {
			outerLoop.remove(outerLoop.size() - 1);
		}

		reverse(outerLoop);

		outerLoop.addAll(0, lowerBoundary);
		outerLoop.add(lowerBoundary.get(0));

		if (outerLoop.size() < 2) {
			throw new InvalidGeometryException("cannot construct a valid wall surface");
		}

		wallOutline = new SimplePolygonXZ(outerLoop);

	}

	public double getLength() {
		return lowerBoundary.get(lowerBoundary.size() - 1).x;
	}

	public double getHeight() {
		return wallOutline.boundingBox().sizeZ();
	}

	public Material getMaterial() {
		return material;
	}

	/** adds an element to the wall, unless the necessary space on the wall is already occupied */
	public void addElementIfSpaceFree(WallElement element) {

		if (!wallOutline.contains(element.outline())) {
			return;
		}

		boolean spaceOccupied = elements.stream().anyMatch(e ->
				e.outline().intersects(element.outline()) || e.outline().contains(element.outline()));

		if (!spaceOccupied) {
			elements.add(element);
		}

	}

	/**
	 * renders the wall
	 *
	 * @param textureOrigin  the origin of the texture coordinates on the wall surface
	 * @param windowHeight  the height for textures with the special height value 0 (used for windows)
	 * @param renderElements  whether the {@link WallElement}s inserted into this surface should also be rendered
	 */
	public void renderTo(Target target, VectorXZ textureOrigin,
			boolean applyWindowTexture, double windowHeight, boolean renderElements) {

		/* render the elements on the wall */

		if (renderElements) {
			for (WallElement e : elements) {
				e.renderTo(target, this);
			}
		}

		/* draw insets around the elements */

		for (WallElement e : elements) {
			if (e.insetDistance() != null) {

				PolygonXYZ frontOutline = convertTo3D(e.outline());
				PolygonXYZ backOutline = frontOutline.add(normalAt(e.outline().getCentroid()).mult(-e.insetDistance()));

				List<VectorXYZ> vsWall = createTriangleStripBetween(
						backOutline.vertices(), frontOutline.vertices());

				Material material = this.material;
				// TODO attempt to simulate ambient occlusion with a different baked ao texture?

				target.drawTriangleStrip(material, vsWall,
						texCoordLists(vsWall, material, NamedTexCoordFunction.STRIP_WALL));

			}
		}

		/* decompose and triangulate the empty wall surface */

		List<SimplePolygonXZ> holes = elements.stream().map(WallElement::outline).collect(toList());

		AxisAlignedRectangleXZ bbox = wallOutline.boundingBox();
		double minZ = bbox.minZ;
		double maxZ = bbox.maxZ;
		List<LineSegmentXZ> verticalLines = lowerBoundary.stream()
				.limit(lowerBoundary.size() - 1).skip(1) // omit first and last point
				.map(p -> new LineSegmentXZ(new VectorXZ(p.x, minZ - 1.0), new VectorXZ(p.x, maxZ + 1.0)))
				.collect(toList());

		List<ShapeXZ> shapes = new ArrayList<>(holes);
		shapes.addAll(verticalLines);

		Collection<? extends PolygonShapeXZ> faces = shapes.isEmpty()
				? singletonList(wallOutline)
				: FaceDecompositionUtil.splitPolygonIntoFaces(wallOutline, shapes);

		if (!holes.isEmpty()) {
			faces.removeIf(f -> holes.stream().anyMatch(hole -> hole.contains(f.getPointInside())));
		}

		List<TriangleXZ> triangles = faces.stream().flatMap(f -> f.getTriangulation().stream()).collect(toList());
		List<TriangleXYZ> trianglesXYZ = triangles.stream().map(t -> convertTo3D(t)).collect(toList());

		/* determine the material depending on whether a window texture should be applied */

		Material material = applyWindowTexture
				? this.material.withAddedLayers(BUILDING_WINDOWS.getTextureLayers())
				: this.material;

		/* calculate texture coordinates */

		List<TextureLayer> textureLayers = material.getTextureLayers();

		List<List<VectorXZ>> texCoordLists = new ArrayList<>(textureLayers.size());

		for (int texLayer = 0; texLayer < textureLayers.size(); texLayer ++) {

			List<VectorXZ> vs = new ArrayList<>();

			for (TriangleXZ triangle : triangles) {
				vs.add(triangle.v1);
				vs.add(triangle.v2);
				vs.add(triangle.v3);
			}

			Double fixedHeight = null;

			if (texLayer >= this.material.getNumTextureLayers()
					|| this.material == GLASS_WALL) {
				// window texture layer
				fixedHeight = windowHeight;
			}

			texCoordLists.add(texCoords(vs, textureLayers.get(texLayer).baseColorTexture.dimensions(),
					textureOrigin, fixedHeight));

		}

		/* render the wall */

		target.drawTriangles(material, trianglesXYZ, texCoordLists);

	}

	public VectorXYZ convertTo3D(VectorXZ v) {

		double ratio = v.x / getLength();

		VectorXYZ point = interpolateOn(lowerBoundaryXYZ, ratio);

		return point.addY(v.z);

	}

	public TriangleXYZ convertTo3D(TriangleXZ t) {
		return t.xyz(v -> convertTo3D(v));
	}

	public PolygonXYZ convertTo3D(PolygonShapeXZ polygon) {
		List<VectorXYZ> outline = new ArrayList<>(polygon.vertices().size());
		polygon.vertices().forEach(v -> outline.add(convertTo3D(v)));
		return new PolygonXYZ(outline);
	}

	/**
	 * Projects a point in global 3D space onto the wall,
	 * then returns the wall surface coordinate of that projection.
	 * Conceptually the inverse of {@link #convertTo3D(VectorXZ)}
	 * (except that it also accepts points which are not on the wall).
	 */
	public VectorXZ toWallCoord(VectorXYZ v) {

		PolylineXZ wallXZ = new PolylineXZ(lowerBoundaryXYZ.stream().map(VectorXYZ::xz).collect(toList()));

		LineSegmentXZ closestSegment =
				min(wallXZ.getSegments(), Comparator.comparing(s -> distanceFromLineSegment(v.xz(), s)));

		VectorXZ projectedPointXZ = projectPerpendicular(v.xz(), closestSegment.p1, closestSegment.p2);
		double relativeLengthProjectedPoint = wallXZ.offsetOf(projectedPointXZ) / wallXZ.getLength();

		return new VectorXZ(
				relativeLengthProjectedPoint * this.getLength(),
				v.y - lowerBoundaryXYZ.get(0).y);

	}

	public VectorXYZ normalAt(VectorXZ v) {

		/* calculate the normal by placing 3 points close to each other on the surface,
		 * and calculating the normal of that triangle */

		double smallXDist = min(0.01, getLength() / 3);
		double smallZDist = 0.01;

		if (v.x + smallXDist > getLength()) {
			assert v.x - smallXDist >= 0;
			v = v.add(-smallXDist, 0);
		}

		VectorXYZ vXYZ = convertTo3D(v);
		VectorXYZ vXYZRight = convertTo3D(v.add(smallXDist, 0));
		VectorXYZ vXYZTop = convertTo3D(v.add(0, smallZDist));

		return vXYZTop.subtract(vXYZ).crossNormalized(vXYZRight.subtract(vXYZ));

	}

	/**
	 * generates texture coordinates for textures placed on the wall surface.
	 * One texture coordinate dimension running along the wall, the other running up the wall.
	 * Input coordinates are surface coordinates.
	 *
	 * @param textureOrigin  the origin of the texture coordinates on the wall surface
	 * @param fixedHeight  if not null, this overrides the texture's height (used for windows)
	 */
	public List<VectorXZ> texCoords(List<VectorXZ> vs, TextureDataDimensions textureDimensions,
			VectorXZ textureOrigin, @Nullable Double fixedHeight) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		/* As surface coords, the input coords are already texture coordinates for a hypothetical
		 * 'unit texture' of width and height 1). We now scale them based on the texture's height and width or,
		 * for textures representing distinct repeating entities (tiles, windows), to an integer number of repeats */

		for (int i = 0; i < vs.size(); i++) {

			double height = textureDimensions.height;
			double width = textureDimensions.width;

			if (fixedHeight != null) {
				height = fixedHeight;
				if (textureDimensions.heightPerEntity != null) {
					height /= (textureDimensions.heightPerEntity / textureDimensions.height);
				}
			} else if (textureDimensions.heightPerEntity != null) {
				long entities = max(1, round(getHeight() / textureDimensions.heightPerEntity));
				double textureRepeats = entities / (textureDimensions.height / textureDimensions.heightPerEntity);
				height = getHeight() / textureRepeats;
			}

			if (textureDimensions.widthPerEntity != null) {
				long entities = max(1, round(getLength() / textureDimensions.widthPerEntity));
				double textureRepeats = entities / (textureDimensions.width / textureDimensions.widthPerEntity);
				width = getLength() / textureRepeats;
			}

			double s = (vs.get(i).x - textureOrigin.x) / width;
			double t = (vs.get(i).z - textureOrigin.z) / height;

			result.add(new VectorXZ(s, t));

		}

		return result;

	}


	/**
	 * generates texture coordinates for textures placed on the wall surface,
	 * compare {@link #texCoords(List, TextureDataDimensions, VectorXZ, Double)}.
	 * Input coordinates are global coordinates and will be projected onto the wall.
	 */
	public List<VectorXZ> texCoordsGlobal(List<VectorXYZ> vs, TextureDataDimensions textureDimensions) {
		List<VectorXZ> wallSurfaceVectors = vs.stream().map(v -> toWallCoord(v)).collect(toList());
		return texCoords(wallSurfaceVectors, textureDimensions, NULL_VECTOR, null);
	}

	/**
	 * returns a {@link TexCoordFunction} based on {@link #texCoordsGlobal(List, TextureDataDimensions)}.
	 */
	public TexCoordFunction texCoordFunction(TextureDataDimensions textureDimensions) {
		return vs -> texCoordsGlobal(vs, textureDimensions);
	}

}
