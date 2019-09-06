package org.osm2world.core.world.modules;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Collections.max;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.*;
import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.*;
import static org.osm2world.core.map_elevation.data.GroundState.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.core.target.common.material.Materials.SINGLE_WINDOW;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;
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
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.NamedTexCoordFunction;
import org.osm2world.core.util.CSSColors;
import org.osm2world.core.util.exception.TriangulationException;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

import com.google.common.collect.Lists;

/**
 * adds buildings to the world
 */
public class BuildingModule extends ConfigurableWorldModule {

	@Override
	public void applyTo(MapData mapData) {

		final boolean useBuildingColors = config.getBoolean("useBuildingColors", true);
		final boolean drawBuildingWindows = config.getBoolean("drawBuildingWindows", true);

		iterate(mapData.getMapAreas(), (MapArea area) -> {

			if (!area.getRepresentations().isEmpty()) return;

			String buildingValue = area.getTags().getValue("building");

			if (buildingValue != null && !buildingValue.equals("no")) {

				Building building = new Building(area,
						useBuildingColors, drawBuildingWindows);
				area.addRepresentation(building);

			}

		});

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

		public Building(MapArea area, boolean useBuildingColors, boolean drawBuildingWindows) {

			this.area = area;

			for (MapOverlap<?,?> overlap : area.getOverlaps()) {
				MapElement other = overlap.getOther(area);
				if (other instanceof MapArea
						&& other.getTags().containsKey("building:part")) {

					MapArea otherArea = (MapArea)other;

					//TODO: check whether the building contains the part (instead of just touching it)
					if (area.getPolygon().contains(otherArea.getPolygon().getOuter())) {
						parts.add(new BuildingPart(this, otherArea, useBuildingColors, drawBuildingWindows));
					}

				}
			}

			/* use the building itself as a part if no parts exist,
			 * or if it's explicitly tagged as a building part at the same time (non-standard mapping) */

			String buildingPartValue = area.getTags().getValue("building:part");

			if (parts.isEmpty() || buildingPartValue != null && !"no".equals(buildingPartValue)) {
				parts.add(new BuildingPart(this, area, useBuildingColors, drawBuildingWindows));
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
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {

			List<EleConnector> groundLevelEntrances = new ArrayList<EleConnector>();

			/* add constraints between entrances with different levels */

			for (BuildingPart part : parts) {

				// add vertical distances

				for (int i = 0; i < part.entrances.size(); i++) {

					BuildingEntrance e1 = part.entrances.get(i);

					for (int j = i+1; j < part.entrances.size(); j++) {

						BuildingEntrance e2 = part.entrances.get(j);

						double heightPerLevel = part.heightWithoutRoof / part.buildingLevels;

						if (e1.getLevel() > e2.getLevel()) {

							enforcer.requireVerticalDistance(EXACT,
									heightPerLevel * (e1.getLevel() - e2.getLevel()),
									e1.connector, e2.connector);

						} else if (e1.getLevel() < e2.getLevel()) {

							enforcer.requireVerticalDistance(EXACT,
									heightPerLevel * (e2.getLevel() - e1.getLevel()),
									e2.connector, e1.connector);

						}

					}

					// collect entrances for next step

					if (e1.getLevel() == 0 && e1.getGroundState() == ON) {
						groundLevelEntrances.add(e1.connector);
					}

				}

			}

			/* make sure that a level=0 ground entrance is the building's lowest point */

			for (EleConnector outlineConnector : outlineConnectors) {
				for (EleConnector entranceConnector : groundLevelEntrances) {
					enforcer.requireVerticalDistance(
							MIN, 0, outlineConnector, entranceConnector);
				}
			}

		}

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

		/** the tags for this part, including tags inherited from the parent */
		private final TagGroup tags;

		private int buildingLevels;
		private int minLevel;

		private double heightWithoutRoof;

		private Material materialWall;
		private Material materialWallWithWindows;
		private Material materialRoof;

		private List<BuildingEntrance> entrances = new ArrayList<BuildingEntrance>();

		private final List<Wall> walls;
		private Roof roof;

		public BuildingPart(Building building, MapArea area, boolean useBuildingColors, boolean drawBuildingWindows) {

			this.building = building;
			this.area = area;
			this.polygon = area.getPolygon();

			this.tags = inheritTags(area.getTags(), building.area.getTags());

			setAttributes(useBuildingColors, drawBuildingWindows);

			for (MapNode node : area.getBoundaryNodes()) {
				if ((node.getTags().contains("building", "entrance")
						|| node.getTags().containsKey("entrance"))
						&& node.getRepresentations().isEmpty()) {

					BuildingEntrance entrance = new BuildingEntrance(this, node);
					entrances.add(entrance);
					node.addRepresentation(entrance);

				}
			}

			/* create walls */

			walls = splitIntoWalls(area, emptyList())
					.stream()
					.map(nodes -> new Wall(this, nodes))
					.collect(toList());

		}

		/**
		 * splits the outer and inner boundaries of a building into separate walls.
		 * TODO: document the two criteria (wall ways, simplified polygon)
		 *
		 * @param buildingPartArea  the building:part=* area (or building=*, for buildings without parts)
		 * @param wallWays  all building:wall=* ways belonging to this building part
		 *
		 * @return list of walls, each represented as a list of nodes.
		 *   The list of nodes is ordered such that the building part's outside is to the right.
		 */
		static List<List<MapNode>> splitIntoWalls(MapArea buildingPartArea, List<MapWay> wallWays) {

			//TODO implement wallWays by inserting additional split points
			//  and passing the way as an additional parameter to the relevant wall(s!)

			List<List<MapNode>> result = new ArrayList<>();

			List<List<MapNode>> nodeRings = new ArrayList<>(buildingPartArea.getHoles());
			nodeRings.add(buildingPartArea.getBoundaryNodes());

			for (List<MapNode> nodeRing : nodeRings) {

				List<MapNode> nodes = new ArrayList<>(nodeRing);

				SimplePolygonXZ simplifiedPolygon = MapArea.polygonFromMapNodeLoop(nodes).getSimplifiedPolygon();

				if (simplifiedPolygon.isClockwise() ^ buildingPartArea.getHoles().contains(nodeRing)) {
					reverse(nodes);
				}

				nodes.remove(nodes.size() - 1); //remove the duplicated node at the end

				/* figure out where we don't want to split the wall (points in the middle of a straight wall) */

				boolean[] noSplitAtNode = new boolean[nodes.size()];

				for (int i = 0; i < nodes.size(); i++) {
					if (!simplifiedPolygon.getVertexCollection().contains(nodes.get(i).getPos())) {
						noSplitAtNode[i] = true;
					}
				}

				/* split the outline into walls, splitting at each node that's not flagged in noSplitAtNode */

				int firstSplitNode = IntStream.range(0, nodes.size())
						.filter(i -> !noSplitAtNode[i])
						.min().getAsInt();

				int i = firstSplitNode;

				List<MapNode> currentWallNodes = new ArrayList<>();
				currentWallNodes.add(nodes.get(firstSplitNode));

				do {

					i = (i + 1) % nodes.size();

					if (!noSplitAtNode[i]) {
						if (currentWallNodes != null) {
							currentWallNodes.add(nodes.get(i));
							assert currentWallNodes.size() >= 2;
							result.add(currentWallNodes);
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

			//TODO
			//renderWalls(target, roof);

			walls.forEach(w -> w.renderTo(target));

			roof.renderTo(target);

		}

		private void renderFloor(Target<?> target, double floorEle) {

			Collection<TriangleXZ> triangles =
				TriangulationUtil.triangulate(polygon);

			List<TriangleXYZ> trianglesXYZ =
				new ArrayList<TriangleXYZ>(triangles.size());

			for (TriangleXZ triangle : triangles) {
				trianglesXYZ.add(triangle.makeClockwise().xyz(floorEle));
			}

			target.drawTriangles(materialWall, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, materialWall, GLOBAL_X_Z));

		}

		private void renderWalls(Target<?> target, Roof roof) {

			double baseEle = building.getGroundLevelEle();

			double floorHeight = calculateFloorHeight(roof);
			boolean renderFloor = (floorHeight > 0);

			if (area.getOverlaps().isEmpty()) {

				renderWalls(target, roof.getPolygon(),
						baseEle, floorHeight, roof);

			} else {

				/* find terrain boundaries on the ground
				 * that overlap with the building */

				List<TerrainBoundaryWorldObject> tbWorldObjects = new ArrayList<TerrainBoundaryWorldObject>();

				for (MapOverlap<?,?> overlap : area.getOverlaps()) {
					MapElement other = overlap.getOther(area);
					if (other.getPrimaryRepresentation() instanceof TerrainBoundaryWorldObject
							&& other.getPrimaryRepresentation().getGroundState() == GroundState.ON
							&& (other.getTags().contains("tunnel", "passage")
									|| other.getTags().contains("tunnel", "building_passage"))) {
						tbWorldObjects.add((TerrainBoundaryWorldObject)
								other.getPrimaryRepresentation());
					}
				}

				/* render building parts where the building polygon does not overlap with terrain boundaries */

				List<SimplePolygonXZ> subtractPolygons = new ArrayList<SimplePolygonXZ>();

				for (TerrainBoundaryWorldObject o : tbWorldObjects) {

					SimplePolygonXZ subtractPoly = o.getOutlinePolygonXZ();

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
					renderWalls(target, p, baseEle, floorHeight, roof);
					if (renderFloor) {
						renderFloor(target, baseEle + floorHeight);
					}
				}

				/* render building parts above the terrain boundaries */

				for (TerrainBoundaryWorldObject o : tbWorldObjects) {

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
						double newFloorHeight = 3;
							//TODO restore clearing - o.getClearingAbove(o.getOutlinePolygon().getSimpleXZPolygon().getCenter());
						if (newFloorHeight < floorHeight) {
							newFloorHeight = floorHeight;
						}
						renderWalls(target, p, baseEle, newFloorHeight, roof);
						renderFloor(target, baseEle);
					}

				}

			}

		}

		private double calculateFloorHeight(Roof roof) {

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

		private void renderWalls(Target<?> target, PolygonWithHolesXZ p,
				double baseEle, double floorHeight,
				Roof roof) {

			drawSimpleWall(target, baseEle, floorHeight,
					roof, p.getOuter().makeCounterclockwise().getVertexLoop());

			for (SimplePolygonXZ polygon : p.getHoles()) {
				drawSimpleWall(target, baseEle, floorHeight,
						roof, polygon.makeClockwise().getVertexLoop());
			}

		}

		private void drawSimpleWall(Target<?> target, double baseEle,
				double floorHeight, Roof roof, List<VectorXZ> vertices) {

			double floorEle = baseEle + floorHeight;

			List<TextureData> textureDataList = materialWallWithWindows.getTextureDataList();

			List<VectorXYZ> mainWallVectors = new ArrayList<VectorXYZ>(vertices.size() * 2);
			List<VectorXYZ> roofWallVectors = new ArrayList<VectorXYZ>(vertices.size() * 2);

			List<List<VectorXZ>> mainWallTexCoordLists = new ArrayList<List<VectorXZ>>(
					textureDataList.size());

			for (int texLayer = 0; texLayer < textureDataList.size(); texLayer ++) {
				mainWallTexCoordLists.add(new ArrayList<VectorXZ>());
			}

			double accumulatedLength = 0;
			double[] previousS = new double[textureDataList.size()];

			for (int i = 0; i < vertices.size(); i++) {

				final VectorXZ coord = vertices.get(i);

				/* update accumulated wall length */

				if (i > 0) {
					accumulatedLength += coord.distanceTo(vertices.get(i-1));
				}

				/* add wall vectors */

				final VectorXYZ upperVector = coord.xyz(roof.getRoofEleAt(coord));
				final VectorXYZ middleVector = coord.xyz(baseEle + heightWithoutRoof);

				double upperEle = upperVector.y;
				double middleEle = middleVector.y;

				mainWallVectors.add(middleVector);
				mainWallVectors.add(new VectorXYZ(coord.x,
						min(floorEle, middleEle), coord.z));

				roofWallVectors.add(upperVector);
				roofWallVectors.add(new VectorXYZ(coord.x,
						min(middleEle, upperEle), coord.z));


				/* add texture coordinates */

				for (int texLayer = 0; texLayer < textureDataList.size(); texLayer ++) {

					TextureData textureData = textureDataList.get(texLayer);
					List<VectorXZ> texCoordList = mainWallTexCoordLists.get(texLayer);

					double s, lowerT, middleT;

					// determine s (width dimension) coordinate

					if (textureData.height > 0) {
						s = accumulatedLength / textureData.width;
					} else {
						if (i == 0) {
							s = 0;
						} else {
							s = previousS[texLayer] + round(vertices.get(i-1)
									.distanceTo(coord) / textureData.width);
						}
					}

					previousS[texLayer] = s;

					// determine t (height dimension) coordinates

					if (textureData.height > 0) {

						lowerT = (floorEle - baseEle) / textureData.height;
						middleT = (middleEle - baseEle) / textureData.height;

					} else {

						lowerT = buildingLevels *
							(floorEle - baseEle) / (middleEle - baseEle);
						middleT = buildingLevels;

					}

					// set texture coordinates

					texCoordList.add(new VectorXZ(s, middleT));
					texCoordList.add(new VectorXZ(s, lowerT));

				}

			}

			target.drawTriangleStrip(materialWallWithWindows, mainWallVectors,
					mainWallTexCoordLists);

			drawStripWithoutDegenerates(target, materialWall, roofWallVectors,
					texCoordLists(roofWallVectors, materialWall, STRIP_WALL));

		}

		/**
		 * sets the building part attributes (height, colors) depending on
		 * the building's and building part's tags.
		 * If available, explicitly tagged data is used,
		 * with tags of the building part overriding building tags.
		 * Otherwise, the values depend on indirect assumptions
		 * (level height) or ultimately the building class as determined
		 * by the "building" key.
		 */
		private void setAttributes(boolean useBuildingColors,
				boolean drawBuildingWindows) {

			/* determine defaults for building type */

			int defaultLevels = 3;
			double defaultHeightPerLevel = 2.5;
			Material defaultMaterialWall = Materials.BUILDING_DEFAULT;
			Material defaultMaterialRoof = Materials.ROOF_DEFAULT;
			Material defaultMaterialWindows = Materials.BUILDING_WINDOWS;
			String defaultRoofShape = "flat";

			String buildingValue = tags.getValue("building:part");
			if (buildingValue == null || buildingValue.equals("yes")) {
				buildingValue = tags.getValue("building");
			}

			switch (buildingValue) {

			case "greenhouse":
				defaultLevels = 1;
				defaultMaterialWall = Materials.GLASS;
				defaultMaterialRoof = Materials.GLASS_ROOF;
				defaultMaterialWindows = null;
				break;

			case "garage":
			case "garages":
				defaultLevels = 1;
				defaultMaterialWall = Materials.CONCRETE;
				defaultMaterialRoof = Materials.CONCRETE;
				defaultMaterialWindows = Materials.GARAGE_DOORS;
				break;

			case "hut":
			case "shed":
				defaultLevels = 1;
				break;

			case "cabin":
				defaultLevels = 1;
				defaultMaterialWall = Materials.WOOD_WALL;
				defaultMaterialRoof = Materials.WOOD;
				break;

			case "roof":
				defaultLevels = 1;
				defaultMaterialWindows = null;
				break;

			case "church":
			case "hangar":
			case "industrial":
				defaultMaterialWindows = null;
				break;

			default:
				if (!tags.containsKey("building:levels")) {
					defaultMaterialWindows = null;
				}
				break;
			}

			if (tags.contains("parking", "multi-storey")) {
				defaultLevels = 5;
				defaultMaterialWindows = null;
			}

			/* determine levels */

			buildingLevels = defaultLevels;

			Float parsedLevels = null;

			if (tags.containsKey("building:levels")) {
				parsedLevels = parseOsmDecimal(tags.getValue("building:levels"), false);
			}

			if (parsedLevels != null) {
				buildingLevels = (int)(float)parsedLevels;
			} else if (parseHeight(tags, -1) > 0) {
				buildingLevels = max(1, (int)(parseHeight(tags, -1) / defaultHeightPerLevel));
			}

			minLevel = 0;

			if (tags.containsKey("building:min_level")) {
				Float parsedMinLevel = parseOsmDecimal(tags.getValue("building:min_level"), false);
				if (parsedMinLevel != null) {
					minLevel = (int)(float)parsedMinLevel;
				}
			}

			/* determine roof shape */

			boolean explicitRoofTagging = true;

			if (!("no".equals(tags.getValue("roof:lines"))) && hasComplexRoof(area)) {
				roof = new ComplexRoof();
			} else {

				String roofShape = tags.getValue("roof:shape");
				if (roofShape == null) { roofShape = tags.getValue("building:roof:shape"); }

				if (roofShape == null) {
					roofShape = defaultRoofShape;
					explicitRoofTagging = false;
				}

				try {

					roof = createRoofForShape(roofShape);

				} catch (InvalidGeometryException e) {
					System.err.println("falling back to FlatRoof: " + e);
					roof = new FlatRoof();
					explicitRoofTagging = false;
				}

			}

			/* determine height */

			double fallbackHeight = buildingLevels * defaultHeightPerLevel + roof.getRoofHeight();
			double height = parseHeight(tags, (float)fallbackHeight);

			// Make sure buildings have at least some height
			height = Math.max(height, 0.001);

			heightWithoutRoof = height - roof.getRoofHeight();

			/* determine materials */

		    if (defaultMaterialRoof == Materials.ROOF_DEFAULT
		    		&& explicitRoofTagging && roof instanceof FlatRoof) {
		    	defaultMaterialRoof = Materials.CONCRETE;
		    }

		    if (useBuildingColors) {

		    	materialWall = buildMaterial(
		    			tags.getValue("building:material"),
		    			tags.getValue("building:colour"),
		    			defaultMaterialWall, false);
		    	materialRoof = buildMaterial(
		    			tags.getValue("roof:material"),
		    			tags.getValue("roof:colour"),
		    			defaultMaterialRoof, true);

		    } else {

		    	materialWall = defaultMaterialWall;
		    	materialRoof = defaultMaterialRoof;

		    }

		    if (materialWall == Materials.GLASS) {
				// avoid placing windows into a glass front
				// TODO: the == currently only works if GLASS is not colorable
				defaultMaterialWindows = null;
		    }

		    materialWallWithWindows = materialWall;

		    if (drawBuildingWindows) {

		    	Material materialWindows = defaultMaterialWindows;

		    	if (materialWindows != null) {

		    		materialWallWithWindows = materialWallWithWindows.
		    				withAddedLayers(materialWindows.getTextureDataList());

		    	}

		    }

		}

		private Material buildMaterial(String materialString,
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
				} else if (CSSColors.colorMap.containsKey(colorString)){
					color = CSSColors.colorMap.get(colorString);
				} else {
					color = parseColor(colorString);
				}

				if (color != null) {
					material = new ImmutableMaterial(
							material.getInterpolation(), color,
							material.getAmbientFactor(),
							material.getDiffuseFactor(),
							material.getSpecularFactor(),
							material.getShininess(),
							material.getTransparency(),
							material.getShadow(),
							material.getAmbientOcclusion(),
							material.getTextureDataList());
				}

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

		/**
		 * draws a triangle strip, but omits degenerate triangles
		 */
		private static final void drawStripWithoutDegenerates(
				Target<?> target, Material material, List<VectorXYZ> vectors,
				List<List<VectorXZ>> texCoordLists) {

			List<TriangleXYZ> triangles = new ArrayList<TriangleXYZ>();
			List<List<VectorXZ>> triangleTexCoordLists = new ArrayList<List<VectorXZ>>(texCoordLists.size());

			for (int i = 0; i < texCoordLists.size(); i ++) {
				triangleTexCoordLists.add(new ArrayList<VectorXZ>());
			}

			for (int triangle = 0; triangle < vectors.size() - 2; triangle++) {

				int indexA = triangle % 2 == 0 ? triangle : triangle + 1;
				int indexB = triangle % 2 == 0 ? triangle + 1 : triangle;
				int indexC = triangle + 2;

				TriangleXYZ t = new TriangleXYZ(
						vectors.get(indexA),
						vectors.get(indexB),
						vectors.get(indexC));

				if (!t.isDegenerate()) {

					triangles.add(t);

					for (int i = 0; i < texCoordLists.size(); i ++) {

						triangleTexCoordLists.get(i).add(texCoordLists.get(i).get(indexA));
						triangleTexCoordLists.get(i).add(texCoordLists.get(i).get(indexB));
						triangleTexCoordLists.get(i).add(texCoordLists.get(i).get(indexC));

					}
				}
			}

			if (triangles.size() > 0) {
				target.drawTriangles(material, triangles, triangleTexCoordLists);
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

				// fallback from roof:direction to roof:slope:direction
				if (slopeDirection == null && tags.containsKey("roof:slope:direction")) {
					Float angle = parseAngle(tags.getValue("roof:slope:direction"));
					if (angle != null) {
						slopeDirection = VectorXZ.fromAngle(toRadians(angle));
					}
				}

				if (slopeDirection != null) {

					SimplePolygonXZ simplifiedOuter =
							polygon.getOuter().getSimplifiedPolygon();

					/* find ridge by calculating the outermost intersections of
					 * the quasi-infinite slope "line" towards the centroid vector
					 * with segments of the polygon */

					VectorXZ center = simplifiedOuter.getCentroid();

					Collection<LineSegmentXZ> intersections =
							simplifiedOuter.intersectionSegments(new LineSegmentXZ(
								center.add(slopeDirection.mult(-1000)), center));

					LineSegmentXZ outermostIntersection = null;
					double distanceOutermostIntersection = -1;

					for (LineSegmentXZ i : intersections) {
						double distance = distanceFromLineSegment(center, i);
						if (distance > distanceOutermostIntersection) {
							outermostIntersection = i;
							distanceOutermostIntersection = distance;
						}
					}

					ridge = outermostIntersection;

					/* calculate maximum distance from ridge */

					double maxDistance = 0.1;

					for (VectorXZ v : polygon.getOuter().getVertexLoop()) {
						double distance = distanceFromLine(v, ridge.p1, ridge.p2);
						if (distance > maxDistance) {
							maxDistance = distance;
						}
					}

					roofLength = maxDistance;

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
					double distance = distanceFromLineSegment(pos, ridge);
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

	private static class Wall implements RenderableToAllTargets {

		private final BuildingPart buildingPart;

		/** nodes, ordered such that the building's outside is to the right */
		private final List<MapNode> nodes;

		public Wall(BuildingPart buildingPart, List<MapNode> nodes) {
			this.buildingPart = buildingPart;
			this.nodes = nodes;
		}

		@Override
		public void renderTo(Target<?> target) {

			double baseEle = buildingPart.building.getGroundLevelEle();
			double floorHeight = buildingPart.calculateFloorHeight(buildingPart.roof);
			double floorEle = baseEle + floorHeight;

			/* calculate the lower boundary of the wall */

			List<VectorXYZ> bottomPoints = nodes.stream()
					.map(MapNode::getPos)
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
				throw new IllegalArgumentException("cannot construct top boundary of wall");
			}

			List<VectorXYZ> topPoints = topPointsXZ.stream()
					.map(p -> p.xyz(buildingPart.roof.getRoofEleAt(p) - floorEle))
					.collect(toList());

			/* construct the surface */

			List<VectorXZ> lowerSurfaceBoundary = toPointsOnSurface(bottomPoints);
			List<VectorXZ> upperSurfaceBoundary = toPointsOnSurface(topPoints);

			WallSurface surface = new WallSurface(lowerSurfaceBoundary, upperSurfaceBoundary);

			/* add windows */

			//TODO remove - surface.addElementIfSpaceFree(new Window(new VectorXZ(surface.getLength() / 2, 3), 2.6, 1.5));

			//TODO: no windows for glass walls, churches etc.

			placeDefaultWindows(surface);

			/* draw the wall */

			List<VectorXYZ> bottomPointsXYZ = addYList(bottomPoints, floorEle);
			surface.renderTo(target, bottomPointsXYZ);

			//TODO: der fehlende Bereich "unter dem Dach" (z.B. bei Runddach) liegt an fehlenden Punkten aus Roof-Umriss
			// drawSimpleWall(target, baseEle, 0, roof, bottomPoints);

			//FIXME draw floor of the building part

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

				//TODO: in the future, allow levels of different height (but calculate level heights in BuildingPart)
				double levelHeight = buildingPart.heightWithoutRoof / buildingPart.buildingLevels;
				double levelMinHeight = levelHeight * level;

				double windowHeight = 0.5 * levelHeight;
				double breastHeight = 0.3 * levelHeight;

				double windowWidth = 1;
				//double windowWidth = 0.81 * windowHeight; //TODO remove

				int numColums = (int) round(surface.getLength() / (2 * windowWidth));

				for (int i = 0; i < numColums; i++) {

					VectorXZ pos = new VectorXZ(i * surface.getLength() / numColums,
							levelMinHeight + breastHeight + windowHeight/2);

					Window window = new Window(pos, windowWidth, windowHeight);
					surface.addElementIfSpaceFree(window);

				}

			}

		}

		/**
		 * a simplified representation of the wall as a 2D plane, with its origin in the bottom left corner.
		 * This streamlines the placement of objects (windows, doors, and similar features) onto the wall.
		 * Afterwards, positions are converted back into 3D space.
		 */
		private class WallSurface {

			private final List<VectorXZ> lowerBoundary;
			private final List<VectorXZ> upperBoundary;

			private final SimplePolygonXZ wallOutline;

			private final List<WallElement> elements = new ArrayList<>();

			/**
			 * Constructs a wall surface from a lower and upper wall boundary.
			 * The boundaries' x coordinates is the position along the wall (starting with 0 for the first point),
			 * the z coordinates refer to height.
			 */
			public WallSurface(List<VectorXZ> lowerBoundary, List<VectorXZ> upperBoundary) {

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
				reverse(outerLoop);
				outerLoop.addAll(0, lowerBoundary);
				outerLoop.add(lowerBoundary.get(0));

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

				boolean spaceOccupied = elements.stream().anyMatch(e -> e.outline().intersects(element.outline()));

				if (!spaceOccupied) {
					elements.add(element);
				}

			}

			/** renders the wall; requires it to be anchored back into 3D space */
			public void renderTo(Target<?> target, List<VectorXYZ> bottomPointsXYZ) {

				/* triangulate the empty wall surface */

				List<SimplePolygonXZ> holes = elements.stream().map(WallElement::outline).collect(toList());

				List<TriangleXZ> triangles = triangulate(wallOutline, holes);
				List<TriangleXYZ> trianglesXYZ = triangles.stream().map(t -> convertTo3D(t, bottomPointsXYZ)).collect(toList());

				List<VectorXZ> texCoords = new ArrayList<>();

				for (TriangleXZ triangle : triangles) {
					texCoords.add(triangle.v1);
					texCoords.add(triangle.v2);
					texCoords.add(triangle.v3);
				}

				target.drawTriangles(buildingPart.materialWall, trianglesXYZ,
						nCopies(buildingPart.materialWall.getNumTextureLayers(), texCoords));

				/* render the elements on the wall */

				for (WallElement e : elements) {
					e.renderTo(target, this, bottomPointsXYZ);
				}

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
						position.add(new VectorXZ(-width/2, -height/2)),
						position.add(new VectorXZ(+width/2, -height/2)),
						position.add(new VectorXZ(+width/2, +height/2)),
						position.add(new VectorXZ(-width/2, +height/2)),
						position.add(new VectorXZ(-width/2, -height/2))));

			}

			@Override
			public void renderTo(Target<?> target, Wall.WallSurface surface, List<VectorXYZ> bottomPointsXYZ) {

				double depth = 0.15;

				VectorXYZ bottomLeft = surface.convertTo3D(outline().getVertex(0), bottomPointsXYZ);
				VectorXYZ bottomRight = surface.convertTo3D(outline().getVertex(1), bottomPointsXYZ);
				VectorXYZ topLeft = surface.convertTo3D(outline().getVertex(3), bottomPointsXYZ);
				VectorXYZ topRight = surface.convertTo3D(outline().getVertex(2), bottomPointsXYZ);

				VectorXYZ toBack = new TriangleXYZ(bottomLeft, topLeft, bottomRight).getNormal().mult(depth);

				VectorXYZ bottomLeftBack = bottomLeft.add(toBack);
				VectorXYZ bottomRightBack = bottomRight.add(toBack);
				VectorXYZ topLeftBack = topLeft.add(toBack);
				VectorXYZ topRightBack = topRight.add(toBack);

				/* draw the window itself */

				List<VectorXYZ> vsWindow = asList(topLeftBack, bottomLeftBack, topRightBack, bottomRightBack);

				target.drawTriangleStrip(SINGLE_WINDOW, vsWindow,
						texCoordLists(vsWindow, SINGLE_WINDOW, STRIP_FIT));

				/* draw the wall around the window */

				List<VectorXYZ> vsWall = asList(
						bottomLeftBack, bottomLeft,
						bottomRightBack, bottomRight,
						topRightBack, topRight,
						topLeftBack, topLeft,
						bottomLeftBack, bottomLeft);

				Material material = new ImmutableMaterial(
						buildingPart.materialWall.getInterpolation(),
						buildingPart.materialWall.getColor(),
						0.2f * buildingPart.materialWall.getAmbientFactor(), //coarsely approximate ambient occlusion
						buildingPart.materialWall.getDiffuseFactor(),
						buildingPart.materialWall.getSpecularFactor(),
						buildingPart.materialWall.getShininess(),
						buildingPart.materialWall.getTransparency(),
						buildingPart.materialWall.getShadow(),
						buildingPart.materialWall.getAmbientOcclusion(),
						buildingPart.materialWall.getTextureDataList());

				target.drawTriangleStrip(material, vsWall,
						texCoordLists(vsWall, material, NamedTexCoordFunction.STRIP_WALL));

			}

		}

	}

	private static class BuildingEntrance implements NodeWorldObject,
		RenderableToAllTargets {

		private final BuildingPart buildingPart;
		private final MapNode node;

		private final EleConnector connector;

		public BuildingEntrance(BuildingPart buildingPart, MapNode node) {
			this.buildingPart = buildingPart;
			this.node = node;
			this.connector = new EleConnector(node.getPos(), node, getGroundState());
		}

		@Override
		public MapNode getPrimaryMapElement() {
			return node;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return singleton(connector);
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {

			/* TODO for level != null and ABOVE/BELO, add vertical distance to ground */

		}

		@Override
		public GroundState getGroundState() {

			boolean onlyOn = true;
			boolean onlyAbove = true;
			boolean onlyBelow = true;

			for (MapWaySegment waySegment : node.getConnectedWaySegments()) {

				if (waySegment.getPrimaryRepresentation() instanceof
						AbstractNetworkWaySegmentWorldObject) {

					switch (waySegment.getPrimaryRepresentation().getGroundState()) {
					case ABOVE: onlyOn = false; onlyBelow = false; break;
					case BELOW: onlyOn = false; onlyAbove = false; break;
					case ON: onlyBelow = false; onlyAbove = false; break;
					}

				}

			}

			if (onlyOn) {
				return ON;
			} else if (onlyAbove) {
				return ABOVE;
			} else if (onlyBelow) {
				return BELOW;
			} else {
				return ON;
			}

		}

		public int getLevel() {

			try {
				return Integer.parseInt(node.getTags().getValue("level"));
			} catch (NumberFormatException e) {
				return 0;
			}

		}

		@Override
		public void renderTo(Target<?> target) {

			/* calculate a vector that points out of the building */

			VectorXZ outOfBuilding = VectorXZ.Z_UNIT;

			for (SimplePolygonXZ polygon :
				buildingPart.polygon.getPolygons()) {

				final List<VectorXZ> vs = polygon.getVertexLoop();
				int entranceI = vs.indexOf(node.getPos());

				if (entranceI != -1) {

					VectorXZ posBefore = vs.get((vs.size() + entranceI - 1) % vs.size());
					VectorXZ posAfter = vs.get((vs.size() + entranceI + 1) % vs.size());

					outOfBuilding = posBefore.subtract(posAfter).rightNormal();
					if (!polygon.isClockwise()) {
						outOfBuilding = outOfBuilding.invert();
					}

					break;

				}

			}

			/* draw the entrance as a box protruding from the building */

			VectorXYZ center = connector.getPosXYZ();

			float height = parseHeight(node.getTags(), 2);
			float width = parseWidth(node.getTags(), 1);

			target.drawBox(Materials.ENTRANCE_DEFAULT,
					center, outOfBuilding, height, width, 0.1);

		}

	}

	static final TagGroup inheritTags(TagGroup ownTags, TagGroup parentTags) {

		List<Tag> tags = Lists.newArrayList(ownTags);

		for (Tag tag : parentTags) {
			if (!ownTags.containsKey(tag.key)) {
				tags.add(tag);
			}
		}

		return new MapBasedTagGroup(tags);

	}

}
