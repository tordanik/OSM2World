package org.osm2world.world.modules.building;

import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.VectorXZ.listXYZ;
import static org.osm2world.math.algorithms.GeometryUtil.insertIntoPolygon;
import static org.osm2world.scene.mesh.LevelOfDetail.*;
import static org.osm2world.util.ValueParseUtil.parseLevels;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.inheritTags;

import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapSegment;
import org.osm2world.map_data.data.MapWay;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolylineShapeXZ;
import org.osm2world.math.shapes.PolylineXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.mesh.LODRange;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.building.LevelAndHeightData.Level;
import org.osm2world.world.modules.building.LevelAndHeightData.Level.LevelType;
import org.osm2world.world.modules.building.indoor.IndoorArea;
import org.osm2world.world.modules.building.indoor.IndoorRoom;

import com.google.common.collect.Streams;

/**
 * an outer wall of a {@link BuildingPart}
 */
public class ExteriorBuildingWall {

	final @Nullable MapWay wallWay;

	private final BuildingPart buildingPart;

	/** points, ordered such that the building's outside is to the right */
	private final PolylineShapeXZ points;

	/** the nodes corresponding to the {@link #points}. No guarantee that all/any points have a matching node! */
	private final Map<VectorXZ, MapNode> pointNodeMap;

	private final double floorHeight;

	/** the tags for this part, including tags inherited from {@link #buildingPart} and its {@link Building} */
	private final TagSet tags;

	public ExteriorBuildingWall(@Nullable MapWay wallWay, BuildingPart buildingPart, List<VectorXZ> points,
			Map<VectorXZ, MapNode> pointNodeMap, double floorHeight) {

		this.wallWay = wallWay;
		this.buildingPart = buildingPart;
		this.points = points.size() == 2 ? new LineSegmentXZ(points.get(0), points.get(1)) : new PolylineXZ(points);
		this.pointNodeMap = pointNodeMap;
		this.floorHeight = floorHeight;

		if (buildingPart == null) {
			this.tags = TagSet.of();
		} else if (wallWay != null) {
			this.tags = inheritTags(wallWay.getTags(), buildingPart.tags);
		} else {
			this.tags = buildingPart.tags;
		}

	}

	public ExteriorBuildingWall(@Nullable MapWay wallWay, BuildingPart buildingPart, List<MapNode> nodes) {
		this(wallWay, buildingPart,
				nodes.stream().map(MapNode::getPos).collect(toList()),
				nodes.stream().collect(toMap(MapNode::getPos, n -> n)),
				buildingPart == null ? 0 : buildingPart.levelStructure.bottomHeight());
	}

	public void renderTo(ProceduralWorldObject.Target target) {

		BuildingDefaults defaults = BuildingDefaults.getDefaultsFor(tags);

		double baseEle = buildingPart.building.getGroundLevelEle();
		double floorEle = baseEle + floorHeight;
		double heightWithoutRoof = buildingPart.levelStructure.heightWithoutRoof();

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

			if (!tags.containsKey("building:levels") || tags.contains("building:levels", "0")
					|| points.getLength() < 1.0) {
				hasWindows = false;
			}

			if (material == Materials.GLASS_WALL) {
				// avoid placing windows into a glass front
				// TODO: the == currently only works if GLASS_WALL is not colorable
				hasWindows = false;
			}

		}

		/* calculate the lower boundary of the wall */

		List<VectorXYZ> bottomPoints = listXYZ(points.vertices(), floorEle);

		/* calculate the upper boundary of the wall (from roof polygons) */

		List<VectorXZ> topPointsXZ = null;

