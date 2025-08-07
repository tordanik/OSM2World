package org.osm2world.world.modules.building;

import static com.google.common.collect.Iterables.getLast;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD3;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD4;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.util.ValueParseUtil.parseLevels;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.inheritTags;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.CAGUtil;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.building.LevelAndHeightData.Level;
import org.osm2world.world.modules.building.LevelAndHeightData.Level.LevelType;
import org.osm2world.world.modules.building.indoor.BuildingPartInterior;
import org.osm2world.world.modules.building.roof.ComplexRoof;
import org.osm2world.world.modules.building.roof.Roof;

/**
 * part of a building, as defined by the Simple 3D Buildings standard.
 * Consists of {@link ExteriorBuildingWall}s, a {@link Roof}, and maybe a {@link BuildingBottom}.
 * This is the core class of the {@link BuildingModule}.
 */
public class BuildingPart implements AreaWorldObject, ProceduralWorldObject {

	public static final double DEFAULT_RIDGE_HEIGHT = 5;
	static final LevelOfDetail INDOOR_MIN_LOD = LOD3;

	final Building building;
	final MapArea area;
	private final PolygonWithHolesXZ polygon;

	final O2WConfig config;

	/** the tags for this part, including tags inherited from the parent */
	final TagSet tags;

	public final LevelAndHeightData levelStructure;

	Roof roof;

	private List<ExteriorBuildingWall> walls = null;
	private List<BuildingBottom> bottoms = null;

	private final @Nullable BuildingPartInterior buildingPartInterior;

	public BuildingPart(Building building, MapArea area, O2WConfig config) {

		area.addRepresentation(this);

		this.building = building;
		this.area = area;
		this.polygon = area.getPolygon();

		this.config = config;

		this.tags = inheritTags(area.getTags(), building.getPrimaryMapElement().getTags());
		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		/* determine the roof shape */

		String roofShape = tags.getValue("roof:shape");

		if (!("no".equals(tags.getValue("roof:lines")))
				&& (roofShape == null || !List.of("flat", "round", "dome", "onion").contains(roofShape))
				&& ComplexRoof.hasComplexRoof(area)) {
			roofShape = "complex";
		} else {
			if (roofShape == null) { roofShape = tags.getValue("building:roof:shape"); }
			if (roofShape == null) { roofShape = defaults.roofShape; }
		}

		/* collect indoor=level elements (if any) for additional level information */

		Map<Integer, TagSet> levelTagSets = new HashMap<>();

		for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
			MapElement other = overlap.getOther(area);
			if (other.getTags().contains("indoor", "level")) {
				List<Integer> levels = parseLevels(other.getTags().getValue("level"), emptyList());
				levels.forEach(level -> levelTagSets.put(level, other.getTags()));
			}
		}

		/* build the roof */

		Material materialRoof = createRoofMaterial(tags, config);

		try {
			roof = Roof.createRoofForShape(roofShape, area, polygon, tags, materialRoof);
		} catch (InvalidGeometryException e) {
			throw new InvalidGeometryException("error constructing roof for " + area + ": " + e);
		}

		/* determine the level structure */

		levelStructure = new LevelAndHeightData(building.getPrimaryMapElement().getTags(),
				area.getTags(), levelTagSets, roof, this.area.getPolygon(), this.area);

		roof.setRoofHeight(levelStructure.height() - levelStructure.heightWithoutRoof());

		/* potentially create indoor environment */

		Boolean hasUsefulIndoorLevelNumbers = true; //TODO get the hasUsefulIndoorLevelNumbers information from the LevelAndHeightData constructor

