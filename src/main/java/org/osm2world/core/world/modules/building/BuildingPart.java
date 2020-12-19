package org.osm2world.core.world.modules.building;

import static com.google.common.collect.Iterables.getLast;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.modules.building.indoor.Indoor;
import org.osm2world.core.world.modules.building.roof.ComplexRoof;
import org.osm2world.core.world.modules.building.roof.FlatRoof;
import org.osm2world.core.world.modules.building.roof.Roof;

/**
 * part of a building, as defined by the Simple 3D Buildings standard.
 * Consists of {@link Wall}s, a {@link Roof}, and maybe a {@link Floor}.
 * This is the core class of the {@link BuildingModule}.
 */
public class BuildingPart implements Renderable {

	static final double DEFAULT_RIDGE_HEIGHT = 5;

	final Building building;
	final MapArea area;
	private final PolygonWithHolesXZ polygon;

	final Configuration config;

	/** the tags for this part, including tags inherited from the parent */
	final TagSet tags;

	final int buildingLevels;
	final int buildingMinLevel;
	final int minLevelWithUnderground;

	final Integer min_level;
	final Integer max_level;
	final List<Integer> non_existent_levels;

	double heightWithoutRoof;

	Roof roof;

	private List<Wall> walls = null;
	private List<Floor> floors = null;

	private HashMap<Integer, Level> levels = new HashMap<>();

	private Indoor indoor = null;