		outer:
		for (boolean allowInsertion : List.of(false, true)) {
			for (SimplePolygonXZ rawPolygon : buildingPart.roof.getPolygon().getRings()) {

				SimplePolygonXZ polygon = buildingPart.roof.getPolygon().getHoles().contains(rawPolygon)
						? rawPolygon.makeClockwise()
						: rawPolygon.makeCounterclockwise();

				VectorXZ firstBottomPoint = bottomPoints.get(0).xz();
				VectorXZ lastBottomPoint = bottomPoints.get(bottomPoints.size() - 1).xz();

				for (VectorXZ bottomPoint : asList(firstBottomPoint, lastBottomPoint)) {
					// insert points that are in the bottom polygon, but not in the top (e.g. with building passage)
					if (!polygon.getVertices().contains(bottomPoint)) {
						if (allowInsertion && polygon.getClosestSegment(bottomPoint).closestPoint(bottomPoint).distanceTo(bottomPoint) < 0.1) {
							try {
								polygon = insertIntoPolygon(polygon, bottomPoint, 0);
							} catch (InvalidGeometryException e) { /* keep the previous polygon */ }
						}
					}
				}

				int firstIndex = polygon.verticesNoDup().indexOf(firstBottomPoint);
				int lastIndex = polygon.verticesNoDup().indexOf(lastBottomPoint);

				if (firstIndex != -1 && lastIndex != -1) {

					topPointsXZ = new ArrayList<>();

					if (lastIndex < firstIndex) {
						lastIndex += polygon.size();
					}

					for (int i = firstIndex; i <= lastIndex; i++) {
						topPointsXZ.add(polygon.getVertex(i % polygon.size()));
					}

					break outer;

				}

			}
		}

		if (topPointsXZ == null) {
			// just use the same points as for the bottom.
			// This might miss some roof details and should only happen with legacy features like building_passage.
			ConversionLog.warn("cannot construct top boundary of wall", buildingPart.area);
			topPointsXZ = points.vertices();
		}

		List<VectorXYZ> topPoints = topPointsXZ.stream()
				.map(p -> p.xyz(baseEle + heightWithoutRoof + buildingPart.roof.getRoofHeightAt(p)))
				.collect(toList());

		/* use configuration to determine which implementation of window rendering to use */

		boolean explicitWindows = Streams.stream(buildingPart.tags).anyMatch(t -> t.key.startsWith("window"));

		Map<WindowImplementation, LODRange> windowImplementations = chooseWindowImplementations(explicitWindows,
				getNodes().stream().anyMatch(Door::isDoorNode), buildingPart.config);

