package org.osm2world.core.world.modules.traffic_sign;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.util.enums.ForwardBackward.*;
import static org.osm2world.core.util.enums.LeftRight.*;
import static org.osm2world.core.world.modules.RoadModule.getConnectedRoads;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseDirection;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.MapRelation.Membership;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.Tag;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.util.enums.ForwardBackward;
import org.osm2world.core.util.enums.LeftRight;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RoadModule.Road;
import org.osm2world.core.world.modules.common.AbstractModule;

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

		String[] signs;
		String[] signsForSecondModel;
		String country = "";
		String tagValue = "";

		/*
		 * Value to hold the side of the road non-explicitly defined signs
		 * will be rendered to. Default value is true. Indicates the rules of the
		 * road as defined in RoadModule.java
		 */
		LeftRight side = RoadModule.RIGHT_HAND_TRAFFIC_BY_DEFAULT ? RIGHT : LEFT;

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
							side = RoadModule.hasRightHandTraffic(topSegment) ? RIGHT : LEFT;

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
							side = RoadModule.hasRightHandTraffic(bottomSegment) ? RIGHT : LEFT;

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

				signs = keysForFirstModel.toArray(new String[0]);
				signsForSecondModel = keysForSecondModel.toArray(new String[0]);

				List<TrafficSignModel> models = new ArrayList<>(containedKeys.size());
				List<TrafficSignModel> modelsForSecondSign = new ArrayList<>(keysForSecondModel.size());

				//configure the traffic sign types

				for (String sign : signs) {

					/* only take into account overtaking=no tags for "overtaking" key */
					if (sign.equals("overtaking") && !wayTags.getValue(sign).equals("no")) {
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

					models.add(TrafficSignModel.create(new TrafficSignIdentifier(country, sign), wayTags, config));

				}

				for (String sign : signsForSecondModel) {

					/* only take into account overtaking=no tags for "overtaking" key */
					if (sign.equals("overtaking") && !wayTags.getValue(sign).equals("no")) {
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

					modelsForSecondSign.add(TrafficSignModel.create(
							new TrafficSignIdentifier(country, sign), wayTags, config));
				}

				if (models.size() > 0) {

					if (firstModel != null) {
						firstModel.signs = models;
						node.addRepresentation(firstModel);
					}
				}
				if (modelsForSecondSign.size() > 0) {

					if (secondModel != null) {
						secondModel.signs = modelsForSecondSign;
						node.addRepresentation(secondModel);
					}

					return;
				}
			}
		}

		return;
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
		ForwardBackward signDirection;

		// TODO: allow traffic_sign:forward and traffic_sign:backward to be tagged at the same time

		if (node.getTags().containsKey("traffic_sign:forward")) {
			tagValue = node.getTags().getValue("traffic_sign:forward");
			signDirection = FORWARD;
		} else if (node.getTags().containsKey("traffic_sign:backward")) {
			tagValue = node.getTags().getValue("traffic_sign:backward");
			signDirection = BACKWARD;
		} else if (node.getTags().containsKey("traffic_sign")) {
			tagValue = node.getTags().getValue("traffic_sign");
			signDirection = FORWARD;
		} else {
			//in case of highway=give_way/stop, treat sign as if it was mapped as forward:
			//affect vehicles moving in the way's direction if no explicit direction tag is defined
			signDirection = FORWARD;
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
		LeftRight side = RoadModule.hasRightHandTraffic(segment) ? RIGHT : LEFT;
		double direction = calculateDirection(node);

		List<VectorXZ> positions = new ArrayList<>();

		positions.add(calculateSignPosition(node, segment, side, signDirection, true));

		if (node.getTags().contains("side", "both")) {
			positions.add(calculateSignPosition(node, segment, side.invert(), signDirection, true));
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

			String pointingDirection;

			//default values
			Color backgroundColor = Color.WHITE;
			Color textColor = Color.BLACK;

			//relation attributes
			MapWay from = null;
			MapWay to = null;
			MapNode intersection = null;
			String destination;
			String distance;
			String arrowColour;

			MapWaySegment toSegment = null;
			MapWaySegment fromSegment = null;

			MapRelation relation = m.getRelation();

			destination = relation.getTags().getValue("destination");
			if (destination == null) {
				System.err.println(
						"Destination can not be null. Destination sign rendering ommited for relation " + relation);
				continue;
			}
			distance = relation.getTags().getValue("distance");

			//parse background color
			if (relation.getTags().getValue("colour:back") != null) {

				Field field;

				try {
					field = Color.class.getField(relation.getTags().getValue("colour:back"));
					backgroundColor = (Color) field.get(null);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			//parse text color
			if (relation.getTags().getValue("colour:text") != null) {

				Field field;

				try {
					field = Color.class.getField(relation.getTags().getValue("colour:text"));
					textColor = (Color) field.get(null);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			arrowColour = relation.getTags().getValue("colour:arrow");
			if (arrowColour == null)
				arrowColour = "BLACK";

			//map to use in configureMaterial
			HashMap<String, String> map = new HashMap<>();

			map.put("destination", destination);
			if (distance != null)
				map.put("distance", distance);

			//List to hold all "from" members of the relation (if multiple)
			List<MapWay> fromMembers = new ArrayList<>();

			//variable to avoid calculating 'from' using vector from "sign" to "intersection"
			//if fromMembers size is 0 because of not acceptable mapping
			boolean wrongFrom = false;

			for (Membership member : relation.getMemberships()) {

				if (member.getRole().equals("from")) {

					if (member.getElement() instanceof MapWay) {
						fromMembers.add((MapWay) member.getElement());
					} else {
						System.err.println("'from' member of relation " + relation + " is not a way."
								+ " It is not being considered for rendering this relation's destination sign");
						wrongFrom = true;
						continue;
					}

				} else if (member.getRole().equals("to")) {

					if (member.getElement() instanceof MapWay) {
						to = (MapWay) member.getElement();
					} else {
						System.err.println("'to' member of relation " + relation + " is not a way."
								+ " It is not being considered for rendering this relation's destination sign");
						continue;
					}

				} else if (member.getRole().equals("intersection")) {

					if (!(member.getElement() instanceof MapNode)) {
						System.err.println("'intersection' member of relation " + relation + " is not a node."
								+ " It is not being considered for rendering this relation's destination sign");
						continue;
					}

					intersection = (MapNode) member.getElement();
				}
			}

			//check intersection first as it is being used in 'from' calculation below
			if (intersection == null) {
				System.err.println("Member 'intersection' was not defined in relation " + relation +
						". Destination sign rendering is omited for this relation.");
				continue;
			}

			/*
			 * if there are multiple "from" members or none at all,
			 * use the vector from "sign" to "intersection" instead
			 */

			if (fromMembers.size() > 1 || (fromMembers.size() == 0 && !wrongFrom)) {
				List<MapNode> signAndIntersection = asList(
						node,
						intersection);

				//create a new MapWay instance from sign node to intersection node
				MapWay signToIntersection = new MapWay(0, TagSet.of(), signAndIntersection);
				from = signToIntersection;

			} else if (fromMembers.size() == 1) {

				from = fromMembers.get(0);
			}

			//check if the rest of the "vital" relation members where defined
			if (from == null || to == null) {
				System.err.println("not all members of relation " + relation + " were defined."
						+ " Destination sign rendering is omited for this relation.");
				continue;
			}

			//get facing direction

			boolean mustBeInverted = false;

			//'from' member is a way
			fromSegment = getAdjacentSegment(from, intersection);

			if (fromSegment != null) {
				if (fromSegment.getEndNode().equals(intersection)) {
					mustBeInverted = true;
				}
			} else {
				System.err.println("Way " + from + " is not connected to intersection " + intersection);
				continue;
			}

			VectorXZ facingDir = fromSegment.getDirection();

			//if the segment is facing towards the intersection, invert its direction vector
			if (mustBeInverted)
				facingDir = facingDir.invert();

			direction = facingDir.angle();

			//'to' member is also a way
			toSegment = getAdjacentSegment(to, intersection);

			if (toSegment == null) {
				System.err
						.println("Way " + to + " is not connected to intersection " + intersection + ". Returning");
				continue;
			}

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

			TrafficSignIdentifier identifier = new TrafficSignIdentifier(null,
					"DESTINATION_SIGN_" + arrowColour.toUpperCase() + "_" + pointingDirection.toUpperCase());

			TrafficSignType type = TrafficSignType.fromConfig(identifier, config);
			if (type == null) { type = TrafficSignType.blankSign(); }

			Material material = type.material.withPlaceholdersFilledIn(map, relation.getTags());

			//set material background color
			material = material.withColor(backgroundColor);

			//set material text color
			material = material.withTextColor(textColor, 1);

			signs.add(new TrafficSignModel(material, type, type.defaultNumPosts, type.defaultHeight));

		}

		node.addRepresentation(new TrafficSignGroup(node, signs, node.getPos(), direction, config));

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
					&& !asList("path", "footway", "platform").contains(way.getTags().getValue("highway"))) {
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
	 * @returns  the node representing the closest junction, or null if none was found
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

					if (nextWay.getTags().contains(key, value)) {
						return true;
					} else {
						return false;
					}
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
		Road r = (Road) segment.getPrimaryRepresentation();

		double roadWidth = r.getWidth();

		//rightNormal() vector will always be orthogonal to the segment, no matter its direction,
		//so we use that to place the signs to the left/right of the way
		VectorXZ rightNormal = segment.getDirection().rightNormal();

		/*
		 * Explicit side tag overwrites all. Applies only to sign mapped on way nodes
		 */
		if (node.getTags().containsKey("side") && isNode) {
			if (node.getTags().getValue("side").equals("right")) {
				return node.getPos().add(rightNormal.mult(roadWidth));
			} else if (node.getTags().getValue("side").equals("left")) {
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
	 * Same as {@link #calculateDirection(MapWaySegment, ForwardBackward)} but also parses the node's
	 * tags for the direction specification and takes into account the
	 * highway=give_way/stop special case. To be used on nodes that are part of ways.
	 */
	private static double calculateDirection(MapNode node) {

		if (node.getConnectedWaySegments().isEmpty()) {
			System.err.println("Node " + node + " is not part of a way.");
			return PI;
		}

		TagSet nodeTags = node.getTags();

		//Direction the way the node is part of, is facing
		VectorXZ wayDir = node.getConnectedWaySegments().get(0).getDirection();

		if (nodeTags.containsKey("direction")) {

			if (!nodeTags.containsAny(asList("direction"), asList("forward", "backward"))) {

				//get direction mapped as angle or cardinal direction
				return parseDirection(nodeTags, PI);

			} else {

				if (nodeTags.getValue("direction").equals("backward")) {

					return wayDir.angle();

				}

			}

		} else if (nodeTags.containsKey("traffic_sign:forward") || nodeTags.containsKey("traffic_sign:backward")) {

			String regex = "traffic_sign:(forward|backward)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher;

			for (Tag tag : nodeTags) {

				matcher = pattern.matcher(tag.key);

				if (matcher.matches()) {

					if (matcher.group(1).equals("backward")) {
						return wayDir.angle();
					}
				}
			}
		} else if (nodeTags.containsAny(asList("highway"), asList("give_way", "stop"))) {

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