	public BuildingPart(Building building, MapArea area, Configuration config) {

		this.building = building;
		this.area = area;
		this.polygon = area.getPolygon();

		this.config = config;

		this.tags = inheritTags(area.getTags(), building.getPrimaryMapElement().getTags());

		/*
		 *  figure out level and height information, plus roofs, based on the building's and building part's tags.
		 * If available, explicitly tagged data is used, with tags of the building part overriding building tags.
		 * Otherwise, the values depend on indirect assumptions (level height)
		 * or ultimately the defaults for this type of building or building part.
		 */

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		double heightPerLevel = defaults.heightPerLevel;

		/* determine levels */

		Float parsedLevels = null;

		if (tags.containsKey("building:levels")) {
			parsedLevels = parseOsmDecimal(tags.getValue("building:levels"), false);
		}

		if (parsedLevels != null) {
			buildingLevels = max(1, (int)(float)parsedLevels);
		} else if (parseHeight(tags, -1) > 0) {
			buildingLevels = max(1, (int) (parseHeight(tags, -1) / heightPerLevel));
		} else {
			buildingLevels = defaults.levels;
		}

		Float parsedMinLevel = null;

		if (tags.containsKey("building:min_level")) {
			parsedMinLevel = parseOsmDecimal(tags.getValue("building:min_level"), false);
		}

		Float parsedUnderground = 0f;

		if (tags.containsKey("building:levels:underground")) {
			parsedUnderground = parseOsmDecimal(tags.getValue("building:levels:underground"), false);
		}

		if (parsedMinLevel != null) {
			if (parsedMinLevel > 0) {
				buildingMinLevel = (int)(float)parsedMinLevel;
				minLevelWithUnderground = buildingMinLevel;
			} else {
				buildingMinLevel = 0;
				if (parsedUnderground == null) {
					minLevelWithUnderground = (int)(float)parsedMinLevel;
				} else {
					minLevelWithUnderground = (int)(float) Math.min(parsedMinLevel, parsedUnderground * (-1));
				}
			}
		} else {
			buildingMinLevel = 0;
			minLevelWithUnderground = Math.min(0, (int)(float)parsedUnderground * (-1));
		}

		min_level = (int)(float) parseOsmDecimal(tags.getValue("min_level"), true, minLevelWithUnderground);
		non_existent_levels = parseList(tags.getValue("non_existent_levels"));
		max_level = (int)(float) parseOsmDecimal(tags.getValue("max_level"), true, levelReversion(buildingLevels - 1));

		/* determine roof height */

		Float roofHeight = null;

		if (tags.containsKey("roof:height")) {
			String valueString = tags.getValue("roof:height");
			roofHeight = parseMeasure(valueString);
		} else if (tags.containsKey("roof:levels")) {
			try {
				roofHeight = (float)defaults.heightPerLevel * Integer.parseInt(tags.getValue("roof:levels"));
			} catch (NumberFormatException e) {}
		}

		if (roofHeight == null) {
			if (tags.contains("roof:shape", "dome")) {
				roofHeight = (float) polygon.getOuter().getDiameter() / 2;
			} else if (buildingLevels == 1) {
				roofHeight = 1.0f;
			} else {
				roofHeight = (float) DEFAULT_RIDGE_HEIGHT;
			}
		}

		/* build the roof */

		Material materialRoof = createRoofMaterial(tags, config);

		if (!("no".equals(tags.getValue("roof:lines"))) && ComplexRoof.hasComplexRoof(area)) {
			roof = new ComplexRoof(area, polygon, tags, roofHeight, materialRoof);
		} else {

			String roofShape = tags.getValue("roof:shape");
			if (roofShape == null) { roofShape = tags.getValue("building:roof:shape"); }
			if (roofShape == null) { roofShape = defaults.roofShape; }

			try {

				roof = Roof.createRoofForShape(roofShape, polygon, tags, roofHeight, materialRoof);

			} catch (InvalidGeometryException e) {
				System.err.println("falling back to FlatRoof for " + area + ": " + e);
				roof = new FlatRoof(polygon, tags, materialRoof);
			}

		}

		if (roof instanceof FlatRoof) {
			roofHeight = 0.0f;
		}

		int roofLevels = 0;

		if (roofHeight > 1) {
			roofLevels = (int)(float) parseOsmDecimal(tags.getValue("roof:levels"), false, 0);
		}

		/* determine building height */

		double fallbackHeight = buildingLevels * heightPerLevel + roofHeight;
		double height = parseHeight(tags, (float)fallbackHeight);

		// Make sure buildings have at least some height
		height = max(height, 0.01);

		heightWithoutRoof = height - roofHeight;

		/* collect all indoor elements */

		Boolean renderIndoor = true;

		if (!tags.containsKey("building:levels")){
			renderIndoor = false;
		}

		if (tags.containsKey("min_level") && tags.containsKey("max_level")) {
			if ((roofLevels + buildingLevels) - minLevelWithUnderground != (max_level - min_level) + 1 - non_existent_levels.size()) {
				renderIndoor = false;
				System.err.println("Warning: min_level, max_level and non_existent_levels do not match building:levels");
			}
		}

		if (renderIndoor) {

			ArrayList<MapElement> indoorElements = new ArrayList<>();

			List<Integer> levelsWithNoObject = new ArrayList<>();

			for (int i = minLevelWithUnderground; i < buildingLevels + roofLevels; i++) {
				levelsWithNoObject.add(i);
			}

			// Find all overlapping indoor elements

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				MapElement other = overlap.getOther(area);
				if (other.getTags().containsKey("indoor")) {

					if (other.getTags().containsKey("level")) {

						/* check all elements are within height limits of building part */

						List<Integer> levelList = parseLevels(other.getTags().getValue("level"));

						if (levelList != null) {

							//TODO handle elements that span building parts

							if (levelConversion(levelList.get(0)) >= minLevelWithUnderground
									&& levelConversion(levelList.get(levelList.size() - 1)) < getBuildingLevels() + roofLevels
									&& !non_existent_levels.contains(levelList.get(0))
									&& !non_existent_levels.contains(levelList.get(levelList.size() - 1)) ) {

								// Handle level elements

								if (other.getTags().contains("indoor", "level")
										&& levelsWithNoObject.contains(levelConversion(parseLevels(other.getTags().getValue("level")).get(0)))) {
									Level l = new Level(other);
									levels.put(l.getLevel(), l);
									levelsWithNoObject.remove((Integer) l.getLevel());
								} else if (!other.getTags().contains("indoor", "level")) {

									// Ensure all nodes are within the building part

									PolygonWithHolesXZ buildingOutline = new PolygonWithHolesXZ(this.getPolygon().getOuter(), emptyList());

									boolean toAdd = false;
									if (other instanceof MapNode) {
										if (buildingOutline.contains(((MapNode) other).getPos())) {
											toAdd = true;
										}
									}else if (other instanceof MapWay) {
										toAdd = true;
										System.out.println("MapWay");
//										for (LineSegmentXZ lineSegment : ((MapWay) other).getWaySegments().stream().map(w -> w.getLineSegment()).collect(Collectors.toList())) {
//											if (!buildingOutline.contains(lineSegment)){
//												toAdd = false;
//											}
//										}
									} else if (other instanceof MapArea) {
										if (buildingOutline.contains(((MapArea) other).getOuterPolygon())) {
											toAdd = true;
										}
									} else if (other instanceof MapWaySegment) {
										if (buildingOutline.contains(((MapWaySegment) other).getLineSegment())) {
											toAdd = true;
										}
									} else {
										System.out.println(other.getClass().toString());
									}

									if (toAdd) {
										indoorElements.add(other);
									}
								}

							}
						}
					}
				}
			}

			// Instantiate level objects for undefined levels

			for (Integer levelNo : levelsWithNoObject) {
				levels.put(levelNo, new Level(levelNo));
			}

			// Update levels with no height data

			Float defaultHeight = calculateDefaultHeight();
			Float roofLevelsDefaultHeight = calculateRoofLevelsDefaultHeight(roofHeight);

			Float cumHeightAboveBase = 0f;

			for (int levelNo = buildingMinLevel; levelNo < buildingLevels; levelNo++) {
				Level lev = levels.get(levelNo);
				lev.updateHeight(defaultHeight);

				lev.setFloorEleAboveBase(cumHeightAboveBase);
				cumHeightAboveBase += lev.getHeight();
			}

			for (int levelNo = buildingLevels; levelNo < buildingLevels + roofLevels; levelNo++) {
				Level lev = levels.get(levelNo);
				lev.updateHeight(roofLevelsDefaultHeight);

				lev.setFloorEleAboveBase(cumHeightAboveBase);
				cumHeightAboveBase += lev.getHeight();
			}

			Float cumHeightBelowBase = 0f;

			for (int levelNo = buildingMinLevel - 1; levelNo > minLevelWithUnderground - 1; levelNo--) {
				Level lev = levels.get(levelNo);
				lev.updateHeight(defaultHeight);

				cumHeightBelowBase -= lev.getHeight();
				lev.setFloorEleAboveBase(cumHeightBelowBase);
			}

			if (!indoorElements.isEmpty()) {
				indoor = new Indoor(indoorElements, this);
			}
		}

