package org.osm2world.core.world.modules;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.osm2world.core.map_elevation.creation.EleConstraintEnforcer.ConstraintType.*;
import static org.osm2world.core.math.GeometryUtil.interpolateElevation;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.*;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.Tag;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.TexCoordFunction;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.NetworkAreaWorldObject;
import org.osm2world.core.world.network.VisibleConnectorNodeWorldObject;

/**
 * adds roads to the world
 */
public class RoadModule extends ConfigurableWorldModule {

	/** determines whether right-hand or left-hand traffic is the default */
	private static final boolean RIGHT_HAND_TRAFFIC_BY_DEFAULT = true;

	@Override
	public void applyTo(MapData mapData) {

		for (MapWaySegment line : mapData.getMapWaySegments()) {
			if (isRoad(line.getTags())) {
				line.addRepresentation(new Road(line, line.getTags()));
			}
		}

		for (MapArea area : mapData.getMapAreas()) {

			if (isRoad(area.getTags())) {
				area.addRepresentation(new RoadArea(area));
			}

		}

		for (MapNode node : mapData.getMapNodes()) {

			TagSet tags = node.getTags();

			List<Road> connectedRoads = getConnectedRoads(node, false);

			if (connectedRoads.size() > 2) {

				node.addRepresentation(new RoadJunction(node));

			} else if (connectedRoads.size() == 2
					&& tags.contains("highway", "crossing")
					&& !tags.contains("crossing", "no")) {

				node.addRepresentation(new RoadCrossingAtConnector(node));

			} else if (connectedRoads.size() == 2) {

				Road road1 = connectedRoads.get(0);
				Road road2 = connectedRoads.get(1);

				if (road1.getWidth() != road2.getWidth()
						/* TODO: || lane layouts not identical */) {
					node.addRepresentation(new RoadConnector(node));
				}

			}

		}

	}

	private static boolean isRoad(TagSet tags) {
		if (tags.containsKey("highway")
				&& !tags.contains("highway", "construction")
				&& !tags.contains("highway", "proposed")) {
			return true;
		} else {
			return tags.contains("railway", "platform")
				|| tags.contains("leisure", "track");
		}
	}

	private static boolean isSteps(TagSet tags) {
		return tags.contains(new Tag("highway","steps"));
	}

	private static boolean isPath(TagSet tags) {
		String highwayValue = tags.getValue("highway");
		return "path".equals(highwayValue)
			|| "footway".equals(highwayValue)
			|| "cycleway".equals(highwayValue)
			|| "bridleway".equals(highwayValue)
			|| "steps".equals(highwayValue);
	}

	private static boolean isOneway(TagSet tags) {
		return tags.contains("oneway", "yes")
				|| (!tags.contains("oneway", "no")
					&& (tags.contains("highway", "motorway")
					|| (tags.contains("highway", "motorway_link"))));
	}

	private static int getDefaultLanes(TagSet tags) {
		String highwayValue = tags.getValue("highway");
		if (highwayValue == null
				|| isPath(tags)
				|| highwayValue.endsWith("_link")
				|| "service".equals(highwayValue)
				|| "track".equals(highwayValue)
				|| "residential".equals(highwayValue)
				|| "living_street".equals(highwayValue)
				|| "pedestrian".equals(highwayValue)
				|| "platform".equals(highwayValue)) {
			return 1;
		} else if ("motorway".equals(highwayValue)){
			return 2;
		} else {
			return isOneway(tags) ? 1 : 2;
		}
	}

	/**
	 * determines surface for a junction or connector/crossing.
	 * If the node has an explicit surface tag, this is evaluated.
	 * Otherwise, the result depends on the surface values of adjacent roads.
	 */
	private static Material getSurfaceForNode(MapNode node) {

		Material surface = getSurfaceMaterial(
				node.getTags().getValue("surface"), null);

		if (surface != null) {
			return surface;
		} else {

			/* choose the surface of any adjacent road */

			for (Road road : getConnectedRoads(node, false)) {
				return road.getSurface();
			}

			throw new IllegalStateException("node " + node + " has no connected roads");

		}

	}

	private static Material getSurfaceForRoad(TagSet tags,
			Material defaultSurface) {

		Material result;

		if (tags.containsKey("tracktype")) {
			if (tags.contains("tracktype", "grade1")) {
				result = ASPHALT;
			} else if (tags.contains("tracktype", "grade2")) {
				result = GRAVEL;
			} else {
				result = EARTH;
			}
		} else {
			result = defaultSurface;
		}

		return getSurfaceMaterial(tags.getValue("surface"), result);

	}

	private static Material getSurfaceMiddleForRoad(TagSet tags,
			Material defaultSurface) {

		Material result;

		if (tags.contains("tracktype", "grade4")
				|| tags.contains("tracktype", "grade5")) {
			result = TERRAIN_DEFAULT;
			// ideally, this would be the terrain type surrounds the track...
		} else {
			result = defaultSurface;
		}

		result = getSurfaceMaterial(tags.getValue("surface:middle"), result);

		if (result == GRASS) {
			result = TERRAIN_DEFAULT;
		}

		return result;

	}

	/**
	 * returns all roads connected to a node
	 * @param requireLanes  only include roads that are not paths and have lanes
	 */
	public static List<Road> getConnectedRoads(MapNode node, boolean requireLanes) {

		return getConnectedNetworkSegments(node, Road.class,
				road -> !requireLanes || (road.getLaneLayout() != null && !isPath(road.tags)));

	}

	/**
	 * find matching lane pairs
	 * (lanes that can be connected at a junction or connector)
	 */
	static Map<Integer, Integer> findMatchingLanes(
			List<Lane> lanes1, List<Lane> lanes2,
			boolean isJunction, boolean isCrossing) {

		Map<Integer, Integer> matches = new HashMap<Integer, Integer>();

		/*
		 * iterate from inside to outside
		 * (only for connectors, where it will lead to desirable connections
		 * between straight motorcar lanes e.g. at highway exits)
		 */

		if (!isJunction) {

			for (int laneI = 0; laneI < lanes1.size()
					&& laneI < lanes2.size(); ++laneI) {

				final Lane lane1 = lanes1.get(laneI);
				final Lane lane2 = lanes2.get(laneI);

				if (isCrossing && !lane1.type.isConnectableAtCrossings) {
					continue;
				} else if (isJunction && !lane1.type.isConnectableAtJunctions) {
					continue;
				}

				if (lane2.type.equals(lane1.type)) {

					matches.put(laneI, laneI);

				}

			}

		}

		/* iterate from outside to inside.
		 * Mostly intended to gather sidewalks and other non-car lanes. */

		for (int laneI = 0; laneI < lanes1.size()
				&& laneI < lanes2.size(); ++laneI) {

			int lane1Index = lanes1.size() - 1 - laneI;
			int lane2Index = lanes2.size() - 1 - laneI;

			final Lane lane1 = lanes1.get(lane1Index);
			final Lane lane2 = lanes2.get(lane2Index);

			if (isCrossing && !lane1.type.isConnectableAtCrossings) {
				continue;
			} else if (isJunction && !lane1.type.isConnectableAtJunctions) {
				continue;
			}

			if (matches.containsKey(lane1Index)
					|| matches.containsValue(lane2Index)) {
				continue;
			}

			if (lane2.type.equals(lane1.type)) {
				matches.put(lane1Index,	lane2Index);
			}

		}

		return matches;

	}

