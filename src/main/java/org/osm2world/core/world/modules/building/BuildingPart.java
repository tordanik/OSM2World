package org.osm2world.core.world.modules.building;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getLast;
import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Collections.max;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.Poly2TriUtil;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.util.exception.TriangulationException;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;

/**
 * part of a building, as defined by the Simple 3D Buildings standard.
 * Consists of {@link Wall}s, a {@link Roof}, and maybe a {@link Floor}.
 * This is the core class of the {@link BuildingModule}.
 */
public class BuildingPart implements Renderable {

	final Building building;
	final MapArea area;
	private final PolygonWithHolesXZ polygon;

	final Configuration config;

	/** the tags for this part, including tags inherited from the parent */
	final TagSet tags;

	int buildingLevels;
	private int minLevel;

	double heightWithoutRoof;

	Roof roof;
	private Material materialRoof;

	private List<Wall> walls = null;
	private List<BuildingPart.Floor> floors = null;

	public BuildingPart(Building building, MapArea area, Configuration config) {

		this.building = building;
		this.area = area;
		this.polygon = area.getPolygon();

		this.config = config;

		this.tags = inheritTags(area.getTags(), building.getPrimaryMapElement().getTags());

		setLevelsHeightAndRoof();

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

					for (SimplePolygonXZ p : polygon.getPolygons()) {
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

				for (SimplePolygonXZ ring : polygon.getPolygons()) {
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

	public PolygonWithHolesXZ getPolygon() {
		return polygon;
	}

	public Roof getRoof() {
		return roof;
	}

	@Override
	public void renderTo(Target target) {

		if (walls == null) {
			// the reason why this is called here rather than the constructor is tunnel=building_passage:
			// in the constructor, the roads' calculations aren't completed yet
			createComponents();
		}

		walls.forEach(w -> w.renderTo(target));

		roof.renderTo(target);

		floors.forEach(f -> f.renderTo(target));

	}

	/** returns the distance between the bottom and the top of a level */
	public double getLevelHeight(int level) {
		//TODO: in the future, allow levels of different height (e.g. based on indoor=level elements)
		return heightWithoutRoof / buildingLevels;
	}

	public double getLevelHeightAboveBase(int level) {
		return (heightWithoutRoof / buildingLevels) * level;
	}

	double calculateFloorHeight() {

		if (tags.containsKey("min_height")) {

			Float minHeight = parseMeasure(tags.getValue("min_height"));
			if (minHeight != null) {
				return minHeight;
			}

		}

		if (minLevel > 0) {

			return (heightWithoutRoof / buildingLevels) * minLevel;

		}

		if (tags.contains("building", "roof") || tags.contains("building:part", "roof")) {

			return heightWithoutRoof - 0.3;

		}

		return 0;

	}

	/**
	 * figures out level and height information, plus roofs, based on the building's and building part's tags.
	 * If available, explicitly tagged data is used, with tags of the building part overriding building tags.
	 * Otherwise, the values depend on indirect assumptions (level height)
	 * or ultimately the building class as determined by the "building" and "building:part" keys.
	 */
	private void setLevelsHeightAndRoof() {

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		double heightPerLevel = defaults.heightPerLevel;

		/* determine levels */

		buildingLevels = defaults.levels;

		Float parsedLevels = null;

		if (tags.containsKey("building:levels")) {
			parsedLevels = parseOsmDecimal(tags.getValue("building:levels"), false);
		}

		if (parsedLevels != null) {
			buildingLevels = (int)(float)parsedLevels;
		} else if (parseHeight(tags, -1) > 0) {
			buildingLevels = max(1, (int)(parseHeight(tags, -1) / heightPerLevel));
		}

		minLevel = 0;

		if (tags.containsKey("building:min_level")) {
			Float parsedMinLevel = parseOsmDecimal(tags.getValue("building:min_level"), false);
			if (parsedMinLevel != null) {
				minLevel = (int)(float)parsedMinLevel;
			}
		}

		/* determine roof shape */

		if (!("no".equals(tags.getValue("roof:lines"))) && hasComplexRoof(area)) {
			roof = new ComplexRoof();
		} else {

			String roofShape = tags.getValue("roof:shape");
			if (roofShape == null) { roofShape = tags.getValue("building:roof:shape"); }
			if (roofShape == null) { roofShape = defaults.roofShape; }

			try {

				roof = createRoofForShape(roofShape);

			} catch (InvalidGeometryException e) {
				System.err.println("falling back to FlatRoof: " + e);
				roof = new FlatRoof();
			}

		}

		/* construct roof material */

		materialRoof = defaults.materialRoof;

	    if (tags.contains("roof:shape", "flat") && materialRoof == Materials.ROOF_DEFAULT) {
	    	materialRoof = Materials.CONCRETE;
	    }

	    if (config.getBoolean("useBuildingColors", true)) {
	    	materialRoof = buildMaterial(
	    			tags.getValue("roof:material"),
	    			tags.getValue("roof:colour"),
	    			materialRoof, true);

	    }

		/* determine height */

		double fallbackHeight = buildingLevels * heightPerLevel + roof.getRoofHeight();
		double height = parseHeight(tags, (float)fallbackHeight);

		// Make sure buildings have at least some height
		height = Math.max(height, 0.01);

		heightWithoutRoof = height - roof.getRoofHeight();

	}

	static Material createWallMaterial(TagSet tags, Configuration config) {

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		if (config.getBoolean("useBuildingColors", true)) {

	    	return BuildingPart.buildMaterial(
	    			tags.getValue("building:material"),
	    			tags.getValue("building:colour"),
	    			defaults.materialWall, false);

	    } else {

	    	return defaults.materialWall;

	    }

	}

	static Material buildMaterial(String materialString,
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

	private Roof createRoofForShape(String roofShape) {

		switch (roofShape) {
		case "pyramidal": return new PyramidalRoof();
		case "onion": return new OnionRoof();
		case "skillion": return new SkillionRoof();
		case "gabled": return new GabledRoof();
		case "hipped": return new HippedRoof();
		case "half-hipped": return new HalfHippedRoof();
		case "gambrel": return new GambrelRoof();
		case "mansard": return new MansardRoof();
		case "dome": return new DomeRoof();
		case "round": return new RoundRoof();
		default: return new FlatRoof();
		}

	}

	private static class Floor implements Renderable {

		private final BuildingPart buildingPart;
		private final Material material;
		private final PolygonWithHolesXZ polygon;
		private final double floorHeight;

		public Floor(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeight) {
			this.buildingPart = buildingPart;
			this.material = material;
			this.polygon = polygon;
			this.floorHeight = floorHeight;
		}

		@Override
		public void renderTo(Target target) {

			double floorEle = buildingPart.building.getGroundLevelEle() + floorHeight;

			Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon);

			List<TriangleXYZ> trianglesXYZ = triangles.stream()
					.map(t -> t.makeClockwise().xyz(floorEle))
					.collect(toList());

			target.drawTriangles(material, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

		}

	}

	private static final float DEFAULT_RIDGE_HEIGHT = 5;

	/**
	 * superclass for roofs based on roof:type tags.
	 * Contains common functionality, such as roof height parsing.
	 */
	abstract private class TaggedRoof implements Roof {

		protected final double roofHeight;

		/*
		 * default roof height if no value is tagged explicitly.
		 * Can optionally be overwritten by subclasses.
		 */
		protected float getDefaultRoofHeight() {
			if (buildingLevels == 1) {
				return 1;
			} else {
				return DEFAULT_RIDGE_HEIGHT;
			}
		}

		TaggedRoof() {

			Float taggedHeight = null;

			if (tags.containsKey("roof:height")) {
				String valueString = tags.getValue("roof:height");
				taggedHeight = parseMeasure(valueString);
			} else if (tags.containsKey("roof:levels")) {
				try {
					taggedHeight = 2.5f * Integer.parseInt(tags.getValue("roof:levels"));
				} catch (NumberFormatException e) {}
			}

			roofHeight = taggedHeight != null ? taggedHeight : getDefaultRoofHeight();

		}

		@Override
		public double getRoofHeight() {
			return roofHeight;
		}

		@Override
		public double getMaxRoofEle() {
			return building.getGroundLevelEle() + heightWithoutRoof + roofHeight;
		}

	}

	private abstract class SpindleRoof extends TaggedRoof {

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}

		@Override
		public double getRoofEleAt(VectorXZ pos) {
			return getMaxRoofEle() - getRoofHeight();
		}

		protected void renderSpindle(Target target, Material material, SimplePolygonXZ polygon,
				List<Double> heights, List<Double> scaleFactors) {

			checkArgument(heights.size() == scaleFactors.size(), "heights and scaleFactors must have same size");

			VectorXZ center = polygon.getCenter();

			/* calculate the polygon relative to the center */

			List<VectorXZ> vertexLoop = new ArrayList<>();

			for (VectorXZ v : polygon.makeCounterclockwise().getVertexList()) {
				vertexLoop.add(v.subtract(center));
			}

			ShapeXZ spindleShape = new SimplePolygonXZ(vertexLoop);

			/* construct a path from the heights */

			List<VectorXYZ> path = new ArrayList<>();

			for (double height : heights) {
				path.add(center.xyz(height));
			}

			/* render the roof using shape extrusion */

			target.drawExtrudedShape(materialRoof, spindleShape, path,
					nCopies(path.size(), Z_UNIT), scaleFactors,
					spindleTexCoordLists(path, spindleShape.getVertexList().size(),
							polygon.getOutlineLength(), material),
					null);

		}

		protected List<List<VectorXZ>> spindleTexCoordLists(List<VectorXYZ> path, int shapeVertexCount,
				double polygonLength, Material material) {

			List<TextureData> textureDataList = material.getTextureDataList();

			switch (textureDataList.size()) {

			case 0: return emptyList();

			case 1: return singletonList(spindleTexCoordList(path,
					shapeVertexCount, polygonLength, textureDataList.get(0)));

			default:

				List<List<VectorXZ>> result = new ArrayList<>();

				for (TextureData textureData : textureDataList) {
					result.add(spindleTexCoordList(path,
							shapeVertexCount, polygonLength, textureData));
				}

				return result;

			}

		}

		protected List<VectorXZ> spindleTexCoordList(List<VectorXYZ> path, int shapeVertexCount,
				double polygonLength, TextureData textureData) {

			List<VectorXZ> result = new ArrayList<>();

			double accumulatedTexHeight = 0;

			for (int i = 0; i < path.size(); i++) {

				if (i > 0) {

					accumulatedTexHeight += path.get(i - 1).distanceTo(path.get(i));

					//TODO use the distance on the extruded surface instead of on the path,
					//e.g. += rings[i-1].get(0).distanceTo(rings[i].get(0));
				}

				result.addAll(spindleTexCoordListForRing(shapeVertexCount,
						polygonLength, accumulatedTexHeight, textureData));

			}

			return result;

		}

		private List<VectorXZ> spindleTexCoordListForRing(int shapeVertexCount, double polygonLength,
				double accumulatedTexHeight, TextureData textureData) {

			double textureRepeats = max(1, round(polygonLength / textureData.width));

			double texWidthSteps = textureRepeats / (shapeVertexCount - 1);

			double texZ = accumulatedTexHeight / textureData.height;

			VectorXZ[] texCoords = new VectorXZ[shapeVertexCount];

			for (int i = 0; i < shapeVertexCount; i++) {
				texCoords[i] = new VectorXZ(i*texWidthSteps, texZ);
			}

			return asList(texCoords);

		}

		@Override
		protected float getDefaultRoofHeight() {
			return (float)polygon.getOuter().getDiameter() / 2;
		}

	}

	private class OnionRoof extends SpindleRoof {

		@Override
		public void renderTo(Target target) {

			double roofY = getMaxRoofEle() - getRoofHeight();

			renderSpindle(target, materialRoof,
					polygon.getOuter().makeClockwise(),
					asList(roofY,
							roofY + 0.15 * roofHeight,
							roofY + 0.52 * roofHeight,
							roofY + 0.72 * roofHeight,
							roofY + 0.82 * roofHeight,
							roofY + 1.0 * roofHeight),
					asList(1.0, 0.8, 1.0, 0.7, 0.15, 0.0));

		}

		@Override
		protected float getDefaultRoofHeight() {
			return (float)polygon.getOuter().getDiameter();
		}

	}

	private class DomeRoof extends SpindleRoof {

		/**
		 * number of height rings to approximate the round dome shape
		 */
		private static final int HEIGHT_RINGS = 10;

		@Override
		public void renderTo(Target target) {

			double roofY = getMaxRoofEle() - getRoofHeight();

			List<Double> heights = new ArrayList<>();
			List<Double> scales = new ArrayList<>();

			for (int ring = 0; ring < HEIGHT_RINGS; ++ring) {
				double relativeHeight = (double)ring / (HEIGHT_RINGS - 1);
				heights.add(roofY + relativeHeight * roofHeight);
				scales.add(sqrt(1.0 - relativeHeight * relativeHeight));
			}

			renderSpindle(target, materialRoof,
					polygon.getOuter().makeClockwise(),
					heights, scales);

		}

	}

	/**
	 * superclass for roofs that have exactly one height value
	 * for each point within their XZ polygon
	 */
	public abstract class HeightfieldRoof extends TaggedRoof {

		/**
		 * returns segments within the roof polygon
		 * that define ridges or edges of the roof
		 */
		public abstract Collection<LineSegmentXZ> getInnerSegments();

		/**
		 * returns segments within the roof polygon
		 * that define apex nodes of the roof
		 */
		public abstract Collection<VectorXZ> getInnerPoints();

		/**
		 * returns roof elevation at a position.
		 * Only required to work for positions that are part of the
		 * polygon, segments or points for the roof.
		 *
		 * @return  elevation, null if unknown
		 */
		protected abstract Double getRoofEleAt_noInterpolation(VectorXZ pos);

		@Override
		public double getRoofEleAt(VectorXZ v) {

			Double ele = getRoofEleAt_noInterpolation(v);

			if (ele != null) {
				return ele;
			} else {

				// get all segments from the roof

				//TODO (performance): avoid doing this for every node

				Collection<LineSegmentXZ> segments =
					new ArrayList<>();

				segments.addAll(this.getInnerSegments());
				segments.addAll(this.getPolygon().getOuter().getSegments());
				for (SimplePolygonXZ hole : this.getPolygon().getHoles()) {
					segments.addAll(hole.getSegments());
				}

				// find the segment with the closest distance to the node

				LineSegmentXZ closestSegment = null;
				double closestSegmentDistance = Double.MAX_VALUE;

				for (LineSegmentXZ segment : segments) {
					double segmentDistance = distanceFromLineSegment(v, segment);
					if (segmentDistance < closestSegmentDistance) {
						closestSegment = segment;
						closestSegmentDistance = segmentDistance;
					}
				}

				// use that segment for height interpolation

				return interpolateValue(v,
						closestSegment.p1,
						getRoofEleAt_noInterpolation(closestSegment.p1),
						closestSegment.p2,
						getRoofEleAt_noInterpolation(closestSegment.p2));

			}
		}

		@Override
		public void renderTo(Target target) {

			/* create the triangulation of the roof */

			Collection<TriangleXZ> triangles;

			try {

				triangles = Poly2TriUtil.triangulate(
						getPolygon().getOuter(),
					    getPolygon().getHoles(),
					    getInnerSegments(),
					    getInnerPoints());

			} catch (TriangulationException e) {

				triangles = JTSTriangulationUtil.triangulate(
						getPolygon().getOuter(),
						getPolygon().getHoles(),
						getInnerSegments(),
						getInnerPoints());

			}

			List<TriangleXYZ> trianglesXYZ = new ArrayList<>(triangles.size());

			for (TriangleXZ triangle : triangles) {
				TriangleXZ tCCW = triangle.makeCounterclockwise();
				trianglesXYZ.add(new TriangleXYZ(
						withRoofEle(tCCW.v1),
						withRoofEle(tCCW.v2),
						withRoofEle(tCCW.v3)));
				//TODO: avoid duplicate objects for points in more than one triangle
			}

			/* draw triangles */

			target.drawTriangles(materialRoof, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, materialRoof, SLOPED_TRIANGLES));

		}

		private VectorXYZ withRoofEle(VectorXZ v) {
			return v.xyz(getRoofEleAt(v));
		}

	}

	private class FlatRoof extends HeightfieldRoof {

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return emptyList();
		}

		@Override
		public double getRoofHeight() {
			return 0;
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			return getMaxRoofEle();
		}

		@Override
		public double getMaxRoofEle() {
			return building.getGroundLevelEle() + heightWithoutRoof;
		}

	}

	private class PyramidalRoof extends HeightfieldRoof {

		private final VectorXZ apex;
		private final List<LineSegmentXZ> innerSegments;

		public PyramidalRoof() {

			super();

			SimplePolygonXZ outerPoly = polygon.getOuter();

			apex = outerPoly.getCentroid();

			innerSegments = new ArrayList<>();
			for (VectorXZ v : outerPoly.getVertices()) {
				innerSegments.add(new LineSegmentXZ(v, apex));
			}

		}

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return singletonList(apex);
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return innerSegments;
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			if (apex.equals(pos)) {
				return getMaxRoofEle();
			} else if (polygon.getOuter().getVertices().contains(pos)) {
				return getMaxRoofEle() - roofHeight;
			} else {
				return null;
			}
		}

	}

