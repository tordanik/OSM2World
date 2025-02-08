package org.osm2world.world.modules.traffic_sign;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.util.enums.ForwardBackward.BACKWARD;
import static org.osm2world.util.enums.ForwardBackward.FORWARD;
import static org.osm2world.util.enums.LeftRight.RIGHT;
import static org.osm2world.world.modules.RoadModule.getConnectedRoads;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseDirection;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.MapRelation.Membership;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.target.common.material.Material;
import org.osm2world.util.enums.ForwardBackward;
import org.osm2world.util.enums.LeftRight;
import org.osm2world.world.modules.RoadModule;
import org.osm2world.world.modules.RoadModule.Road;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * adds traffic signs to the world
 */
public class TrafficSignModule extends AbstractModule {

	/**
	 * Checks if {@code node}'s way contains traffic sign tags to be rendered
	 */
	private void createSignFromTagOnWay(MapNode node) {

		final VectorXZ DEFAULT_OFFSET_VECTOR = new VectorXZ(1, 1);

		// possible values are yes/limited/no
		String state = config.getString("deduceTrafficSignsFromWayTags", "no");
		if ("no".equals(state)) return;

		String country;
		String tagValue;

		//The (potential) signs to be rendered
		TrafficSignGroup firstModel = null;
		TrafficSignGroup secondModel = null;

		//the way this node is mapped on & its way tags
		MapWay way = node.getConnectedWaySegments().get(0).getWay();
		TagSet wayTags = way.getTags();

		List<String> supportedKeys = asList("maxspeed", "overtaking", "maxwidth", "maxheight", "maxweight",
				"traffic_sign", "traffic_sign:backward", "traffic_sign:forward");

		if (supportedKeys.stream().anyMatch(it -> wayTags.containsKey(it))) {

			List<String> containedKeys = new ArrayList<>(supportedKeys);
			containedKeys.removeIf(it -> !wayTags.containsKey(it));

			List<String> keysForFirstModel = new ArrayList<>();
			List<String> keysForSecondModel = new ArrayList<>();

			if (!containedKeys.isEmpty()) {

				String tempKey;

				//determine rendering positions
				for (String key : containedKeys) {

					//if a way adjacent to the beginning of this way does not contain the same traffic sign
					if (!adjacentWayContains(true, way, key, wayTags.getValue(key))
							&& !key.contains("backward")) {

						boolean renderMe = true;

						//when at the beginning of the way, handle the sign
						//as it was mapped as sign:forward (affect vehicles moving at the same
						//direction as the way)
						if (!key.contains("forward")) {
							tempKey = key + ":forward";

							//check the way adjacent to the beginning of this way for sign mapped with forward direction
							if (adjacentWayContains(true, way, tempKey, wayTags.getValue(key))) {

								renderMe = false;
							}

						} else {

							//check the way adjacent to the beginning of this way for sign mapped without direction specified
							tempKey = key.substring(0, key.indexOf(':'));

							if (adjacentWayContains(true, way, tempKey, wayTags.getValue(key))) {

								renderMe = false;
							}

							tempKey = key;
						}

						if (renderMe) {
							//get first way segment
							MapWaySegment topSegment = way.getWaySegments().get(0);

							//place sign group at the beginning of the way
							MapNode firstModelNode = topSegment.getStartNode();
							firstModel = new TrafficSignGroup(firstModelNode, config);

							//get segment's driving side
							LeftRight side = RoadModule.getDrivingSide(topSegment, config);

							//set the model's facing direction
							firstModel.direction = calculateDirection(topSegment,
									tempKey.contains("forward") ? FORWARD : BACKWARD);

							//Avoid the case where the first node of the way may be a junction
							if (topSegment.getStartNode().getConnectedWaySegments().size() < 3) {
								firstModel.position = calculateSignPosition(firstModelNode, topSegment, side,
										tempKey.contains("forward") ? FORWARD : BACKWARD, false);
							} else {

								if (RoadModule.isRoad(wayTags)) {
									VectorXZ offset = NULL_VECTOR;
									//FIXME - disabled during merge
									//VectorXZ offset = ((Road)topSegment.getPrimaryRepresentation()).getStartOffset();
									firstModel.position = firstModelNode.getPos().add(offset);
								} else {
									firstModel.position = firstModelNode.getPos().add(DEFAULT_OFFSET_VECTOR);
								}
							}

							keysForFirstModel.add(key);
						}
					}

					/*
					 * if no same key-value pair exists in an adjacent way (at the end of this way) and the way is not tagged as oneway
					 * (oneways should have their sign mapped at the beginning of them, if applicable)
					 * render an extra traffic sign model at the end of the way.
					 * Configuration definition deduceTrafficSignsFromWayTags=limited "turns off" this mechanism
					 */
					if (!adjacentWayContains(false, way, key, wayTags.getValue(key))
							&& !RoadModule.isOneway(way.getTags())
							&& !state.equalsIgnoreCase("limited") && !key.contains("forward")) {

						boolean renderMe = true;

						//when at the end of the way, handle traffic sign
						//as it was mapped as sign:backward
						if (!key.contains("backward")) {
							tempKey = key + ":backward";

							//check the way adjacent to the beginning of this way for sign mapped with forward direction
							if (adjacentWayContains(false, way, tempKey, wayTags.getValue(key))) {
								//continue;
								renderMe = false;
							}

						} else {

							//check the way adjacent to the beginning of this way for sign mapped without direction specified
							tempKey = key.substring(0, key.indexOf(':'));

							if (adjacentWayContains(false, way, tempKey, wayTags.getValue(key))) {
								//continue;
								renderMe = false;
							}

							tempKey = key;
						}

						if (renderMe) {

							//get number of segments from the way this node is part of
							int numOfSegments = way.getWaySegments().size();

							//get last way segment
							MapWaySegment bottomSegment = way.getWaySegments().get(numOfSegments - 1);

							MapNode secondModelNode = bottomSegment.getEndNode();
							secondModel = new TrafficSignGroup(secondModelNode, config);

							//get segment's driving side
							LeftRight side = RoadModule.getDrivingSide(bottomSegment, config);

							secondModel.direction = calculateDirection(bottomSegment,
									tempKey.contains("forward") ? FORWARD : BACKWARD);

							/*
							 * new TrafficSignModel at the end of the way.
							 * Avoid the case where the last node of the way may be a junction
							 */
							if (bottomSegment.getEndNode().getConnectedWaySegments().size() < 3) {

								secondModel.position = calculateSignPosition(secondModelNode, bottomSegment, side,
										tempKey.contains("forward") ? FORWARD : BACKWARD, false);

							} else {

								if (RoadModule.isRoad(wayTags)) {
									VectorXZ offset = NULL_VECTOR;
									//FIXME - disabled during merge
									//VectorXZ offset = ((Road)bottomSegment.getPrimaryRepresentation()).getEndOffset();
									secondModel.position = secondModelNode.getPos().add(offset);
								} else {
									secondModel.position = secondModelNode.getPos().add(DEFAULT_OFFSET_VECTOR);
								}
							}

							keysForSecondModel.add(key);
						}
					}
				}

				List<TrafficSignModel> modelsForFirstSign = new ArrayList<>(containedKeys.size());
				List<TrafficSignModel> modelsForSecondSign = new ArrayList<>(keysForSecondModel.size());

				//configure the traffic sign types

				for (List<String> signs : List.of(keysForFirstModel, keysForSecondModel)) {
					for (String sign : signs) {

						/* only take into account overtaking=no tags for "overtaking" key */
						if (sign.equals("overtaking") && !wayTags.contains("overtaking", "no")) {
							continue;
						}

						//distinguish between human-readable values and traffic sign IDs
						tagValue = wayTags.getValue(sign);

						if (tagValue.contains(":")) {

							String[] countryAndSigns = tagValue.split(":", 2);
							if (countryAndSigns.length != 2)
								continue;
							country = countryAndSigns[0];

							sign = countryAndSigns[1];

						} else {
							country = null;
						}

						var newModel = TrafficSignModel.create(new TrafficSignIdentifier(country, sign), wayTags, config);

						if (signs == keysForFirstModel) {
							modelsForFirstSign.add(newModel);
						} else {
							modelsForSecondSign.add(newModel);
						}

					}
				}

				if (modelsForFirstSign.size() > 0) {

					if (firstModel != null) {
						firstModel.signs = modelsForFirstSign;
						node.addRepresentation(firstModel);
					}
				}
				if (modelsForSecondSign.size() > 0) {

					if (secondModel != null) {
						secondModel.signs = modelsForSecondSign;
						node.addRepresentation(secondModel);
					}

				}
			}
		}
	}

