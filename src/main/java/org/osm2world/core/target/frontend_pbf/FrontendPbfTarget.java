package org.osm2world.core.target.frontend_pbf;

import static java.lang.Math.round;
import static java.util.Collections.*;
import static org.osm2world.core.map_data.creation.EmptyTerrainBuilder.EMPTY_SURFACE_TAG;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.target.common.ExtrudeOption.*;
import static org.osm2world.core.target.common.material.Materials.TERRAIN_DEFAULT;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Shadow;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.ExtrusionGeometry;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.InstanceGeometry;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.TextureLayer;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.TextureLayer.TexCoordFunction;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.TextureLayer.Wrap;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.Transparency;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.MaterialBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.ModelBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Shape;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Shape.ShapeType;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.ShapeBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.StringBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Tile;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.TriangleGeometry;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector2dBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector3dBlock;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.PoolModule.Pool;
import org.osm2world.core.world.modules.WaterModule.AreaFountain;
import org.osm2world.core.world.modules.WaterModule.RiverJunction;
import org.osm2world.core.world.modules.WaterModule.Water;
import org.osm2world.core.world.modules.WaterModule.Waterway;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class FrontendPbfTarget extends AbstractTarget<RenderableToModelTarget>
		implements ModelTarget<RenderableToModelTarget> {

	/**
	 * whether empty terrain should be faked as a big rectangle slightly below
	 * other ground-level geometries. This reduces the number of triangles used
	 * for empty terrain. Waterbodies are still subtracted from this rectangle.
	 */
	private final boolean USE_FLOOR_PLATE = true;

	private final double FLOOR_PLATE_Y = -0.03;

	/**
	 * a block containing all elements of a particular type.
	 * Elements are then referenced by their index (i.e. position) in the block.
	 * This saves space in the resulting protobuf file
	 * because identical elements only need to be transmitted once.
	 */
	static interface Block<T> {

		public List<T> getElements();

		/** adds the element to the block if necessary, and returns its index */
		public int toIndex(T element);

	}

	/**
	 * simple implementation of {@link Block}.
	 * Works for any content type, but slow for large numbers of elements.
	 */
	static class SimpleBlock<T> implements Block<T> {

		List<T> elements = new ArrayList<T>();

		public List<T> getElements() {
			return elements;
		}

		/** adds the element to the block if necessary, and returns its index */
		public int toIndex(T element) {

			int index = elements.indexOf(element);

			if (index < 0) {
				elements.add(element);
				index = elements.size() - 1;
			}

			return index;

		}

	}

	/**
	 * implementation of {@link Block} that's optimized for {@link Vector3D} instances.
	 */
	static class VectorBlock<T extends Vector3D> implements Block<T> {

		private static class Entry<T extends Vector3D> implements Comparable<Entry<T>> {

			private final T element;
			private final int index;

			public Entry(T element, int index) {
				this.element = element;
				this.index = index;
			}

			@Override
			public int compareTo(Entry<T> o) {
				return ComparisonChain.start()
						.compare(this.element.getX(), o.element.getX())
						.compare(this.element.getY(), o.element.getY())
						.compare(this.element.getZ(), o.element.getZ())
						.result();
			}

		}

		List<T> elements = new ArrayList<T>();
		List<Entry<T>> sortedEntrys = new ArrayList<Entry<T>>();

		public List<T> getElements() {
			return elements;
		}

		/** adds the element to the block if necessary, and returns its index */
		public int toIndex(T element) {

			int index = binarySearch(sortedEntrys, new Entry<T>(element, -1));

			if (index >= 0) {
				return sortedEntrys.get(index).index;
			} else {

				elements.add(element);

				Entry<T> entry = new Entry<T>(element, elements.size() - 1);
				sortedEntrys.add(-(index + 1), entry);

				return entry.index;

			}

		}

	}

	private static class TriangleData {

		private final List<VectorXYZ> vertices;
		private final List<VectorXYZ> normals;
		private final List<List<VectorXZ>> texCoordLists;

		public TriangleData(int numTextureLayers) {

			vertices = new ArrayList<VectorXYZ>();
			normals = new ArrayList<VectorXYZ>();

			texCoordLists = new ArrayList<List<VectorXZ>>(numTextureLayers);

			for (int i = 0; i < numTextureLayers; i++) {
				texCoordLists.add(new ArrayList<VectorXZ>());
			}

		}

		public void add(List<VectorXYZ> vs,
				List<VectorXYZ> normals,
				List<List<VectorXZ>> texCoordLists) {

			assert (vs.size() == normals.size());
			assert (texCoordLists.size() == this.texCoordLists.size());

			this.vertices.addAll(vs);
			this.normals.addAll(normals);

			for (int i = 0; i < texCoordLists.size(); i++) {

				assert (vs.size() == texCoordLists.get(i).size());

				this.texCoordLists.get(i).addAll(texCoordLists.get(i));

			}

		}

		public List<VectorXYZ> getVertices() {
			return unmodifiableList(vertices);
		}

		public List<VectorXYZ> getNormals() {
			return unmodifiableList(normals);
		}

		public List<List<VectorXZ>> getTexCoordLists() {
			return unmodifiableList(texCoordLists);
		}

	}

	/**
	 * internally used {@link Target} implementation that collects all geometry for a single object
	 */
	private class WorldObjectBuilder extends AbstractTarget<RenderableToModelTarget>
			implements ModelTarget<RenderableToModelTarget> {

		private final WorldObject worldObject;

		private final Map<Material, TriangleData> currentTriangles = new HashMap<Material, TriangleData>();
		private final List<ExtrusionGeometry> currentExtrusionGeometries = new ArrayList<ExtrusionGeometry>();
		private final Multimap<Model, InstanceParameters> currentModelInstances = HashMultimap.create();

		/**
		 * @param worldObject  the OSM2World {@link WorldObject} that will provide the id and type,
		 *                     can be null.
		 */
		public WorldObjectBuilder(WorldObject worldObject) {
			this.worldObject = worldObject;
		}

		@Override
		public Class<RenderableToModelTarget> getRenderableType() {
			return RenderableToModelTarget.class;
		}

		@Override
		public void render(RenderableToModelTarget renderable) {
			renderable.renderTo(this);
		}

		@Override
		public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles,
				List<List<VectorXZ>> texCoordLists) {

			TriangleData triangleData;

			if (currentTriangles.containsKey(material)) {
				triangleData = currentTriangles.get(material);
			} else {
				triangleData = new TriangleData(material.getTextureDataList().size());
				currentTriangles.put(material, triangleData);
			}

			/* ensure an index-addressable order for the triangles */

			List<? extends TriangleXYZ> triangleList =
					triangles instanceof List
					? (List<? extends TriangleXYZ>) triangles
					: new ArrayList<TriangleXYZ>(triangles);

			/* create a vertex list from the triangles */

			List<VectorXYZ> vertices = new ArrayList<VectorXYZ>(triangleList.size() * 3);

			for (TriangleXYZ triangle : triangleList) {
				vertices.addAll(triangle.getVertices());
			}

			/* remove degenerate triangles */

			for (int i = triangleList.size() - 1; i >= 0; i--) { //go backwards because we're doing index-based deletion

				if (triangleList.get(i).isDegenerate()) { // filter degenerate triangles

					vertices.remove(3 * i + 2);
					vertices.remove(3 * i + 1);
					vertices.remove(3 * i);

					for (int layer = 0; layer < texCoordLists.size(); layer++) {
						texCoordLists.get(layer).remove(3 * i + 2);
						texCoordLists.get(layer).remove(3 * i + 1);
						texCoordLists.get(layer).remove(3 * i);
					}

				}

			}

			triangleData.add(vertices,
					nCopies(vertices.size(), Y_UNIT), //TODO replace with actual normals by extending PrimitiveTarget
					texCoordLists);

		}

		@Override
		public void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles,
				List<List<VectorXZ>> texCoordLists) {
			drawTriangles(material, triangles, texCoordLists);
		}

		@Override
		public void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path, List<VectorXYZ> upVectors,
				List<Double> scaleFactors, List<List<VectorXZ>> texCoordLists, EnumSet<ExtrudeOption> options) {

			ExtrusionGeometry.Builder geometryBuilder = ExtrusionGeometry.newBuilder();

			geometryBuilder.setMaterial(materialBlock.toIndex(material));

			geometryBuilder.setShape(shapeBlock.toIndex(shape));

			for (VectorXYZ v : path) {
				geometryBuilder.addPath(vector3dBlock.toIndex(v));
			}

			for (VectorXYZ v : upVectors) {
				geometryBuilder.addUpVectors(vector3dBlock.toIndex(v));
			}

			if (scaleFactors != null) {
				for (double scaleFactor : scaleFactors) {
					geometryBuilder.addScaleFactors(round(scaleFactor * 1000));
				}
			}

			if (options != null && options.contains(START_CAP)) {
				geometryBuilder.setStartCap(true);
			}

			if (options != null && options.contains(END_CAP)) {
				geometryBuilder.setEndCap(true);
			}

			currentExtrusionGeometries.add(geometryBuilder.build());

		}

		/** draw an instanced model */
		@Override
		public void drawModel(Model model, VectorXYZ position,
				double direction, Double height, Double width, Double length) {

			currentModelInstances.put(model, new InstanceParameters(
					position, direction, height, width, length));

		}

		public FrontendPbf.WorldObject build() {

			if (isEmpty()) {
				throw new IllegalStateException("a WorldObject needs geometry");
			}

			/* build the object's triangle geometries */

			List<TriangleGeometry> triangleGeometries = new ArrayList<TriangleGeometry>();

			for (Material material : currentTriangles.keySet()) {

				TriangleGeometry.Builder geometryBuilder = TriangleGeometry.newBuilder();

				geometryBuilder.setMaterial(materialBlock.toIndex(material));

				TriangleData triangleData = currentTriangles.get(material);

				/* write the vertices */

				for (VectorXYZ v : triangleData.getVertices()) {
					geometryBuilder.addVertices(vector3dBlock.toIndex(v));
				}

				/* write the texture coordinates */

				List<VectorXZ> texCoords = new ArrayList<VectorXZ>();

				for (int layer = 0; layer < triangleData.getTexCoordLists().size(); layer++) {

					// check if the tex coords can be calculated in the client
					if (material.getTextureDataList().get(layer).coordFunction != GLOBAL_X_Z) {

						// append the texture coordinates for this layer
						texCoords.addAll(triangleData.getTexCoordLists().get(layer));

					}

				}

				for (VectorXZ v : texCoords) {
					geometryBuilder.addTexCoords(vector2dBlock.toIndex(v));
				}

				/* build the geometry */

				triangleGeometries.add(geometryBuilder.build());

			}

			/* build the object's instance geometries */

			List<InstanceGeometry> instanceGeometries = new ArrayList<InstanceGeometry>();

			for (Model model : currentModelInstances.keySet()) {

				InstanceGeometry.Builder geometryBuilder = InstanceGeometry.newBuilder();

				geometryBuilder.setModel(modelBlock.toIndex(model));

				boolean allUnrotated = true;
				boolean allUnscaled = true;

				for (InstanceParameters instanceParams : currentModelInstances.get(model)) {

					geometryBuilder.addPosition(vector3dBlock.toIndex(instanceParams.position));

					int direction = (int)round(instanceParams.direction * 1000.0);
					geometryBuilder.addDirection(direction);
					allUnrotated &= (direction == 0);

					//FIXME: this assumes that 1 is the unscaled height, which happens to be true for trees
					double scaleDouble = instanceParams.height == null ? 1 : instanceParams.height;
					int scale = (int)round(scaleDouble * 1000.0);
					geometryBuilder.addScale(scale);
					allUnscaled &= (scale == 1000);

				}

				if (allUnrotated) {
					geometryBuilder.clearDirection();
				}

				if (allUnscaled) {
					geometryBuilder.clearScale();
				}

				instanceGeometries.add(geometryBuilder.build());

			}

			/* build the actual object */

			FrontendPbf.WorldObject.Builder objectBuilder = FrontendPbf.WorldObject.newBuilder();

			if (worldObject != null) {

				MapElement element = worldObject.getPrimaryMapElement();

				if (element != null) {

					if (element instanceof MapArea) {
						objectBuilder.setOsmId(((MapArea)element).getOsmObject().toString());
					} else if (element instanceof MapWaySegment) {
						objectBuilder.setOsmId(((MapWaySegment)element).getOsmWay().toString());
					} else if (element instanceof MapNode) {
						objectBuilder.setOsmId(((MapNode)element).getOsmNode().toString());
					}

				}

				objectBuilder.setTypeName(stringBlock.toIndex(worldObject.getClass().getSimpleName()));

			}

			objectBuilder.addAllTriangleGeometries(triangleGeometries);
			objectBuilder.addAllExtrusionGeometries(currentExtrusionGeometries);
			objectBuilder.addAllInstanceGeometries(instanceGeometries);

			return objectBuilder.build();

		}

		public boolean isEmpty() {
			return currentTriangles.isEmpty()
					&& currentExtrusionGeometries.isEmpty()
					&& currentModelInstances.isEmpty();
		}

	}

	/** prefix for the URL of texture files */
	private static final String TEXTURE_BASE_URL = "textures/";

	/** factor applied to coordinate values before rounding to integers */
	private final int COORD_PRECISION_FACTOR = 1000;

	private final OutputStream outputStream;
	private final AxisAlignedBoundingBoxXZ bbox;

	private final Block<VectorXYZ> vector3dBlock = new VectorBlock<VectorXYZ>();
	private final Block<VectorXZ> vector2dBlock = new VectorBlock<VectorXZ>();
	private final Block<String> stringBlock = new SimpleBlock<String>();
	private final Block<ShapeXZ> shapeBlock = new SimpleBlock<ShapeXZ>();
	private final Block<Material> materialBlock = new SimpleBlock<Material>();
	private final Block<Model> modelBlock = new SimpleBlock<Model>();

	private final List<FrontendPbf.WorldObject> objects = new ArrayList<FrontendPbf.WorldObject>();

	private final List<SimplePolygonXZ> waterAreas = new ArrayList<SimplePolygonXZ>();

	private WorldObjectBuilder currentObjectBuilder = new WorldObjectBuilder(null);

	/**
	 *
	 * @param outputStream
	 * @param bbox  the desired bounding box for the output.
	 *              Objects are part of the output if their center is inside this box.
	 */
	public FrontendPbfTarget(OutputStream outputStream, AxisAlignedBoundingBoxXZ bbox) {

		this.outputStream = outputStream;
		this.bbox = bbox;

		/* reserve index 0 for optional strings */

		stringBlock.toIndex("");

		/* initialize the vector blocks with frequently used values,
		 * to make sure these have low indices (= more compact varints) */

		//TODO implement, then check size differences

	}

	@Override
	public Class<RenderableToModelTarget> getRenderableType() {
		return RenderableToModelTarget.class;
	}

	@Override
	public void render(RenderableToModelTarget renderable) {
		renderable.renderTo(this);
	}

	@Override
	public void beginObject(WorldObject object) {

		finishCurrentObject();

		/* start a new object */

		currentObjectBuilder = new WorldObjectBuilder(object);

	}

	@Override
	public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {
		currentObjectBuilder.drawTriangles(material, triangles, texCoordLists);
	}

	@Override
	public void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {
		currentObjectBuilder.drawTrianglesWithNormals(material, triangles, texCoordLists);
	}

	@Override
	public void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path, List<VectorXYZ> upVectors,
			List<Double> scaleFactors, List<List<VectorXZ>> texCoordLists, EnumSet<ExtrudeOption> options) {
		currentObjectBuilder.drawExtrudedShape(material, shape, path, upVectors, scaleFactors, texCoordLists, options);
	}

	@Override
	public void drawModel(Model model, VectorXYZ position,
			double direction, Double height, Double width, Double length) {
		currentObjectBuilder.drawModel(model, position, direction, height, width, length);
	}

	private Shape convertShape(ShapeXZ s) {

		Shape.Builder shapeBuilder = Shape.newBuilder();

		if (s instanceof CircleXZ) {

			CircleXZ circle = (CircleXZ) s;

			shapeBuilder.setType(ShapeType.CIRCLE);

			shapeBuilder.addParameters(vector2dBlock.toIndex(circle.getCenter()));
			shapeBuilder.addParameters(round(circle.getRadius() * 1000));

		} else if (s instanceof PolygonShapeXZ) {

			shapeBuilder.setType(ShapeType.POLYGON);

			for (int i = 0; i < s.getVertexList().size() - 1; i++) { //omit the duplicated vector
				shapeBuilder.addParameters(vector2dBlock.toIndex(s.getVertexList().get(i)));
			}

		} else {

			shapeBuilder.setType(ShapeType.POLYLINE);

			for (VectorXZ v : s.getVertexList()) {
				shapeBuilder.addParameters(vector2dBlock.toIndex(v));
			}

		}

		return shapeBuilder.build();

	}

	private FrontendPbf.Material convertMaterial(Material material) {

		FrontendPbf.Material.Builder materialBuilder = FrontendPbf.Material.newBuilder();

		materialBuilder.setAmbientR(material.ambientColor().getRed());
		materialBuilder.setAmbientG(material.ambientColor().getGreen());
		materialBuilder.setAmbientB(material.ambientColor().getBlue());

		materialBuilder.setDiffuseR(material.diffuseColor().getRed());
		materialBuilder.setDiffuseG(material.diffuseColor().getGreen());
		materialBuilder.setDiffuseB(material.diffuseColor().getBlue());

		Color specularColor = Material.multiplyColor(
				material.getColor(), material.getSpecularFactor());

		materialBuilder.setSpecularR(specularColor.getRed());
		materialBuilder.setSpecularG(specularColor.getGreen());
		materialBuilder.setSpecularB(specularColor.getBlue());

		materialBuilder.setShininess(material.getShininess());

		switch (material.getTransparency()) {
			case TRUE: materialBuilder.setTransparency(Transparency.TRUE); break;
			case BINARY: materialBuilder.setTransparency(Transparency.BINARY); break;
			case FALSE: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported transparency: " + material.getTransparency());
		}

		if (material.getShadow() == Shadow.FALSE) {
			materialBuilder.setCastShadow(false);
		}

		for (TextureData textureData : material.getTextureDataList()) {
			materialBuilder.addTextureLayer(convertTextureLayer(textureData));
		}

		return materialBuilder.build();

	}

	private TextureLayer convertTextureLayer(TextureData textureData) {

		TextureLayer.Builder layerBuilder = TextureLayer.newBuilder();

		layerBuilder.setTextureURL(TEXTURE_BASE_URL + textureData.file.getName());

		switch (textureData.wrap) {
			case CLAMP:
			case CLAMP_TO_BORDER: layerBuilder.setWrap(Wrap.CLAMP); break;
			case REPEAT: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported transparency: " + textureData.wrap);
		}

		layerBuilder.setColorable(textureData.colorable);

		layerBuilder.setTextureHeight((int)round(textureData.height * 1000));
		layerBuilder.setTextureWidth((int)round(textureData.width * 1000));

		if (textureData.coordFunction == GLOBAL_X_Z) {
			//TODO: GLOBAL_X_Z could also be the module's default rather than a config setting
			layerBuilder.setTexCoordFunction(TexCoordFunction.GLOBAL_X_Z);
		}

		return layerBuilder.build();

	}

	private FrontendPbf.WorldObject convertModel(Model m) {

		WorldObjectBuilder objectBuilder = new WorldObjectBuilder(null);

		m.render(objectBuilder, NULL_VECTOR, 0, null, null, null);

		return objectBuilder.build();

	}

	/**
	 * completes the {@link FrontendPbf.WorldObject} for which information is currently
	 * being collected in {@link #currentObjectBuilder}
	 */
	private void finishCurrentObject() {

		/* special handling for water areas */

		if (USE_FLOOR_PLATE) {
			if (isWater(currentObjectBuilder.worldObject)) {

				SimplePolygonXZ outline = ((TerrainBoundaryWorldObject) currentObjectBuilder.worldObject)
						.getOutlinePolygonXZ();

				if (outline != null) {
					waterAreas.add(outline);
				}

				return;

			}
		}

		/* check for reasons not to build the object */

		boolean ignoreCurrentObject = currentObjectBuilder.isEmpty();

		if (currentObjectBuilder.worldObject != null
				&& currentObjectBuilder.worldObject.getPrimaryMapElement() != null) {

			MapElement mapElement = currentObjectBuilder.worldObject.getPrimaryMapElement();

			VectorXZ center = null;

			if (mapElement instanceof MapNode) {
				center = ((MapNode) mapElement).getPos();
			} else if (mapElement instanceof MapWaySegment) {
				center = ((MapWaySegment) mapElement).getCenter();
			} else if (mapElement instanceof MapArea) {
				center = ((MapArea) mapElement).getOuterPolygon().getCenter();
			}

			ignoreCurrentObject |= !bbox.contains(center);

			ignoreCurrentObject |= USE_FLOOR_PLATE
					&& mapElement.getTags().contains(EMPTY_SURFACE_TAG);

			ignoreCurrentObject |= USE_FLOOR_PLATE && isWater(currentObjectBuilder.worldObject);

		}

		/* build the current object */

		if (!ignoreCurrentObject) {
			objects.add(currentObjectBuilder.build());
		}

	}

	/**
	 * implements the #USE_FLOOR_PLATE option
	 */
	private FrontendPbf.WorldObject buildFloorPlate() throws InvalidGeometryException {

		WorldObjectBuilder builder = new WorldObjectBuilder(null);

		for (PolygonWithHolesXZ poly : CAGUtil.subtractPolygons(bbox.polygonXZ(), waterAreas)) {

			List<TriangleXZ> triangles = TriangulationUtil.triangulate(poly);

			List<TriangleXYZ> trianglesXYZ = new ArrayList<TriangleXYZ>(triangles.size());

			for (TriangleXZ triangle : triangles) {
				trianglesXYZ.add(triangle.xyz(FLOOR_PLATE_Y));

			}

			builder.drawTriangles(TERRAIN_DEFAULT, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, TERRAIN_DEFAULT, GLOBAL_X_Z));

		}

		return builder.build();

	}

	private static final boolean isWater(WorldObject object) {

		return object instanceof Water
			|| object instanceof AreaFountain
			|| object instanceof RiverJunction
			|| object instanceof Waterway
			|| object instanceof Pool;

	}

	@Override
	public void finish() {

		/* build the last object */

		finishCurrentObject();

		/* create a floor plate (if that option is active */

		if (USE_FLOOR_PLATE) {
			try {
				objects.add(buildFloorPlate());
			} catch (InvalidGeometryException e) {
				System.err.println("Error while producing the floor plate: " + e);
			}
		}

		/* build the blocks */

		//model block needs to be first, because it adds content (e.g. vectors) to the other blocks
		ModelBlock.Builder modelBlockBuilder = ModelBlock.newBuilder();

		for (Model m : modelBlock.getElements()) {
			modelBlockBuilder.addModels(convertModel(m));
		}

		Vector3dBlock.Builder vector3dBlockBuilder = Vector3dBlock.newBuilder();

		for (VectorXYZ v : vector3dBlock.getElements()) {
			vector3dBlockBuilder.addCoords(round(v.x*COORD_PRECISION_FACTOR));
			vector3dBlockBuilder.addCoords(round(v.y*COORD_PRECISION_FACTOR));
			vector3dBlockBuilder.addCoords(round(v.z*COORD_PRECISION_FACTOR));
		}

		Vector2dBlock.Builder vector2dBlockBuilder = Vector2dBlock.newBuilder();

		for (VectorXZ v : vector2dBlock.getElements()) {
			vector2dBlockBuilder.addCoords(round(v.x*COORD_PRECISION_FACTOR));
			vector2dBlockBuilder.addCoords(round(v.z*COORD_PRECISION_FACTOR));
		}

		StringBlock.Builder stringBlockBuilder = StringBlock.newBuilder();
		stringBlockBuilder.addAllStrings(stringBlock.getElements());

		ShapeBlock.Builder shapeBlockBuilder = ShapeBlock.newBuilder();

		for (ShapeXZ s : shapeBlock.getElements()) {
			shapeBlockBuilder.addShapes(convertShape(s));
		}

		MaterialBlock.Builder materialBlockBuilder = MaterialBlock.newBuilder();

		for (Material m : materialBlock.getElements()) {
			materialBlockBuilder.addMaterials(convertMaterial(m));
		}

		/* build the tile */

		Tile.Builder tileBuilder = Tile.newBuilder();

		tileBuilder.setVector3DBlock(vector3dBlockBuilder);
		tileBuilder.setVector2DBlock(vector2dBlockBuilder);
		tileBuilder.setStringBlock(stringBlockBuilder);
		tileBuilder.setShapeBlock(shapeBlockBuilder);
		tileBuilder.setMaterialBlock(materialBlockBuilder);
		tileBuilder.setModelBlock(modelBlockBuilder);

		tileBuilder.addAllObjects(objects);

		/* write the protobuf */

		try {
			tileBuilder.build().writeTo(outputStream);
		} catch (IOException e) {
			//TODO proper error handling
			throw new Error(e);
		}

	}

	public static void writePbfFile(File outputFile, MapData mapData) throws IOException {

		FileOutputStream output = null;

		try {

			output = new FileOutputStream(outputFile);

			writePbfStream(output, mapData);

		} finally {
			if (output != null) {
				output.close();
			}
		}

	}

	public static void writePbfStream(OutputStream output, MapData mapData) throws IOException {

		FrontendPbfTarget target = new FrontendPbfTarget(output, mapData.getBoundary());

		TargetUtil.renderWorldObjects(target, mapData, false);

		target.finish();

	}

}
