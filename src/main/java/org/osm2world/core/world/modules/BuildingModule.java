package org.osm2world.core.world.modules;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getLast;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Collections.max;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.*;
import static org.osm2world.core.map_elevation.data.GroundState.ON;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;
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

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.math.NumberUtils;
import org.openstreetmap.josm.plugins.graphview.core.data.EmptyTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.Poly2TriUtil;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.algorithms.JTSTriangulationUtil;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolylineShapeXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.NamedTexCoordFunction;
import org.osm2world.core.util.exception.TriangulationException;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;

import com.google.common.collect.Streams;

/**
 * adds buildings to the world
 */
public class BuildingModule extends ConfigurableWorldModule {

	@Override
	public void applyTo(MapData mapData) {

		iterate(mapData.getMapAreas(), (MapArea area) -> {

			if (!area.getRepresentations().isEmpty()) return;

			String buildingValue = area.getTags().getValue("building");

			if (buildingValue != null && !buildingValue.equals("no")) {

				Building building = new Building(area, config);
				area.addRepresentation(building);

			}

		});

	}

	/**
	 * how windows on building walls should be implemented.
	 * This represents a trade-off between visuals and various performance factors.
	 */
	private static enum WindowImplementation {

		/** no windows at all */
		NONE,
		/** a repeating texture image on a flat wall */
		FLAT_TEXTURES,
		/** windows with actual geometry */
		GEOMETRY;

		public static WindowImplementation getValue(String value, WindowImplementation defaultValue) {

			if (value != null) {
				try {
					return WindowImplementation.valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {}
			}

			return defaultValue;

		}

	}