		for (int i = getMinLevel(); i < getBuildingLevels() + 1; i++) {
			getBuilding().addListWindowNodes(area.getBoundaryNodes(), i);
		}

	}

	/** creates the walls, floors etc. making up this part */
	private void createComponents() {

		Material materialWall = createWallMaterial(tags, config);

		double floorHeight = calculateFloorHeight();

		/* find passages through this building part */

		double clearingAbovePassage = 2.5;

		List<TerrainBoundaryWorldObject> buildingPassages = area.getOverlaps().stream()
				.map(o -> o.getOther(area))
				.filter(o -> o.getTags().containsAny(asList("tunnel"), asList("building_passage", "passage")))
				.filter(o -> o.getPrimaryRepresentation() instanceof TerrainBoundaryWorldObject)
				.map(o -> (TerrainBoundaryWorldObject)o.getPrimaryRepresentation())
				.filter(o -> o.getGroundState() == GroundState.ON)
				.filter(o -> clearingAbovePassage > floorHeight)
				.collect(toList());

		if (buildingPassages.isEmpty()) {

			/* create walls and floor normally (no building passages) */

			walls = splitIntoWalls(area, this);

			if (floorHeight > 0) {
				floors = singletonList(new Floor(this, materialWall, polygon, floorHeight));
			} else {
				floors = emptyList();
			}

		} else {

			Map<PolygonWithHolesXZ, Double> polygonFloorHeightMap = new HashMap<>();

			/* construct those polygons where the area does not overlap with terrain boundaries */

			List<SimplePolygonShapeXZ> subtractPolygons = new ArrayList<>();

			for (TerrainBoundaryWorldObject o : buildingPassages) {

				SimplePolygonShapeXZ subtractPoly = o.getOutlinePolygonXZ().getOuter();

				subtractPolygons.add(subtractPoly);

				if (o instanceof WaySegmentWorldObject) {

					// extend the subtract polygon for segments that end
					// at a common node with this building part's outline.
					// (otherwise, the subtract polygon will probably
					// not exactly line up with the polygon boundary)

					WaySegmentWorldObject waySegmentWO = (WaySegmentWorldObject)o;
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

			subtractPolygons.addAll(roof.getPolygon().getHoles());

			Collection<PolygonWithHolesXZ> buildingPartPolys =
					CAGUtil.subtractPolygons(
							roof.getPolygon().getOuter(),
							subtractPolygons);

			for (PolygonWithHolesXZ p : buildingPartPolys) {
				polygonFloorHeightMap.put(p, floorHeight);
			}

			/* construct the polygons directly above the passages */

			for (TerrainBoundaryWorldObject o : buildingPassages) {

				Collection<PolygonWithHolesXZ> raisedBuildingPartPolys;

				Collection<PolygonWithHolesXZ> polysAboveTBWOs =
						CAGUtil.intersectPolygons(Arrays.asList(
								polygon.getOuter(), o.getOutlinePolygon().getSimpleXZPolygon()));


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

			/* create the walls and floors */

			floors = new ArrayList<>();
			walls = new ArrayList<>();

			for (PolygonWithHolesXZ polygon : polygonFloorHeightMap.keySet()) {

				floors.add(new Floor(this, materialWall, polygon, polygonFloorHeightMap.get(polygon)));

				for (SimplePolygonXZ ring : polygon.getRings()) {
					ring = polygon.getOuter().equals(ring) ? ring.makeCounterclockwise() : ring.makeClockwise();
					ring = ring.getSimplifiedPolygon();
					for (int i = 0; i < ring.size(); i++) {
						walls.add(new Wall(null, this,
								asList(ring.getVertex(i), ring.getVertex(i + 1)),
								emptyMap(),
								polygonFloorHeightMap.get(polygon)));
					}
				}

			}

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
	static List<Wall> splitIntoWalls(MapArea buildingPartArea, BuildingPart buildingPart) {

		List<Wall> result = new ArrayList<>();

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

						result.add(new Wall(wallWay, buildingPart, currentWallNodes));

					}
					currentWallNodes = new ArrayList<>();
				}

				currentWallNodes.add(nodes.get(i));

			} while (i != firstSplitNode);

		}

		return result;

	}

	@Override
	public void renderTo(Target target) {

		if (walls == null) {
			// the reason why this is called here rather than the constructor is tunnel=building_passage:
			// in the constructor, the roads' calculations aren't completed yet
			createComponents();
		}

		if (!config.getBoolean("noOuterWalls", false)){
			walls.forEach(w -> w.renderTo(target));
		}

		if (!config.getBoolean("noRoofs", false)) {
			roof.renderTo(target, building.getGroundLevelEle() + heightWithoutRoof);
		}


		// TODO don't render floors inside building

		floors.forEach(f -> f.renderTo(target));

		if (indoor != null){
			indoor.renderTo(target);
		}

	}

	public Collection<AttachmentSurface> getAttachmentSurfaces() {

		List<AttachmentSurface> surfaces = new ArrayList<>();

		if (indoor != null) {
			surfaces.addAll(indoor.getAttachmentSurfaces());
		}

		surfaces.addAll(roof.getAttachmentSurfaces(building.getGroundLevelEle() + heightWithoutRoof, max_level + 1));

		return surfaces;
	}

	public PolygonWithHolesXZ getPolygon() {
		return polygon;
	}

	public Roof getRoof() {
		return roof;
	}

	public Building getBuilding() { return building; }

	public Configuration getConfig() { return config; }

	public TagSet getTags() { return tags; }

	int getBuildingLevels() { return buildingLevels; }

	public int getMinLevel() { return buildingMinLevel; }

	public Indoor getIndoor(){ return indoor; }

	public double getHeightWithoutRoof() { return heightWithoutRoof; }

	/** returns the distance between the bottom and the top of a level */
	public double getLevelHeight(int level) {
		if (levels.containsKey(level)) {
			return levels.get(level).getHeight();
		} else {
			return 2.5;
		}
	}

	public double getLevelHeightAboveBase(int level) {
		return levels.get(level) != null ? levels.get(level).getFloorEleAboveBase() : 0;
	}

	public Double getBuildingPartBaseEle(){
		return building.getGroundLevelEle() + ((heightWithoutRoof/buildingLevels) * buildingMinLevel);
	}

	public double calculateFloorHeight() {

		if (tags.containsKey("min_height")) {

			Float minHeight = parseMeasure(tags.getValue("min_height"));
			if (minHeight != null) {
				return minHeight;
			}

		}

		if (buildingMinLevel > 0) {

			return (heightWithoutRoof / buildingLevels) * buildingMinLevel;

		}

		if (tags.contains("building", "roof") || tags.contains("building:part", "roof")) {

			return heightWithoutRoof - 0.3;

		}

		return 0;

	}

	private Float calculateDefaultHeight(){

		Float cumUndeterminedHeight = (float) heightWithoutRoof - (float) ((heightWithoutRoof/buildingLevels) * buildingMinLevel);
		int noDefaultHeightLevels = 0;

		// Underground and roof level heights are not taken into account

		for (Integer levelNo : levels.keySet()){
			if (levelNo >= 0 && levelNo < buildingLevels) {
				if (levels.get(levelNo).getHeight() == 0) {
					noDefaultHeightLevels += 1;
				}
				cumUndeterminedHeight -= levels.get(levelNo).getHeight();
			}
		}

		return cumUndeterminedHeight/noDefaultHeightLevels;
	}

	private Float calculateRoofLevelsDefaultHeight(Float roofHeight){

		if (roofHeight < 1){
			return null;
		} else {
			Float cumUndeterminedHeight = roofHeight;
			int noDefaultHeightLevels = 0;

			for (Integer levelNo : levels.keySet()){
				if (levelNo >= buildingLevels) {
					if (levels.get(levelNo).getHeight() == 0) {
						noDefaultHeightLevels += 1;
					}
					cumUndeterminedHeight -= levels.get(levelNo).getHeight();
				}
			}

			return cumUndeterminedHeight/noDefaultHeightLevels;
		}
	}

	public boolean containsLevel(int level){
		return levels.containsKey(level);
	}

	public int levelConversion(Integer level){

			int temp = level - min_level;

			for (Integer noLevel : non_existent_levels) {
				if (level >= noLevel) {
					temp -= 1;
				}
			}

			return minLevelWithUnderground + temp;

	}

	public int levelReversion(Integer level){

			int temp = level - minLevelWithUnderground;

			temp += min_level;

			for (Integer noLevel : non_existent_levels) {
				if (level > levelConversion(noLevel)) {
					temp += 1;
				}
			}

			return temp;

	}

	private final class Level{

		private final String name;
		private final String ref;
		private	Float height;
		private final int level;
		private Float floorEleAboveBase = 0f;

		private PolygonWithHolesXZ outlineArea;

		Level(MapElement element){
			TagSet tags = element.getTags();

			this.name = tags.getValue("name");
			this.ref = tags.getValue("ref");
			this.level = levelConversion(parseLevels(tags.getValue("level")).get(0));

			// Default heights need to be calculated once all levels are accounted for
			this.height = parseHeight(tags, 0);

			if (element instanceof MapArea){
				this.outlineArea = ((MapArea) element).getPolygon();
			}

		}

		Level(Integer levelNo){

			this.name = null;
			this.ref = null;
			this.level = levelNo;
			this.height = 0f;
			this.outlineArea = null;

		}

		String getName() { return name; }

		String getRef() { return ref; }

		Float getHeight() { return height; }

		int getLevel() { return level; }

		Float getFloorEleAboveBase() { return floorEleAboveBase; }

		void setFloorEleAboveBase(Float floorEleAboveBase) { this.floorEleAboveBase = floorEleAboveBase; }

		void updateHeight(Float defaultHeight){
			if (height == 0){
				height = defaultHeight;
			}
		}

		PolygonWithHolesXZ getOutlineArea() {
			if (outlineArea == null){
				return polygon;
			} else {
				return outlineArea;
			}
		}
	}

	static Material createWallMaterial(TagSet tags, Configuration config) {

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

	private static Material createRoofMaterial(TagSet tags, Configuration config) {

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
			} else if ("glass".equals(materialString)) {
				material = roof ? Materials.GLASS_ROOF : Materials.GLASS_WALL;
			} else if ("wood".equals(materialString)) {
				material = Materials.WOOD_WALL;
			} else if (Materials.getSurfaceMaterial(materialString) != null) {
				material = Materials.getSurfaceMaterial(materialString);
			}
		}

		boolean colorable = material.getNumTextureLayers() == 0
				|| material.getTextureDataList().get(0).colorable;

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

}