	/**
	 * Handles signs mapped as stand-alone nodes (next to the road)
	 */
	private void createSignFromSeparateNode(MapNode node) {

		List<TrafficSignIdentifier> signIds =
				TrafficSignIdentifier.parseTrafficSignValue(node.getTags().getValue("traffic_sign"));

		List<TrafficSignModel> signs = new ArrayList<>(signIds.size());
		signIds.forEach(sign -> signs.add(TrafficSignModel.create(sign, node.getTags(), config)));

		if (!signIds.isEmpty()) {
			node.addRepresentation(new TrafficSignGroup(node, signs, node.getPos(),
					parseDirection(node.getTags()), config));
		}

	}

	/**
	 * Handles signs mapped on nodes that are part of the highway way
	 */
	private void createSignFromHighwayNode(MapNode node) {

		String tagValue = "";
		@Nullable ForwardBackward relativeSignDirection;

		// TODO: allow traffic_sign:forward and traffic_sign:backward to be tagged at the same time

		if (node.getTags().containsKey("traffic_sign:forward")) {
			tagValue = node.getTags().getValue("traffic_sign:forward");
			relativeSignDirection = FORWARD;
		} else if (node.getTags().containsKey("traffic_sign:backward")) {
			tagValue = node.getTags().getValue("traffic_sign:backward");
			relativeSignDirection = BACKWARD;
		} else {
			if (node.getTags().containsKey("traffic_sign")) {
				tagValue = node.getTags().getValue("traffic_sign");
			}
			// sign direction is relevant even if no traffic_sign is present (for highway=give_way/stop)
			if (node.getTags().containsAny(List.of("direction", "traffic_sign:direction"), List.of("forward"))) {
				relativeSignDirection = FORWARD;
			} else if (node.getTags().containsAny(List.of("direction", "traffic_sign:direction"), List.of("backward"))) {
				relativeSignDirection = BACKWARD;
			} else {
				relativeSignDirection = null;
			}
		}

		/* get the list of traffic signs from the tag's value */

		List<TrafficSignIdentifier> signIds = TrafficSignIdentifier.parseTrafficSignValue(tagValue);

		/* handle the special cases highway=give_way/stop */

		String highwayValue = node.getTags().getValue("highway");

		if (asList("give_way", "stop").contains(highwayValue)
				&& !signIds.contains(new TrafficSignIdentifier(null, highwayValue))) {
			signIds = new ArrayList<>(signIds);
			signIds.add(new TrafficSignIdentifier(null, highwayValue));
		}

		/* calculate the rotation and position(s) */

		MapWaySegment segment = node.getConnectedWaySegments().get(0);
		LeftRight side = RoadModule.getDrivingSide(segment, config);
		double direction = calculateDirection(node, relativeSignDirection);

		List<VectorXZ> positions = new ArrayList<>();

		positions.add(calculateSignPosition(node, segment, side, relativeSignDirection, true));

		if (node.getTags().contains("side", "both")) {
			positions.add(calculateSignPosition(node, segment, side.invert(), relativeSignDirection, true));
		}

		/* create a visual representation of the group of signs at each position */

		List<TrafficSignModel> signs = new ArrayList<>(signIds.size());
		signIds.forEach(sign -> signs.add(TrafficSignModel.create(sign, node.getTags(), config)));

		if (signs.size() > 0) {
			for (VectorXZ position : positions) {
				node.addRepresentation(new TrafficSignGroup(node, signs, position, direction, config));
			}
		}

	}

