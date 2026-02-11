package org.osm2world.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.scene.material.DefaultMaterials.*;
import static org.osm2world.util.ValueParseUtil.parseMeasure;
import static org.osm2world.util.ValueParseUtil.parseMeasureWithSpecialDefaultUnit;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.filterWorldObjectCollisions;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.scene.material.*;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.style.Style;
import org.osm2world.util.image.ImageUtil;
import org.osm2world.world.data.*;
import org.osm2world.world.modules.common.ConfigurableWorldModule;
import org.osm2world.world.modules.common.WorldModuleBillboardUtil;
import org.osm2world.world.modules.common.WorldModuleParseUtil;

/**
 * adds trees, tree rows, tree groups and forests to the world
 */
public class TreeModule extends ConfigurableWorldModule {

	private static final List<String> LEAF_TYPE_KEYS = asList("leaf_type", "wood");

	private static enum LeafType {

		BROADLEAVED("broadleaved", "deciduous"),
		NEEDLELEAVED("needleleaved", "coniferous");

		private final List<String> values;

		private LeafType(String... values) {
			this.values = asList(values);
		}

		public static LeafType getValue(TagSet tags) {
			for (LeafType type : values()) {
				if (tags.containsAny(LEAF_TYPE_KEYS, type.values)) {
					return type;
				}
			}
			return null;
		}

	}

	private static final List<String> LEAF_CYCLE_KEYS = asList("leaf_cycle", "wood");

	private static enum LeafCycle {

		EVERGREEN("evergreen"),
		DECIDUOUS("deciduous"),
		SEMI_EVERGREEN("semi_evergreen"),
		SEMI_DECIDUOUS("semi_deciduous");

		private final List<String> values;

		private LeafCycle(String... values) {
			this.values = asList(values);
		}

		public static LeafCycle getValue(TagSet tags) {
			for (LeafCycle type : values()) {
				if (tags.containsAny(LEAF_CYCLE_KEYS, type.values)) {
					return type;
				}
			}
			return null;
		}

	}

	private static enum TreeSpecies {

		APPLE_TREE("malus");

		private final String value;

		private TreeSpecies(String value) {
			this.value = value;
		}

		public static TreeSpecies getValue(TagSet tags) {

			String speciesString = tags.getValue("species");

			if (speciesString != null) {

				for (TreeSpecies species : values()) {
					if (speciesString.contains(species.value)) {
						return species;
					}
				}

			}

			// default to apple trees for orchards

			if (tags.contains("landuse", "orchard")) {
				return APPLE_TREE;
			} else {
				return null;
			}

		}

	}

	/**
	 * @param height  tree height in meters
	 * @param crownDiameter  diameter of the tree's crown in meters
	 * @param trunkDiameter  diameter of the tree's trunk (at breast height) in meters, or null if unknown
	 */
	private record TreeDimensions(double height, double crownDiameter, @Nullable Double trunkDiameter) {

		/**
		 * parse height and other dimensions (optionally modified by some random factor for forests)
		 *
		 * @param random  randomness generator to slightly vary the values, can be set to null if no randomness is desired
		 * @param model  used to get default ratios between dimensions, such as height to width. Optional.
		 */
		public static TreeDimensions fromTags(TagSet tags, @Nullable Random random, @Nullable TreeModel model,
				double defaultHeight) {

			double defaultHeightToWidth = model != null ? model.defaultHeightToWidth() : 2;
			double defaultCrownToTrunk = 30;

			double scaleFactor = random != null ? 0.5 + 0.75 * random.nextDouble() : 1.0;

			Double trunkDiameter = parseMeasureWithSpecialDefaultUnit(tags.getValue("diameter"), 1e-3);

			if (trunkDiameter == null) {
				Double trunkCircumference = parseMeasure(tags.getValue("circumference"));
				if (trunkCircumference != null) {
					trunkDiameter = trunkCircumference / PI;
				}
			}

			Double crownDiameter = parseMeasure(tags.getValue("diameter_crown"));
			Double height = parseMeasure(tags.getValue("height"));

			if (height == null) {
				height = parseMeasure(tags.getValue("est_height"));
				if (height == null) {
					if (crownDiameter != null) {
						height = crownDiameter * defaultHeightToWidth;
					} else if (trunkDiameter != null) {
						height = trunkDiameter * defaultCrownToTrunk * defaultHeightToWidth;
					} else {
						height = defaultHeight;
					}
				}
			}

			if (crownDiameter == null) {
				crownDiameter = height / defaultHeightToWidth;
			}

			return new TreeDimensions(scaleFactor * height,scaleFactor * crownDiameter,
					trunkDiameter != null ? scaleFactor * trunkDiameter : null);

		}

	}