	/**
	 * determines connected lanes at a junction, crossing or connector
	 */
	private static List<LaneConnection> buildLaneConnections(
			MapNode node, boolean isJunction, boolean isCrossing) {

		List<Road> roads = getConnectedRoads(node, true);

		/* check whether the oneway special case applies (for one oneway splitting into multiple, or vice versa) */

		if (isJunction) {

			boolean allOneway = true;
			int firstInboundIndex = -1;

			for (int i = 0; i < roads.size(); i++) {

				Road road = roads.get(i);

				if (!isOneway(road.tags)) {
					allOneway = false;
					break;
				}

				if (firstInboundIndex == -1 && road.segment.getEndNode() == node) {
					firstInboundIndex = i;
				}

			}

			if (firstInboundIndex != -1) {

				// sort into inbound and outbound oneways
				// (need to be sequential blocks in the road list)

				List<Road> inboundOnewayRoads = new ArrayList<>();
				List<Road> outboundOnewayRoads = new ArrayList<>();

				int i = 0;

				for (i = firstInboundIndex; i < roads.size(); i++) {

					if (roads.get(i).segment.getEndNode() != node) {
						break; //not inbound
					}

					inboundOnewayRoads.add(roads.get(i));

				}

				reverse(inboundOnewayRoads);

				for (/* continue previous loop */;
						i % roads.size() != firstInboundIndex; i++) {

					outboundOnewayRoads.add(roads.get(i % roads.size()));

				}

				if (allOneway &&
						(inboundOnewayRoads.size() == 1 || outboundOnewayRoads.size() == 1)) {
					return buildLaneConnections_allOneway(node,
							inboundOnewayRoads, outboundOnewayRoads);
				}

			}

		}

		/* apply normal treatment (not oneway-specific) */

		List<LaneConnection> result = new ArrayList<LaneConnection>();

		for (int i = 0; i < roads.size(); i++) {

			final Road road1 = roads.get(i);
			final Road road2 = roads.get(
					(i+1) % roads.size());

			addLaneConnectionsForRoadPair(result,
					node, road1, road2,
					isJunction, isCrossing);

		}

		return result;

	}

	/**
	 * builds lane connections at a junction of just oneway roads.
	 * Intended to handle motorway merges and splits well.
	 * Inbound and outbound roads must not be mixed,
	 * but build two separate continuous blocks instead.
	 *
	 * @param inboundOnewayRoadsLTR  inbound roads, left to right
	 * @param outboundOnewayRoadsLTR  outbound roads, left to right
	 */
	private static List<LaneConnection> buildLaneConnections_allOneway(
			MapNode node, List<Road> inboundOnewayRoadsLTR,
			List<Road> outboundOnewayRoadsLTR) {

		List<Lane> inboundLanes = new ArrayList<Lane>();
		List<Lane> outboundLanes = new ArrayList<Lane>();

		for (Road road : inboundOnewayRoadsLTR) {
			inboundLanes.addAll(road.getLaneLayout().getLanesLeftToRight());
		}
		for (Road road : outboundOnewayRoadsLTR) {
			outboundLanes.addAll(road.getLaneLayout().getLanesLeftToRight());
		}

		Map<Integer, Integer> matches = findMatchingLanes(inboundLanes,
				outboundLanes, false, false);

		/* build connections */

		List<LaneConnection> result = new ArrayList<RoadModule.LaneConnection>();

		for (int lane1Index : matches.keySet()) {

			final Lane lane1 = inboundLanes.get(lane1Index);
			final Lane lane2 = outboundLanes.get(matches.get(lane1Index));

			result.add(buildLaneConnection(lane1, lane2,
					RoadPart.LEFT, //TODO: road part is not always the same
					false, true));

		}

		return result;

	}

	/**
	 * determines connected lanes at a junction, crossing or connector
	 * for a pair of two of the junction's roads.
	 * Only connections between the left part of road1 with the right part of
	 * road2 will be taken into account.
	 */
	private static void addLaneConnectionsForRoadPair(
			List<LaneConnection> result,
			MapNode node, Road road1, Road road2,
			boolean isJunction, boolean isCrossing) {

		/* get some basic info about the roads */

		final boolean isRoad1Inbound = road1.segment.getEndNode() == node;
		final boolean isRoad2Inbound = road2.segment.getEndNode() == node;

		final List<Lane> lanes1, lanes2;

		lanes1 = road1.getLaneLayout().getLanes(
				isRoad1Inbound ? RoadPart.LEFT : RoadPart.RIGHT);

		lanes2 = road2.getLaneLayout().getLanes(
				isRoad2Inbound ? RoadPart.RIGHT : RoadPart.LEFT);

		/* determine which lanes are connected */

		Map<Integer, Integer> matches =
				findMatchingLanes(lanes1, lanes2, isJunction, isCrossing);

		/* build the lane connections */

		for (int lane1Index : matches.keySet()) {

			final Lane lane1 = lanes1.get(lane1Index);
			final Lane lane2 = lanes2.get(matches.get(lane1Index));

			result.add(buildLaneConnection(lane1, lane2, RoadPart.LEFT,
					!isRoad1Inbound, !isRoad2Inbound));

		}

		//TODO: connect "disappearing" lanes to a point on the other side
		//      or draw caps (only for connectors)

	}

	private static LaneConnection buildLaneConnection(
			Lane lane1, Lane lane2, RoadPart roadPart,
			boolean atLane1Start, boolean atLane2Start) {

		List<VectorXYZ> leftLaneBorder = new ArrayList<VectorXYZ>();
		leftLaneBorder.add(lane1.getBorderNode(
				atLane1Start, atLane1Start));
		leftLaneBorder.add(lane2.getBorderNode(
				atLane2Start, !atLane2Start));

		List<VectorXYZ> rightLaneBorder = new ArrayList<VectorXYZ>();
		rightLaneBorder.add(lane1.getBorderNode(
				atLane1Start, !atLane1Start));
		rightLaneBorder.add(lane2.getBorderNode(
				atLane2Start, atLane2Start));

		return new LaneConnection(lane1.type, RoadPart.LEFT,
				lane1.road.rightHandTraffic,
				leftLaneBorder, rightLaneBorder);

	}

	/**
	 * representation for junctions between roads.
	 */
	public static class RoadJunction extends JunctionNodeWorldObject<Road> implements TerrainBoundaryWorldObject {

		public RoadJunction(MapNode node) {
			super(node, Road.class);
		}

		@Override
		public void renderTo(Target target) {

			Material material = getSurfaceForNode(node);
			Collection<TriangleXYZ> triangles = super.getTriangulation();

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

			/* connect some lanes such as sidewalks between adjacent roads */

			List<LaneConnection> connections = buildLaneConnections(
					node, true, false);

			for (LaneConnection connection : connections) {
				connection.renderTo(target);
			}

		}

	}

	/* TODO: crossings at junctions - when there is, e.g., a footway connecting to the road!
	 * (ideally, this would be implemented using more flexibly configurable
	 * junctions which can have "preferred" segments that influence
	 * the junction shape more/exclusively)
	 */