	private void createDestinationSign(MapNode node) {

		double direction = PI;
		List<TrafficSignModel> signs = new ArrayList<>();

		for (Membership m : node.getMemberships()) {

			MapRelation relation = m.getRelation();

			String pointingDirection;

			String destination = relation.getTags().getValue("destination");
			if (destination == null) {
				ConversionLog.warn("Missing destination in destination_sign: " + relation);
				continue;
			}

			String distance = relation.getTags().getValue("distance");

			Color backgroundColor = parseColor(relation.getTags().getValue("colour:back"), Color.WHITE);
			Color textColor = parseColor(relation.getTags().getValue("colour:text"), Color.BLACK);

			//map to use in configureMaterial
			HashMap<String, String> map = new HashMap<>();

			map.put("destination", destination);
			if (distance != null)
				map.put("distance", distance);

			//List to hold all "from" members of the relation (if multiple)
			List<MapWay> fromMembers = new ArrayList<>();
			MapWay to = null;
			MapNode intersection = null;

			for (Membership member : relation.getMembers()) {
				if (member.getRole().equals("from") && member.getElement() instanceof MapWay w) {
					fromMembers.add(w);
				} else if (member.getRole().equals("to") && member.getElement() instanceof MapWay w) {
					to = w;
				} else if (member.getRole().equals("intersection") && member.getElement() instanceof MapNode n) {
					intersection = n;
				}
			}

			if (intersection == null || to == null) {
				ConversionLog.warn("Member 'intersection' or 'to' was not defined in relation " + relation +
						". Destination sign rendering is omitted for this relation.");
				continue;
			}

			MapWay from;
			if (fromMembers.size() != 1) {
				// use the vector from "sign" to "intersection" instead
				from = new MapWay(0, TagSet.of(), asList(node, intersection));
			} else {
				from = fromMembers.get(0);
			}

			// get adjacent segments

			MapWaySegment toSegment = getAdjacentSegment(to, intersection);
			MapWaySegment fromSegment = getAdjacentSegment(from, intersection);

			if (toSegment == null) {
				ConversionLog.warn("Way " + to + " is not connected to intersection " + intersection + ".");
				continue;
			}
			if (fromSegment == null) {
				ConversionLog.warn("Way " + from + " is not connected to intersection " + intersection);
				continue;
			}

			//get facing direction

			VectorXZ facingDir = fromSegment.getDirection();

			//if the segment is facing towards the intersection, invert its direction vector
			if (fromSegment.getEndNode().equals(intersection)) {
				facingDir = facingDir.invert();
			}

			direction = facingDir.angle();

			//explicit direction definition overwrites from-derived facing direction
			if (node.getTags().containsKey("direction")) {
				direction = parseDirection(node.getTags(), PI);
			}

			//get the other ends of the segments
			MapNode otherToSegmentNode = toSegment.getOtherNode(intersection);
			MapNode otherFromSegmentNode = fromSegment.getOtherNode(intersection);

			boolean isRightOf = GeometryUtil.isRightOf(otherToSegmentNode.getPos(), otherFromSegmentNode.getPos(),
					intersection.getPos());

			if (isRightOf) {
				pointingDirection = "RIGHT";
			} else {
				pointingDirection = "LEFT";
			}

			TrafficSignType type = buildDestinationSignType(pointingDirection,
					relation.getTags().getValue("colour:arrow"));
			if (type == null) { buildDestinationSignType(pointingDirection, "BLACK"); }
			if (type == null) { type = TrafficSignType.blankSign(); }

			Material material = type.material.withPlaceholdersFilledIn(map, relation.getTags());

			//set material background color
			material = material.withColor(backgroundColor);

			//set material text color
			material = material.withTextColor(textColor, 1);

			signs.add(new TrafficSignModel(material, type, type.defaultNumPosts, type.defaultHeight));

		}

		if (!signs.isEmpty()) {
			node.addRepresentation(new TrafficSignGroup(node, signs, node.getPos(), direction, config));
		}

	}