		for (WindowImplementation windowImplementation : windowImplementations.keySet()) {

			LODRange lodRange = windowImplementations.get(windowImplementation);
			if (buildingPart.config.containsKey("lod") &&
					!lodRange.contains(buildingPart.config.getLod())) continue;
			target.setCurrentLodRange(lodRange);

			/* construct the surface(s) */

			@Nullable WallSurface mainSurface, roofSurface;

			double maxHeight = max(topPoints, comparingDouble(v -> v.y)).y - floorEle;

			if (windowImplementation != WindowImplementation.FLAT_TEXTURES
					|| !hasWindows || maxHeight + floorHeight - heightWithoutRoof < 0.01) {

				roofSurface = null;

				try {
					mainSurface = new WallSurface(material, bottomPoints, topPoints);
				} catch (InvalidGeometryException e) {
					mainSurface = null;
				}

			} else {

				// using window textures. Need to separate the bit of wall "in the roof" which should not have windows.

				double middlePointsHeight = Math.min(heightWithoutRoof - floorHeight,
						min(topPoints, comparingDouble(v -> v.y)).y - floorEle);

				List<VectorXYZ> middlePoints = asList(
						bottomPoints.get(0).addY(middlePointsHeight),
						bottomPoints.get(bottomPoints.size() - 1).addY(middlePointsHeight));

				try {
					mainSurface = new WallSurface(material, bottomPoints, middlePoints);
				} catch (InvalidGeometryException e) {
					mainSurface = null;
				}

				try {
					roofSurface = new WallSurface(material, middlePoints, topPoints);
				} catch (InvalidGeometryException e) {
					roofSurface = null;
				}

			}

			boolean individuallyMappedWindows = false;

			if (mainSurface != null && windowImplementation != WindowImplementation.NONE) {

				/* add individually mapped doors and windows (if any) */
				//TODO: doors at corners of the building (or boundaries between building:wall=yes ways) do not work yet
				//TODO: cannot place doors into roof walls yet

				for (MapNode node : getNodes()) {

					Set<Integer> levels = new HashSet<>();
					levels.add(min(parseLevels(node.getTags().getValue("level"), singletonList(0))));
					levels.addAll(parseLevels(node.getTags().getValue("repeat_on"), emptyList()));

					for (int level : levels) {

						if (getBuildingPart().levelStructure.hasLevel(level)) {

							VectorXZ pos = new VectorXZ(points.offsetOf(node.getPos()),
									buildingPart.levelStructure.level(level).relativeEle
											- buildingPart.levelStructure.bottomHeight());

							if (Door.isDoorNode(node)) {

								DoorParameters params = DoorParameters.fromTags(node.getTags(), this.tags);
								if (lodRange.max().ordinal() < 3
										|| buildingPart.config.getLod().ordinal() < 3) {
									params = params.withInset(0.0);
								}
								mainSurface.addElementIfSpaceFree(new Door(pos, params));

							} else if (node.getTags().containsKey("window")
									&& !node.getTags().contains("window", "no")) {

								boolean transparent = determineWindowTransparency(node, level);

								TagSet windowTags = inheritTags(node.getTags(), tags);
								WindowParameters params = new WindowParameters(windowTags, buildingPart.levelStructure.level(level).height);
								GeometryWindow window = new GeometryWindow(new VectorXZ(pos.x, pos.z + params.breast), params, transparent);
								mainSurface.addElementIfSpaceFree(window);

								individuallyMappedWindows = true;

							}
						}
					}
				}

			}

			if (mainSurface != null) {

				/* add garage doors */

				if (tags.containsAny(asList("building", "building:part"), asList("garage", "garages"))) {
					if (buildingPart.area.getBoundaryNodes().stream().noneMatch(Door::isDoorNode)) {
						if (points.getLength() > buildingPart.area.getOuterPolygon().getOutlineLength() / 8) {
							// not the narrow side of a long building with several garages
							placeDefaultGarageDoors(mainSurface);
						}
					}
				}

				/* add windows (after doors, because default windows should be displaced by them) */

				if (hasWindows && !individuallyMappedWindows
						&& (windowImplementation == WindowImplementation.INSET_TEXTURES
						|| windowImplementation == WindowImplementation.FULL_GEOMETRY)) {
					placeDefaultWindows(mainSurface, windowImplementation);
				}

			}

			/* draw the wall */

			int levelCount = buildingPart.levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND)).size();
			Double windowHeight = (heightWithoutRoof - buildingPart.levelStructure.bottomHeight()) / levelCount;

			if (!hasWindows && buildingPart.getTags().contains("building", "roof")) {
				// the single "level" of wall below the roof is not a suitable indicator of level height for glass walls
				windowHeight = null;
			}

			if (mainSurface != null) {
				mainSurface.renderTo(target, new VectorXZ(0, -floorHeight),
						hasWindows && !individuallyMappedWindows
								&& windowImplementation == WindowImplementation.FLAT_TEXTURES,
						windowHeight,
						"wall", "wall_mounted");
			}

			if (roofSurface != null) {
				roofSurface.renderTo(target, NULL_VECTOR, false, windowHeight,
						"wall", "wall_mounted");
			}

		}

		target.setCurrentLodRange(null);

	}

	@Override
	public String toString() {
		if (getNodes().size() == points.vertices().size()) {
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
		for (VectorXZ point : points.vertices()) {
			if (pointNodeMap.containsKey(point)) {
				result.add(pointNodeMap.get(point));
			}
		}
		return result;
	}

	protected BuildingPart getBuildingPart() { return buildingPart; }

	protected double getFloorHeight() { return floorHeight; }

	protected TagSet getTags() { return tags; }

	protected PolylineShapeXZ getPoints() { return points; }

	private static Map<WindowImplementation, LODRange> chooseWindowImplementations(
			boolean explicitWindows, boolean hasDoor, O2WConfig config) {

		var explicitWI = WindowImplementation.getValue(config.getString("explicitWindowImplementation"), null);
		var implicitWI = WindowImplementation.getValue(config.getString("implicitWindowImplementation"), null);

		if (explicitWindows) {
			if (explicitWI != null) {
				return Map.of(explicitWI, new LODRange(LOD0, LOD4));
			} else {
				return Map.of(
						WindowImplementation.NONE, new LODRange(LOD0),
						WindowImplementation.FLAT_TEXTURES, new LODRange(LOD1, LOD2),
						WindowImplementation.FULL_GEOMETRY, new LODRange(LOD3, LOD4)
				);
			}
		} else {
			if (implicitWI != null) {
				return Map.of(implicitWI, new LODRange(LOD0, LOD4));
			} else {
				return Map.of(
						WindowImplementation.NONE, new LODRange(LOD0, LOD1),
						WindowImplementation.FLAT_TEXTURES, new LODRange(LOD2, LOD3),
						WindowImplementation.FULL_GEOMETRY, new LODRange(LOD4)
				);
			}
		}

	}

	/** places the default (i.e. not explicitly mapped) windows rows onto a wall surface */
	private void placeDefaultWindows(WallSurface surface, WindowImplementation implementation) {

		List<Window> windows = new ArrayList<>();

		for (Level level : buildingPart.levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND))) {

			WindowParameters windowParams = new WindowParameters(tags, level.height);

			double levelEle = level.relativeEle - buildingPart.levelStructure.bottomHeight();

			int numColums = windowParams.numberWindows != null
					? windowParams.numberWindows
					: (int) round(surface.getLength() / (2 * windowParams.overallProperties.width));

			for (int i = 0; i < numColums; i++) {

				VectorXZ pos = new VectorXZ((i + 0.5) * surface.getLength() / numColums,
						levelEle + windowParams.breast);

				Window window = implementation == WindowImplementation.FULL_GEOMETRY
						? new GeometryWindow(pos, windowParams, false)
						: new TexturedWindow(pos, windowParams);
				windows.add(window);

			}

		}

		surface.addElementsIfSpaceFree(windows);

	}

	/** places default (i.e. not explicitly mapped) doors onto garage walls */
	private void placeDefaultGarageDoors(WallSurface surface) {

		TagSet doorTags = TagSet.of("door", "overhead");
		DoorParameters params = DoorParameters.fromTags(doorTags, this.tags);

		if (buildingPart.config.getLod().ordinal() < 3) {
			params = params.withInset(0.0);
		}

		double doorDistance = 1.25 * params.width;
		int numDoors = (int) round(surface.getLength() / doorDistance);

		for (int i = 0; i < numDoors; i++) {
			VectorXZ pos = new VectorXZ(surface.getLength() / numDoors * (i + 0.5), 0);
			surface.addElementIfSpaceFree(new Door(pos, params));
		}

	}

	/**
	 * checks if a window should be transparent.
	 * For windows in an outer wall, this is the case if the indoor space behind the window has been mapped.
	 */
	public static boolean determineWindowTransparency(MapNode node, int level) {

		List<? extends WorldObject> connectedFeatures = node.getConnectedSegments().stream()
				.map(MapSegment::getElement)
				.flatMap(it -> it.getRepresentations().stream())
				.toList();

		if (connectedFeatures.stream().anyMatch(it -> it instanceof BuildingPart)) {
			// window in the outer wall, check if indoor space behind it is mapped
			return connectedFeatures.stream().anyMatch(it -> {
				if (it instanceof IndoorRoom r) {
					return r.getLevelRange().contains(level);
				} else if (it instanceof IndoorArea a) {
					return a.getFloorLevel() == level;
				} else {
					return false;
				}
			});
		} else {
			return true;
			// TODO check if both sides are mapped. Indoor mapping might be incomplete.
		}

	}

}