	/**
	 * visible connectors where a road changes width or lane layout
	 */
	public static class RoadConnector
		extends VisibleConnectorNodeWorldObject<Road>
		implements TerrainBoundaryWorldObject {

		private static final double MAX_CONNECTOR_LENGTH = 5;

		public RoadConnector(MapNode node) {
			super(node, Road.class);
		}

		@Override
		public float getLength() {

			// length is at most a third of the shorter road segment's length

			List<Road> roads = getConnectedRoads(node, false);

			return (float)Math.min(Math.min(
					roads.get(0).segment.getLineSegment().getLength() / 3,
					roads.get(1).segment.getLineSegment().getLength() / 3),
					MAX_CONNECTOR_LENGTH);

		}

		@Override
		public void renderTo(Target target) {

			List<LaneConnection> connections = buildLaneConnections(
					node, false, false);

			/* render connections */

			for (LaneConnection connection : connections) {
				connection.renderTo(target);
			}

			/* render area not covered by connections */

			//TODO: subtract area covered by connections

			Material material = getSurfaceForNode(node);

			Collection<TriangleXYZ> trianglesXYZ = getTriangulation();

			target.drawTriangles(material, trianglesXYZ,
					triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

		}

	}

	/**
	 * representation for crossings (zebra crossing etc.) on roads
	 */
	public static class RoadCrossingAtConnector
		extends VisibleConnectorNodeWorldObject<Road>
		implements TerrainBoundaryWorldObject {

		private static final float CROSSING_WIDTH = 3f;

		public RoadCrossingAtConnector(MapNode node) {
			super(node, Road.class);
		}

		@Override
		public float getLength() {
			return parseWidth(node.getTags(), CROSSING_WIDTH);
		}

		@Override
		public void renderTo(Target target) {

			VectorXYZ startLeft = getEleConnectors().getPosXYZ(
					startPos.subtract(cutVector.mult(0.5 * startWidth)));
			VectorXYZ startRight = getEleConnectors().getPosXYZ(
					startPos.add(cutVector.mult(0.5 * startWidth)));

			VectorXYZ endLeft = getEleConnectors().getPosXYZ(
					endPos.subtract(cutVector.mult(0.5 * endWidth)));
			VectorXYZ endRight = getEleConnectors().getPosXYZ(
					endPos.add(cutVector.mult(0.5 * endWidth)));

			/* determine surface material */

			Material surface = getSurfaceForNode(node);

			if (node.getTags().contains("crossing", "zebra")
					|| node.getTags().contains("crossing_ref", "zebra")) {

				surface = surface.withAddedLayers(
						ROAD_MARKING_ZEBRA.getTextureDataList());

			} else if (!node.getTags().contains("crossing", "unmarked")) {

				surface = surface.withAddedLayers(
						ROAD_MARKING_CROSSING.getTextureDataList());

			}

			/* draw crossing */

			List<VectorXYZ> vs = asList(endLeft, startLeft, endRight, startRight);

			target.drawTriangleStrip(surface, vs,
					texCoordLists(vs, surface, GLOBAL_X_Z));

			/* draw lane connections */

			List<LaneConnection> connections = buildLaneConnections(
					node, false, true);

			for (LaneConnection connection : connections) {
				connection.renderTo(target);
			}

		}

	}

	/** representation of a road */
	public static class Road extends AbstractNetworkWaySegmentWorldObject implements TerrainBoundaryWorldObject {

		protected static final float DEFAULT_LANE_WIDTH = 3.5f;

		protected static final float DEFAULT_ROAD_CLEARING = 5;
		protected static final float DEFAULT_PATH_CLEARING = 2;

		protected static final ShapeXZ HANDRAIL_SHAPE = new PolylineXZ(
			new VectorXZ(+0.02, -0.05), new VectorXZ(+0.02,     0),
			new VectorXZ(-0.02,     0), new VectorXZ(-0.02, -0.05));

		public final boolean rightHandTraffic;

		public final LaneLayout laneLayout;
		public final float width;

		final private TagSet tags;
		final public VectorXZ startCoord, endCoord;

		final private boolean steps;

		public Road(MapWaySegment line, TagSet tags) {

			super(line);

			this.tags = tags;
			this.startCoord = line.getStartNode().getPos();
			this.endCoord = line.getEndNode().getPos();

			if (RIGHT_HAND_TRAFFIC_BY_DEFAULT) {
				if (tags.contains("driving_side", "left")) {
					rightHandTraffic = false;
				} else {
					rightHandTraffic = true;
				}
			} else {
				if (tags.contains("driving_side", "right")) {
					rightHandTraffic = true;
				} else {
					rightHandTraffic = false;
				}
			}

			this.steps = isSteps(tags);

			if (steps) {
				this.laneLayout = null;
				this.width = parseWidth(tags, 1.0f);

				createAttchmentConnectors();
			} else {
				this.laneLayout = buildBasicLaneLayout();
				this.width = calculateWidth();
				laneLayout.setCalculatedValues(width);
			}

		}

		/**
		 * creates a lane layout from several basic tags.
		 */
		private LaneLayout buildBasicLaneLayout() {

			boolean isOneway = isOneway(tags);

			/* determine which special lanes and attributes exist */

			String divider = tags.getValue("divider");

			boolean leftSidewalk = tags.contains("sidewalk", "left")
					|| tags.contains("sidewalk", "both");
			boolean rightSidewalk = tags.contains("sidewalk", "right")
					|| tags.contains("sidewalk", "both");

			boolean leftCycleway = tags.contains("cycleway:left", "lane")
					|| tags.contains("cycleway", "lane");
			boolean rightCycleway = tags.contains("cycleway:right", "lane")
					|| tags.contains("cycleway", "lane");

			boolean leftBusBay = tags.contains("bus_bay", "left")
					|| tags.contains("bus_bay", "both");
			boolean rightBusBay = tags.contains("bus_bay", "right")
					|| tags.contains("bus_bay", "both");

			/* get individual values for each lane */

			TagSet[] laneTagsRight = getPerLaneTags(RoadPart.RIGHT);
			TagSet[] laneTagsLeft = getPerLaneTags(RoadPart.LEFT);

			/* determine the number of lanes */

			Float lanes = null;

			if (tags.containsKey("lanes")) {
				lanes = parseOsmDecimal(tags.getValue("lanes"), false);
			}

			Float lanesRight = null;
			Float lanesLeft = null;

			//TODO handle oneway case

			String rightKey = rightHandTraffic ? "lanes:forward" : "lanes:backward";

			if (laneTagsRight != null) {
				lanesRight = (float)laneTagsRight.length;
			} else if (tags.containsKey(rightKey)) {
				lanesRight = parseOsmDecimal(tags.getValue(rightKey), false);
			}

			String leftKey = rightHandTraffic ? "lanes:backward" : "lanes:forward";

			if (laneTagsLeft != null) {
				lanesLeft = (float)laneTagsLeft.length;
			} else if (tags.containsKey(leftKey)) {
				lanesLeft = parseOsmDecimal(tags.getValue(leftKey), false);
			}

			int vehicleLaneCount;
			int vehicleLaneCountRight;
			int vehicleLaneCountLeft;

			if (lanesRight != null && lanesLeft != null) {

				vehicleLaneCountRight = (int)(float)lanesRight;
				vehicleLaneCountLeft = (int)(float)lanesLeft;

				vehicleLaneCount = vehicleLaneCountRight + vehicleLaneCountLeft;

				//TODO incorrect in case of center lanes

			} else {

				if (lanes == null) {
					vehicleLaneCount = getDefaultLanes(tags);
				} else {
					vehicleLaneCount = (int)(float) lanes;
				}

				if (lanesRight != null) {

					vehicleLaneCountRight = (int)(float)lanesRight;
					vehicleLaneCount = max(vehicleLaneCount, vehicleLaneCountRight);
					vehicleLaneCountLeft = vehicleLaneCount - vehicleLaneCountRight;

				} else if (lanesLeft != null) {

					vehicleLaneCountLeft = (int)(float)lanesLeft;
					vehicleLaneCount = max(vehicleLaneCount, vehicleLaneCountLeft);
					vehicleLaneCountRight = vehicleLaneCount - vehicleLaneCountLeft;

				} else {

					vehicleLaneCountLeft = vehicleLaneCount / 2;
					vehicleLaneCountRight = vehicleLaneCount - vehicleLaneCountLeft;

				}

			}

			/* create the layout */

			LaneLayout layout = new LaneLayout();

			// central divider

			if (vehicleLaneCountRight > 0 && vehicleLaneCountLeft > 0) {

				LaneType dividerType = DASHED_LINE;

				if ("dashed_line".equals(divider)) {
					dividerType = DASHED_LINE;
				} else if ("solid_line".equals(divider)) {
					dividerType = SOLID_LINE;
				} else if ("no".equals(divider)) {
					dividerType = null;
				} else {

					//no explicit divider tagging, try to infer from overtaking rules

					boolean overtakingForward = tags.contains("overtaking:forward", "yes")
							|| !tags.contains("overtaking:forward", "no")
							&& !tags.contains("overtaking", "backward")
							&& !tags.contains("overtaking", "no");
					boolean overtakingBackward = tags.contains("overtaking:backward", "yes")
							|| !tags.contains("overtaking:backward", "no")
							&& !tags.contains("overtaking", "forward")
							&& !tags.contains("overtaking", "no");

					if (!overtakingForward && !overtakingBackward) {
						dividerType = SOLID_LINE;
					} //TODO else if ... for combined solid and dashed lines

				}

				if (dividerType != null) {
					layout.getLanes(RoadPart.RIGHT).add(new Lane(this,
							dividerType, RoadPart.RIGHT, TagSet.of()));
				}

			}

			// left and right road part

			for (RoadPart roadPart : RoadPart.values()) {

				int lanesPart = (roadPart == RoadPart.RIGHT)
						? vehicleLaneCountRight
						: vehicleLaneCountLeft;

				TagSet[] laneTags = (roadPart == RoadPart.RIGHT)
						? laneTagsRight
						: laneTagsLeft;

				for (int i = 0; i < lanesPart; ++ i) {

					if (i > 0) {

						// divider between lanes in the same direction

						layout.getLanes(roadPart).add(new Lane(this,
								DASHED_LINE, roadPart, TagSet.of()));

					}


					//lane itself

					TagSet tags = (laneTags != null)
							? laneTags[i]
							: TagSet.of();

					layout.getLanes(roadPart).add(new Lane(this,
							VEHICLE_LANE, roadPart, tags));

				}

			}

			//special lanes

			//TODO: reduce code duplication by iterating over special lane types

			if (leftCycleway) {
				layout.leftLanes.add(new Lane(this, CYCLEWAY, RoadPart.LEFT, inheritTags(
						getTagsWithPrefix(tags, "cycleway:left:", null),
						getTagsWithPrefix(tags, "cycleway:both:", null))));
			}
			if (rightCycleway) {
				layout.rightLanes.add(new Lane(this, CYCLEWAY, RoadPart.RIGHT, inheritTags(
						getTagsWithPrefix(tags, "cycleway:right:", null),
						getTagsWithPrefix(tags, "cycleway:both:", null))));
			}

			if (leftBusBay) {
				layout.leftLanes.add(new Lane(this, DASHED_LINE, RoadPart.LEFT, TagSet.of()));
				layout.leftLanes.add(new Lane(this, BUS_BAY, RoadPart.LEFT, inheritTags(
						getTagsWithPrefix(tags, "bus_bay:left:", null),
						getTagsWithPrefix(tags, "bus_bay:both:", null))));
			}
			if (rightBusBay) {
				layout.rightLanes.add(new Lane(this, DASHED_LINE, RoadPart.RIGHT, TagSet.of()));
				layout.rightLanes.add(new Lane(this, BUS_BAY, RoadPart.RIGHT, inheritTags(
						getTagsWithPrefix(tags, "bus_bay:right:", null),
						getTagsWithPrefix(tags, "bus_bay:both:", null))));
			}

			if (leftSidewalk) {

				TagSet kerbTags = inheritTags(
						getTagsWithPrefix(tags, "sidewalk:left:kerb", "kerb"),
						getTagsWithPrefix(tags, "sidewalk:both:kerb", "kerb"));

				if (!kerbTags.contains("kerb", "no")) {
					layout.leftLanes.add(new Lane(this, KERB, RoadPart.LEFT, kerbTags));
				}

				layout.leftLanes.add(new Lane(this,SIDEWALK, RoadPart.LEFT, inheritTags(
						getTagsWithPrefix(tags, "sidewalk:left:", null),
						getTagsWithPrefix(tags, "sidewalk:both:", null))));

			}
			if (rightSidewalk) {

				TagSet kerbTags = inheritTags(
						getTagsWithPrefix(tags, "sidewalk:left:kerb", "kerb"),
						getTagsWithPrefix(tags, "sidewalk:right:kerb", "kerb"));

				if (!kerbTags.contains("kerb", "no")) {
					layout.rightLanes.add(new Lane(this, KERB, RoadPart.RIGHT, kerbTags));
				}

				layout.rightLanes.add(new Lane(this, SIDEWALK, RoadPart.RIGHT, inheritTags(
						getTagsWithPrefix(tags, "sidewalk:right:", null),
						getTagsWithPrefix(tags, "sidewalk:both:", null))));

			}

			return layout;

		}

		/**
		 * evaluates tags using the :lanes key suffix
		 *
		 * @return  array with values; null if the tag isn't used
		 */
		@SuppressWarnings("unchecked")
		private TagSet[] getPerLaneTags(RoadPart roadPart) {

			/* determine which of the suffixes :lanes[:forward|:backward] matter */

			List<String> relevantSuffixes;

			if (roadPart == RoadPart.RIGHT ^ !rightHandTraffic) {
				// the forward part

				if (isOneway(tags)) {
					relevantSuffixes = asList(":lanes", ":lanes:forward");
				} else {
					relevantSuffixes = asList(":lanes:forward");
				}

			} else {
				// the backward part

				relevantSuffixes = asList(":lanes:backward");

			}

			/* evaluate tags with one of the relevant suffixes */

			List<Tag>[] resultLists = null;

			for (String suffix : relevantSuffixes) {

				for (Tag tag : tags) {
					if (tag.key.endsWith(suffix)) {

						String baseKey = tag.key.substring(0,
								tag.key.lastIndexOf(suffix));

						String[] values = tag.value.split("\\|");

						if (resultLists == null) {

							resultLists = new List[values.length];

							for (int i = 0; i < resultLists.length; i++) {
								resultLists[i] = new ArrayList<>();
							}

						} else if (values.length != resultLists.length) {

							// inconsistent number of lanes
							return null;

						}

						for (int i = 0; i < values.length; i++) {
							if (!resultLists[i].stream().anyMatch(t -> t.key.equals(baseKey))) {
								resultLists[i].add(new Tag(baseKey, values[i].trim()));
							}
						}

					}
				}

			}

			/* build a TagGroup for each lane from the result */

			if (resultLists == null) {
				return null;
			} else {
				return stream(resultLists).map(TagSet::of).toArray(TagSet[]::new);
			}

		}

		/**
		 * returns all tags from a TagGroup that have a given prefix for their key.
		 * Can be used to identify tags for special lanes. Using the prefix "sidewalk:left",
		 * for example, the tag sidewalk:left:width = 2 m will be part of the result as width = 2 m.
		 *
		 * @param prefix  prefix that tags need to have in order to be part of the result.
		 *   Stripped from the resulting tags.
		 * @param newPrefix  prefix that is added to the resulting tags, after lanePrefix has been removed.
		 *   Can be (and often is) null.
		 */
		static TagSet getTagsWithPrefix(TagSet tags, String prefix, String newPrefix) {

			List<Tag> result = new ArrayList<Tag>();

			for (Tag tag : tags) {

				if (tag.key.startsWith(prefix)) {

					String newKey = tag.key.substring(prefix.length());

					if (newPrefix != null) {
						newKey = newPrefix + newKey;
					}

					result.add(new Tag(newKey, tag.value));

				}

			}

			return TagSet.of(result);

		}

		private float calculateWidth() {

			// if the width of all lanes is known, use the sum as the road's width

			Float sumWidth = calculateLaneBasedWidth(false, false);

			if (sumWidth != null) return sumWidth;

			// if the width of the road is explicitly tagged, use that value
			// (note that this has lower priority than the sum of lane widths,
			// to avoid errors when the two values don't match)

			float explicitWidth = parseWidth(tags, -1);

			if (explicitWidth != -1) return explicitWidth;

			// if there is some basic info on lanes, use that

			if (asList("lanes", "lanes:forward", "lanes:backward", "divider").stream().anyMatch(tags::containsKey)) {

				return calculateLaneBasedWidth(true, false);

			}

			// if all else fails, make a guess

			return calculateLaneBasedWidth(true, true)
					+ estimateVehicleLanesWidth();

		}

		/**
		 * calculates the width of the road as the sum of the widths
		 * of its lanes
		 *
		 * @param useDefaults  whether to use a default for unknown widths
		 * @param ignoreVehicleLanes  ignoring full-width lanes,
		 * 	which means that only sidewalks, cycleways etc. will be counted
		 *
		 * @return  the estimated width, or null if a lane has unknown width
		 * 	and no defaults are permitted
		 */
		private Float calculateLaneBasedWidth(boolean useDefaults,
				boolean ignoreVehicleLanes) {

			float width = 0;

			for (Lane lane : laneLayout.getLanesLeftToRight()) {

				if (lane.type == VEHICLE_LANE && ignoreVehicleLanes) continue;

				if (lane.getAbsoluteWidth() == null) {
					if (useDefaults) {
						width += DEFAULT_LANE_WIDTH;
					} else {
						return null;
					}
				} else {
					width += lane.getAbsoluteWidth();
				}

			}

			return width;

		}

		/**
		 * calculates a rough estimate of the road's vehicle lanes' total width
		 * based on road type and oneway
		 */
		private float estimateVehicleLanesWidth() {

			String highwayValue = tags.getValue("highway");

			float width = 0;

			/* guess the combined width of all vehicle lanes */

			if (!tags.containsKey("lanes") && !tags.containsKey("divider")) {

				if (isPath(tags)) {
					width = 1f;
				}

				else if ("service".equals(highwayValue)
						|| "track".equals(highwayValue)) {
					if (tags.contains("service", "parking_aisle")) {
						width = DEFAULT_LANE_WIDTH * 0.8f;
					} else {
						width = DEFAULT_LANE_WIDTH;
					}
				} else if ("primary".equals(highwayValue) || "secondary".equals(highwayValue)) {
					width = 2 * DEFAULT_LANE_WIDTH;
				} else if ("motorway".equals(highwayValue)) {
					width = 2.5f * DEFAULT_LANE_WIDTH;
				}

				else if (tags.containsKey("oneway") && !tags.getValue("oneway").equals("no")) {
					width = DEFAULT_LANE_WIDTH;
				}

				else {
					width = 4;
				}

			}

			return width;

		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {

			super.defineEleConstraints(enforcer);

			/* impose sensible maximum incline (35% is "the world's steepest residential street") */

			if (!isPath(tags) && !isSteps(tags) && !tags.containsKey("incline")) {
				enforcer.requireIncline(MAX, +0.35, getCenterlineEleConnectors());
				enforcer.requireIncline(MIN, -0.35, getCenterlineEleConnectors());
			}

		}

		@Override
		public float getWidth() {
			return width;
		}

		public Material getSurface() {
			return getSurfaceForRoad(tags, ASPHALT);
		}

		public LaneLayout getLaneLayout() {
			return laneLayout;
		}

		private void renderStepsTo(Target target) {

			final VectorXZ startWithOffset = getStartPosition();
			final VectorXZ endWithOffset = getEndPosition();

			List<VectorXYZ> leftOutline = getOutline(false);
			List<VectorXYZ> rightOutline = getOutline(true);


			double lineLength = VectorXZ.distance (
					segment.getStartNode().getPos(), segment.getEndNode().getPos());

			/* evaluate material and color */

			Material material = null;

			if (tags.containsKey("material")) {
				material = Materials.getMaterial(tags.getValue("material"));
			}

			if (material == null && tags.containsKey("surface")) {
				material = Materials.getSurfaceMaterial(tags.getValue("surface"));
			}

			if (material == null) {
				material = CONCRETE;
			}

			material = material.withColor(parseColor(tags.getValue("colour"), CSS_COLORS));

			/* render ground first (so gaps between the steps look better) */

			List<VectorXYZ> vs = createTriangleStripBetween(
					leftOutline, rightOutline);

			target.drawTriangleStrip(ASPHALT, vs,
					texCoordLists(vs, ASPHALT, GLOBAL_X_Z));

			/* determine the length of each individual step */

			float stepLength = 0.3f;

			if (tags.containsKey("step_count")) {
				try {
					int stepCount = Integer.parseInt(tags.getValue("step_count"));
					stepLength = (float)lineLength / stepCount;
				} catch (NumberFormatException e) { /* don't overwrite default length */ }
			}

			/* locate the position on the line at the beginning/end of each step
			 * (positions on the line spaced by step length),
			 * interpolate heights between adjacent points with elevation */

			List<VectorXYZ> centerline = getCenterline();

			List<VectorXZ> stepBorderPositionsXZ =
				GeometryUtil.equallyDistributePointsAlong(
					stepLength, true, startWithOffset, endWithOffset);

			List<VectorXYZ> stepBorderPositions = new ArrayList<VectorXYZ>();
			for (VectorXZ posXZ : stepBorderPositionsXZ) {
				VectorXYZ posXYZ = interpolateElevation(posXZ,
						centerline.get(0),
						centerline.get(centerline.size() - 1));
				stepBorderPositions.add(posXYZ);
			}

			/* draw steps */

			for (int step = 0; step < stepBorderPositions.size() - 1; step++) {

				VectorXYZ frontCenter = stepBorderPositions.get(step);
				VectorXYZ backCenter = stepBorderPositions.get(step+1);

				VectorXYZ frontLeft = frontCenter.subtract(segment.getRightNormal().mult(0.5 * width));
				VectorXYZ frontRight = frontCenter.add(segment.getRightNormal().mult(0.5 * width));

				VectorXYZ backLeft = backCenter.subtract(segment.getRightNormal().mult(0.5 * width));
				VectorXYZ backRight = backCenter.add(segment.getRightNormal().mult(0.5 * width));

				boolean frontIsLower = frontCenter.y < backCenter.y;

				if (abs(frontCenter.y - backCenter.y) < 0.01 && tags.containsKey("incline")) {
					if (tags.contains("incline", "up")
							|| parseIncline(tags.getValue("incline")) != null && parseIncline(tags.getValue("incline")) > 0) {
						frontIsLower = true;
					} else if (tags.contains("incline", "down")
							|| parseIncline(tags.getValue("incline")) != null && parseIncline(tags.getValue("incline")) < 0) {
						frontIsLower = false;
					}
				}

				VectorXYZ edgeLeft, edgeRight;

				if (frontIsLower) {
					double edgeY = max(backCenter.y, frontCenter.y + 0.1);
					edgeLeft = frontLeft.y(edgeY);
					edgeRight = frontRight.y(edgeY);
				} else {
					double edgeY = max(frontCenter.y, backCenter.y + 0.1);
					edgeLeft = backLeft.y(edgeY);
					edgeRight = backRight.y(edgeY);
				}

				List<VectorXYZ> topStrip = asList(frontLeft, frontRight, edgeLeft, edgeRight, backLeft, backRight);
				target.drawTriangleStrip(material, topStrip,
						texCoordLists(topStrip, material, STRIP_WALL));

				List<TriangleXYZ> sideTriangles = asList(
						new TriangleXYZ(frontLeft, edgeLeft, backLeft),
						new TriangleXYZ(backRight, edgeRight, frontRight));

				target.drawTriangles(material, sideTriangles,
						triangleTexCoordLists(sideTriangles, material, SLOPED_TRIANGLES));

			}

			/* draw handrails */

			List<List<VectorXYZ>> handrailFootprints =
				new ArrayList<List<VectorXYZ>>();

			if (segment.getTags().contains("handrail:left","yes")) {
				handrailFootprints.add(leftOutline);
			}
			if (segment.getTags().contains("handrail:right","yes")) {
				handrailFootprints.add(rightOutline);
			}

			int centerHandrails = 0;
			if (segment.getTags().contains("handrail:center","yes")) {
				centerHandrails = 1;
			} else if (segment.getTags().containsKey("handrail:center")) {
				try {
					centerHandrails = Integer.parseInt(
							segment.getTags().getValue("handrail:center"));
				} catch (NumberFormatException e) {}
			}


			for (int i = 0; i < centerHandrails; i++) {
				handrailFootprints.add(createLineBetween(
						leftOutline, rightOutline,
						(i + 1.0f) / (centerHandrails + 1)));
			}

			for (List<VectorXYZ> handrailFootprint : handrailFootprints) {

				List<VectorXYZ> handrailLine = new ArrayList<VectorXYZ>();
				for (VectorXYZ v : handrailFootprint) {
					handrailLine.add(v.y(v.y + 1));
				}

				target.drawExtrudedShape(HANDRAIL_DEFAULT, HANDRAIL_SHAPE, handrailLine,
						nCopies(handrailLine.size(), Y_UNIT), null, null, null);

				target.drawColumn(HANDRAIL_DEFAULT, 4,
						handrailFootprint.get(0),
						1, 0.03, 0.03, false, true);
				target.drawColumn(HANDRAIL_DEFAULT, 4,
						handrailFootprint.get(handrailFootprint.size()-1),
						1, 0.03, 0.03, false, true);

			}

		}

		private void renderLanesTo(Target target) {

			List<Lane> lanesLeftToRight = laneLayout.getLanesLeftToRight();

			/* draw lanes themselves */

			for (Lane lane : lanesLeftToRight) {
				lane.renderTo(target);
			}

			/* close height gaps at left and right border of the road */

			Lane firstLane = lanesLeftToRight.get(0);
			Lane lastLane = lanesLeftToRight.get(lanesLeftToRight.size() - 1);

			if (firstLane.getHeightAboveRoad() > 0) {

				List<VectorXYZ> vs = createTriangleStripBetween(
						getOutline(false),
						addYList(getOutline(false), firstLane.getHeightAboveRoad()));

				target.drawTriangleStrip(getSurface(), vs,
						texCoordLists(vs, getSurface(), STRIP_WALL));

			}

			if (lastLane.getHeightAboveRoad() > 0) {

				List<VectorXYZ> vs = createTriangleStripBetween(
						addYList(getOutline(true), lastLane.getHeightAboveRoad()),
						getOutline(true));

				target.drawTriangleStrip(getSurface(), vs,
						texCoordLists(vs, getSurface(), STRIP_WALL));

			}

		}

		@Override
		public void renderTo(Target target) {

			if (steps) {
				renderStepsTo(target);
			} else {
				renderLanesTo(target);
			}

		}

	}

	public static class RoadArea extends NetworkAreaWorldObject implements TerrainBoundaryWorldObject {

		public RoadArea(MapArea area) {
			super(area);
		}

		@Override
		public void renderTo(Target target) {

			String surface = area.getTags().getValue("surface");
			Material material = getSurfaceMaterial(surface, ASPHALT);
			Collection<TriangleXYZ> triangles = getTriangulation();

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

		}

		@Override
		public GroundState getGroundState() {
			if (BridgeModule.isBridge(area.getTags())) {
				return GroundState.ABOVE;
			} else if (TunnelModule.isTunnel(area.getTags())) {
				return GroundState.BELOW;
			} else {
				return GroundState.ON;
			}
		}

	}

	static enum RoadPart {
		LEFT, RIGHT
		//TODO add CENTRE lane support
	}

	static class LaneLayout {

		public final List<Lane> leftLanes = new ArrayList<Lane>();
		public final List<Lane> rightLanes = new ArrayList<Lane>();

		public List<Lane> getLanes(RoadPart roadPart) {
			switch (roadPart) {
			case LEFT: return leftLanes;
			case RIGHT: return rightLanes;
			default: throw new Error("unhandled road part value");
			}
		}

		public List<Lane> getLanesLeftToRight() {
			List<Lane> result = new ArrayList<Lane>();
			result.addAll(leftLanes);
			Collections.reverse(result);
			result.addAll(rightLanes);
			return result;
		}

		/**
		 * calculates and sets all lane attributes
		 * that are not known during lane creation
		 */
		public void setCalculatedValues(double totalRoadWidth) {

			/* determine width of lanes without explicitly assigned width */

			int lanesWithImplicitWidth = 0;
			double remainingWidth = totalRoadWidth;

			for (RoadPart part : RoadPart.values()) {
				for (Lane lane : getLanes(part)) {
					if (lane.getAbsoluteWidth() == null) {
						lanesWithImplicitWidth += 1;
					} else {
						remainingWidth -= lane.getAbsoluteWidth();
					}
				}
			}

			double implicitLaneWidth = remainingWidth / lanesWithImplicitWidth;

			/* calculate a factor to reduce all lanes' width
			 * if the sum of their widths would otherwise
			 * be larger than that of the road */

			double laneWidthScaling = 1.0;

			if (remainingWidth < 0) {

				double widthSum = totalRoadWidth - remainingWidth;

				implicitLaneWidth = 1;
				widthSum += lanesWithImplicitWidth * implicitLaneWidth;

				laneWidthScaling = totalRoadWidth / widthSum;

			}

			/* assign values */

			for (RoadPart part : asList(RoadPart.LEFT, RoadPart.RIGHT)) {

				double heightAboveRoad = 0;

				for (Lane lane : getLanes(part)) {

					double relativeWidth;

					if (lane.getAbsoluteWidth() == null) {
						relativeWidth = laneWidthScaling *
								(implicitLaneWidth / totalRoadWidth);
					} else {
						relativeWidth = laneWidthScaling *
								(lane.getAbsoluteWidth() / totalRoadWidth);
					}

					lane.setCalculatedValues1(relativeWidth, heightAboveRoad);

					heightAboveRoad += lane.getHeightOffset();

				}

			}

			/* calculate relative lane positions based on relative width */

			double accumulatedWidth = 0;

			for (Lane lane : getLanesLeftToRight()) {

				double relativePositionLeft = accumulatedWidth;

				accumulatedWidth += lane.getRelativeWidth();

				double relativePositionRight = accumulatedWidth;

				if (relativePositionRight > 1) { //avoids precision problems
					relativePositionRight = 1;
				}

				lane.setCalculatedValues2(relativePositionLeft,
						relativePositionRight);

			}

		}

		/**
		 * calculates and sets all lane attributes
		 * that are not known during lane creation
		 */


	}

	/**
	 * a lane or lane divider of the road segment.
	 *
	 * Field values depend on neighboring lanes and are therefore calculated
	 * and defined in two phases. Results are then set using
	 * {@link #setCalculatedValues1(double, double)} and
	 * {@link #setCalculatedValues2(double, double)}, respectively.
	 */
	static final class Lane implements Renderable {

		public final Road road;
		public final LaneType type;
		public final RoadPart roadPart;
		public final TagSet laneTags;

		private int phase = 0;

		private double relativeWidth;
		private double heightAboveRoad;

		private double relativePositionLeft;
		private double relativePositionRight;

		public Lane(Road road, LaneType type, RoadPart roadPart,
				TagSet laneTags) {
			this.road = road;
			this.type = type;
			this.roadPart = roadPart;
			this.laneTags = laneTags;
		}

		/** returns width in meters or null for undefined width */
		public Double getAbsoluteWidth() {
			return type.getAbsoluteWidth(road.tags, laneTags);
		}

		/** returns height increase relative to inner neighbor */
		public double getHeightOffset() {
			return type.getHeightOffset(road.tags, laneTags);
		}

		public void setCalculatedValues1(double relativeWidth,
				double heightAboveRoad) {

			assert phase == 0;

			this.relativeWidth = relativeWidth;
			this.heightAboveRoad = heightAboveRoad;

			phase = 1;

		}

		public void setCalculatedValues2(double relativePositionLeft,
				double relativePositionRight) {

			assert phase == 1;

			this.relativePositionLeft = relativePositionLeft;
			this.relativePositionRight = relativePositionRight;

			phase = 2;

		}

		public Double getRelativeWidth() {
			assert phase > 0;
			return relativeWidth;
		}

		public double getHeightAboveRoad() {
			assert phase > 0;
			return heightAboveRoad;
		}

		/**
		 * provides access to the first and last node
		 * of the lane's left and right border
		 */
		public VectorXYZ getBorderNode(boolean start, boolean right) {

			assert phase > 1;

			double relativePosition = right
					? relativePositionRight
					: relativePositionLeft;

			if (relativePosition < 0 || relativePosition > 1) {
				System.out.println("PROBLEM");
			}

			VectorXYZ roadPoint = road.getPointOnCut(start, relativePosition);

			return roadPoint.add(0, getHeightAboveRoad(), 0);

		}

		public void renderTo(Target target) {

			assert phase > 1;

			if (road.isBroken()) return;

			List<VectorXYZ> leftLaneBorder = createLineBetween(
					road.getOutline(false), road.getOutline(true),
					(float)relativePositionLeft);
			leftLaneBorder = addYList(leftLaneBorder, getHeightAboveRoad());

			List<VectorXYZ> rightLaneBorder = createLineBetween(
					road.getOutline(false), road.getOutline(true),
					(float)relativePositionRight);
			rightLaneBorder = addYList(rightLaneBorder, getHeightAboveRoad());

			type.render(target, roadPart, road.rightHandTraffic,
					road.tags, laneTags, leftLaneBorder, rightLaneBorder);

		}

		@Override
		public String toString() {
			return "{" + type + ", " + roadPart + "}";
		}

	}

	/**
	 * a connection between two lanes (e.g. at a junction)
	 */
	static class LaneConnection implements Renderable {

		public final LaneType type;
		public final RoadPart roadPart;
		public final boolean rightHandTraffic;

		private final List<VectorXYZ> leftBorder;
		private final List<VectorXYZ> rightBorder;

		private LaneConnection(LaneType type, RoadPart roadPart,
				boolean rightHandTraffic,
				List<VectorXYZ> leftBorder, List<VectorXYZ> rightBorder) {
			this.type = type;
			this.roadPart = roadPart;
			this.rightHandTraffic = rightHandTraffic;
			this.leftBorder = leftBorder;
			this.rightBorder = rightBorder;
		}

		/**
		 * returns the outline of this connection.
		 * For determining the total terrain covered by junctions and connectors.
		 */
		public PolygonXYZ getOutline() {

			List<VectorXYZ> outline = new ArrayList<VectorXYZ>();

			outline.addAll(leftBorder);

			List<VectorXYZ>rOutline = new ArrayList<VectorXYZ>(rightBorder);
			Collections.reverse(rOutline);
			outline.addAll(rOutline);

			outline.add(outline.get(0));

			return new PolygonXYZ(outline);

		}

		public void renderTo(Target target) {

			type.render(target, roadPart, rightHandTraffic,
					TagSet.of(), TagSet.of(), leftBorder, rightBorder);

		}

	}

	/**
	 * a type of lanes. Determines visual appearance,
	 * and contains the intelligence for evaluating type-specific tags.
	 */
	static abstract class LaneType {

		private final String typeName;
		public final boolean isConnectableAtCrossings;
		public final boolean isConnectableAtJunctions;

		private LaneType(String typeName,
				boolean isConnectableAtCrossings,
				boolean isConnectableAtJunctions) {

			this.typeName = typeName;
			this.isConnectableAtCrossings = isConnectableAtCrossings;
			this.isConnectableAtJunctions = isConnectableAtJunctions;

		}

		public abstract void render(Target target, RoadPart roadPart,
				boolean rightHandTraffic,
				TagSet roadTags, TagSet laneTags,
				List<VectorXYZ> leftLaneBorder,
				List<VectorXYZ> rightLaneBorder);

		public abstract Double getAbsoluteWidth(
				TagSet roadTags, TagSet laneTags);

		public abstract double getHeightOffset(
				TagSet roadTags, TagSet laneTags);

		@Override
		public String toString() {
			return typeName;
		}

	}

	static abstract class FlatTexturedLane extends LaneType {

		private FlatTexturedLane(String typeName,
				boolean isConnectableAtCrossings,
				boolean isConnectableAtJunctions) {

			super(typeName, isConnectableAtCrossings, isConnectableAtJunctions);

		}

		@Override
		public void render(Target target, RoadPart roadPart,
				boolean rightHandTraffic,
				TagSet roadTags, TagSet laneTags,
				List<VectorXYZ> leftLaneBorder,
				List<VectorXYZ> rightLaneBorder) {

			Material surface = getSurface(roadTags, laneTags);
			Material surfaceMiddle = getSurfaceMiddle(roadTags, laneTags);

			/* draw lane triangle strips */

			if (surfaceMiddle == null || surfaceMiddle.equals(surface)) {

				List<VectorXYZ> vs = createTriangleStripBetween(
						leftLaneBorder, rightLaneBorder);

				boolean mirrorLeftRight = laneTags.containsKey("turn")
						&& laneTags.getValue("turn").contains("left");


				if (!roadTags.contains("highway", "motorway")) {

					// add turn arrows only if the lane section is long enough (rough rule of thumb)
					double length = leftLaneBorder.get(0).distanceToXZ(leftLaneBorder.get(leftLaneBorder.size() - 1));
					if (length > 4.0) {
						surface = addTurnArrows(surface, laneTags);
					}

				}

				target.drawTriangleStrip(surface, vs,
						texCoordLists(vs, surface, new ArrowTexCoordFunction(
								roadPart, rightHandTraffic, mirrorLeftRight)));

			} else {

				List<VectorXYZ> leftMiddleBorder =
					createLineBetween(leftLaneBorder, rightLaneBorder, 0.3f);
				List<VectorXYZ> rightMiddleBorder =
					createLineBetween(leftLaneBorder, rightLaneBorder, 0.7f);

				List<VectorXYZ> vsLeft = createTriangleStripBetween(
						leftLaneBorder, leftMiddleBorder);
				List<VectorXYZ> vsMiddle = createTriangleStripBetween(
						leftMiddleBorder, rightMiddleBorder);
				List<VectorXYZ> vsRight = createTriangleStripBetween(
						rightMiddleBorder, rightLaneBorder);

				target.drawTriangleStrip(surface, vsLeft,
						texCoordLists(vsLeft, surface, GLOBAL_X_Z));
				target.drawTriangleStrip(surfaceMiddle, vsMiddle,
						texCoordLists(vsMiddle, surfaceMiddle, GLOBAL_X_Z));
				target.drawTriangleStrip(surface, vsRight,
						texCoordLists(vsRight, surface, GLOBAL_X_Z));

			}

		}

		@Override
		public double getHeightOffset(TagSet roadTags, TagSet laneTags) {
			return 0;
		}

		protected Material getSurface(TagSet roadTags, TagSet laneTags) {

			return getSurfaceMaterial(laneTags.getValue("surface"),
					getSurfaceForRoad(roadTags, ASPHALT));

		}

		protected Material getSurfaceMiddle(TagSet roadTags, TagSet laneTags) {

			return getSurfaceMaterial(laneTags.getValue("surface:middle"),
					getSurfaceMiddleForRoad(roadTags, null));

		}

	}

	static final LaneType VEHICLE_LANE = new FlatTexturedLane(
			"VEHICLE_LANE", false, false) {

		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {

			double width = parseWidth(laneTags, -1);

			if (width == -1) {
				return null;
			} else {
				return width;
			}

		}

	};

	static final LaneType BUS_BAY = new FlatTexturedLane(
			"BUS_BAY", false, false) {

		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {

			double width = parseWidth(laneTags, -1);

			if (width == -1) {
				return null;
			} else {
				return width;
			}

		}

	};

	static final LaneType CYCLEWAY = new FlatTexturedLane(
			"CYCLEWAY", false, false) {

		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {
			return (double)parseWidth(laneTags, 0.5f);
		}

		@Override
		protected Material getSurface(TagSet roadTags, TagSet laneTags) {
			Material material = super.getSurface(roadTags, laneTags);
			if (material == ASPHALT) return RED_ROAD_MARKING;
			else return material;
		}

	};

	static final LaneType SIDEWALK = new FlatTexturedLane(
			"SIDEWALK", true, true) {

		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {
			return (double)parseWidth(laneTags, 1.0f);
		}

	};

	static final LaneType SOLID_LINE = new FlatTexturedLane(
			"SOLID_LINE", false, false) {

		@Override
		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {
			return (double)parseWidth(laneTags, 0.1f);
		}

		@Override
		protected Material getSurface(TagSet roadTags, TagSet laneTags) {
			return ROAD_MARKING;
		}

	};

	static final LaneType DASHED_LINE = new FlatTexturedLane(
			"DASHED_LINE", false, false) {

		@Override
		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {
			return (double)parseWidth(laneTags, 0.1f);
		}

		@Override
		protected Material getSurface(TagSet roadTags, TagSet laneTags) {
			return ROAD_MARKING_DASHED;
		}

	};

	static final LaneType KERB = new LaneType(
			"KERB", true, true) {

		@Override
		public void render(Target target, RoadPart roadPart,
				boolean rightHandTraffic, TagSet roadTags, TagSet laneTags,
				List<VectorXYZ> leftLaneBorder,
				List<VectorXYZ> rightLaneBorder) {

			List<VectorXYZ> borderFront0, borderFront1;
			List<VectorXYZ> borderTop0, borderTop1;

			double height = getHeightOffset(roadTags, laneTags);

			if (roadPart == RoadPart.LEFT) {
				borderTop0 = addYList(leftLaneBorder, height);
				borderTop1 = addYList(rightLaneBorder, height);
				borderFront0 = borderTop1;
				borderFront1 = rightLaneBorder;
			} else {
				borderFront0 = leftLaneBorder;
				borderFront1 = addYList(leftLaneBorder, height);
				borderTop0 = borderFront1;
				borderTop1 = addYList(rightLaneBorder, height);
			}

			List<VectorXYZ> vsTop = createTriangleStripBetween(
					borderTop0, borderTop1);
			target.drawTriangleStrip(Materials.KERB, vsTop,
					texCoordLists(vsTop, Materials.KERB, STRIP_FIT_HEIGHT));

			if (height > 0) {
				List<VectorXYZ> vsFront = createTriangleStripBetween(
						borderFront0, borderFront1);
				target.drawTriangleStrip(Materials.KERB, vsFront,
						texCoordLists(vsFront, Materials.KERB, STRIP_FIT_HEIGHT));
			}

		}

		@Override
		public Double getAbsoluteWidth(TagSet roadTags, TagSet laneTags) {
			return (double)parseWidth(laneTags, 0.15f);
		}

		@Override
		public double getHeightOffset(TagSet roadTags, TagSet laneTags) {
			//TODO split dividerTags and laneTags

			String kerb = laneTags.getValue("kerb");

			if ("lowered".equals(kerb) || "rolled".equals(kerb)) {
				return 0.03;
			} else if ("flush".equals(kerb)) {
				return 0;
			} else {
				return 0.12;
			}

		}

	};

	/**
	 * adds a texture layer for turn arrows (if any) to a material
	 *
	 * @return  a material based on the input, possibly with added turn arrows
	 */
	private static Material addTurnArrows(Material material,
			TagSet laneTags) {

		Material arrowMaterial = null;

		/* find the right material  */

		String turn = laneTags.getValue("turn");

		if (turn != null) {

			if (turn.contains("through") && turn.contains("right")) {

				arrowMaterial = ROAD_MARKING_ARROW_THROUGH_RIGHT;

			} else if (turn.contains("through") && turn.contains("left")) {

				arrowMaterial = ROAD_MARKING_ARROW_THROUGH_RIGHT;

			} else if (turn.contains("through")) {

				arrowMaterial = ROAD_MARKING_ARROW_THROUGH;

			} else if (turn.contains("right") && turn.contains("left")) {

				arrowMaterial = ROAD_MARKING_ARROW_RIGHT_LEFT;

			} else if (turn.contains("right")) {

				arrowMaterial = ROAD_MARKING_ARROW_RIGHT;

			} else if (turn.contains("left")) {

				arrowMaterial = ROAD_MARKING_ARROW_RIGHT;

			}

		}

		/* apply the results */

		if (arrowMaterial != null) {
			material = material.withAddedLayers(arrowMaterial.getTextureDataList());
		}

		return material;

	}

	/**
	 * a texture coordinate function for arrow road markings on turn lanes.
	 * Has special features including centering the arrow, placing it at an
	 * offset from the end of the road, and taking available space into account.
	 *
	 * To reduce the number of necessary textures, it uses mirrored versions of
	 * the various right-pointing arrows for left-pointing arrows.
	 */
	private static class ArrowTexCoordFunction implements TexCoordFunction {

		private final RoadPart roadPart;
		private final boolean rightHandTraffic;
		private final boolean mirrorLeftRight;

		private ArrowTexCoordFunction(RoadPart roadPart,
				boolean rightHandTraffic, boolean mirrorLeftRight) {

			this.roadPart = roadPart;
			this.rightHandTraffic = rightHandTraffic;
			this.mirrorLeftRight = mirrorLeftRight;

		}

		@Override
		public List<VectorXZ> apply(List<VectorXYZ> vs, TextureData textureData) {

			if (vs.size() % 2 == 1) {
				throw new IllegalArgumentException("not a triangle strip lane");
			}

			List<VectorXZ> result = new ArrayList<VectorXZ>(vs.size());

			boolean forward = roadPart == RoadPart.LEFT ^ rightHandTraffic;

			/* calculate length of the lane */

			double totalLength = 0;

			for (int i = 0; i+2 < vs.size(); i += 2) {
				totalLength += vs.get(i).distanceToXZ(vs.get(i+2));
			}

			/* calculate texture coordinate list */

			double accumulatedLength = forward ? totalLength : 0;

			for (int i = 0; i < vs.size(); i++) {

				VectorXYZ v = vs.get(i);

				// increase accumulated length after every second vector

				if (i > 0 && i % 2 == 0) {

					double segmentLength = v.xz().distanceTo(vs.get(i-2).xz());

					if (forward) {
						accumulatedLength -= segmentLength;
					} else {
						accumulatedLength += segmentLength;
					}

				}

				// determine width of the lane at that point

				double width = (i % 2 == 0)
						? v.distanceTo(vs.get(i+1))
						: v.distanceTo(vs.get(i-1));

				// determine whether this vertex should get the higher or
				// lower t coordinate from the vertex pair

				boolean higher = i % 2 == 0;

				if (!forward) {
					higher = !higher;
				}

				if (mirrorLeftRight) {
					higher = !higher;
				}

				// calculate texture coords

				double s, t;

				s = accumulatedLength / textureData.width;

				if (width > textureData.height) {
					double padding = ((width / textureData.height) - 1)  / 2;
					t = higher ? 0 - padding : 1 + padding;
				} else {
					t = higher ? 0 : 1;
				}

				result.add(new VectorXZ(s, t));

			}

			return result;

		}

	}
}