	private boolean useBillboards = false;
	private double defaultTreeHeight = 10;
	private double defaultTreeHeightForest = 20;

	@Override
	public void setConfiguration(@Nonnull O2WConfig config) {
		super.setConfiguration(config);
		useBillboards = config.getBoolean("useBillboards", false);
		defaultTreeHeight = config.getDouble("defaultTreeHeight", 10);
		defaultTreeHeightForest = config.getDouble("defaultTreeHeightForest", 20);
	}

	@Override
	public final void applyTo(MapData mapData) {

		for (MapNode node : mapData.getMapNodes()) {

			if (node.getTags().contains("natural", "tree")) {
				node.addRepresentation(new Tree(node));
			}

		}

		for (MapWaySegment segment : mapData.getMapWaySegments()) {
			if (segment.getTags().contains(new Tag("natural", "tree_row"))) {
				segment.addRepresentation(new TreeRow(segment));
			}
		}

		for (MapArea area : mapData.getMapAreas()) {

			if (area.getTags().contains("natural", "wood")
					|| area.getTags().contains("landuse", "forest")
					|| area.getTags().contains("landcover", "trees")
					|| area.getTags().containsKey("wood")
					|| area.getTags().contains("landuse", "orchard")) {
				area.addRepresentation(new Forest(area, mapData));
			}

		}

	}

	/**
	 * retrieves a suitable {@link TreeModel} from {@link #existingModels}, or creates it if necessary
	 *
	 * @param seed       an object to be used as the seed for random decisions
	 */
	private TreeModel getTreeModel(Vector3D seed, LeafType leafType, LeafCycle leafCycle, TreeSpecies species,
			@Nullable TreeDimensions dimensions) {

		var r = new Random((long)(seed.getX() * 10) + (long)(seed.getZ() * 10000));

		// "random" decision to flip the tree texture
		boolean mirrored = r.nextBoolean();

		// if leaf type is unknown, make another random decision
		if (leafType == null) {
			leafType = r.nextBoolean() ? LeafType.NEEDLELEAVED : LeafType.BROADLEAVED;
		}

		TreeModel model = null;

		for (TreeModel existingModel : existingModels) {
			if (existingModel.leafType() == leafType
					&& existingModel.leafCycle() == leafCycle
					&& existingModel.species() == species
					&& existingModel.mirrored() == mirrored
					&& Objects.equals(existingModel.dimensions(), dimensions)
					&& (existingModel instanceof TreeBillboardModel) == useBillboards) {
				model = existingModel;
				break;
			}
		}

		if (model == null) {
			model = useBillboards
					? new TreeBillboardModel(leafType, leafCycle, species, mirrored, dimensions, config.mapStyle())
					: new TreeGeometryModel(leafType, leafCycle, species, dimensions, config.mapStyle());
			existingModels.add(model);
		}

		return model;

	}

	private interface TreeModel extends Model {

		LeafType leafType();
		LeafCycle leafCycle();
		@Nullable TreeSpecies species();
		boolean mirrored();
		double defaultHeightToWidth();
		@Nullable TreeDimensions dimensions();

	}

	private record TreeBillboardModel(
			LeafType leafType,
			LeafCycle leafCycle,
			@Nullable TreeSpecies species,
			boolean mirrored,
			@Nullable TreeDimensions dimensions,
			Style mapStyle
	) implements TreeModel {

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			Material material = getMaterial().get(mapStyle);

			return WorldModuleBillboardUtil.buildCrosstree(material, params.position(),
					dimensions != null ? dimensions.crownDiameter : defaultHeightToWidth() * params.height(),
					params.height(), mirrored);

		}

		private MaterialOrRef getMaterial() {
			return species == TreeSpecies.APPLE_TREE
					? TREE_BILLBOARD_BROAD_LEAVED_FRUIT
					: leafType == LeafType.NEEDLELEAVED
					? TREE_BILLBOARD_CONIFEROUS
					: TREE_BILLBOARD_BROAD_LEAVED;
		}