	/**
	 * a building. Rendering a building is implemented as rendering all of its {@link BuildingPart}s.
	 */
	public static class Building implements AreaWorldObject,
		WorldObjectWithOutline, RenderableToAllTargets {

		private final MapArea area;
		private final List<BuildingPart> parts =
				new ArrayList<BuildingModule.BuildingPart>();

		private final EleConnectorGroup outlineConnectors;

		public Building(MapArea area, Configuration config) {

			this.area = area;

			for (MapOverlap<?,?> overlap : area.getOverlaps()) {
				MapElement other = overlap.getOther(area);
				if (other instanceof MapArea
						&& other.getTags().containsKey("building:part")) {

					MapArea otherArea = (MapArea)other;

					if (roughlyContains(area.getPolygon(), otherArea.getPolygon().getOuter())) {
						parts.add(new BuildingPart(this, otherArea, config));
					}

				}
			}

			/* use the building itself as a part if no parts exist,
			 * or if it's explicitly tagged as a building part at the same time (non-standard mapping) */

			String buildingPartValue = area.getTags().getValue("building:part");

			if (parts.isEmpty() || buildingPartValue != null && !"no".equals(buildingPartValue)) {
				parts.add(new BuildingPart(this, area, config));
			}

			/* create connectors along the outline.
			 * Because the ground around buildings is not necessarily plane,
			 * they aren't directly used for ele, but instead their minimum.
			 */

			outlineConnectors = new EleConnectorGroup();
			outlineConnectors.addConnectorsFor(area.getPolygon(), null, ON);

		}

		public List<BuildingPart> getParts() {
			return parts;
		}

		@Override
		public MapArea getPrimaryMapElement() {
			return area;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public EleConnectorGroup getEleConnectors() {
			return outlineConnectors;
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) { }

		//TODO
//		@Override
//		public double getClearingAbove(VectorXZ pos) {
//			double maxClearingAbove = 0;
//			for (BuildingPart part : parts) {
//				double clearing = part.getClearingAbove(pos);
//				maxClearingAbove = max(clearing, maxClearingAbove);
//			}
//			return maxClearingAbove;
//		}

		@Override
		public SimplePolygonXZ getOutlinePolygonXZ() {
			return area.getPolygon().getOuter().makeCounterclockwise();
		}

		public double getGroundLevelEle() {

			double minEle = POSITIVE_INFINITY;

			for (EleConnector c : outlineConnectors) {
				if (c.getPosXYZ().y < minEle) {
					minEle = c.getPosXYZ().y;
				}
			}

			return minEle;

		}

		@Override
		public PolygonXYZ getOutlinePolygon() {
			return getOutlinePolygonXZ().xyz(getGroundLevelEle());
		}

		@Override
		public void renderTo(Target<?> target) {
			for (BuildingPart part : parts) {
				part.renderTo(target);
			}
		}

	}

	public static class BuildingPart implements RenderableToAllTargets {

		private final Building building;
		private final MapArea area;
		private final PolygonWithHolesXZ polygon;

		private final Configuration config;

		/** the tags for this part, including tags inherited from the parent */
		private final TagGroup tags;

		private int buildingLevels;
		private int minLevel;

		private double heightWithoutRoof;

		private Roof roof;
		private Material materialRoof;

		private List<Wall> walls = null;
		private List<Floor> floors = null;

		public BuildingPart(Building building, MapArea area, Configuration config) {

			this.building = building;
			this.area = area;
			this.polygon = area.getPolygon();

			this.config = config;

			this.tags = inheritTags(area.getTags(), building.area.getTags());

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
				.filter(o -> o.getTags().containsAny("tunnel", asList("building_passage", "passage")))
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
						raisedBuildingPartPolys = new ArrayList<PolygonWithHolesXZ>();
						for (PolygonWithHolesXZ p : polysAboveTBWOs) {
							List<SimplePolygonXZ> subPolys = new ArrayList<SimplePolygonXZ>();
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

		public double getClearingAbove(VectorXZ pos) {
			return heightWithoutRoof + roof.getRoofHeight();
		}

		@Override
		public void renderTo(Target<?> target) {

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

		/** returns the distance between the bottom and the top of a level */
		public double getLevelHeightAboveBase(int level) {
			return (heightWithoutRoof / buildingLevels) * level;
		}

		private double calculateFloorHeight() {

			if (tags.containsKey("min_height")) {

				Float minHeight = parseMeasure(tags.getValue("min_height"));
				if (minHeight != null) {
					return minHeight;
				}

			}

			if (minLevel > 0) {

				return (heightWithoutRoof / buildingLevels) * minLevel;

			}

			if (tags.contains("building", "roof")
					|| tags.contains("building:part", "roof")) {

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

		private static Material createWallMaterial(TagGroup tags, Configuration config) {

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

		private static Material buildMaterial(String materialString,
				String colorString, Material defaultMaterial,
				boolean roof) {

			Material material = defaultMaterial;

			if (materialString != null) {
				if ("brick".equals(materialString)) {
					material = Materials.BRICK;
				} else if ("glass".equals(materialString)) {
					material = roof ? Materials.GLASS_ROOF : Materials.GLASS;
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

		private static class Floor implements RenderableToAllTargets {

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
			public void renderTo(Target<?> target) {

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

		public static interface Roof extends RenderableToAllTargets {

			/**
			 * returns the outline (with holes) of the roof.
			 * The shape will be generally identical to that of the
			 * building itself, but additional vertices might have
			 * been inserted into segments.
			 */
			PolygonWithHolesXZ getPolygon();

			/**
			 * returns roof elevation at a position.
			 */
			double getRoofEleAt(VectorXZ coord);

			/**
			 * returns maximum roof height
			 */
			double getRoofHeight();

			/**
			 * returns maximum roof elevation
			 */
			double getMaxRoofEle();

		}

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

				roofHeight =
					taggedHeight != null ? taggedHeight : getDefaultRoofHeight();

			}

			@Override
			public double getRoofHeight() {
				return roofHeight;
			}

			@Override
			public double getMaxRoofEle() {
				return building.getGroundLevelEle() +
						heightWithoutRoof + roofHeight;
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
			protected void renderSpindle(
					Target<?> target, Material material,
					SimplePolygonXZ polygon,
					List<Double> heights, List<Double> scaleFactors) {

				checkArgument(heights.size() == scaleFactors.size(),
						"heights and scaleFactors must have same size");

				VectorXZ center = polygon.getCenter();

				/* calculate the polygon relative to the center */

				List<VectorXZ> vertexLoop = new ArrayList<VectorXZ>();

				for (VectorXZ v : polygon.makeCounterclockwise().getVertexList()) {
					vertexLoop.add(v.subtract(center));
				}

				ShapeXZ spindleShape = new SimplePolygonXZ(vertexLoop);

				/* construct a path from the heights */

				List<VectorXYZ> path = new ArrayList<VectorXYZ>();

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

			protected List<List<VectorXZ>> spindleTexCoordLists(
					List<VectorXYZ> path, int shapeVertexCount,
					double polygonLength, Material material) {

				List<TextureData> textureDataList =
					material.getTextureDataList();

				switch (textureDataList.size()) {

				case 0: return emptyList();

				case 1: return singletonList(spindleTexCoordList(path,
						shapeVertexCount, polygonLength, textureDataList.get(0)));

				default:

					List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();

					for (TextureData textureData : textureDataList) {
						result.add(spindleTexCoordList(path,
								shapeVertexCount, polygonLength, textureData));
					}

					return result;

				}

			}

			protected List<VectorXZ> spindleTexCoordList(
					List<VectorXYZ> path, int shapeVertexCount,
					double polygonLength, TextureData textureData) {

				List<VectorXZ> result = new ArrayList<VectorXZ>();

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

			private List<VectorXZ> spindleTexCoordListForRing(
					int shapeVertexCount, double polygonLength,
					double accumulatedTexHeight, TextureData textureData) {

				double textureRepeats = max(1,
						round(polygonLength / textureData.width));

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
			public void renderTo(Target<?> target) {

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
			public void renderTo(Target<?> target) {

				double roofY = getMaxRoofEle() - getRoofHeight();

				List<Double> heights = new ArrayList<Double>();
				List<Double> scales = new ArrayList<Double>();

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
						new ArrayList<LineSegmentXZ>();

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
			public void renderTo(Target<?> target) {

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

				List<TriangleXYZ> trianglesXYZ =
						new ArrayList<TriangleXYZ>(triangles.size());

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
						triangleTexCoordLists(trianglesXYZ,
								materialRoof, SLOPED_TRIANGLES));

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

				innerSegments = new ArrayList<LineSegmentXZ>();
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

					LineSegmentXZ longestSeg = max(simplifiedPolygon.getSegments(),
							comparingDouble(LineSegmentXZ::getLength));

					ridgeDirection =
						longestSeg.p2.subtract(longestSeg.p1).normalize();

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

				double maxDistance = 0;

				for (VectorXZ v : outerPoly.getVertices()) {
					maxDistance = max (maxDistance,
							distanceFromLineSegment(v, ridge));
				}

				maxDistanceToRidge = maxDistance;

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
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
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
						interpolateBetween(cap1.p1, cap1.p2,
								0.5 - ridgeOffset / cap1.getLength()),
						interpolateBetween(cap1.p1, cap1.p2,
								0.5 + ridgeOffset / cap1.getLength()));

				cap2part = new LineSegmentXZ(
						interpolateBetween(cap2.p1, cap2.p2,
								0.5 - ridgeOffset / cap1.getLength()),
						interpolateBetween(cap2.p1, cap2.p2,
								0.5 + ridgeOffset / cap1.getLength()));

			}

			@Override
			public PolygonWithHolesXZ getPolygon() {

				PolygonXZ newOuter = polygon.getOuter();

				newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
				newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);

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
						new LineSegmentXZ(ridge.p1, cap1part.p1),
						new LineSegmentXZ(ridge.p1, cap1part.p2),
						new LineSegmentXZ(ridge.p2, cap2part.p1),
						new LineSegmentXZ(ridge.p2, cap2part.p2));
			}

			@Override
			public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
				if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
					return getMaxRoofEle();
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
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
				capParts = new ArrayList<LineSegmentXZ>(rings*2);
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

				List<LineSegmentXZ> innerSegments = new ArrayList<LineSegmentXZ>(rings * 2 + 1);
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
				} else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
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

				roofHeightMap = new HashMap<VectorXZ, Double>();
				Set<VectorXZ> nodeSet = new HashSet<VectorXZ>();

				ridgeAndEdgeSegments = new ArrayList<LineSegmentXZ>();

				List<MapNode> nodes = area.getBoundaryNodes();
				boolean usePartRoofHeight = false;

				if (tags.containsKey("roof:height")){
					roofHeight = parseMeasure(tags.getValue("roof:height"));
					usePartRoofHeight = true;
				} else
					roofHeight = DEFAULT_RIDGE_HEIGHT;

				List<MapWaySegment> edges = new ArrayList<MapWaySegment>();
				List<MapWaySegment> ridges = new ArrayList<MapWaySegment>();

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
				List<VectorXZ> simplified = new ArrayList<VectorXZ>();
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
					TagGroup tags = overlap.e1.getTags();
					if (tags.contains("roof:ridge", "yes")
							|| tags.contains("roof:edge", "yes")) {
						return true;
					}
				}
			}
			return false;
		}

	}

	static class Wall implements RenderableToAllTargets {

		final @Nullable MapWay wallWay;

		private final BuildingPart buildingPart;

		/** points, ordered such that the building's outside is to the right */
		private final PolylineShapeXZ points;

		/** the nodes corresponding to the {@link #points}. No guarantee that all/any points have a matching node! */
		private final Map<VectorXZ, MapNode> pointNodeMap;

		private final double floorHeight;

		/** the tags for this part, including tags inherited from {@link #buildingPart} and its {@link Building} */
		private final TagGroup tags;

		public Wall(@Nullable MapWay wallWay, BuildingPart buildingPart, List<VectorXZ> points,
				Map<VectorXZ, MapNode> pointNodeMap, double floorHeight) {

			this.wallWay = wallWay;
			this.buildingPart = buildingPart;
			this.points = points.size() == 2 ? new LineSegmentXZ(points.get(0), points.get(1)) : new PolylineXZ(points);
			this.pointNodeMap = pointNodeMap;
			this.floorHeight = floorHeight;

			if (buildingPart == null) {
				this.tags = EmptyTagGroup.EMPTY_TAG_GROUP;
			} else if (wallWay != null) {
				this.tags = inheritTags(wallWay.getTags(), buildingPart.tags);
			} else {
				this.tags = buildingPart.tags;
			}

		}

		public Wall(@Nullable MapWay wallWay, BuildingPart buildingPart, List<MapNode> nodes) {
			this(wallWay, buildingPart,
					nodes.stream().map(MapNode::getPos).collect(toList()),
					nodes.stream().collect(toMap(MapNode::getPos, n -> n)),
					buildingPart == null ? 0 : buildingPart.calculateFloorHeight());
		}

		@Override
		public void renderTo(Target<?> target) {

			BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

			double baseEle = buildingPart.building.getGroundLevelEle();
			double floorEle = baseEle + floorHeight;
			double heightWithoutRoof = buildingPart.heightWithoutRoof;

			Material material = BuildingPart.createWallMaterial(tags, buildingPart.config);

			/* determine if the wall has windows */

			boolean hasWindows = defaults.hasWindows;

			if (tags.containsKey("window")) {

				// explicit tagging gets preference over heuristics
				if (tags.contains("window", "no")) {
					hasWindows = false;
				} else {
					hasWindows = true;
				}

			} else {

				if (!tags.containsKey("building:levels")
						|| points.getLength() < 1.0) {
					hasWindows = false;
				}

				if (material == Materials.GLASS) {
					// avoid placing windows into a glass front
					// TODO: the == currently only works if GLASS is not colorable
					hasWindows = false;
				}

			}

			/* use configuration to determine which implementation of window rendering to use */

			WindowImplementation windowImplementation;

			if (Streams.stream(buildingPart.tags).anyMatch(t -> t.key.startsWith("window"))) {
				//explicitly mapped windows, use different (usually higher LOD) setting
				windowImplementation = WindowImplementation.getValue(
						buildingPart.config.getString("explicitWindowImplementation"), WindowImplementation.GEOMETRY);
			} else {
				windowImplementation = WindowImplementation.getValue(
					buildingPart.config.getString("implicitWindowImplementation"), WindowImplementation.FLAT_TEXTURES);
			}

			/* calculate the lower boundary of the wall */

			List<VectorXYZ> bottomPoints = points.getVertexList().stream()
					.map(p -> p.xyz(0)) //set to 0 because y is relative height within the wall here
					.collect(toList());

			/* calculate the upper boundary of the wall (from roof polygons) */

			List<VectorXZ> topPointsXZ = null;

			for (SimplePolygonXZ rawPolygon : buildingPart.roof.getPolygon().getPolygons()) {

				SimplePolygonXZ polygon = buildingPart.roof.getPolygon().getHoles().contains(rawPolygon)
						? rawPolygon.makeClockwise()
						: rawPolygon.makeCounterclockwise();

				int firstIndex = polygon.getVertices().indexOf(bottomPoints.get(0).xz());
				int lastIndex = polygon.getVertices().indexOf(bottomPoints.get(bottomPoints.size() - 1).xz());

				if (firstIndex != -1 && lastIndex != -1) {

					topPointsXZ = new ArrayList<>();

					if (lastIndex < firstIndex) {
						lastIndex += polygon.size();
					}

					for (int i = firstIndex; i <= lastIndex; i ++) {
						topPointsXZ.add(polygon.getVertex(i % polygon.size()));
					}

					break;

				}

			}

			if (topPointsXZ == null) {
				// just use the same points as for the bottom.
				// This might miss some roof details and should only happen with legacy features like building_passage.
				System.err.println("Warning: cannot construct top boundary of wall for " + buildingPart.area);
				topPointsXZ = points.getVertexList();
			}

			List<VectorXYZ> topPoints = topPointsXZ.stream()
					.map(p -> p.xyz(buildingPart.roof.getRoofEleAt(p) - floorEle))
					.collect(toList());

			/* construct the surface(s) */

			List<VectorXZ> lowerSurfaceBoundary = toPointsOnSurface(bottomPoints);
			List<VectorXZ> upperSurfaceBoundary = toPointsOnSurface(topPoints);

			WallSurface mainSurface, roofSurface;

			double maxHeight = max(upperSurfaceBoundary, comparingDouble(v -> v.z)).z;

			if (windowImplementation != WindowImplementation.FLAT_TEXTURES
					|| !hasWindows || maxHeight + floorHeight - heightWithoutRoof < 0.01) {

				roofSurface = null;

				try {
					mainSurface = new WallSurface(material, lowerSurfaceBoundary, upperSurfaceBoundary);
				} catch (InvalidGeometryException e) {
					mainSurface = null;
				}

			} else {

				// using window textures. Need to separate the bit of wall "in the roof" which should not have windows.

				List<VectorXZ> middleSurfaceBoundary = asList(
						new VectorXZ(lowerSurfaceBoundary.get(0).x, heightWithoutRoof - floorHeight),
						new VectorXZ(lowerSurfaceBoundary.get(bottomPoints.size() - 1).x, heightWithoutRoof - floorHeight));

				try {
					mainSurface = new WallSurface(material, lowerSurfaceBoundary, middleSurfaceBoundary);
				} catch (InvalidGeometryException e) {
					mainSurface = null;
				}

				middleSurfaceBoundary = middleSurfaceBoundary.stream()
						.map(v -> v.subtract(new VectorXZ(0, heightWithoutRoof - floorHeight)))
						.collect(toList());
				upperSurfaceBoundary = upperSurfaceBoundary.stream()
						.map(v -> v.subtract(new VectorXZ(0, heightWithoutRoof - floorHeight)))
						.collect(toList());

				try {
					roofSurface = new WallSurface(material, middleSurfaceBoundary, upperSurfaceBoundary);
				} catch (InvalidGeometryException e) {
					roofSurface = null;
				}

			}

			/* add doors (if any) */
			//TODO: doors at corners of the building (or boundaries between building:wall=yes ways) do not work yet
			//TODO: cannot place doors into roof walls yet

			for (MapNode node : getNodes()) {
				if ((node.getTags().contains("building", "entrance")
						|| node.getTags().containsKey("entrance"))) {

					int level = NumberUtils.toInt(node.getTags().getValue("level"), 0);

					VectorXZ pos = new VectorXZ(points.offsetOf(node.getPos()),
							buildingPart.getLevelHeightAboveBase(level));

					mainSurface.addElementIfSpaceFree(new Door(pos, node));

				}
			}

			if (tags.containsAny(asList("building", "building:part"), asList("garage", "garages"))) {
				if (!buildingPart.area.getBoundaryNodes().stream().anyMatch(
						n -> n.getTags().containsAnyKey(asList("entrance", "door")))) {
					placeDefaultGarageDoors(mainSurface);
				}
			}

			/* add windows (after doors, because default windows should be displaced by them) */

			if (hasWindows && windowImplementation == WindowImplementation.GEOMETRY) {
				placeDefaultWindows(mainSurface);
			}

			/* draw the wall */

			List<VectorXYZ> bottomPointsXYZ = addYList(bottomPoints, floorEle);

			if (mainSurface != null) {
				mainSurface.renderTo(target, bottomPointsXYZ,
						hasWindows && windowImplementation == WindowImplementation.FLAT_TEXTURES);
			}

			if (roofSurface != null) {
				List<VectorXYZ> middlePointsXYZ = addYList(bottomPoints, floorEle + heightWithoutRoof - floorHeight);
				roofSurface.renderTo(target, middlePointsXYZ, false);
			}

		}

		@Override
		public String toString() {
			if (getNodes().size() == points.getVertexList().size()) {
				return getNodes().toString();
			} else {
				return points.toString();
			}
		}

		/**
		 * returns the list of nodes forming this wall.
		 * Not guaranteed to contain all points, or even be non-empty, as some points may not be based on nodes
		 */
		List<MapNode> getNodes() {
			List<MapNode> result = new ArrayList<>();
			for (VectorXZ point : points.getVertexList()) {
				if (pointNodeMap.containsKey(point)) {
					result.add(pointNodeMap.get(point));
				}
			}
			return result;
		}


		/**
		 * converts a list of 3d points to 2d coordinates on a vertical wall surface.
		 *
		 * The surface is defined by the same list of points! That is,
		 * the wall is assumed to start at the first point's XZ position,
		 * and continue through the other points' XZ positions.
		 * Y coordinates are preserved.
		 */
		private List<VectorXZ> toPointsOnSurface(List<VectorXYZ> points) {

			//TODO consider using PolylineShapeXZ.offsetOf here

			List<VectorXZ> result = new ArrayList<>(points.size());

			double accumulatedLength = 0;
			VectorXYZ previousPoint = points.get(0);

			for (VectorXYZ point : points) {
				accumulatedLength += previousPoint.distanceToXZ(point);
				result.add(new VectorXZ(accumulatedLength, point.y));
				previousPoint = point;
			}

			return result;

		}

		/** places the default (i.e. not explicitly mapped) windows rows onto a wall surface */
		private void placeDefaultWindows(WallSurface surface) {

			for (int level = 0; level < buildingPart.buildingLevels; level++) {

				double levelHeight = buildingPart.getLevelHeight(level);
				double heightAboveBase = buildingPart.getLevelHeightAboveBase(level);

				double windowHeight = 0.5 * levelHeight;
				double breastHeight = 0.3 * levelHeight;

				double windowWidth = 1;

				int numColums = (int) round(surface.getLength() / (2 * windowWidth));

				for (int i = 0; i < numColums; i++) {

					VectorXZ pos = new VectorXZ(i * surface.getLength() / numColums,
							heightAboveBase + breastHeight);

					Window window = new Window(pos, windowWidth, windowHeight);
					surface.addElementIfSpaceFree(window);

				}

			}

		}

		/** places default (i.e. not explicitly mapped) doors onto garage walls */
		private void placeDefaultGarageDoors(WallSurface surface) {

			MapBasedTagGroup doorTags = new MapBasedTagGroup(new Tag("door", "overhead"));

			double doorDistance = 1.25 * DoorParameters.fromTags(doorTags, this.tags).width;
			int numDoors = (int) round(surface.getLength() / doorDistance);

			for (int i = 0; i < numDoors; i++) {
				VectorXZ pos = new VectorXZ(surface.getLength() / numDoors * (i + 0.5), 0);
				surface.addElementIfSpaceFree(new Door(pos , doorTags));
			}

		}

		/**
		 * a simplified representation of the wall as a 2D plane, with its origin in the bottom left corner.
		 * This streamlines the placement of objects (windows, doors, and similar features) onto the wall.
		 * Afterwards, positions are converted back into 3D space.
		 */
		private class WallSurface {

			private final Material material;

			private final List<VectorXZ> lowerBoundary;
			private final List<VectorXZ> upperBoundary;
			private final SimplePolygonXZ wallOutline;

			private final List<WallElement> elements = new ArrayList<>();

			/**
			 * Constructs a wall surface from a lower and upper wall boundary.
			 * The boundaries' x coordinates is the position along the wall (starting with 0 for the first point),
			 * the z coordinates refer to height.
			 *
			 * @throws InvalidGeometryException  if the lower and upper boundary do not represent a proper surface.
			 * This can happen, for example, because the wall has a zero or almost-zero height.
			 */
			public WallSurface(Material material, List<VectorXZ> lowerBoundary, List<VectorXZ> upperBoundary)
					throws IllegalArgumentException {

				this.material = material;
				this.lowerBoundary = lowerBoundary;
				this.upperBoundary = upperBoundary;

				if (lowerBoundary.size() < 2)
					throw new IllegalArgumentException("need at least two bottom points");
				if (upperBoundary.size() < 2)
					throw new IllegalArgumentException("need at least two top points");
				if (lowerBoundary.get(0).x != 0)
					throw new IllegalArgumentException("origin is in the bottom left corner");
				if (upperBoundary.get(0).x != 0)
					throw new IllegalArgumentException("origin is in the bottom left corner");

				/* TODO: check for other problems, e.g. intersecting lower and upper boundary,
				   last points of the boundaries having different x values, ... */

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

			/** renders the wall; requires it to be anchored back into 3D space */
			public void renderTo(Target<?> target, List<VectorXYZ> bottomPointsXYZ,
					boolean applyWindowTexture) {

				/* render the elements on the wall */

				for (WallElement e : elements) {
					e.renderTo(target, this, bottomPointsXYZ);
				}

				/* triangulate the empty wall surface */

				List<SimplePolygonXZ> holes = elements.stream().map(WallElement::outline).collect(toList());

				List<TriangleXZ> triangles = triangulate(wallOutline, holes);
				List<TriangleXYZ> trianglesXYZ = triangles.stream().map(t -> convertTo3D(t, bottomPointsXYZ)).collect(toList());

				/* determine the material depending on whether a window texture should be applied */

				Material material = applyWindowTexture
						? this.material.withAddedLayers(BUILDING_WINDOWS.getTextureDataList())
						: this.material;

				/* calculate basic texture coordinates (for a hypothetical 'unit texture' of width and height 1) */

				List<TextureData> textureDataList = material.getTextureDataList();

				List<List<VectorXZ>> texCoordLists = new ArrayList<>(textureDataList.size());

				for (int texLayer = 0; texLayer < textureDataList.size(); texLayer ++) {

					List<VectorXZ> texCoords = new ArrayList<>();

					for (TriangleXZ triangle : triangles) {
						texCoords.add(triangle.v1);
						texCoords.add(triangle.v2);
						texCoords.add(triangle.v3);
					}

					texCoordLists.add(texCoords);

				}

				/* scale the texture coordinates based on the texture's height and width or,
				 * for textures with the special height value 0 (windows!), to an integer number of repetitions */

				for (int texLayer = 0; texLayer < textureDataList.size(); texLayer ++) {

					TextureData textureData = textureDataList.get(texLayer);
					List<VectorXZ> texCoords = texCoordLists.get(texLayer);

					boolean specialWindowHandling = (textureData.height == 0);

					for (int i = 0; i < texCoords.size(); i++) {

						double height = textureData.height;
						double width = textureData.width;

						if (specialWindowHandling) {
							height = buildingPart.heightWithoutRoof / buildingPart.buildingLevels;
							width = getLength() / max(1, round(getLength() / textureData.width));
						}

						double s = texCoords.get(i).x / width;
						double t = (texCoords.get(i).z + floorHeight) / height;

						texCoords.set(i, new VectorXZ(s, t));

					}

				}

				/* render the wall */

				target.drawTriangles(material, trianglesXYZ, texCoordLists);

			}

			private VectorXYZ convertTo3D(VectorXZ v, List<VectorXYZ> bottomPointsXYZ) {

				double ratio = v.x / getLength();

				VectorXYZ point = interpolateOn(bottomPointsXYZ, ratio);

				return point.addY(v.z);

			}

			private TriangleXYZ convertTo3D(TriangleXZ t, List<VectorXYZ> bottomPointsXYZ) {
				return new TriangleXYZ(
						convertTo3D(t.v1, bottomPointsXYZ),
						convertTo3D(t.v2, bottomPointsXYZ),
						convertTo3D(t.v3, bottomPointsXYZ));
			}

			private PolygonXYZ convertTo3D(PolygonShapeXZ polygon, List<VectorXYZ> bottomPointsXYZ) {
				List<VectorXYZ> outline = new ArrayList<>(polygon.getVertexList().size());
				polygon.getVertexList().forEach(v -> outline.add(convertTo3D(v, bottomPointsXYZ)));
				return new PolygonXYZ(outline);
			}

		}

		/**
		 * something that can be placed into a wall, such as a window or door
		 */
		private static interface WallElement {

			/**
			 * returns the space on the 2D wall surface occupied by this element.
			 * The element is responsible for handling rendering inside this area.
			 */
			public SimplePolygonXZ outline(); //TODO allow any ShapeXZ; requires an intersect method though

			public void renderTo(Target<?> target, Wall.WallSurface surface, List<VectorXYZ> bottomPointsXYZ);

		}

		private class Window implements WallElement {

			/** position on a wall surface */
			private final VectorXZ position;

			private final double width;
			private final double height;

			public Window(VectorXZ position, double width, double height) {
				this.position = position;
				this.width = width;
				this.height = height;
			}

			@Override
			public SimplePolygonXZ outline() {

				return new SimplePolygonXZ(asList(
						position.add(new VectorXZ(-width/2, 0)),
						position.add(new VectorXZ(+width/2, 0)),
						position.add(new VectorXZ(+width/2, +height)),
						position.add(new VectorXZ(-width/2, +height)),
						position.add(new VectorXZ(-width/2, 0))));

			}

			@Override
			public void renderTo(Target<?> target, Wall.WallSurface surface, List<VectorXYZ> bottomPointsXYZ) {

				double depth = 0.10;

				PolygonXYZ frontOutline = surface.convertTo3D(outline(), bottomPointsXYZ);

				List<VectorXYZ> vs = frontOutline.getVertices();
				VectorXYZ toBack = new TriangleXYZ(vs.get(0), vs.get(2), vs.get(1)).getNormal().mult(depth);
				PolygonXYZ backOutline = frontOutline.add(toBack);

				/* draw the window itself */

				VectorXYZ bottomLeft = backOutline.getVertices().get(0);
				VectorXYZ bottomRight = backOutline.getVertices().get(1);
				VectorXYZ topLeft = backOutline.getVertices().get(3);
				VectorXYZ topRight = backOutline.getVertices().get(2);

				List<VectorXYZ> vsWindow = asList(topLeft, bottomLeft, topRight, bottomRight);

				target.drawTriangleStrip(SINGLE_WINDOW, vsWindow,
						texCoordLists(vsWindow, SINGLE_WINDOW, STRIP_FIT));

				/* draw the wall around the window */

				List<VectorXYZ> vsWall = createTriangleStripBetween(
						backOutline.getVertexLoop(), frontOutline.getVertexLoop());

				Material material = new ImmutableMaterial(
						surface.material.getInterpolation(),
						surface.material.getColor(),
						0.5f * surface.material.getAmbientFactor(), //coarsely approximate ambient occlusion
						surface.material.getDiffuseFactor(),
						surface.material.getSpecularFactor(),
						surface.material.getShininess(),
						surface.material.getTransparency(),
						surface.material.getShadow(),
						surface.material.getAmbientOcclusion(),
						surface.material.getTextureDataList());

				target.drawTriangleStrip(material, vsWall,
						texCoordLists(vsWall, material, NamedTexCoordFunction.STRIP_WALL));

			}

		}

		private class Door implements WallElement {

			/** position on a wall surface */
			private final VectorXZ position;

			private final @Nullable MapNode node;
			private final TagGroup tags;

			private final DoorParameters parameters;


			public Door(VectorXZ position, TagGroup tags) {
				this.position = position;
				this.node = null;
				this.tags = tags;
				this.parameters = DoorParameters.fromTags(tags, Wall.this.tags);
			}

			public Door(VectorXZ position, MapNode node) {

				this.position = position;
				this.node = node;
				this.tags = node.getTags();

				this.parameters = DoorParameters.fromTags(tags, Wall.this.tags);

			}

			@Override
			public SimplePolygonXZ outline() {

				return new SimplePolygonXZ(asList(
						position.add(new VectorXZ(-parameters.width/2, 0)),
						position.add(new VectorXZ(+parameters.width/2, 0)),
						position.add(new VectorXZ(+parameters.width/2, +parameters.height)),
						position.add(new VectorXZ(-parameters.width/2, +parameters.height)),
						position.add(new VectorXZ(-parameters.width/2, 0))));

			}

			@Override
			public void renderTo(Target<?> target, Wall.WallSurface surface, List<VectorXYZ> bottomPointsXYZ) {

				Material doorMaterial = ENTRANCE_DEFAULT;

				switch (parameters.type) {
				case "no":
					doorMaterial = VOID;
					break;
				case "overhead":
					doorMaterial = GARAGE_DOOR;
					break;
				}

				doorMaterial = doorMaterial.withColor(parameters.color);

				double depth = 0.10;

				PolygonXYZ frontOutline = surface.convertTo3D(outline(), bottomPointsXYZ);

				List<VectorXYZ> verts = frontOutline.getVertices();
				VectorXYZ toBack = new TriangleXYZ(verts.get(0), verts.get(2), verts.get(1)).getNormal().mult(depth);
				PolygonXYZ backOutline = frontOutline.add(toBack);

				/* draw the door itself */

				VectorXYZ bottomLeft = backOutline.getVertices().get(0);
				VectorXYZ bottomRight = backOutline.getVertices().get(1);
				VectorXYZ topLeft = backOutline.getVertices().get(3);
				VectorXYZ topRight = backOutline.getVertices().get(2);

				if (parameters.numberOfWings == 1 || !"hinged".equals(parameters.type)) {

					List<VectorXYZ> vsDoor = asList(topLeft, bottomLeft, topRight, bottomRight);
					target.drawTriangleStrip(doorMaterial, vsDoor,
							texCoordLists(vsDoor, doorMaterial, STRIP_FIT));

				} else {

					if (parameters.numberOfWings > 2) {
						System.err.println("Warning: Unusual door:wings for " + node + ": " + parameters.numberOfWings);
					}

					VectorXYZ bottomCenter = interpolateBetween(bottomLeft, bottomRight, 0.5);
					VectorXYZ topCenter = interpolateBetween(topLeft, topRight, 0.5);

					List<VectorXYZ> vsDoorLeft = asList(topLeft, bottomLeft, topCenter, bottomCenter);
					target.drawTriangleStrip(doorMaterial, vsDoorLeft,
							texCoordLists(vsDoorLeft, doorMaterial, STRIP_FIT));

					List<VectorXYZ> vsDoorRight = asList(topCenter, bottomCenter, topRight, bottomRight);
					target.drawTriangleStrip(doorMaterial, vsDoorRight,
							texCoordLists(vsDoorRight, doorMaterial, (List<VectorXYZ> vs, TextureData textureData) -> {
								//mirror the door for the right wing
								//TODO: maybe create a general-purpose function from this?
								List<VectorXZ> result = STRIP_FIT.apply(vs, textureData);
								return result.stream().map(v -> new VectorXZ(1 - v.x, v.z)).collect(toList());
							}));

				}

				/* draw the wall around the door */

				List<VectorXYZ> vsWall = createTriangleStripBetween(
						backOutline.getVertexLoop(), frontOutline.getVertexLoop());

				Material material = new ImmutableMaterial(
						surface.material.getInterpolation(),
						surface.material.getColor(),
						0.5f * surface.material.getAmbientFactor(), //coarsely approximate ambient occlusion
						surface.material.getDiffuseFactor(),
						surface.material.getSpecularFactor(),
						surface.material.getShininess(),
						surface.material.getTransparency(),
						surface.material.getShadow(),
						surface.material.getAmbientOcclusion(),
						surface.material.getTextureDataList());

				target.drawTriangleStrip(material, vsWall,
						texCoordLists(vsWall, material, NamedTexCoordFunction.STRIP_WALL));

			}

		}

	}

	public static class DoorParameters {

		public final String type;
		public final @Nullable String materialName;
		public final @Nullable Color color;

		public final double width;
		public final double height;

		public final int numberOfWings;

		public DoorParameters(String type, String materialName, Color color,
				double width, double height, int numberOfWings) {
			this.type = type;
			this.materialName = materialName;
			this.color = color;
			this.width = width;
			this.height = height;
			this.numberOfWings = numberOfWings;
		}

		/**
		 * extracts door parameters from a set of tags
		 * and (optionally) another set of tags describing the building part and/or wall the door is in
		 */
		public static DoorParameters fromTags(TagGroup tags, @Nullable TagGroup parentTags) {

			/* determine the type */

			String type = "hinged";

			if (parentTags.containsAny(
					asList("building", "building:part"),
					asList("garage", "garages"))) {
				type = "overhead";
			}

			if (tags.containsKey("door")) {
				type = tags.getValue("door");
			}

			/* determine material and other attributes */

			String materialName = null;
			if (tags.containsKey("material")) {
				materialName = tags.getValue("material");
			}

			Color color = parseColor(tags.getValue("colour"), CSS_COLORS);

			int numberOfWings = NumberUtils.toInt(tags.getValue("door:wings"), 1);

			/* parse or estimate width and height */

			double defaultWidth = 1.0;
			double defaultHeight = 2.0;

			switch (type) {
			case "overhead":
				defaultWidth = 2.5;
				defaultHeight = 2.125;
				break;
			case "hinged":
				if (numberOfWings == 2) {
					defaultWidth *= 2;
				}
				break;
			}

			double width = parseWidth(tags, (float)defaultWidth);
			double height = parseHeight(tags, (float)defaultHeight);

			/* return the result */

			return new DoorParameters(type, materialName, color, width, height, numberOfWings);

		}

	}

	/** default properties for a particular building or building:part type */
	public static class BuildingDefaults {

		public final int levels;
		public final double heightPerLevel;
		public final String roofShape;
		public final Material materialWall;
		public final Material materialRoof;
		public final boolean hasWindows;

		public BuildingDefaults(int levels, double heightPerLevel, String roofShape,
				Material materialWall, Material materialRoof, boolean hasWindows) {
			this.levels = levels;
			this.heightPerLevel = heightPerLevel;
			this.roofShape = roofShape;
			this.materialWall = materialWall;
			this.materialRoof = materialRoof;
			this.hasWindows = hasWindows;
		}

		public static BuildingDefaults getDefaultsFor(TagGroup tags) {

			String type = tags.getValue("building:part");
			if (type == null || "yes".equals(type)) {
				type = tags.getValue("building");
			}

			if (type == null) {
				throw new IllegalArgumentException("Tags do not contain a building type: " + tags);
			}

			/* determine defaults for building type */

			int levels = 3;
			double heightPerLevel = 2.5;
			Material materialWall = Materials.BUILDING_DEFAULT;
			Material materialRoof = Materials.ROOF_DEFAULT;
			boolean hasWindows = true;
			String roofShape = "flat";

			switch (type) {

			case "greenhouse":
				levels = 1;
				materialWall = Materials.GLASS;
				materialRoof = Materials.GLASS_ROOF;
				hasWindows = false;
				break;

			case "garage":
			case "garages":
				levels = 1;
				materialWall = Materials.CONCRETE;
				materialRoof = Materials.CONCRETE;
				hasWindows = false;
				break;

			case "hut":
			case "shed":
				levels = 1;
				break;

			case "cabin":
				levels = 1;
				materialWall = Materials.WOOD_WALL;
				materialRoof = Materials.WOOD;
				break;

			case "roof":
				levels = 1;
				hasWindows = false;
				break;

			case "church":
			case "hangar":
			case "industrial":
				hasWindows = false;
				break;

			}

			/* handle other tags */

			if (tags.contains("parking", "multi-storey")) {
				levels = 5;
				hasWindows = false;
			}

			/* return an object populated with the results */

	    	return new BuildingDefaults(levels, heightPerLevel, roofShape,
	    			materialWall, materialRoof, hasWindows);

		}

	}

}
