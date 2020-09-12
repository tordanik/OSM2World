package org.osm2world.core.world.modules.building;

import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;
import static org.osm2world.core.math.GeometryUtil.insertIntoPolygon;
import static org.osm2world.core.math.VectorXZ.*;
import static org.osm2world.core.util.ValueParseUtil.parseLevels;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.inheritTags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolylineShapeXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;

import com.google.common.collect.Streams;

public class Wall implements Renderable {

	final @Nullable MapWay wallWay;

	private final BuildingPart buildingPart;

	/** points, ordered such that the building's outside is to the right */
	private final PolylineShapeXZ points;

	/** the nodes corresponding to the {@link #points}. No guarantee that all/any points have a matching node! */
	private final Map<VectorXZ, MapNode> pointNodeMap;

	private final double floorHeight;

	/** the tags for this part, including tags inherited from {@link #buildingPart} and its {@link Building} */
	private final TagSet tags;

	public Wall(@Nullable MapWay wallWay, BuildingPart buildingPart, List<VectorXZ> points,
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

	public Wall(@Nullable MapWay wallWay, BuildingPart buildingPart, List<MapNode> nodes) {
		this(wallWay, buildingPart,
				nodes.stream().map(MapNode::getPos).collect(toList()),
				nodes.stream().collect(toMap(MapNode::getPos, n -> n)),
				buildingPart == null ? 0 : buildingPart.calculateFloorHeight());
	}

	@Override
	public void renderTo(Target target) {

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

		/* use configuration to determine which implementation of window rendering to use */

		WindowImplementation windowImplementation;

		if (Streams.stream(buildingPart.tags).anyMatch(t -> t.key.startsWith("window"))) {
			//explicitly mapped windows, use different (usually higher LOD) setting
			windowImplementation = WindowImplementation.getValue(
					buildingPart.config.getString("explicitWindowImplementation"), WindowImplementation.FULL_GEOMETRY);
		} else {
			windowImplementation = WindowImplementation.getValue(
				buildingPart.config.getString("implicitWindowImplementation"), WindowImplementation.FLAT_TEXTURES);
		}

		/* calculate the lower boundary of the wall */

		List<VectorXYZ> bottomPoints = listXYZ(points.getVertexList(), floorEle);

		/* calculate the upper boundary of the wall (from roof polygons) */

		List<VectorXZ> topPointsXZ = null;

		for (SimplePolygonXZ rawPolygon : buildingPart.roof.getPolygon().getPolygons()) {

			SimplePolygonXZ polygon = buildingPart.roof.getPolygon().getHoles().contains(rawPolygon)
					? rawPolygon.makeClockwise()
					: rawPolygon.makeCounterclockwise();

			VectorXZ firstBottomPoint = bottomPoints.get(0).xz();
			VectorXZ lastBottomPoint = bottomPoints.get(bottomPoints.size() - 1).xz();

			for (VectorXZ bottomPoint : asList(firstBottomPoint, lastBottomPoint)) {
				// insert points that are in the bottom polygon, but not in the top (e.g. with building passage)
				if (!polygon.getVertices().contains(bottomPoint)
						&& polygon.getClosestSegment(bottomPoint).closestPoint(bottomPoint).distanceTo(bottomPoint) < 0.1) {
					try {
						polygon = insertIntoPolygon(polygon, bottomPoint, 0);
					} catch (InvalidGeometryException e) { /* keep the previous polygon */ }
				}
			}

			int firstIndex = polygon.getVertices().indexOf(firstBottomPoint);
			int lastIndex = polygon.getVertices().indexOf(lastBottomPoint);

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
				.map(p -> p.xyz(baseEle + heightWithoutRoof + buildingPart.roof.getRoofHeightAt(p)))
				.collect(toList());

		/* construct the surface(s) */

		WallSurface mainSurface, roofSurface;

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

			List<VectorXYZ> middlePoints = asList(
					bottomPoints.get(0).addY(heightWithoutRoof - floorHeight),
					bottomPoints.get(bottomPoints.size() - 1).addY(heightWithoutRoof - floorHeight));

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

		/* add individually mapped doors and windows (if any) */
		//TODO: doors at corners of the building (or boundaries between building:wall=yes ways) do not work yet
		//TODO: cannot place doors into roof walls yet

		boolean individuallyMappedWindows = false;

		for (MapNode node : getNodes()) {

			Set<Integer> levels = new HashSet<>();
			levels.add(min(parseLevels(node.getTags().getValue("level"), singletonList(0))));
			levels.addAll(parseLevels(node.getTags().getValue("repeat_on"), emptyList()));

			levels = levels.stream().map(l -> buildingPart.levelConversion(l)).collect(toSet());

			for (int level : levels) {

				if (getBuildingPart().containsLevel(level)) {

					VectorXZ pos = new VectorXZ(points.offsetOf(node.getPos()),
							buildingPart.getLevelHeightAboveBase(level)
									- buildingPart.getLevelHeightAboveBase(buildingPart.buildingMinLevel));

					if ((node.getTags().contains("building", "entrance")
							|| node.getTags().containsKey("entrance")
							|| node.getTags().containsKey("door"))) {

						DoorParameters params = DoorParameters.fromTags(node.getTags(), this.tags);
						mainSurface.addElementIfSpaceFree(new Door(pos, params));

					} else if (node.getTags().containsKey("window")
							&& !node.getTags().contains("window", "no")) {

						boolean transparent = buildingPart.getBuilding().queryWindowSegments(node, level);

						TagSet windowTags = inheritTags(node.getTags(), tags);
						WindowParameters params = new WindowParameters(windowTags, buildingPart.getLevelHeight(level));
						GeometryWindow window = new GeometryWindow(new VectorXZ(pos.x, pos.z + params.breast), params, transparent);
						mainSurface.addElementIfSpaceFree(window);

						individuallyMappedWindows = true;

					}
				}
			}
		}

		if (tags.containsAny(asList("building", "building:part"), asList("garage", "garages"))) {
			if (!buildingPart.area.getBoundaryNodes().stream().anyMatch(
					n -> n.getTags().containsKey("entrance") || n.getTags().containsKey("door"))) {
				placeDefaultGarageDoors(mainSurface);
			}
		}

		/* add windows (after doors, because default windows should be displaced by them) */

		if (hasWindows && !individuallyMappedWindows
				&& (windowImplementation == WindowImplementation.INSET_TEXTURES
					|| windowImplementation == WindowImplementation.FULL_GEOMETRY)) {
			placeDefaultWindows(mainSurface, windowImplementation);
		}

		/* draw the wall */

		double windowHeight = buildingPart.heightWithoutRoof / buildingPart.buildingLevels;

		if (mainSurface != null) {
			mainSurface.renderTo(target, new VectorXZ(0, -floorHeight),
					hasWindows && !individuallyMappedWindows
						&& windowImplementation == WindowImplementation.FLAT_TEXTURES,
					windowHeight, true);
		}

		if (roofSurface != null) {
			roofSurface.renderTo(target, NULL_VECTOR, false, windowHeight, true);
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

	protected BuildingPart getBuildingPart() { return buildingPart; }

	protected double getFloorHeight() { return floorHeight; }

	protected TagSet getTags() { return tags; }

	protected PolylineShapeXZ getPoints() { return points; }

	/** places the default (i.e. not explicitly mapped) windows rows onto a wall surface */
	private void placeDefaultWindows(WallSurface surface, WindowImplementation implementation) {

		for (int level = 0; level < buildingPart.buildingLevels; level++) {

			double levelHeight = buildingPart.getLevelHeight(level);
			double heightAboveBase = buildingPart.getLevelHeightAboveBase(level);

			WindowParameters windowParams = new WindowParameters(tags, levelHeight);

			int numColums = windowParams.numberWindows != null
					? windowParams.numberWindows
					: (int) round(surface.getLength() / (2 * windowParams.width));

			for (int i = 0; i < numColums; i++) {

				VectorXZ pos = new VectorXZ((i + 0.5) * surface.getLength() / numColums,
						heightAboveBase + windowParams.breast);

				Window window = implementation == WindowImplementation.FULL_GEOMETRY
						? new GeometryWindow(pos, windowParams, false)
						: new TexturedWindow(pos, windowParams);
				surface.addElementIfSpaceFree(window);

			}

		}

	}

	/** places default (i.e. not explicitly mapped) doors onto garage walls */
	private void placeDefaultGarageDoors(WallSurface surface) {

		TagSet doorTags = TagSet.of("door", "overhead");

		double doorDistance = 1.25 * DoorParameters.fromTags(doorTags, this.tags).width;
		int numDoors = (int) round(surface.getLength() / doorDistance);

		for (int i = 0; i < numDoors; i++) {
			VectorXZ pos = new VectorXZ(surface.getLength() / numDoors * (i + 0.5), 0);
			surface.addElementIfSpaceFree(new Door(pos, DoorParameters.fromTags(doorTags, TagSet.of())));
		}

	}

}
