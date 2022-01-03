package org.osm2world.core.target.frontend_pbf;

import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.binarySearch;
import static org.osm2world.core.map_data.creation.EmptyTerrainBuilder.EMPTY_SURFACE_VALUE;
import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.core.target.common.ExtrudeOption.*;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseDirection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.triangulate.ConstraintEnforcementException;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.Tag;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.MeshTarget;
import org.osm2world.core.target.common.material.ImageFileTexture;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Shadow;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.model.ExternalResourceModel;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.texcoord.GlobalXZTexCoordFunction;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Animation;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Animation.AnimationType;
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
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector2dBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector3dBlock;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.color.LColor;
import org.osm2world.core.world.data.NodeModelInstance;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.BarrierModule.BollardRow;
import org.osm2world.core.world.modules.BarrierModule.CylinderBollard;
import org.osm2world.core.world.modules.BarrierModule.HandRail;
import org.osm2world.core.world.modules.BicycleParkingModule.BicycleStands;
import org.osm2world.core.world.modules.PowerModule.WindTurbine;
import org.osm2world.core.world.modules.StreetFurnitureModule.Bench;
import org.osm2world.core.world.modules.StreetFurnitureModule.GritBin;
import org.osm2world.core.world.modules.StreetFurnitureModule.PostBox;
import org.osm2world.core.world.modules.StreetFurnitureModule.VendingMachineVice;
import org.osm2world.core.world.modules.StreetFurnitureModule.WasteBasket;
import org.osm2world.core.world.modules.TreeModule.Forest;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class FrontendPbfTarget extends MeshTarget {

	/**
	 * whether empty terrain should be faked as a big rectangle slightly below other ground-level geometries.
	 * This reduces the number of triangles used for empty terrain.
	 */
	private final boolean USE_FLOOR_PLATE = true;

	private final double FLOOR_PLATE_Y = -0.03;

	private final List<Class<? extends WorldObject>> LOD_2_FEATURES = asList(HandRail.class, BicycleStands.class,
			WasteBasket.class, VendingMachineVice.class, PostBox.class, Bench.class, GritBin.class,
			BollardRow.class);

	private final List<Class<? extends Model>> LOD_2_MODEL_FEATURES = asList(CylinderBollard.class);

	/**
	 * materials which default to being shadowless
	 *
	 * These can only be treated as shadowless because the disabled terrain makes them (almost) always face straight up,
	 * i.e. triangle's normal is roughly equal to Y_UNIT. With terrain enabled, this wouldn't necessarily be the case.
	 * TODO: find a solution (e.g. check triangle normal) that works with terrain and can be used for all targets
	 */
	private static final List<Material> DEFAULT_SHADOWLESS_MATERIALS = asList(ASPHALT, CARPET, EARTH, GRASS,
			GRASS_PAVER, GRAVEL, HELIPAD_MARKING, ICE, PAVING_STONE, PEBBLESTONE, PITCH_BEACHVOLLEYBALL, PITCH_SOCCER,
			PITCH_TENNIS_ASPHALT, PITCH_TENNIS_CLAY, PITCH_TENNIS_GRASS, PITCH_TENNIS_SINGLES_ASPHALT,
			PITCH_TENNIS_SINGLES_CLAY, PITCH_TENNIS_SINGLES_GRASS, RAILWAY, RAIL_BALLAST, RED_ROAD_MARKING,
			ROAD_MARKING, ROAD_MARKING_ARROW_RIGHT, ROAD_MARKING_ARROW_RIGHT_LEFT, ROAD_MARKING_ARROW_THROUGH,
			ROAD_MARKING_ARROW_THROUGH_RIGHT, ROAD_MARKING_CROSSING, ROAD_MARKING_DASHED, ROAD_MARKING_ZEBRA, ROCK,
			ROOF_DEFAULT, RUNWAY_CENTER_MARKING, SAND, SCREE, SCRUB, SETT, SKYBOX, SNOW, SOLAR_PANEL, TARTAN,
			TAXIWAY_CENTER_MARKING, TERRAIN_DEFAULT, UNHEWN_COBBLESTONE, WATER);

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

	/**
	 * internally used {@link Target} implementation that collects all geometry for a single object
	 */
	private class WorldObjectBuilder {

		private final WorldObject worldObject;

		private final List<Mesh> currentMeshes = new ArrayList<>();
		private final Multimap<Model, InstanceParameters> currentModelInstances = HashMultimap.create();

		/**
		 * @param worldObject  the OSM2World {@link WorldObject} that will provide the id and type,
		 *                     can be null.
		 */
		public WorldObjectBuilder(WorldObject worldObject) {
			this.worldObject = worldObject;
		}

		public void drawMesh(Mesh mesh) {
			currentMeshes.add(mesh);
		}

		/** draw an instanced model */
		public void drawModel(Model model, VectorXYZ position,
				double direction, Double height, Double width, Double length) {
			InstanceParameters instanceParams = new InstanceParameters(position, direction, height, width, length);
			currentModelInstances.put(model, instanceParams);
		}

		public FrontendPbf.WorldObject build() {

			if (isEmpty()) {
				throw new IllegalStateException("a WorldObject needs geometry");
			}

			MapElement element = worldObject == null ? null : worldObject.getPrimaryMapElement();

			/* check for 3DMR ids */

			String id3DMR = element == null ? null : element.getTags().getValue("3dmr");

			if (id3DMR != null) {

				currentMeshes.clear();
				currentModelInstances.clear();

				Model model3DMR = new ExternalResourceModel("3dmr:" + id3DMR);

				VectorXZ center = null;

				if (element instanceof MapNode) {
					center = ((MapNode) element).getPos();
				} else if (element instanceof MapWaySegment) {
					center = ((MapWaySegment) element).getCenter();
				} else if (element instanceof MapArea) {
					center = ((MapArea) element).getOuterPolygon().getCenter();
				}

				double direction = parseDirection(element.getTags(), 0);

				currentModelInstances.put(model3DMR, new InstanceParameters(center.xyz(0), direction, null, null, null));

			}

			/* build the object's triangle and extrusion geometries */

			List<FrontendPbf.TriangleGeometry> triangleGeometries = new ArrayList<>();
			List<FrontendPbf.ExtrusionGeometry> extrusionGeometries = new ArrayList<>();

			for (Mesh mesh : currentMeshes) {

				if (mesh.geometry instanceof ExtrusionGeometry) {
					extrusionGeometries.add(buildExtrusionGeometry(mesh.material, (ExtrusionGeometry) mesh.geometry));
				} else {
					triangleGeometries.add(buildTriangleGeometry(mesh.material, mesh.geometry.asTriangles()));
				}

			}

			/* build the object's instance geometries */

			List<InstanceGeometry> instanceGeometries = new ArrayList<InstanceGeometry>();

			for (Model model : currentModelInstances.keySet()) {

				InstanceGeometry.Builder geometryBuilder = InstanceGeometry.newBuilder();

				if (model instanceof ExternalResourceModel) {
					geometryBuilder.setResourceIdentifier(((ExternalResourceModel)model).getResourceIdentifier());
				} else {
					geometryBuilder.setModel(modelBlock.toIndex(model));
				}

				boolean allUnrotated = true;
				boolean allUnscaled = true;

				for (InstanceParameters instanceParams : currentModelInstances.get(model)) {

					geometryBuilder.addPosition(round(instanceParams.position.x * COORD_PRECISION_FACTOR));
					geometryBuilder.addPosition(round(instanceParams.position.y * COORD_PRECISION_FACTOR));
					geometryBuilder.addPosition(round(instanceParams.position.z * COORD_PRECISION_FACTOR));

					int direction = (int)round(instanceParams.direction * 1000.0);
					geometryBuilder.addDirection(direction);
					allUnrotated &= (direction == 0);

					// this assumes that 1 is the unscaled height, which is why convertModel passes 1 to Model.render
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

				if (model == WindTurbine.ROTOR) {

					// hard-coded animation of wind turbine rotors
					Animation.Builder animationBuilder = Animation.newBuilder();
					animationBuilder.setType(AnimationType.ROTATION_X);
					animationBuilder.setRunsPerSecond(0.3);
					geometryBuilder.setAnimation(animationBuilder );

				}

				instanceGeometries.add(geometryBuilder.build());

			}

			/* build the actual object */

			FrontendPbf.WorldObject.Builder objectBuilder = FrontendPbf.WorldObject.newBuilder();

			if (worldObject != null) {

				if (element != null) {
					objectBuilder.setOsmId(element.getElementWithId().toString());
				}

				objectBuilder.setTypeName(stringBlock.toIndex(worldObject.getClass().getSimpleName()));

				if (LOD_2_FEATURES.contains(worldObject.getClass())
						|| (worldObject instanceof NodeModelInstance
								&& LOD_2_MODEL_FEATURES.contains(((NodeModelInstance)worldObject).model.getClass()))) {
					objectBuilder.setMinLod(2);
				}

			}

			objectBuilder.addAllTriangleGeometries(triangleGeometries);
			objectBuilder.addAllExtrusionGeometries(extrusionGeometries);
			objectBuilder.addAllInstanceGeometries(instanceGeometries);

			return objectBuilder.build();

		}


		private FrontendPbf.TriangleGeometry buildTriangleGeometry(Material material, TriangleGeometry geom) {

			FrontendPbf.TriangleGeometry.Builder geometryBuilder = FrontendPbf.TriangleGeometry.newBuilder();

			geometryBuilder.setMaterial(materialBlock.toIndex(material));

			/* write the vertices */

			for (VectorXYZ v : geom.vertices()) {
				geometryBuilder.addVertices(vector3dBlock.toIndex(v));
			}

			/* write the texture coordinates */

			List<VectorXZ> texCoords = new ArrayList<>();

			for (int layer = 0; layer < geom.texCoordFunctions.size(); layer++) {

				// check if the tex coords can be calculated in the client
				if (!(geom.texCoordFunctions.get(layer) instanceof GlobalXZTexCoordFunction)) {

					// append the texture coordinates for this layer
					texCoords.addAll(geom.texCoords().get(layer));

				}

			}

			for (VectorXZ v : texCoords) {
				geometryBuilder.addTexCoords(vector2dBlock.toIndex(v));
			}

			/* build the geometry */

			return geometryBuilder.build();

		}

		private FrontendPbf.ExtrusionGeometry buildExtrusionGeometry(Material material, ExtrusionGeometry geom) {

			FrontendPbf.ExtrusionGeometry.Builder geometryBuilder = FrontendPbf.ExtrusionGeometry.newBuilder();

			geometryBuilder.setMaterial(materialBlock.toIndex(material));

			geometryBuilder.setShape(shapeBlock.toIndex(geom.shape));

			for (VectorXYZ v : geom.path) {
				geometryBuilder.addPath(vector3dBlock.toIndex(v));
			}

			if (geom.upVectors != null) {
				for (VectorXYZ v : geom.upVectors) {
					geometryBuilder.addUpVectors(vector3dBlock.toIndex(v));
				}
			}

			if (geom.scaleFactors != null) {
				for (double scaleFactor : geom.scaleFactors) {
					geometryBuilder.addScaleFactors(round(scaleFactor * 1000));
				}
			}

			if (geom.options != null && geom.options.contains(START_CAP)) {
				geometryBuilder.setStartCap(true);
			}

			if (geom.options != null && geom.options.contains(END_CAP)) {
				geometryBuilder.setEndCap(true);
			}

			return geometryBuilder.build();

		}

		public boolean isEmpty() {
			return currentMeshes.isEmpty() && currentModelInstances.isEmpty();
		}

	}

	/** prefix for the URL of texture files */
	private static final String TEXTURE_BASE_URL = "textures/";

	/** factor applied to coordinate values before rounding to integers */
	private final int COORD_PRECISION_FACTOR = 1000;

	private static final Tag EMPTY_SURFACE_TAG = new Tag("surface", EMPTY_SURFACE_VALUE);

	private final OutputStream outputStream;
	private final AxisAlignedRectangleXZ bbox;
	private final MapProjection projection;

	private final Block<VectorXYZ> vector3dBlock = new VectorBlock<>();
	private final Block<VectorXZ> vector2dBlock = new VectorBlock<>();
	private final Block<String> stringBlock = new SimpleBlock<>();
	private final Block<ShapeXZ> shapeBlock = new SimpleBlock<>();
	private final Block<Material> materialBlock = new SimpleBlock<>();
	private final Block<Model> modelBlock = new SimpleBlock<>();

	private final List<FrontendPbf.WorldObject> objects = new ArrayList<>();

	private WorldObjectBuilder currentObjectBuilder = new WorldObjectBuilder(null);

	/**
	 * Creates a {@link FrontendPbfTarget}. Writing only completes once {@link #finish()} is called.
	 *
	 * @param outputStream  the stream to write protobuf data to
	 * @param bbox  the desired bounding box for the output.
	 *              Objects are part of the output if their center is inside this box.
	 */
	public FrontendPbfTarget(OutputStream outputStream, AxisAlignedRectangleXZ bbox, MapProjection projection) {

		this.outputStream = outputStream;
		this.bbox = bbox;
		this.projection = projection;

		/* reserve index 0 for optional strings */

		stringBlock.toIndex("");

		/* initialize the vector blocks with frequently used values,
		 * to make sure these have low indices (= more compact varints) */

		//TODO implement, then check size differences

	}

	@Override
	public void beginObject(WorldObject object) {

		finishCurrentObject();

		/* start a new object */

		currentObjectBuilder = new WorldObjectBuilder(object);

	}

	@Override
	public void drawMesh(Mesh mesh) {
		currentObjectBuilder.drawMesh(mesh);
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

			for (int i = 0; i < s.vertices().size() - 1; i++) { //omit the duplicated vector
				shapeBuilder.addParameters(round(s.vertices().get(i).x * COORD_PRECISION_FACTOR));
				shapeBuilder.addParameters(round(s.vertices().get(i).z * COORD_PRECISION_FACTOR));
			}

		} else {

			shapeBuilder.setType(ShapeType.POLYLINE);

			for (VectorXZ v : s.vertices()) {
				shapeBuilder.addParameters(round(v.x * COORD_PRECISION_FACTOR));
				shapeBuilder.addParameters(round(v.z * COORD_PRECISION_FACTOR));
			}

		}

		return shapeBuilder.build();

	}

	private FrontendPbf.Material convertMaterial(Material material) {

		FrontendPbf.Material.Builder materialBuilder = FrontendPbf.Material.newBuilder();

		float[] baseColorFactor = material.getNumTextureLayers() == 0
				? new float[] {1f, 1f, 1f}
				: material.getTextureLayers().get(0).baseColorFactor(LColor.fromAWT(material.getColor()), 1f);

		materialBuilder.setBaseColorR(round(baseColorFactor[0] * 255));
		materialBuilder.setBaseColorG(round(baseColorFactor[1] * 255));
		materialBuilder.setBaseColorB(round(baseColorFactor[2] * 255));

		switch (material.getTransparency()) {
			case TRUE: materialBuilder.setTransparency(Transparency.TRUE); break;
			case BINARY: materialBuilder.setTransparency(Transparency.BINARY); break;
			case FALSE: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported transparency: " + material.getTransparency());
		}

		if (material.getShadow() == Shadow.FALSE || DEFAULT_SHADOWLESS_MATERIALS.contains(material)) {
			materialBuilder.setCastShadow(false);
		}

		if (material.isDoubleSided()) {
			materialBuilder.setDoubleSided(true);
		}

		for (org.osm2world.core.target.common.material.TextureLayer textureLayer : material.getTextureLayers()) {
			materialBuilder.addTextureLayer(convertTextureLayer(textureLayer));
		}

		return materialBuilder.build();

	}

	private TextureLayer convertTextureLayer(org.osm2world.core.target.common.material.TextureLayer textureLayer) {

		TextureLayer.Builder layerBuilder = TextureLayer.newBuilder();

		TextureData baseColorTexture = textureLayer.baseColorTexture;

		if (baseColorTexture instanceof ImageFileTexture) {
			layerBuilder.setBaseColorTextureURI(TEXTURE_BASE_URL + ((ImageFileTexture)baseColorTexture).getFile().getName());
		} else {
			layerBuilder.setBaseColorTextureURI(baseColorTexture.getDataUri());
		}

		if (textureLayer.ormTexture instanceof ImageFileTexture) {
			layerBuilder.setOrmTextureURI(TEXTURE_BASE_URL + ((ImageFileTexture)textureLayer.ormTexture).getFile().getName());
		} else if (textureLayer.ormTexture != null) {
			layerBuilder.setOrmTextureURI(textureLayer.ormTexture.getDataUri());
		}

		if (textureLayer.normalTexture instanceof ImageFileTexture) {
			layerBuilder.setNormalTextureURI(TEXTURE_BASE_URL + ((ImageFileTexture)textureLayer.normalTexture).getFile().getName());
		} else if (textureLayer.normalTexture != null) {
			layerBuilder.setNormalTextureURI(textureLayer.normalTexture.getDataUri());
		}

		if (textureLayer.displacementTexture instanceof ImageFileTexture) {
			layerBuilder.setDisplacementTextureURI(TEXTURE_BASE_URL + ((ImageFileTexture)textureLayer.displacementTexture).getFile().getName());
		} else if (textureLayer.displacementTexture != null) {
			layerBuilder.setDisplacementTextureURI(textureLayer.displacementTexture.getDataUri());
		}

		switch (textureLayer.baseColorTexture.wrap) {
			case CLAMP:
			case CLAMP_TO_BORDER: layerBuilder.setWrap(Wrap.CLAMP); break;
			case REPEAT: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported wrap: " + baseColorTexture.wrap);
		}

		layerBuilder.setColorable(textureLayer.colorable);

		layerBuilder.setTextureHeight((int)round(textureLayer.baseColorTexture.height * 1000));
		layerBuilder.setTextureWidth((int)round(textureLayer.baseColorTexture.width * 1000));

		if (textureLayer.baseColorTexture.coordFunction instanceof GlobalXZTexCoordFunction) {
			//TODO: GLOBAL_X_Z could also be the module's default rather than a config setting
			layerBuilder.setTexCoordFunction(TexCoordFunction.GLOBAL_X_Z);
		}

		return layerBuilder.build();

	}

	private FrontendPbf.WorldObject convertModel(Model m) {
		WorldObjectBuilder objectBuilder = new WorldObjectBuilder(null);
		m.buildMeshes(new InstanceParameters(NULL_VECTOR, 0.0, 1.0, null, null)).forEach(it -> objectBuilder.drawMesh(it));
		return objectBuilder.build();
	}

	/**
	 * completes the {@link FrontendPbf.WorldObject} for which information is currently
	 * being collected in {@link #currentObjectBuilder}
	 */
	private void finishCurrentObject() {

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

			// ignore objects (mostly) outside the bbox, except forests (which are filtered on a per-tree level)
			ignoreCurrentObject |= !bbox.contains(center) && !(currentObjectBuilder.worldObject instanceof Forest);

			ignoreCurrentObject |= USE_FLOOR_PLATE
					&& mapElement.getTags().contains(EMPTY_SURFACE_TAG);

		}

		/* build the current object */

		if (!ignoreCurrentObject) {
			objects.add(currentObjectBuilder.build());
		}

	}

	/**
	 * implements the {@link #USE_FLOOR_PLATE} option
	 */
	private FrontendPbf.WorldObject buildFloorPlate() throws InvalidGeometryException {

		MeshTarget target = new MeshTarget();

		Collection<TriangleXZ> triangles = bbox.getTriangulation();

		List<TriangleXYZ> trianglesXYZ = new ArrayList<>(triangles.size());

		for (TriangleXZ triangle : triangles) {
			trianglesXYZ.add(triangle.xyz(FLOOR_PLATE_Y));
		}

		target.drawTriangles(TERRAIN_DEFAULT, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, TERRAIN_DEFAULT, GLOBAL_X_Z));

		WorldObjectBuilder builder = new WorldObjectBuilder(null);
		target.getMeshes().forEach(it -> builder.drawMesh(it));
		return builder.build();

	}

	@Override
	public void finish() {

		/* build the last object */

		finishCurrentObject();

		/* create a floor plate (if that option is active */

		if (USE_FLOOR_PLATE) {
			try {
				objects.add(buildFloorPlate());
			} catch (InvalidGeometryException | IllegalStateException | TopologyException | ConstraintEnforcementException e) {
				System.err.println("Error while producing the floor plate: " + e);
			}
		}

		/* build the blocks */

		//model block needs to be first, because it adds content (e.g. vectors) to the other blocks
		ModelBlock.Builder modelBlockBuilder = ModelBlock.newBuilder();

		FaultTolerantIterationUtil.forEach(modelBlock.getElements(), (Model m) -> {
			modelBlockBuilder.addModels(convertModel(m));
		});

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

	public static void writePbfFile(File outputFile, MapData mapData,
			AxisAlignedRectangleXZ bbox, MapProjection projection) throws IOException {

		FileOutputStream output = null;

		try {

			output = new FileOutputStream(outputFile);

			writePbfStream(output, mapData, bbox, projection);

		} finally {
			if (output != null) {
				output.close();
			}
		}

	}

	public static void writePbfStream(OutputStream output, MapData mapData,
			AxisAlignedRectangleXZ bbox, MapProjection projection) throws IOException {

		if (bbox == null) {
			bbox = mapData.getBoundary();
		}

		FrontendPbfTarget target = new FrontendPbfTarget(output, bbox, projection);

		TargetUtil.renderWorldObjects(target, mapData, false);

		target.finish();

	}

}