	private @Nullable TrafficSignType buildDestinationSignType(@Nonnull String pointingDirection,
															   @Nullable String arrowColour) {
		if (arrowColour == null) return null;
		TrafficSignIdentifier identifier = new TrafficSignIdentifier(null,
				"DESTINATION_SIGN_" + arrowColour.toUpperCase() + "_" + pointingDirection.toUpperCase());
		return TrafficSignType.fromConfig(identifier, config);
	}

	@Override
	protected void applyToNode(MapNode node) {

		/* if node is the "sign" member of at least one destination_sign relation */

		if (node.getMemberships().stream().anyMatch(it -> "sign".equals(it.getRole())
				&& it.getRelation().getTags().contains("type", "destination_sign"))) {
			createDestinationSign(node);
		}

		/* if "traffic_sign = *" or any of the human-readable signs (maxspeed|overtaking|maxwidth|maxweight|maxheight) are mapped as a way tag */

		if (!node.getConnectedWaySegments().isEmpty()) {
			createSignFromTagOnWay(node);
		}

		/* if sign is a simple traffic sign mapped on a node (not a destination sign or a human-readable value mapped on a way) */

		if (node.getTags().containsAny(asList("traffic_sign", "traffic_sign:forward", "traffic_sign:backward", "highway"), null)) {
			if (isInHighway(node)) {
				createSignFromHighwayNode(node);
			} else {
				createSignFromSeparateNode(node);
			}
		}

	}