	private class SkillionRoof extends HeightfieldRoof {

		private final LineSegmentXZ ridge;
		private final double roofLength;

		public SkillionRoof() {

			/* parse slope direction */

			VectorXZ slopeDirection = null;

			if (tags.containsKey("roof:direction")) {
				Float angle = parseAngle(tags.getValue("roof:direction"));
				if (angle != null) {
					slopeDirection = VectorXZ.fromAngle(toRadians(angle));
				}
			}

			if (slopeDirection != null) {

				SimplePolygonXZ simplifiedOuter = polygon.getOuter().getSimplifiedPolygon();

				/* find ridge by calculating the outermost intersections of
				 * the quasi-infinite slope "line" towards the centroid vector
				 * with segments of the polygon */

				VectorXZ center = simplifiedOuter.getCentroid();

				Collection<LineSegmentXZ> intersectedSegments = simplifiedOuter.intersectionSegments(
						new LineSegmentXZ(center.add(slopeDirection.mult(-1000)), center));

				ridge = max(intersectedSegments, comparingDouble(i -> distanceFromLineSegment(center, i)));

				/* calculate maximum distance from ridge */

				roofLength = polygon.getOuter().getVertexList().stream()
						.mapToDouble(v -> distanceFromLine(v, ridge.p1, ridge.p2))
						.max().getAsDouble();

			} else {

				ridge = null;
				roofLength = Double.NaN;

			}

		}

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return emptyList();
		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		protected Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			if (ridge == null) {
				return getMaxRoofEle();
			} else {
				double distance = distanceFromLine(pos, ridge.p1, ridge.p2);
				double relativeDistance = distance / roofLength;
				return getMaxRoofEle() - relativeDistance * roofHeight;
			}
		}



	}

	/**
	 * tagged roof with a ridge.
	 * Deals with ridge calculation for various subclasses.
	 */
	abstract private class RoofWithRidge extends HeightfieldRoof {

		/** absolute distance of ridge to outline */
		protected final double ridgeOffset;

		protected final LineSegmentXZ ridge;

		/** the roof cap that is closer to the first vertex of the ridge */
		protected final LineSegmentXZ cap1;
		/** the roof cap that is closer to the second vertex of the ridge */
		protected final LineSegmentXZ cap2;

		/** maximum distance of any outline vertex to the ridge */
		protected final double maxDistanceToRidge;

		/**
		 * creates an instance and calculates the final fields
		 *
		 * @param relativeRoofOffset  distance of ridge to outline
		 *    relative to length of roof cap; 0 if ridge ends at outline
		 */
		public RoofWithRidge(double relativeRoofOffset) {

			super();

			SimplePolygonXZ outerPoly = polygon.getOuter();

			SimplePolygonXZ simplifiedPolygon =
				outerPoly.getSimplifiedPolygon();

			/* determine ridge direction based on tag if it exists,
			 * otherwise choose direction of longest polygon segment */

			VectorXZ ridgeDirection = null;

			if (tags.containsKey("roof:direction")) {
				Float angle = parseAngle(tags.getValue("roof:direction"));
				if (angle != null) {
					ridgeDirection = VectorXZ.fromAngle(toRadians(angle)).rightNormal();
				}
			}

			if (ridgeDirection == null && tags.containsKey("roof:ridge:direction")) {
				Float angle = parseAngle(tags.getValue("roof:ridge:direction"));
				if (angle != null) {
					ridgeDirection = VectorXZ.fromAngle(toRadians(angle));
				}
			}

			if (ridgeDirection == null) {

				LineSegmentXZ longestSeg = max(simplifiedPolygon.getSegments(), comparingDouble(s -> s.getLength()));

				ridgeDirection = longestSeg.p2.subtract(longestSeg.p1).normalize();

				if (tags.contains("roof:orientation", "across")) {
					ridgeDirection = ridgeDirection.rightNormal();
				}

			}

			/* calculate the two outermost intersections of the
			 * quasi-infinite ridge line with segments of the polygon */

			VectorXZ p1 = outerPoly.getCentroid();

			Collection<LineSegmentXZ> intersections =
				simplifiedPolygon.intersectionSegments(new LineSegmentXZ(
						p1.add(ridgeDirection.mult(-1000)),
						p1.add(ridgeDirection.mult(1000))
				));

			if (intersections.size() < 2) {
				throw new InvalidGeometryException("cannot handle roof geometry for element " + area);
			}

			//TODO choose outermost instead of any pair of intersections
			Iterator<LineSegmentXZ> it = intersections.iterator();
			cap1 = it.next();
			cap2 = it.next();

			/* base ridge on the centers of the intersected segments
			 * (the intersections itself are not used because the
			 * tagged ridge direction is likely not precise)       */

			VectorXZ c1 = cap1.getCenter();
			VectorXZ c2 = cap2.getCenter();

			ridgeOffset = min(
					cap1.getLength() * relativeRoofOffset,
					0.4 * c1.distanceTo(c2));

			if (relativeRoofOffset == 0) {

				ridge = new LineSegmentXZ(c1, c2);

			} else {

				ridge = new LineSegmentXZ(
						c1.add( p1.subtract(c1).normalize().mult(ridgeOffset) ),
						c2.add( p1.subtract(c2).normalize().mult(ridgeOffset) ));

			}

			/* calculate maxDistanceToRidge */

			maxDistanceToRidge = outerPoly.getVertices().stream()
					.mapToDouble(v -> distanceFromLineSegment(v, ridge))
					.max().getAsDouble();

		}

	}

	private class GabledRoof extends RoofWithRidge {

		public GabledRoof() {
			super(0);
		}

		@Override
		public PolygonWithHolesXZ getPolygon() {

			PolygonXZ newOuter = polygon.getOuter();

			newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);

			return new PolygonWithHolesXZ(
					newOuter.asSimplePolygon(),
					polygon.getHoles());

		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return singleton(ridge);
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			double distRidge = distanceFromLineSegment(pos, ridge);
			double relativePlacement = distRidge / maxDistanceToRidge;
			return getMaxRoofEle() - roofHeight * relativePlacement;
		}

	}

	private class HippedRoof extends RoofWithRidge {

		public HippedRoof() {
			super(1/3.0);
		}

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return asList(
					ridge,
					new LineSegmentXZ(ridge.p1, cap1.p1),
					new LineSegmentXZ(ridge.p1, cap1.p2),
					new LineSegmentXZ(ridge.p2, cap2.p1),
					new LineSegmentXZ(ridge.p2, cap2.p2));
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
				return getMaxRoofEle();
			} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) {
				return getMaxRoofEle() - roofHeight;
			} else {
				return null;
			}
		}

	}

	private class HalfHippedRoof extends RoofWithRidge {

		private final LineSegmentXZ cap1part, cap2part;

		public HalfHippedRoof() {

			super(1/6.0);

			cap1part = new LineSegmentXZ(
					interpolateBetween(cap1.p1, cap1.p2, 0.5 - ridgeOffset / cap1.getLength()),
					interpolateBetween(cap1.p1, cap1.p2, 0.5 + ridgeOffset / cap1.getLength()));

			cap2part = new LineSegmentXZ(
					interpolateBetween(cap2.p1, cap2.p2, 0.5 - ridgeOffset / cap1.getLength()),
					interpolateBetween(cap2.p1, cap2.p2, 0.5 + ridgeOffset / cap1.getLength()));

		}

		@Override
		public PolygonWithHolesXZ getPolygon() {

			PolygonXZ newOuter = polygon.getOuter();

			newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);

			return new PolygonWithHolesXZ(newOuter.asSimplePolygon(), polygon.getHoles());

		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return asList(ridge,
					new LineSegmentXZ(ridge.p1, cap1part.p1),
					new LineSegmentXZ(ridge.p1, cap1part.p2),
					new LineSegmentXZ(ridge.p2, cap2part.p1),
					new LineSegmentXZ(ridge.p2, cap2part.p2));
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
				return getMaxRoofEle();
			} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) {
				if (distanceFromLineSegment(pos, cap1part) < 0.05) {
					return getMaxRoofEle()
						- roofHeight * ridgeOffset / (cap1.getLength()/2);
				} else if (distanceFromLineSegment(pos, cap2part) < 0.05) {
					return getMaxRoofEle()
						- roofHeight * ridgeOffset / (cap2.getLength()/2);
				} else {
					return getMaxRoofEle() - roofHeight;
				}
			} else {
				return null;
			}
		}

	}

	private class GambrelRoof extends RoofWithRidge {

		private final LineSegmentXZ cap1part, cap2part;

		public GambrelRoof() {

			super(0);

			cap1part = new LineSegmentXZ(
					interpolateBetween(cap1.p1, cap1.p2, 1/6.0),
					interpolateBetween(cap1.p1, cap1.p2, 5/6.0));

			cap2part = new LineSegmentXZ(
					interpolateBetween(cap2.p1, cap2.p2, 1/6.0),
					interpolateBetween(cap2.p1, cap2.p2, 5/6.0));

		}

		@Override
		public PolygonWithHolesXZ getPolygon() {

			PolygonXZ newOuter = polygon.getOuter();

			newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);

			//TODO: add intersections of additional edges with outline?

			return new PolygonWithHolesXZ(
					newOuter.asSimplePolygon(),
					polygon.getHoles());

		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return asList(ridge,
					new LineSegmentXZ(cap1part.p1, cap2part.p2),
					new LineSegmentXZ(cap1part.p2, cap2part.p1));
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {

			double distRidge = distanceFromLineSegment(pos, ridge);
			double relativePlacement = distRidge / maxDistanceToRidge;

			if (relativePlacement < 2/3.0) {
				return getMaxRoofEle()
					- 1/2.0 * roofHeight * relativePlacement;
			} else {
				return getMaxRoofEle() - 1/3.0 * roofHeight
					- 2 * roofHeight * (relativePlacement - 2/3.0);
			}

		}

	}

	private class RoundRoof extends RoofWithRidge {
		private final static double ROOF_SUBDIVISION_METER = 2.5;

		private final List<LineSegmentXZ> capParts;
		private final int rings;
		private final double radius;

		public RoundRoof() {

			super(0);

			if (roofHeight < maxDistanceToRidge) {
				double squaredHeight = roofHeight * roofHeight;
				double squaredDist = maxDistanceToRidge * maxDistanceToRidge;
				double centerY =  (squaredDist - squaredHeight) / (2 * roofHeight);
				radius = sqrt(squaredDist + centerY * centerY);
			} else {
				radius = 0;
			}

			rings = (int)Math.max(3, roofHeight/ROOF_SUBDIVISION_METER);
			capParts = new ArrayList<>(rings*2);
			// TODO: would be good to vary step size with slope
			float step = 0.5f / (rings + 1);
			for (int i = 1; i <= rings; i++) {
				capParts.add(new LineSegmentXZ(
						interpolateBetween(cap1.p1, cap1.p2, i * step),
						interpolateBetween(cap1.p1, cap1.p2, 1 - i * step)));

				capParts.add(new LineSegmentXZ(
						interpolateBetween(cap2.p1, cap2.p2, i * step),
						interpolateBetween(cap2.p1, cap2.p2, 1 - i * step)));
			}
		}

		@Override
		public PolygonWithHolesXZ getPolygon() {

			PolygonXZ newOuter = polygon.getOuter();

			newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
			newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);

			for (LineSegmentXZ capPart : capParts){
				newOuter = insertIntoPolygon(newOuter, capPart.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, capPart.p2, 0.2);
			}

			//TODO: add intersections of additional edges with outline?
			return new PolygonWithHolesXZ(
					newOuter.asSimplePolygon(),
					polygon.getHoles());

		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}
		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {

			List<LineSegmentXZ> innerSegments = new ArrayList<>(rings * 2 + 1);
			innerSegments.add(ridge);
			for (int i = 0; i < rings * 2; i += 2) {
				LineSegmentXZ cap1part = capParts.get(i);
				LineSegmentXZ cap2part = capParts.get(i+1);
				innerSegments.add(new LineSegmentXZ(cap1part.p1, cap2part.p2));
				innerSegments.add(new LineSegmentXZ(cap1part.p2, cap2part.p1));
			}

			return innerSegments;
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			double distRidge = distanceFromLineSegment(pos, ridge);
			double ele;

			if (radius > 0) {
				double relativePlacement = distRidge / radius;
				ele = getMaxRoofEle() - radius
					+ sqrt(1.0 - relativePlacement * relativePlacement) * radius;
			} else {
				// This could be any interpolator
				double relativePlacement = distRidge / maxDistanceToRidge;
				ele = getMaxRoofEle() - roofHeight +
				(1 - (Math.pow(relativePlacement, 2.5))) * roofHeight;
			}

			return Math.max(ele, getMaxRoofEle() - roofHeight);
		}
	}

	private class MansardRoof extends RoofWithRidge {

		private final LineSegmentXZ mansardEdge1, mansardEdge2;

		public MansardRoof() {

			super(1/3.0);

			mansardEdge1 = new LineSegmentXZ(
					interpolateBetween(cap1.p1, ridge.p1, 1/3.0),
					interpolateBetween(cap2.p2, ridge.p2, 1/3.0));

			mansardEdge2 = new LineSegmentXZ(
					interpolateBetween(cap1.p2, ridge.p1, 1/3.0),
					interpolateBetween(cap2.p1, ridge.p2, 1/3.0));

		}

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return polygon;
		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return asList(ridge,
					mansardEdge1,
					mansardEdge2,
					new LineSegmentXZ(ridge.p1, mansardEdge1.p1),
					new LineSegmentXZ(ridge.p1, mansardEdge2.p1),
					new LineSegmentXZ(ridge.p2, mansardEdge1.p2),
					new LineSegmentXZ(ridge.p2, mansardEdge2.p2),
					new LineSegmentXZ(cap1.p1, mansardEdge1.p1),
					new LineSegmentXZ(cap2.p2, mansardEdge1.p2),
					new LineSegmentXZ(cap1.p2, mansardEdge2.p1),
					new LineSegmentXZ(cap2.p1, mansardEdge2.p2),
					new LineSegmentXZ(mansardEdge1.p1, mansardEdge2.p1),
					new LineSegmentXZ(mansardEdge1.p2, mansardEdge2.p2));
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {

			if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
				return getMaxRoofEle();
			} else if (getPolygon().getOuter().getVertexCollection().contains(pos)) {
				return getMaxRoofEle() - roofHeight;
			} else if (mansardEdge1.p1.equals(pos)
					|| mansardEdge1.p2.equals(pos)
					|| mansardEdge2.p1.equals(pos)
					|| mansardEdge2.p2.equals(pos)) {
				return getMaxRoofEle() - 1/3.0 * roofHeight;
			} else {
				return null;
			}

		}

	}

	/**
	 * roof that has been mapped with explicit roof edge/ridge/apex elements
	 */
	private class ComplexRoof extends HeightfieldRoof {

		private double roofHeight = 0;
		private final Map<VectorXZ, Double> roofHeightMap;
		private PolygonWithHolesXZ simplePolygon;
		private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;

		public ComplexRoof() {

			/* find ridge and/or edges
			 * (apex nodes don't need to be handled separately
			 *  as they should always be part of an edge segment) */

			roofHeightMap = new HashMap<>();
			Set<VectorXZ> nodeSet = new HashSet<>();

			ridgeAndEdgeSegments = new ArrayList<>();

			List<MapNode> nodes = area.getBoundaryNodes();
			boolean usePartRoofHeight = false;

			if (tags.containsKey("roof:height")){
				roofHeight = parseMeasure(tags.getValue("roof:height"));
				usePartRoofHeight = true;
			} else
				roofHeight = DEFAULT_RIDGE_HEIGHT;

			List<MapWaySegment> edges = new ArrayList<>();
			List<MapWaySegment> ridges = new ArrayList<>();

			for (MapOverlap<?,?> overlap : area.getOverlaps()) {

				if (overlap instanceof MapOverlapWA) {

					MapWaySegment waySegment = ((MapOverlapWA)overlap).e1;

					boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
					boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");

					if (!(isRidge || isEdge))
						continue;

					boolean inside = polygon.contains(waySegment.getCenter());

					// check also endpoints as pnpoly algo is not reliable when
					// segment lies on the polygon edge
					boolean containsStart = nodes.contains(waySegment.getStartNode());
					boolean containsEnd = nodes.contains(waySegment.getEndNode());

					if (!inside && !(containsStart && containsEnd))
						continue;

					if (isEdge)
						edges.add(waySegment);
					else
						ridges.add(waySegment);

					ridgeAndEdgeSegments.add(waySegment.getLineSegment());
				}
			}

			for (MapWaySegment waySegment : edges){
				for (MapNode node : waySegment.getStartEndNodes()) {

					// height of node (above roof base)
					Float nodeHeight = null;

					if (node.getTags().containsKey("roof:height")) {
						nodeHeight = parseMeasure(node.getTags()
								.getValue("roof:height"));
					// hmm, shouldnt edges be interpolated? some seem to think they dont
					} else if (waySegment.getTags().containsKey("roof:height")) {
						nodeHeight = parseMeasure(waySegment.getTags()
						        .getValue("roof:height"));
					} else if (node.getTags().contains("roof:apex",	"yes")) {
						nodeHeight = (float)roofHeight;
					}

					if (nodeHeight == null) {
						nodeSet.add(node.getPos());
						continue;
					}

					roofHeightMap.put(node.getPos(), (double) nodeHeight);

					if (usePartRoofHeight)
						roofHeight = max(roofHeight, nodeHeight);
				}
			}

			for (MapWaySegment waySegment : ridges){
				// height of node (above roof base)
				Float nodeHeight = null;

				if (waySegment.getTags().containsKey("roof:height")) {
					nodeHeight = parseMeasure(waySegment.getTags()
							.getValue("roof:height"));
				} else {
					nodeHeight = (float) roofHeight;
				}

				if (usePartRoofHeight)
					roofHeight = max(roofHeight, nodeHeight);

				for (MapNode node : waySegment.getStartEndNodes())
						roofHeightMap.put(node.getPos(), (double) nodeHeight);
			}

			/* join colinear segments, but not the nodes that are connected to ridge/edges
			 * often there are nodes that are only added to join one building to another
			 * but these interfere with proper triangulation.
			 * TODO: do the same for holes */
			List<VectorXZ> vertices = polygon.getOuter().getVertexLoop();
			List<VectorXZ> simplified = new ArrayList<>();
			VectorXZ vPrev = vertices.get(vertices.size() - 2);

			for (int i = 0, size = vertices.size() - 1; i < size; i++ ){
				VectorXZ v = vertices.get(i);

				if (i == 0 || roofHeightMap.containsKey(v) || nodeSet.contains(v)) {
					simplified.add(v);
					vPrev = v;
					continue;
				}
				VectorXZ vNext = vertices.get(i + 1);
				LineSegmentXZ l = new LineSegmentXZ(vPrev, vNext);

				// TODO define as static somewhere: 10 cm tolerance
				if (distanceFromLineSegment(v, l) < 0.01){
					continue;
				}

				roofHeightMap.put(v, 0.0);
				simplified.add(v);
				vPrev = v;
			}

			if (simplified.size() > 2) {
				try{
					simplified.add(simplified.get(0));
					simplePolygon = new PolygonWithHolesXZ(new SimplePolygonXZ(simplified),
							polygon.getHoles());
				} catch (InvalidGeometryException e) {
					System.err.print(e.getMessage());
					simplePolygon = polygon;
				}
			} else
				simplePolygon = polygon;

			/* add heights for outline nodes that don't have one yet */

			for (VectorXZ v : simplePolygon.getOuter().getVertices()) {
				if (!roofHeightMap.containsKey(v)) {
					roofHeightMap.put(v, 0.0);
				}
			}

			for (SimplePolygonXZ hole : simplePolygon.getHoles()) {
				for (VectorXZ v : hole.getVertices()) {
					if (!roofHeightMap.containsKey(v)) {
						roofHeightMap.put(v, 0.0);
					}
				}
			}

			/* add heights for edge nodes that are not also
			 * ridge/outline/apex nodes. This will just use base height
			 * for them instead of trying to interpolate heights along
			 * chains of edge segments. Results are therefore wrong,
			 * but there's no reason to map them like that anyway. */

			for (LineSegmentXZ segment : ridgeAndEdgeSegments) {
				if (!roofHeightMap.containsKey(segment.p1)) {
					roofHeightMap.put(segment.p1, 0.0);
				}
				if (!roofHeightMap.containsKey(segment.p2)) {
					roofHeightMap.put(segment.p2, 0.0);
				}
			}

		}

		@Override
		public PolygonWithHolesXZ getPolygon() {
			return simplePolygon;
		}

		@Override
		public Collection<VectorXZ> getInnerPoints() {
			return emptyList();
		}

		@Override
		public Collection<LineSegmentXZ> getInnerSegments() {
			return ridgeAndEdgeSegments;
		}

		@Override
		public double getRoofHeight() {
			return roofHeight;
		}

		@Override
		public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
			if (roofHeightMap.containsKey(pos)) {
				return building.getGroundLevelEle()
					+ heightWithoutRoof + roofHeightMap.get(pos);
			} else {
				return null;
			}
		}

		@Override
		public double getMaxRoofEle() {
			return building.getGroundLevelEle()
				+ heightWithoutRoof + roofHeight;
		}

	}

	public static boolean hasComplexRoof(MapArea area) {
		for (MapOverlap<?,?> overlap : area.getOverlaps()) {
			if (overlap instanceof MapOverlapWA) {
				TagSet tags = overlap.e1.getTags();
				if (tags.contains("roof:ridge", "yes")
						|| tags.contains("roof:edge", "yes")) {
					return true;
				}
			}
		}
		return false;
	}

}