		if (hasUsefulIndoorLevelNumbers) { //TODO check config for enabling/disabling indoor rendering
			buildingPartInterior = createInteriorComponents();
		} else {
			buildingPartInterior = null;
		}

	}

	/** creates the walls, floors etc. making up this part */
	private void createComponents() {

		Material materialWall = createWallMaterial(tags, config);

		List<Level> levels = levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND, LevelType.ROOF));
		double floorHeight = levels.isEmpty() ? 0 : levels.get(0).relativeEle;

		/* find passages through this building part */

		double clearingAbovePassage = 2.5;

		List<WorldObject> buildingPassages = area.getOverlaps().stream()
				.map(o -> o.getOther(area))
				.filter(o -> o.getTags().containsAny(List.of("tunnel"), asList("building_passage", "passage")))
				.map(MapElement::getPrimaryRepresentation)
				.filter(o -> o != null && !o.getRawGroundFootprint().isEmpty())
				.filter(o -> o.getGroundState() == GroundState.ON)
				.filter(o -> clearingAbovePassage > floorHeight)
				.toList();

		if (buildingPassages.isEmpty()) {

			/* create walls and floor normally (no building passages) */

			walls = splitIntoWalls(area, this);

			if (floorHeight > 0) {
				bottoms = singletonList(new BuildingBottom(this, materialWall, polygon, floorHeight));
			} else {
				bottoms = emptyList();
			}

		} else {

			Map<PolygonWithHolesXZ, Double> polygonFloorHeightMap = new HashMap<>();

			/* construct those polygons where the area does not overlap with the footprint of buildingPassages */

			List<SimplePolygonShapeXZ> subtractPolygons = new ArrayList<>();

			for (WorldObject o : buildingPassages) {

				for (PolygonShapeXZ subtractPolyShape : o.getRawGroundFootprint()) {

					SimplePolygonShapeXZ subtractPoly = subtractPolyShape.getOuter();

					subtractPolygons.add(subtractPoly);

					if (o instanceof WaySegmentWorldObject) {

						// extend the subtract polygon for segments that end
						// at a common node with this building part's outline.
						// (otherwise, the subtract polygon will probably
						// not exactly line up with the polygon boundary)

						WaySegmentWorldObject waySegmentWO = (WaySegmentWorldObject) o;
						VectorXZ start = waySegmentWO.getStartPosition();
						VectorXZ end = waySegmentWO.getEndPosition();

						boolean startCommonNode = false;
						boolean endCommonNode = false;

						for (SimplePolygonXZ p : polygon.getRings()) {
							startCommonNode |= p.getVertexCollection().contains(start);
							endCommonNode |= p.getVertexCollection().contains(end);
						}

						VectorXZ direction = end.subtract(start).normalize();

						if (startCommonNode) {
							subtractPolygons.add(subtractPoly.shift(direction));
						}

						if (endCommonNode) {
							subtractPolygons.add(subtractPoly.shift(direction.invert()));
						}

					}

				}

			}

			subtractPolygons.addAll(roof.getPolygon().getHoles());

			Collection<PolygonWithHolesXZ> buildingPartPolys =
					CAGUtil.subtractPolygons(
							roof.getPolygon().getOuter(),
							subtractPolygons);

			for (PolygonWithHolesXZ p : buildingPartPolys) {
				polygonFloorHeightMap.put(p, floorHeight);
			}

			/* construct the polygons directly above the passages */

			for (WorldObject o : buildingPassages) {
				for (PolygonShapeXZ b : o.getRawGroundFootprint()) {

					Collection<PolygonWithHolesXZ> raisedBuildingPartPolys;

					Collection<PolygonWithHolesXZ> polysAboveTBWOs =
							CAGUtil.intersectPolygons(asList(polygon.getOuter(), asSimplePolygon(b.getOuter())));

					if (polygon.getHoles().isEmpty()) {
						raisedBuildingPartPolys = polysAboveTBWOs;
					} else {
						raisedBuildingPartPolys = new ArrayList<>();
						for (PolygonWithHolesXZ p : polysAboveTBWOs) {
							List<SimplePolygonXZ> subPolys = new ArrayList<>();
							subPolys.addAll(polygon.getHoles());
							subPolys.addAll(p.getHoles());
							raisedBuildingPartPolys.addAll(
									CAGUtil.subtractPolygons(p.getOuter(), subPolys));
						}
					}

					for (PolygonWithHolesXZ p : raisedBuildingPartPolys) {
						double newFloorHeight = clearingAbovePassage;
						if (newFloorHeight < floorHeight) {
							newFloorHeight = floorHeight;
						}
						polygonFloorHeightMap.put(p, newFloorHeight);
					}

				}
			}

			/* create the walls and floors */

			bottoms = new ArrayList<>();
			walls = new ArrayList<>();

			for (PolygonWithHolesXZ polygon : polygonFloorHeightMap.keySet()) {

				bottoms.add(new BuildingBottom(this, materialWall, polygon, polygonFloorHeightMap.get(polygon)));

				for (SimplePolygonXZ ring : polygon.getRings()) {
					ring = polygon.getOuter().equals(ring) ? ring.makeCounterclockwise() : ring.makeClockwise();
					ring = ring.getSimplifiedPolygon();
					for (int i = 0; i < ring.size(); i++) {
						walls.add(new ExteriorBuildingWall(null, this,
								asList(ring.getVertex(i), ring.getVertexAfter(i)),
								emptyMap(),
								polygonFloorHeightMap.get(polygon)));
					}
				}

			}

		}
	}

	/** creates indoor components like rooms and interior walls */
	private @Nullable BuildingPartInterior createInteriorComponents() {

		ArrayList<MapElement> indoorElements = new ArrayList<>();

		for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
			MapElement other = overlap.getOther(this.area);
			if (other.getTags().containsKey("indoor") && !other.getTags().contains("indoor", "level")) {

				// ensure that the element is on a valid level
				List<Integer> levelList = parseLevels(other.getTags().getValue("level"));
				if (levelList != null && levelStructure.hasLevel(levelList.get(0))
						&& levelStructure.hasLevel(levelList.get(levelList.size() - 1))) {

					// Ensure all nodes are within this building part
					//TODO handle elements that span building parts

					if ((other instanceof MapNode
									&& polygon.contains(((MapNode) other).getPos()))
							|| (other instanceof MapArea
									&& polygon.contains(((MapArea) other).getOuterPolygon()))
							|| (other instanceof MapWaySegment
									&& polygon.contains(((MapWaySegment) other).getLineSegment()))) {
						indoorElements.add(other);
					}
				}
			}

		}

		if (!indoorElements.isEmpty()) {
			return new BuildingPartInterior(indoorElements, this);
		} else {
			return null;
		}

	}

	/**
	 * splits the outer and inner boundaries of a building into separate walls.
	 * The boundaries are split at the beginning and end of building:wall=* ways belonging to this building part,
	 * as well as any node where the angle isn't (almost) completely straight.
	 *
	 * @param buildingPartArea  the building:part=* area (or building=*, for buildings without parts)
	 * @param buildingPart  the parent object for the walls created by this method. Non-null except for tests.
	 *
	 * @return list of walls, each represented as a list of nodes.
	 *   The list of nodes is ordered such that the building part's outside is to the right.
	 */
	static List<ExteriorBuildingWall> splitIntoWalls(MapArea buildingPartArea, BuildingPart buildingPart) {

		List<ExteriorBuildingWall> result = new ArrayList<>();

		for (List<MapNode> nodeRing : buildingPartArea.getRings()) {

			List<MapNode> nodes = new ArrayList<>(nodeRing);

			SimplePolygonXZ simplifiedPolygon = MapArea.polygonFromMapNodeLoop(nodes).getSimplifiedPolygon();

			if (simplifiedPolygon.isClockwise() ^ buildingPartArea.getHoles().contains(nodeRing)) {
				reverse(nodes);
			}

			nodes.remove(nodes.size() - 1); //remove the duplicated node at the end

			/* figure out where we don't want to split the wall:
			 * points in the middle of a straight wall, unless they are the start/end point of a wall way */

			boolean[] splitAtNode = new boolean[nodes.size()];
			Set<MapWay> wallWays = new HashSet<>();

			for (int i = 0; i < nodes.size(); i++) {

				MapNode node = nodes.get(i);

				boolean isCorner = simplifiedPolygon.getVertexCollection().contains(node.getPos());
				boolean isStartOrEndOfWallWay = false;

				for (MapWay way : node.getConnectedWays()) {
					if (way.getTags().containsKey("building:wall")
							&& !way.getTags().contains("building:wall", "no")
							&& (way.getNodes().get(0).equals(node) || getLast(way.getNodes()).equals(node))) {
						isStartOrEndOfWallWay = true;
						wallWays.add(way);
					}
				}

				splitAtNode[i] = isCorner || isStartOrEndOfWallWay;

			}

			/* split the outline into walls, splitting at each node that's not flagged in noSplitAtNode */

			int firstSplitNode = IntStream.range(0, nodes.size())
					.filter(i -> splitAtNode[i])
					.min().getAsInt();

			int i = firstSplitNode;

			List<MapNode> currentWallNodes = new ArrayList<>();
			currentWallNodes.add(nodes.get(firstSplitNode));

			do {

				i = (i + 1) % nodes.size();

				if (splitAtNode[i]) {
					if (currentWallNodes != null) {

						currentWallNodes.add(nodes.get(i));
						assert currentWallNodes.size() >= 2;

						MapWay wallWay = null;

						for (MapWay w : wallWays) {
							boolean containsAllSegments = true;
							for (int j = 0; j + 1 < currentWallNodes.size(); j++) {
								List<MapNode> pair = asList(currentWallNodes.get(j), currentWallNodes.get(j + 1));
								if (!w.getWaySegments().stream().anyMatch(s ->
										s.getStartEndNodes().containsAll(pair))) {
									containsAllSegments = false;
								}
							}
							if (containsAllSegments) {
								wallWay = w;
								break;
							}
						}

						result.add(new ExteriorBuildingWall(wallWay, buildingPart, currentWallNodes));

					}
					currentWallNodes = new ArrayList<>();
				}

				currentWallNodes.add(nodes.get(i));

			} while (i != firstSplitNode);

		}

		return result;

	}

	@Override
	public void buildMeshesAndModels(Target target) {

		if (walls == null) {
			// the reason why this is called here rather than the constructor is tunnel=building_passage:
			// in the constructor, the roads' calculations aren't completed yet
			createComponents();
		}

		if (!config.getBoolean("noOuterWalls", false)){
			walls.forEach(w -> w.renderTo(target));
		}

		if (!config.getBoolean("noRoofs", false)) {
			roof.renderTo(target, building.getGroundLevelEle() + levelStructure.heightWithoutRoof());
		}


		// TODO don't render floors inside building

		bottoms.forEach(f -> f.renderTo(target));

		target.setCurrentLodRange(INDOOR_MIN_LOD, LOD4);

		if (buildingPartInterior != null) {
			buildingPartInterior.buildMeshesAndModels(target);
		}

	}

	@Override
	public Collection<AttachmentSurface> getAttachmentSurfaces() {

		if (walls == null) {
			// the reason why this is called here rather than the constructor is tunnel=building_passage:
			// in the constructor, the roads' calculations aren't completed yet
			createComponents();
		}

		List<AttachmentSurface> surfaces = new ArrayList<>();

		if (buildingPartInterior != null) {
			surfaces.addAll(buildingPartInterior.getAttachmentSurfaces());
		}

		int roofAttachmentLevel = levelStructure.levels.get(levelStructure.levels.size() - 1).level + 1;
		//TODO: support multiple roof levels
		surfaces.addAll(roof.getAttachmentSurfaces(
				building.getGroundLevelEle() + levelStructure.heightWithoutRoof(), roofAttachmentLevel));

		return surfaces;
	}

	public PolygonWithHolesXZ getPolygon() {
		return polygon;
	}

	@Override
	public Collection<PolygonShapeXZ> getRawGroundFootprint(){
		if (levelStructure.bottomHeight() <= 0 && getIndoor() != null) {
			return List.of(getPolygon());
		} else {
			return emptyList();
		}
	}

	public Roof getRoof() {
		return roof;
	}

	public Building getBuilding() { return building; }

	public O2WConfig getConfig() { return config; }

	public TagSet getTags() { return tags; }

	@Override
	public MapArea getPrimaryMapElement() {
		return area;
	}

	@Override
	public WorldObject getParent() {
		return building;
	}

	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return emptySet();
	}

	BuildingPartInterior getIndoor() {
		return buildingPartInterior;
	}

	static Material createWallMaterial(TagSet tags, O2WConfig config) {

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		if (config.getBoolean("useBuildingColors", true)) {

			return buildMaterial(
					tags.getValue("building:material"),
					tags.getValue("building:colour"),
					defaults.materialWall, false);

		} else {
			return defaults.materialWall;
		}

	}

	private static Material createRoofMaterial(TagSet tags, O2WConfig config) {

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		if (config.getBoolean("useBuildingColors", true)) {

			return buildMaterial(
					tags.getValue("roof:material"),
					tags.getValue("roof:colour"),
					defaults.materialRoof, true);
		} else {
			return defaults.materialRoof;
		}

	}

	public static Material buildMaterial(String materialString,
										 String colorString, Material defaultMaterial,
										 boolean roof) {

		Material material = defaultMaterial;

		if (materialString != null) {
			if ("brick".equals(materialString)) {
				material = Materials.BRICK;
			} else if ("glass".equals(materialString)
					|| "mirror".equals(materialString)) {
				material = roof ? Materials.GLASS_ROOF : Materials.GLASS_WALL;
			} else if ("copper".equals(materialString) && roof) {
				material = Materials.COPPER_ROOF;
			} else if ("wood".equals(materialString)
					|| "bamboo".equals(materialString)) {
				material = Materials.WOOD_WALL;
			} else if (Materials.getSurfaceMaterial(materialString) != null) {
				material = Materials.getSurfaceMaterial(materialString);
			} else if (Materials.getMaterial(materialString) != null) {
				material = Materials.getMaterial(materialString);
			}
		}

		boolean colorable = material.getNumTextureLayers() == 0
				|| material.getTextureLayers().get(0).colorable;

		if (colorString != null && colorable) {

			Color color;

			if ("white".equals(colorString)) {
				color = new Color(240, 240, 240);
			} else if ("black".equals(colorString)) {
				color = new Color(76, 76, 76);
			} else if ("grey".equals(colorString) || "gray".equals(colorString)) {
				color = new Color(100, 100, 100);
			} else if ("red".equals(colorString)) {
				if (roof) {
					color = new Color(204, 0, 0);
				} else {
					color = new Color(255, 190, 190);
				}
			} else if ("green".equals(colorString)) {
				if (roof) {
					color = new Color(150, 200, 130);
				} else {
					color = new Color(190, 255, 190);
				}
			} else if ("blue".equals(colorString)) {
				if (roof) {
					color = new Color(100, 50, 200);
				} else {
					color = new Color(190, 190, 255);
				}
			} else if ("yellow".equals(colorString)) {
				color = new Color(255, 255, 175);
			} else if ("pink".equals(colorString)) {
				color = new Color(225, 175, 225);
			} else if ("orange".equals(colorString)) {
				color = new Color(255, 225, 150);
			} else if ("brown".equals(colorString)) {
				if (roof) {
					color = new Color(120, 110, 110);
				} else {
					color = new Color(170, 130, 80);
				}
			} else {
				color = parseColor(colorString, CSS_COLORS);
			}

			material = material.withColor(color);

		}

		return material;

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + area + ")";
	}

}