	static boolean isInHighway(MapNode node) {
		for (MapWaySegment way : node.getConnectedWaySegments()) {
			if (way.getTags().containsKey("highway")
					&& !"platform".equals(way.getTags().getValue("highway"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the junction closest to {@code node}.
	 * The junction must be reachable by traveling forward or backward on the road {@code node} is part of.
	 * A node with 3 or more roads connected to it is considered a junction.
	 *
	 * @param node  must not be a junction itself; != null
	 * @return  the node representing the closest junction, or null if none was found
	 */
	static @Nullable MapNode findClosestJunction(MapNode node) {

		if (getConnectedRoads(node, false).size() > 2) {
			throw new IllegalArgumentException("Node " + node + " itself is a junction");
		}

		List<MapNode> foundJunctions = new ArrayList<>();

		for (Road firstRoad : getConnectedRoads(node, false)) {

			Road currentRoad = firstRoad;
			MapNode currentNode = currentRoad.segment.getOtherNode(node);

			while (currentNode != node) { // this check avoids infinite loops with circular roads

				List<Road> connectedRoads = getConnectedRoads(currentNode, false);

				if (connectedRoads.size() >= 3) {
					foundJunctions.add(currentNode);
					break;
				} else if (connectedRoads.size() == 1) {
					//dead end
					break;
				} else {

					// go to the next road segment
					assert connectedRoads.size() == 2 && connectedRoads.contains(currentRoad);
					if (connectedRoads.get(0).equals(currentRoad)) {
						currentRoad = connectedRoads.get(1);
					} else {
						currentRoad = connectedRoads.get(0);
					}

					currentNode = currentRoad.segment.getOtherNode(currentNode);

				}

			}

		}

		return foundJunctions.stream().min((Comparator.comparingDouble(j -> j.getPos().distanceTo(node.getPos()))))
				.orElse(null);

	}

	/**
	 * find the segment of {@code way} whose start (or end) node
	 * is the {@code intersection} . Returns the found segment,
	 * null if not found.
	 */
	private static MapWaySegment getAdjacentSegment(MapWay way, MapNode intersection) {

		List<MapWaySegment> waySegments = way.getWaySegments(); //get the way's segments

		for (MapWaySegment segment : asList(waySegments.get(0), waySegments.get(waySegments.size() - 1))) {

			if (segment.getStartNode().equals(intersection) || segment.getEndNode().equals(intersection)) {
				return segment;
			}
		}

		return null;
	}


	/**
	 * Check if the top (bottom) of the given {@code way} is connected
	 * to another way that contains the same key-value tag. Applicable to
	 * traffic sign mapping cases where signs must be rendered both at
	 * the beginning and the end of the way they apply to
	 *
	 * @param topOrBottom
	 * A boolean value indicating whether to check the
	 * beginning or end of the {@code way}. True for beginning, false for end.
	 */
	private static boolean adjacentWayContains(boolean topOrBottom, MapWay way, String key, String value) {

		//decide whether to check beginning or end of the way
		int index = 0;
		MapNode wayNode;

		if (!topOrBottom) {

			//check the last node of the last segment

			index = way.getWaySegments().size() - 1;

			wayNode = way.getWaySegments().get(index).getEndNode();

		} else {

			//check the first node of the first segment

			wayNode = way.getWaySegments().get(index).getStartNode();
		}

		//if the node is also part of another way
		if (wayNode.getConnectedSegments().size() == 2) {

			for (MapWaySegment segment : wayNode.getConnectedWaySegments()) {

				//if current segment is part of this other way
				if (!segment.getWay().equals(way)) {

					//check if this other way also contains the key-value pair
					MapWay nextWay = segment.getWay();

					return nextWay.getTags().contains(key, value);
				}
			}
		}

		return false;
	}

	/**
	 * calculates the position of a traffic sign
	 * based on the {@code side} of the road it is supposed to be and the width of the road
	 */
	private static VectorXZ calculateSignPosition(MapNode node, MapWaySegment segment, LeftRight side ,
			ForwardBackward signDirection, boolean isNode) {

		//if way is not a road
		if (!RoadModule.isRoad(segment.getTags())) {
			return node.getPos();
		}

		//get the rendered segment's width
		double roadWidth = 2;
		if (segment.getPrimaryRepresentation() instanceof Road r) {
			roadWidth = r.getWidth();
		}

		//rightNormal() vector will always be orthogonal to the segment, no matter its direction,
		//so we use that to place the signs to the left/right of the way
		VectorXZ rightNormal = segment.getDirection().rightNormal();

		/*
		 * Explicit side tag overwrites all. Applies only to sign mapped on way nodes
		 */
		if (node.getTags().containsKey("side") && isNode) {
			if (node.getTags().contains("side", "right")) {
				return node.getPos().add(rightNormal.mult(roadWidth));
			} else if (node.getTags().contains("side", "left")) {
				return node.getPos().add(rightNormal.mult(-roadWidth));
			}
		}

		if (side == RIGHT) {

			if (signDirection == FORWARD) {
				return node.getPos().add(rightNormal.mult(roadWidth));
			} else {
				return node.getPos().add(rightNormal.mult(-roadWidth));
			}

		} else {

			if (signDirection == FORWARD) {
				return node.getPos().add(rightNormal.mult(-roadWidth));
			} else {
				return node.getPos().add(rightNormal.mult(roadWidth));
			}

		}
	}

	/** calculates the rotation angle of a traffic sign based on the direction of {@code segment} */
	private static double calculateDirection(MapWaySegment segment, ForwardBackward signDirection) {

		VectorXZ wayDir = segment.getDirection();

		if (signDirection == FORWARD) {
			return wayDir.invert().angle();
		} else {
			return wayDir.angle();
		}
	}

	/**
	 * Calculates the direction for traffic sign nodes that are part of ways.
	 * Same as {@link #calculateDirection(MapWaySegment, ForwardBackward)} but also parses the node's
	 * tags for the direction specification and takes into account the
	 * highway=give_way/stop special case.
	 */
	private static double calculateDirection(MapNode node, @Nullable ForwardBackward relativeDirection) {

		if (node.getConnectedWaySegments().isEmpty()) {
			ConversionLog.warn("Node " + node + " is not part of a way.");
			return PI;
		}

		TagSet nodeTags = node.getTags();

		//Direction the way the node is part of, is facing
		VectorXZ wayDir = node.getConnectedWaySegments().get(0).getDirection();

		//Explicit direction mapped as angle or cardinal direction (if any)
		Double direction = parseDirection(nodeTags);

		if (direction != null) {
			return direction;
		} else if (relativeDirection != null) {
			return switch (relativeDirection) {
				case FORWARD -> wayDir.invert().angle();
				case BACKWARD -> wayDir.angle();
			};
		} else if (nodeTags.containsAny(List.of("highway"), List.of("give_way", "stop"))) {

			/*
			 * if no direction is specified with any of the above cases and a
			 * sign of type highway=give_way/stop is mapped, calculate model's
			 * direction based on the position of the junction closest to the node
			 */

			if (RoadModule.getConnectedRoads(node, false).size() <= 2
					&& RoadModule.getConnectedRoads(node, false).size() > 0) {

				MapNode closestJunction = TrafficSignModule.findClosestJunction(node);

				if (closestJunction != null) {
					return (node.getPos().subtract(closestJunction.getPos())).normalize().angle();
				} else {
					return wayDir.angle();
				}
			}
		}

		/*
		 * Either traffic_sign:forward or direction=forward is specified or no forward/backward is specified at all.
		 * traffic_sign:forward affects vehicles moving in the same direction as
		 * the way. Therefore they must face the sign so the way's direction is inverted.
		 * Vehicles facing the sign is the most common case so it is set as the default case
		 * as well
		 */
		wayDir = wayDir.invert();
		return wayDir.angle();

	}

}