		@Override
		public double defaultHeightToWidth() {
			List<TextureLayer> textureLayers = getMaterial().get(mapStyle).textureLayers();
			if (!textureLayers.isEmpty()) {
				TextureData texture = textureLayers.get(0).baseColorTexture;
				TextureDataDimensions textureDimensions = texture.dimensions();
				if (textureDimensions.widthPerEntity() != null && textureDimensions.heightPerEntity() != null) {
					return textureDimensions.heightPerEntity() / textureDimensions.widthPerEntity();
				} else {
					float aspectRatio = requireNonNullElse(ImageUtil.getAspectRatio(texture), texture.dimensions().getAspectRatio());
					return 1.0 / aspectRatio;
				}
			} else {
				return 2;
			}
		}

	}

	private record TreeGeometryModel(
			LeafType leafType,
			LeafCycle leafCycle,
			@Nullable TreeSpecies species,
			@Nullable TreeDimensions dimensions,
			Style mapStyle
	) implements TreeModel {

		@Override
		public boolean mirrored() {
			return false;
		}

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			double height = params.height();
			VectorXYZ posXYZ = params.position();

			boolean coniferous = (leafType == LeafType.NEEDLELEAVED);

			double stemRatio = coniferous?0.3:0.5;
			double width = dimensions != null ? dimensions.crownDiameter : defaultHeightToWidth() * height;
			double trunkRadius = dimensions != null && dimensions.trunkDiameter != null ? dimensions.trunkDiameter / 2
					: width / 8;

			ExtrusionGeometry trunk = ExtrusionGeometry.createColumn(null,
					posXYZ, height*stemRatio,trunkRadius, 0.8 * trunkRadius,
					false, true, null, TREE_TRUNK.get(mapStyle).textureDimensions());

			ExtrusionGeometry crown = ExtrusionGeometry.createColumn(null,
					posXYZ.addY(height*stemRatio), height*(1-stemRatio), width / 2,
					coniferous ? 0 : width / 2, true, true, null,
					TREE_CROWN.get(mapStyle).textureDimensions());

			return List.of(
					new Mesh(trunk, TREE_TRUNK.get(mapStyle)),
					new Mesh(crown, TREE_CROWN.get(mapStyle))
			);

		}

		@Override
		public double defaultHeightToWidth() {
			return 2.5;
		}
	}

	private final List<TreeModel> existingModels = new ArrayList<>();

	public class Tree extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private final TreeDimensions dimensions;
		private final TreeModel model;

		public Tree(MapNode node) {

			super(node);

			TagSet tags = node.getTags();

			/* inherit information from the tree row this tree belongs to, if any */

			Optional<MapWaySegment> parentTreeRow = node.getConnectedWaySegments().stream()
					.filter(s -> s.getTags().contains("natural", "tree_row")).findAny();
			if (parentTreeRow.isPresent()) {
				tags = WorldModuleParseUtil.inheritTags(tags, parentTreeRow.get().getTags());
			}

			/* interpret the tags */

			var leafType = LeafType.getValue(tags);
			var leafCycle = LeafCycle.getValue(tags);
			var species = TreeSpecies.getValue(tags);

			TreeModel dimensionlessModel = getTreeModel(node.getPos(), leafType, leafCycle, species, null);
			dimensions = TreeDimensions.fromTags(tags, null, dimensionlessModel, defaultTreeHeight);
			model = getTreeModel(node.getPos(), leafType, leafCycle, species, dimensions);

		}

		@Override
		public void buildMeshesAndModels(Target target) {
			target.addSubModel(new ModelInstance(model, new InstanceParameters(getBase(), 0, dimensions.height)));
		}

	}

	public class TreeRow implements WaySegmentWorldObject, ProceduralWorldObject {

		private final MapWaySegment segment;

		private final List<EleConnector> treeConnectors;

		private final LeafType leafType;
		private final LeafCycle leafCycle;
		private final TreeSpecies species;

		public TreeRow(MapWaySegment segment) {

			this.segment = segment;

			/* determine details about the trees in the row */

			leafType = LeafType.getValue(segment.getTags());
			leafCycle = LeafCycle.getValue(segment.getTags());
			species = TreeSpecies.getValue(segment.getTags());

			/* place trees along the way this segment belongs to */

			List<VectorXZ> treePositions = new ArrayList<>(GeometryUtil.equallyDistributePointsAlong(
					4 /* TODO: derive from tree count */ ,
					true,
					segment.getWay().getPolylineXZ()));

			/* delete implicit trees if there's already an explicit tree nearby */

			List<VectorXZ> explicitTreePositions = segment.getWay().getNodes().stream()
					.filter(n -> n.getTags().contains("natural", "tree"))
					.map(MapNode::getPos)
					.toList();

			treePositions.removeIf(p -> explicitTreePositions.stream().anyMatch(it -> it.distanceTo(p) < 10));

			/* create a connector for each tree position on the current segment */

			treeConnectors = treePositions.stream()
					.filter(it -> {
						if (getStartPosition().equals(it)) {
							// prevent adding a tree node to two segments if it's exactly on the shared node
							return segment.getIndexInWay() == 0;
						} else if (getEndPosition().equals(it)) {
							return true;
						} else {
							return GeometryUtil.isBetween(it, getStartPosition(), getEndPosition());
						}
					})
					.map(it -> new EleConnector(it, null, getGroundState()))
					.toList();

		}

		@Override
		public MapWaySegment getPrimaryMapElement() {
			return segment;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return treeConnectors;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			for (EleConnector treeConnector : treeConnectors) {
				VectorXYZ pos = treeConnector.getPosXYZ();
				TreeModel treeModel = getTreeModel(pos, leafType, leafCycle, species, null);
				TreeDimensions dimensions = TreeDimensions.fromTags(segment.getTags(), null, treeModel, defaultTreeHeight);
				treeModel = getTreeModel(pos, leafType, leafCycle, species, dimensions);
				target.addSubModel(new ModelInstance(treeModel, new InstanceParameters(pos, 0, dimensions.height)));
			}

		}

		//TODO: there is significant code duplication with Forest...

	}


	public class Forest extends CachingProceduralWorldObject implements AreaWorldObject {

		private final MapArea area;
		private final MapData mapData;

		private Collection<EleConnector> treeConnectors = null;

		private final LeafType leafType;
		private final LeafCycle leafCycle;
		private final TreeSpecies species;

		public Forest(MapArea area, MapData mapData) {

			this.area = area;
			this.mapData = mapData;

			leafType = LeafType.getValue(area.getTags());
			leafCycle = LeafCycle.getValue(area.getTags());
			species = TreeSpecies.getValue(area.getTags());

		}

		private void createTreeConnectors(double density) {

			/* collect other objects that the trees should not be placed on */

			Collection<WorldObject> avoidedObjects = new ArrayList<>();

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				for (WorldObject otherRep : overlap.getOther(area).getRepresentations()) {
					if (otherRep.getGroundState() == GroundState.ON
							&& otherRep.getOverlapPriority() >= 20) {
						avoidedObjects.add(otherRep);
					}
				}
			}

			/* place the trees */

			List<VectorXZ> treePositions =
				GeometryUtil.distributePointsOn(area.getId(),
						area.getPolygon(), mapData.getBoundary(),
						density, 0.3f);

			filterWorldObjectCollisions(treePositions, avoidedObjects);

			/* create a terrain connector for each tree */

			treeConnectors = treePositions.stream()
					.map(it -> new EleConnector(it, null, getGroundState()))
					.toList();

		}

		@Override
		public MapArea getPrimaryMapElement() {
			return area;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {

			if (treeConnectors == null) {
				createTreeConnectors(config.treesPerSquareMeter());
			}

			return treeConnectors;

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			for (EleConnector treeConnector : treeConnectors) {
				VectorXYZ pos = treeConnector.getPosXYZ();
				TreeModel treeModel = getTreeModel(pos, leafType, leafCycle, species, null);
				TreeDimensions dimensions = TreeDimensions.fromTags(area.getTags(), new Random(area.getId()), treeModel,
						area.getTags().contains("landuse", "orchard") ? defaultTreeHeight : defaultTreeHeightForest);
				treeModel = getTreeModel(pos, leafType, leafCycle, species, dimensions);
				target.addSubModel(new ModelInstance(treeModel, new InstanceParameters(pos, 0, dimensions.height)));
			}

		}

	}

}
