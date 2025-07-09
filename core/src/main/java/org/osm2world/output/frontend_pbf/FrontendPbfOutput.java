package org.osm2world.output.frontend_pbf;

import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.binarySearch;
import static java.util.Collections.emptyList;
import static org.osm2world.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.output.common.ExtrudeOption.END_CAP;
import static org.osm2world.output.common.ExtrudeOption.START_CAP;
import static org.osm2world.scene.material.Materials.*;
import static org.osm2world.scene.mesh.MeshStore.*;
import static org.osm2world.scene.mesh.MeshStore.MergeMeshes.MergeOption.*;
import static org.osm2world.scene.mesh.MeshStore.ReplaceTexturesWithAtlas.generateTextureAtlasGroup;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseDirection;

import java.io.*;
import java.util.*;

import javax.annotation.Nullable;

import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.triangulate.ConstraintEnforcementException;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapRelationElement;
import org.osm2world.map_data.data.MapWay;
import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.output.common.MeshOutput;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.common.compression.CompressionUtil;
import org.osm2world.output.frontend_pbf.FrontendPbf.*;
import org.osm2world.output.frontend_pbf.FrontendPbf.Animation.AnimationType;
import org.osm2world.output.frontend_pbf.FrontendPbf.Material.TextureLayer;
import org.osm2world.output.frontend_pbf.FrontendPbf.Material.TextureLayer.TexCoordFunction;
import org.osm2world.output.frontend_pbf.FrontendPbf.Material.TextureLayer.Wrap;
import org.osm2world.output.frontend_pbf.FrontendPbf.Material.Transparency;
import org.osm2world.output.frontend_pbf.FrontendPbf.Shape.ShapeType;
import org.osm2world.output.gltf.GltfModel;
import org.osm2world.scene.color.LColor;
import org.osm2world.scene.material.ImageFileTexture;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Shadow;
import org.osm2world.scene.material.RuntimeTexture;
import org.osm2world.scene.material.TextureData;
import org.osm2world.scene.mesh.*;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.MeshStore.ReplaceTexturesWithAtlas.TextureAtlasGroup;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.scene.model.*;
import org.osm2world.scene.texcoord.GlobalXZTexCoordFunction;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.BarrierModule.BollardRow;
import org.osm2world.world.modules.BarrierModule.HandRail;
import org.osm2world.world.modules.PowerModule.WindTurbine;
import org.osm2world.world.modules.StreetFurnitureModule.*;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Writes experimental Protobuf tiles in a custom format for WebGL rendering.
 */
public class FrontendPbfOutput extends MeshOutput {

	/**
	 * whether gaps in the terrain should be concealed with a big rectangle slightly below other ground-level geometries
	 */
	private final boolean USE_FLOOR_PLATE = true;

	private final double FLOOR_PLATE_Y = -0.03;

	/**
	 * whether some textures should be combined into a texture atlas.
	 * However, if textures could otherwise be referenced and shared between tiles, it greatly increases tile size.
	 * For {@link RuntimeTexture}s, this isn't a problem because they cannot be shared between tiles anyway.
	 */
	private static final TextureAtlasMode USE_TEXTURE_ATLAS = TextureAtlasMode.RUNTIME_ONLY;
	private enum TextureAtlasMode { ALWAYS, RUNTIME_ONLY, NEVER }

	private final List<Class<? extends WorldObject>> LOD_2_FEATURES = asList(HandRail.class, WasteBasket.class,
			VendingMachine.class, PostBox.class, Bench.class, GritBin.class, BollardRow.class);

	/**
	 * materials which default to being shadowless.
	 * These can only be treated as shadowless because the disabled terrain makes them (almost) always face straight up,
	 * i.e. triangle's normal is roughly equal to Y_UNIT. With terrain enabled, this wouldn't necessarily be the case.
	 * TODO: find a solution (e.g. check triangle normal) that works with terrain and can be used for all outputs
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

		List<T> elements = new ArrayList<>();

		@Override
		public List<T> getElements() {
			return elements;
		}

		/** adds the element to the block if necessary, and returns its index */
		@Override
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

		List<T> elements = new ArrayList<>();
		List<Entry<T>> sortedEntrys = new ArrayList<>();

		@Override
		public List<T> getElements() {
			return elements;
		}

		/** adds the element to the block if necessary, and returns its index */
		@Override
		public int toIndex(T element) {

			int index = binarySearch(sortedEntrys, new Entry<>(element, -1));

			if (index >= 0) {
				return sortedEntrys.get(index).index;
			} else {

				elements.add(element);

				Entry<T> entry = new Entry<>(element, elements.size() - 1);
				sortedEntrys.add(-(index + 1), entry);

				return entry.index;

			}

		}

	}

	/** prefix for the URL of texture files */
	private static final String TEXTURE_BASE_URL = "textures/";

	/** factor applied to coordinate values before rounding to integers */
	private final int COORD_PRECISION_FACTOR = 1000;

	private final @Nullable File outputFile;
	private final OutputStream outputStream;
	private final Compression compression;
	private final AxisAlignedRectangleXZ bbox;

	private final Block<VectorXYZ> vector3dBlock = new VectorBlock<>();
	private final Block<VectorXZ> vector2dBlock = new VectorBlock<>();
	private final Block<String> stringBlock = new SimpleBlock<>();
	private final Block<ShapeXZ> shapeBlock = new SimpleBlock<>();
	private final Block<Material> materialBlock = new SimpleBlock<>();
	private final Block<Model> modelBlock = new SimpleBlock<>();

	private final Map<MeshMetadata, Multimap<Model, InstanceParameters>> modelInstancesByWO = new HashMap<>();

	/**
	 * Creates a {@link FrontendPbfOutput}. Writing only completes once {@link #finish()} is called.
	 *
	 * @param outputFile  the file to write protobuf data to
	 * @param bbox  the desired bounding box for the output.
	 *              Objects are part of the output if their center is inside this box.
	 */
	public FrontendPbfOutput(File outputFile, @Nullable Compression compression, AxisAlignedRectangleXZ bbox)
			throws FileNotFoundException {
		this(outputFile, new FileOutputStream(outputFile), compression, bbox);
	}

	/**
	 * variant of {@link #FrontendPbfOutput(File, Compression, AxisAlignedRectangleXZ)}
	 * which accepts arbitrary {@link OutputStream}s
	 *
	 * @param outputStream  the stream to write protobuf data to
	 */
	public FrontendPbfOutput(OutputStream outputStream, @Nullable Compression compression, AxisAlignedRectangleXZ bbox) {
		this(null, outputStream, compression, bbox);
	}

	private FrontendPbfOutput(@Nullable File outputFile, OutputStream outputStream, @Nullable Compression compression,
			AxisAlignedRectangleXZ bbox) {

		this.outputFile = outputFile;
		this.outputStream = outputStream;
		this.compression = compression != null ? compression : Compression.NONE;
		this.bbox = bbox;

		/* reserve index 0 for optional strings */

		stringBlock.toIndex("");

		/* initialize the vector blocks with frequently used values,
		 * to make sure these have low indices (= more compact varints) */

		//TODO implement, then check size differences

	}

	@Override
	public void drawModel(ModelInstance modelInstance) {

		if (!bbox.contains(modelInstance.params().position().xz())) return;

		MeshMetadata worldObjectMetadata = new MeshMetadata(currentWorldObject.getPrimaryMapElement().getElementWithId(),
				currentWorldObject.getClass());

		if (!modelInstancesByWO.containsKey(worldObjectMetadata)) {
			modelInstancesByWO.put(worldObjectMetadata, HashMultimap.create());
		}

		Multimap<Model, InstanceParameters> map = modelInstancesByWO.get(worldObjectMetadata);
		map.put(modelInstance.model(), modelInstance.params());

	}

	private static VectorXZ getCenter(MapRelationElement element) {
		if (element instanceof MapNode mapNode) {
			return mapNode.getPos();
		} else if (element instanceof MapWay mapWay) {
			PolylineXZ polyline = mapWay.getPolylineXZ();
			return polyline.pointAtOffset(polyline.getLength() / 2);
		} else if (element instanceof MapArea mapArea) {
			return mapArea.getOuterPolygon().getCenter();
		} else {
			throw new Error("Unknown element type " + element.getClass());
		}
	}

	private Shape convertShape(ShapeXZ s) {

		Shape.Builder shapeBuilder = Shape.newBuilder();

		if (s instanceof CircleXZ circle) {

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

		for (org.osm2world.scene.material.TextureLayer textureLayer : material.getTextureLayers()) {
			materialBuilder.addTextureLayer(convertTextureLayer(textureLayer));
		}

		return materialBuilder.build();

	}

	private TextureLayer convertTextureLayer(org.osm2world.scene.material.TextureLayer textureLayer) {

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
			case CLAMP: layerBuilder.setWrap(Wrap.CLAMP); break;
			case REPEAT: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported wrap: " + baseColorTexture.wrap);
		}

		layerBuilder.setColorable(textureLayer.colorable);

		layerBuilder.setTextureHeight((int)round(textureLayer.baseColorTexture.dimensions().height() * 1000));
		layerBuilder.setTextureWidth((int)round(textureLayer.baseColorTexture.dimensions().width() * 1000));

		if (textureLayer.baseColorTexture.coordFunction instanceof GlobalXZTexCoordFunction) {
			//TODO: GLOBAL_X_Z could also be the module's default rather than a config setting
			layerBuilder.setTexCoordFunction(TexCoordFunction.GLOBAL_X_Z);
		}

		return layerBuilder.build();

	}

	private FrontendPbf.WorldObject convertModel(Model m, @Nullable TextureAtlasGroup textureAtlasGroup) {
		InstanceParameters params = new InstanceParameters(NULL_VECTOR, 0.0, 1.0);
		List<Mesh> meshes = m.buildMeshes(params);
		if (textureAtlasGroup != null) {
			var tempMeshStore = new MeshStore(meshes, null);
			tempMeshStore = tempMeshStore.process(List.of(new ReplaceTexturesWithAtlas(textureAtlasGroup)));
			meshes = tempMeshStore.meshes();
		}
		return buildWorldObject(null, meshes, HashMultimap.create());
	}

	/**
	 * implements the {@link #USE_FLOOR_PLATE} option
	 */
	private FrontendPbf.WorldObject buildFloorPlate() throws InvalidGeometryException {

		var output = new MeshOutput();

		Collection<TriangleXZ> triangles = bbox.getTriangulation();

		List<TriangleXYZ> trianglesXYZ = new ArrayList<>(triangles.size());

		for (TriangleXZ triangle : triangles) {
			trianglesXYZ.add(triangle.xyz(FLOOR_PLATE_Y));
		}

		output.drawTriangles(TERRAIN_DEFAULT, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, TERRAIN_DEFAULT, GLOBAL_X_Z));

		return buildWorldObject(null, output.getMeshes(), HashMultimap.create());

	}

	private List<FrontendPbf.WorldObject> buildWorldObjects(@Nullable MeshMetadata worldObjectMetadata,
			Collection<Mesh> meshes, Multimap<Model, InstanceParameters> modelInstances) {

		List<FrontendPbf.WorldObject> result = new ArrayList<>();

		for (int minLod = 0; minLod <= 4; minLod++) {
			for (int maxLod = minLod; maxLod <= 4; maxLod++) {

				List<Mesh> meshesAtLod = new ArrayList<>();
				for (Mesh m : meshes) {
					if (m.lodRange.min() == LevelOfDetail.fromInt(minLod)
							&& m.lodRange.max() == LevelOfDetail.fromInt(maxLod)) {
						meshesAtLod.add(m);
					}
				}

				Multimap<Model, InstanceParameters> models = HashMultimap.create();

				if (minLod == 0 && maxLod == 4) {
					models = modelInstances;
				}

				if (!meshesAtLod.isEmpty() || !models.isEmpty()) {
					result.add(buildWorldObject(worldObjectMetadata, meshesAtLod, models));
				}

			}
		}

		return result;

	}

	private FrontendPbf.WorldObject buildWorldObject(@Nullable MeshMetadata worldObjectMetadata,
			Collection<Mesh> meshes, Multimap<Model, InstanceParameters> modelInstances) {

		if (meshes.isEmpty() && modelInstances.isEmpty()) {
			throw new IllegalStateException("a WorldObject needs geometry");
		}

		MapRelationElement element = worldObjectMetadata == null ? null : worldObjectMetadata.mapElement();

		/* check for 3DMR ids */

		String id3DMR = element == null ? null : element.getTags().getValue("3dmr");

		if (id3DMR != null) {

			meshes = emptyList();
			modelInstances = HashMultimap.create();

			Model model3DMR = new ExternalResourceModel("3dmr:" + id3DMR);

			VectorXZ center = getCenter(element);
			double direction = parseDirection(element.getTags(), 0);

			modelInstances.put(model3DMR, new InstanceParameters(center.xyz(0), direction));

		}

		/* build the object's triangle and extrusion geometries */

		List<FrontendPbf.TriangleGeometry> triangleGeometries = new ArrayList<>();
		List<FrontendPbf.ExtrusionGeometry> extrusionGeometries = new ArrayList<>();

		for (Mesh mesh : meshes) {

			if (mesh.geometry instanceof ExtrusionGeometry) {
				extrusionGeometries.add(buildExtrusionGeometry(mesh.material, (ExtrusionGeometry) mesh.geometry));
			} else {
				triangleGeometries.add(buildTriangleGeometry(mesh.material, mesh.geometry.asTriangles()));
			}

		}

		/* build the object's instance geometries */

		List<InstanceGeometry> instanceGeometries = new ArrayList<>();

		for (Model model : modelInstances.keySet()) {

			InstanceGeometry.Builder geometryBuilder = InstanceGeometry.newBuilder();

			if (model instanceof ExternalResourceModel externalResourceModel) {
				geometryBuilder.setResourceIdentifier(externalResourceModel.resourceIdentifier());
			} else if (model instanceof GltfModel gltfModel
					&& gltfModel.getSource() instanceof ExternalModelSource.External3DMRSource s) {
				geometryBuilder.setResourceIdentifier(s.toString());
			} else {
				geometryBuilder.setModel(modelBlock.toIndex(model));
			}

			boolean allUnrotated = true;
			boolean allUnscaled = true;

			for (InstanceParameters instanceParams : modelInstances.get(model)) {

				geometryBuilder.addPosition(round(instanceParams.position().x * COORD_PRECISION_FACTOR));
				geometryBuilder.addPosition(round(instanceParams.position().y * COORD_PRECISION_FACTOR));
				geometryBuilder.addPosition(round(instanceParams.position().z * COORD_PRECISION_FACTOR));

				int direction = (int)round(instanceParams.direction() * 1000.0);
				geometryBuilder.addDirection(direction);
				allUnrotated &= (direction == 0);

				// this assumes that 1 is the unscaled height, which is why convertModel passes 1 to Model.render
				double scaleDouble = instanceParams.height() == null ? 1 : instanceParams.height();
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

		if (worldObjectMetadata != null) {

			if (element != null) {
				objectBuilder.setOsmId(element.toString());
			}

			objectBuilder.setTypeName(stringBlock.toIndex(worldObjectMetadata.modelClass().getSimpleName()));

			if (LOD_2_FEATURES.contains(worldObjectMetadata.modelClass())) {
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

		for (int layer = 0; layer < geom.texCoords.size(); layer++) {

			// check if the tex coords can be calculated in the client
			if (!(material.getTextureLayers().get(layer).baseColorTexture.coordFunction instanceof GlobalXZTexCoordFunction)) {

				// append the texture coordinates for this layer
				texCoords.addAll(geom.texCoords.get(layer));

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

	@Override
	public void finish() {

		List<FrontendPbf.WorldObject> objects = new ArrayList<>();

		/* create texture atlases */

		MeshStore instanceMeshStore = new MeshStore();

		for (MeshMetadata metadata : modelInstancesByWO.keySet()) {
			for (java.util.Map.Entry<Model, InstanceParameters> entry : modelInstancesByWO.get(metadata).entries()) {
				for (Mesh mesh : entry.getKey().buildMeshes(entry.getValue())) {
					instanceMeshStore.addMesh(mesh, metadata);
				}
			}
		}

		@Nullable TextureAtlasGroup textureAtlasGroup = switch (USE_TEXTURE_ATLAS) {
			case ALWAYS -> generateTextureAtlasGroup(List.of(this.meshStore, instanceMeshStore), t -> false);
			case RUNTIME_ONLY -> generateTextureAtlasGroup(List.of(this.meshStore, instanceMeshStore),
					t -> !(t instanceof RuntimeTexture));
			case NEVER -> null;
		};

		/* pre-process meshes */

		List<MeshProcessingStep> processingSteps = new ArrayList<>(List.of(
				new ClipToBounds(bbox, true),
				new MergeMeshes(EnumSet.of(SEPARATE_NORMAL_MODES, SINGLE_COLOR_MESHES, PRESERVE_GEOMETRY_TYPES))
		));

		if (textureAtlasGroup != null) {
			processingSteps.add(1, new ReplaceTexturesWithAtlas(textureAtlasGroup));
		}

		MeshStore meshStore = this.meshStore.process(processingSteps);

		/* convert all WorldObjects */

		Multimap<MeshMetadata, Mesh> meshesByMetadata = meshStore.meshesByMetadata();

		Set<MeshMetadata> knownObjectMetadata = new HashSet<>(modelInstancesByWO.keySet());
		knownObjectMetadata.addAll(meshesByMetadata.keySet());

		for (MeshMetadata objectMetadata : knownObjectMetadata) {

			objects.addAll(buildWorldObjects(objectMetadata, meshesByMetadata.get(objectMetadata),
					modelInstancesByWO.getOrDefault(objectMetadata, HashMultimap.create())));

		}

		/* create a floor plate (if that option is active */

		if (USE_FLOOR_PLATE) {
			try {
				objects.add(buildFloorPlate());
			} catch (InvalidGeometryException | IllegalStateException | TopologyException
					| ConstraintEnforcementException e) {
				ConversionLog.error("Error while producing the floor plate", e);
			}
		}

		/* build the blocks */

		//model block needs to be first, because it adds content (e.g. vectors) to the other blocks
		ModelBlock.Builder modelBlockBuilder = ModelBlock.newBuilder();

		FaultTolerantIterationUtil.forEach(modelBlock.getElements(), (Model m) -> {
			modelBlockBuilder.addModels(convertModel(m, textureAtlasGroup));
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

		String archiveName = outputFile == null ? null : outputFile.getName();
		CompressionUtil.writeWithCompression(outputStream, compression, archiveName, os -> {
			try {
				tileBuilder.build().writeTo(outputStream);
			} catch (IOException e) {
				//TODO proper error handling
				throw new Error(e);
			}
		});

	}

}
