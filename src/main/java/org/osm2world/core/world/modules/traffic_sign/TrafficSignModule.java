package org.osm2world.core.world.modules.traffic_sign;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXYZ.X_UNIT;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.RoadModule.getConnectedRoads;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.MapRelation.Membership;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RoadModule.Road;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds traffic signs to the world
 */
public class TrafficSignModule extends AbstractModule {

	/**
	 * Checks if {@code node} is part of any type=destination_sign relationships.
	 * Adds destination sign world representation if it is
	 */
	private void createDestinationSign(MapNode node) {
		for (Membership m : node.getMemberships()) {
			if (m.getRelation().getTags().contains("type", "destination_sign") && m.getRole().equals("sign")) {
				node.addRepresentation(new DestinationSign(node));
				return;
			}
		}
	}

	/**
	 * Checks if {@code node}'s way contains traffic sign tags to be rendered
	 *
	 * @param state The value extracted from the configuration key deduceTrafficSignsFromWayTags = yes/limited/no
	 */
	private void createSignFromTagOnWay(MapNode node, String state) {

		final VectorXZ DEFAULT_OFFSET_VECTOR = new VectorXZ(1, 1);

		String[] signs;
		String[] signsForSecondModel;
		String country = "";
		String tagValue = "";

		//a TrafficSignType instance to hold the sign's attributes
		TrafficSignTypeInstance attributes;

		/*
		 * Value to hold the side of the road non-explicitly defined signs
		 * will be rendered to. Default value is true. Indicates the rules of the
		 * road as defined in RoadModule.java
		 */
		boolean side = RoadModule.RIGHT_HAND_TRAFFIC_BY_DEFAULT;

		//The (potential) signs to be rendered
		TrafficSignModel firstModel = null;
		TrafficSignModel secondModel = null;

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

				String tempKey = "";

				//determine rendering positions
				for (String key : containedKeys) {

					//if a way adjacent to the beginning of this way does not contain the same traffic sign
					if (!TrafficSignModel.adjacentWayContains(true, way, key, wayTags.getValue(key))
							&& !key.contains("backward")) {

						boolean renderMe = true;

						//when at the beginning of the way, handle the sign
						//as it was mapped as sign:forward (affect vehicles moving at the same
						//direction as the way)
						if (!key.contains("forward")) {
							tempKey = key + ":forward";

							//check the way adjacent to the beginning of this way for sign mapped with forward direction
							if (TrafficSignModel.adjacentWayContains(true, way, tempKey, wayTags.getValue(key))) {

								renderMe = false;
							}

						} else {

							//check the way adjacent to the beginning of this way for sign mapped without direction specified
							tempKey = key.substring(0, key.indexOf(':'));

							if (TrafficSignModel.adjacentWayContains(true, way, tempKey, wayTags.getValue(key))) {

								renderMe = false;
							}

							tempKey = key;
						}

						if (renderMe) {

							//get first way segment
							MapWaySegment topSegment = way.getWaySegments().get(0);

							//new TrafficSignModel at the beginning of the way
							firstModel = new TrafficSignModel(topSegment.getStartNode());

							//get segment's driving side
							side = RoadModule.hasRightHandTraffic(topSegment);

							//set the model's facing direction
							firstModel.calculateDirection(topSegment, tempKey);

							//Avoid the case where the first node of the way may be a junction
							if (topSegment.getStartNode().getConnectedWaySegments().size() < 3) {
								firstModel.calculatePosition(topSegment, side, tempKey, false);
							} else {

								if (RoadModule.isRoad(wayTags)) {
									VectorXZ offset = NULL_VECTOR;
									//FIXME - disabled during merge
									//VectorXZ offset = ((Road)topSegment.getPrimaryRepresentation()).getStartOffset();
									firstModel.position = firstModel.node.getPos().add(offset);
								} else {
									firstModel.position = firstModel.node.getPos().add(DEFAULT_OFFSET_VECTOR);
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
					if (!TrafficSignModel.adjacentWayContains(false, way, key, wayTags.getValue(key))
							&& !RoadModule.isOneway(way.getTags())
							&& !state.equalsIgnoreCase("limited") && !key.contains("forward")) {

						boolean renderMe = true;

						//when at the end of the way, handle traffic sign
						//as it was mapped as sign:backward
						if (!key.contains("backward")) {
							tempKey = key + ":backward";

							//check the way adjacent to the beginning of this way for sign mapped with forward direction
							if (TrafficSignModel.adjacentWayContains(false, way, tempKey, wayTags.getValue(key))) {
								//continue;
								renderMe = false;
							}

						} else {

							//check the way adjacent to the beginning of this way for sign mapped without direction specified
							tempKey = key.substring(0, key.indexOf(':'));

							if (TrafficSignModel.adjacentWayContains(false, way, tempKey, wayTags.getValue(key))) {
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

							secondModel = new TrafficSignModel(bottomSegment.getEndNode());

							//get segment's driving side
							side = RoadModule.hasRightHandTraffic(bottomSegment);

							secondModel.calculateDirection(bottomSegment, tempKey);

							/*
							 * new TrafficSignModel at the end of the way.
							 * Avoid the case where the last node of the way may be a junction
							 */
							if (bottomSegment.getEndNode().getConnectedWaySegments().size() < 3) {

								secondModel.calculatePosition(bottomSegment, side, tempKey, false);

							} else {

								if (RoadModule.isRoad(wayTags)) {
									VectorXZ offset = NULL_VECTOR;
									//FIXME - disabled during merge
									//VectorXZ offset = ((Road)bottomSegment.getPrimaryRepresentation()).getEndOffset();
									secondModel.position = secondModel.node.getPos().add(offset);
								} else {
									secondModel.position = secondModel.node.getPos().add(DEFAULT_OFFSET_VECTOR);
								}
							}

							keysForSecondModel.add(key);
						}
					}
				}

				signs = keysForFirstModel.toArray(new String[0]);
				signsForSecondModel = keysForSecondModel.toArray(new String[0]);

				List<TrafficSignTypeInstance> types = new ArrayList<>(containedKeys.size());
				List<TrafficSignTypeInstance> typesForSecondSign = new ArrayList<>(keysForSecondModel.size());

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

					attributes = configureSignTypeInstance(new TrafficSignIdentifier(country, sign), wayTags);

					types.add(attributes);

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

					attributes = configureSignTypeInstance(new TrafficSignIdentifier(country, sign), wayTags);

					typesForSecondSign.add(attributes);
				}

				if (types.size() > 0) {

					if (firstModel != null) {

						firstModel.types = types;
						node.addRepresentation(new TrafficSign(firstModel, config));
					}
				}
				if (typesForSecondSign.size() > 0) {

					if (secondModel != null) {

						secondModel.types = typesForSecondSign;
						node.addRepresentation(new TrafficSign(secondModel, config));
					}

					return;
				}
			}
		}

		return;
	}

	/**
	 * Handles signs mapped on nodes
	 * (either on a way node or on an explicitly defined one)
	 */
	private void createSignFromNode(MapNode node) {

		if (!node.getTags().containsAny(asList("traffic_sign", "traffic_sign:forward", "traffic_sign:backward"), null)
				&& !node.getTags().containsAny(asList("highway"), asList("give_way", "stop"))) {
			return;
		}

		String tagValue = "";

		//The (potential) signs to be rendered
		TrafficSignModel firstModel = null;
		TrafficSignModel secondModel = null;

		//if node is part of a way
		if (isInHighway(node)) {

			String tempKey = "";

			//one rendering is required
			firstModel = new TrafficSignModel(node);

			if (node.getTags().containsKey("traffic_sign:forward")) {
				tagValue = node.getTags().getValue("traffic_sign:forward");
				tempKey = "traffic_sign:forward";
			} else if (node.getTags().containsKey("traffic_sign:backward")) {
				tagValue = node.getTags().getValue("traffic_sign:backward");
				tempKey = "traffic_sign:backward";
			} else if (node.getTags().containsKey("traffic_sign")) {
				tagValue = node.getTags().getValue("traffic_sign");
				tempKey = "traffic_sign:forward";
			} else {
				//in case of highway=give_way/stop, treat sign as if it was mapped as forward:
				//affect vehicles moving in the way's direction if no explicit direction tag is defined
				tempKey = "highway:forward";
			}

			//the segment this node is part of
			MapWaySegment segment = node.getConnectedWaySegments().get(0);

			//calculate the model's position

			boolean side = RoadModule.hasRightHandTraffic(segment);

			firstModel.calculatePosition(segment, side, tempKey, true);

			firstModel.calculateDirection();

			if (node.getTags().contains("side", "both")) {

				//if side=both , create another model instance and place them to the left & right of the road

				secondModel = new TrafficSignModel(node);

				secondModel.direction = firstModel.direction;
				secondModel.calculatePosition(segment, !side, tempKey, true);
			}

		} else {

			//if sign is explicitly mapped on node next to a way

			if (node.getTags().containsKey("traffic_sign"))
				tagValue = node.getTags().getValue("traffic_sign");

			firstModel = new TrafficSignModel(node);
			firstModel.position = node.getPos();
			firstModel.direction = parseDirection(node.getTags(), PI);

		}

		/* get the list of traffic signs from the tag's value */

		List<TrafficSignIdentifier> signs = TrafficSignIdentifier.parseTrafficSignValue(tagValue);

		/* handle the special cases highway=give_way/stop */

		String highwayValue = node.getTags().getValue("highway");

		if (asList("give_way", "stop").contains(highwayValue)
				&& !signs.contains(new TrafficSignIdentifier(null, highwayValue))) {
			signs = new ArrayList<>(signs);
			signs.add(new TrafficSignIdentifier(null, highwayValue));
		}

		/* create a visual representation for the traffic sign */

		List<TrafficSignTypeInstance> types = new ArrayList<>(signs.size());
		signs.forEach(sign -> types.add(configureSignTypeInstance(sign, node.getTags())));

		if (types.size() > 0) {

			if (firstModel != null) {
				firstModel.types = types;
				node.addRepresentation(new TrafficSign(firstModel, config));
			}

			if (secondModel != null) {
				secondModel.types = types;
				node.addRepresentation(new TrafficSign(secondModel, config));
			}
		}
	}

	@Override
	protected void applyToNode(MapNode node) {

		String signsOnWaysState = config.getString("deduceTrafficSignsFromWayTags", "yes");

		/* if sign is a destination sign */

		if (!node.getMemberships().isEmpty()) {
			createDestinationSign(node);
		}

		/* if "traffic_sign = *" or any of the human-readable signs (maxspeed|overtaking|maxwidth|maxweight|maxheight) are mapped as a way tag */

		if (!node.getConnectedWaySegments().isEmpty() && !signsOnWaysState.equalsIgnoreCase("no")) {
			createSignFromTagOnWay(node, signsOnWaysState);
		}

		/* if sign is a simple traffic sign mapped on a node (not a destination sign or a human-readable value mapped on a way) */

		createSignFromNode(node);

	}

	private static boolean isInHighway(MapNode node) {
		if (node.getConnectedWaySegments().size() > 0) {
			for (MapWaySegment way : node.getConnectedWaySegments()) {
				if (way.getTags().containsKey("highway")
						&& !asList("path", "footway", "platform").contains(way.getTags().getValue("highway"))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Prepare a {@link TrafficSignTypeInstance} object to (usually) be added in a {@link TrafficSignModel}
	 */
	private TrafficSignTypeInstance configureSignTypeInstance(TrafficSignIdentifier signId, TagSet tagsOfElement) {

		/* prepare map with subtype and/or brackettext values */

		Map<String, String> map = new HashMap<>();

		if (signId.bracketText != null) {
			map.put("traffic_sign.brackettext", signId.bracketText);
		}
		if (signId.subType() != null) {
			map.put("traffic_sign.subtype", signId.subType());
		}

		/* load information about this type of sign from the configuration */

		TrafficSignType type = TrafficSignType.fromConfig(signId, config);
		if (type == null) { type = TrafficSignType.blankSign(); }

		/* build the model */

		return new TrafficSignTypeInstance(
				type.material.withPlaceholdersFilledIn(map, tagsOfElement),
				type.defaultNumPosts, type.defaultHeight);

	}

	/**
	 * Finds the junction closest to {@code node}.
	 * The junction must be reachable by traveling forward or backward on the road {@code node} is part of.
	 * A node with 3 or more roads connected to it is considered a junction.
	 *
	 * @param node  must not be a junction itself; != null
	 * @returns  the node representing the closest junction, or null if none was found
	 */
	public static @Nullable MapNode findClosestJunction(MapNode node) {

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
	public static MapWaySegment getAdjacentSegment(MapWay way, MapNode intersection) {

		List<MapWaySegment> waySegments = way.getWaySegments(); //get the way's segments

		for (MapWaySegment segment : asList(waySegments.get(0), waySegments.get(waySegments.size() - 1))) {

			if (segment.getStartNode().equals(intersection) || segment.getEndNode().equals(intersection)) {
				return segment;
			}
		}

		return null;
	}

	private final class DestinationSign extends NoOutlineNodeWorldObject implements Renderable {

		//relation attributes
		private Color backgroundColor;
		private Color textColor;

		//calculated value
		private double rotation;

		private MapRelation relation;

		private ArrayList<TrafficSignTypeInstance> signInstances = new ArrayList<>();
		private double postRadius;

		public DestinationSign(MapNode node) {

			super(node);

			//default values
			this.backgroundColor = Color.WHITE;
			this.textColor = Color.BLACK;
			this.postRadius = config.getDouble("standardPoleRadius", 0.05);

			determineDestinationSignMaterial();
		}

		private void determineDestinationSignMaterial() {

			//collection with the relations this node is part of
			Collection<Membership> membList = node.getMemberships();

			for (Membership m : membList) {

				String pointingDirection;

				//relation attributes
				MapWay from = null;
				MapWay to = null;
				MapNode intersection = null;
				String destination;
				String distance;
				String arrowColour;

				MapWaySegment toSegment = null;
				MapWaySegment fromSegment = null;

				this.relation = m.getRelation();

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
						this.backgroundColor = (Color) field.get(null);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				//parse text color
				if (relation.getTags().getValue("colour:text") != null) {

					Field field;

					try {
						field = Color.class.getField(relation.getTags().getValue("colour:text"));
						this.textColor = (Color) field.get(null);

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
							System.err.println("'from' member of relation " + this.relation + " is not a way."
									+ " It is not being considered for rendering this relation's destination sign");
							wrongFrom = true;
							continue;
						}

					} else if (member.getRole().equals("to")) {

						if (member.getElement() instanceof MapWay) {
							to = (MapWay) member.getElement();
						} else {
							System.err.println("'to' member of relation " + this.relation + " is not a way."
									+ " It is not being considered for rendering this relation's destination sign");
							continue;
						}

					} else if (member.getRole().equals("intersection")) {

						if (!(member.getElement() instanceof MapNode)) {
							System.err.println("'intersection' member of relation " + this.relation + " is not a node."
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

				this.rotation = facingDir.angle();

				//'to' member is also a way
				toSegment = getAdjacentSegment(to, intersection);

				if (toSegment == null) {
					System.err
							.println("Way " + to + " is not connected to intersection " + intersection + ". Returning");
					continue;
				}

				//explicit direction definition overwrites from-derived facing direction
				if (node.getTags().containsKey("direction")) {

					this.rotation = parseDirection(node.getTags(), PI);
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
				material = material.withColor(this.backgroundColor);

				//set material text color
				material = material.withTextColor(this.textColor, 1);

				TrafficSignTypeInstance signInstance = new TrafficSignTypeInstance(material,
						type.defaultNumPosts, type.defaultHeight);
				signInstances.add(signInstance);

			}
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target target) {

			/* get basic parameters */

			if (signInstances.size() == 0)
				return;

			double height = parseHeight(node.getTags(), (float) signInstances.get(0).defaultHeight);

			double[] signHeights = new double[signInstances.size()];
			double[] signWidths = new double[signInstances.size()];

			for (int sign = 0; sign < signInstances.size(); sign++) {

				TextureData textureData = null;

				if (signInstances.get(sign).material.getNumTextureLayers() != 0) {
					textureData = signInstances.get(sign).material.getTextureLayers().get(0).baseColorTexture;
				}

				if (textureData == null) {
					signHeights[sign] = 0.6;
					signWidths[sign] = 0.6;
				} else {
					signHeights[sign] = textureData.height;
					signWidths[sign] = textureData.width;
				}

			}

			/* position the post(s) */

			int numPosts = signInstances.get(0).numPosts;

			List<VectorXYZ> positions = new ArrayList<>(numPosts);

			for (int i = 0; i < numPosts; i++) {
				double relativePosition = 0.5 - (i + 1) / (double) (numPosts + 1);
				positions.add(getBase().add(X_UNIT.mult(relativePosition * signWidths[0])));
			}

			/* create the front and back side of the sign */

			List<List<VectorXYZ>> signGeometries = new ArrayList<>();

			double distanceBetweenSigns = 0.1;
			double upperHeight = height;

			for (int sign = 0; sign < signInstances.size(); sign++) {

				double signHeight = signHeights[sign];
				double signWidth = signWidths[sign];

				List<VectorXYZ> vs = asList(
						getBase().add(+signWidth / 2, upperHeight, postRadius),
						getBase().add(+signWidth / 2, upperHeight - signHeight, postRadius),
						getBase().add(-signWidth / 2, upperHeight, postRadius),
						getBase().add(-signWidth / 2, upperHeight - signHeight, postRadius));

				signGeometries.add(vs);

				upperHeight -= signHeight + distanceBetweenSigns;
			}

			/* rotate the sign around the base to match the direction value */

			for (List<VectorXYZ> vs : signGeometries) {
				for (int i = 0; i < vs.size(); i++) {
					VectorXYZ v = vs.get(i);
					v = v.rotateVec(rotation, getBase(), VectorXYZ.Y_UNIT);
					vs.set(i, v);
				}
			}

			if (positions.size() > 1) { // if 1, the post is exactly on the base
				for (int i = 0; i < positions.size(); i++) {
					VectorXYZ v = positions.get(i);
					v = v.rotateVec(this.rotation, getBase(), VectorXYZ.Y_UNIT);
					positions.set(i, v);
				}
			}

			/* render the post(s) */

			for (VectorXYZ position : positions) {
				target.drawColumn(STEEL, null, position,
						height, postRadius, postRadius,
						false, true);
			}

			/* render the sign (front, then back) */

			for (int sign = 0; sign < signInstances.size(); sign++) {

				TrafficSignTypeInstance type = signInstances.get(sign);
				List<VectorXYZ> vs = signGeometries.get(sign);

				target.drawTriangleStrip(type.material, vs,
						texCoordLists(vs, type.material, STRIP_FIT));

				vs = asList(vs.get(2), vs.get(3), vs.get(0), vs.get(1));

				target.drawTriangleStrip(STEEL, vs,
						texCoordLists(vs, STEEL, STRIP_FIT));

			}
		}
	}

	private static final class TrafficSign extends NoOutlineNodeWorldObject implements Renderable {

		private final List<TrafficSignTypeInstance> types;
		private final Configuration config;
		private final double postRadius;
		private TrafficSignModel model;

		public TrafficSign(TrafficSignModel model, Configuration config) {

			super(model.node);

			this.model = model;
			this.config = config;
			this.postRadius = config.getDouble("standardPoleRadius", 0.05);
			this.types = model.types;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target target) {

			/* get basic parameters */

			double height = parseHeight(node.getTags(), (float) types.get(0).defaultHeight);

			double[] signHeights = new double[types.size()];
			double[] signWidths = new double[types.size()];

			for (int sign = 0; sign < types.size(); sign++) {

				TextureData textureData = null;

				if (types.get(sign).material.getNumTextureLayers() != 0) {
					textureData = types.get(sign).material.getTextureLayers().get(0).baseColorTexture;
				}

				if (textureData == null) {
					signHeights[sign] = 0.6;
					signWidths[sign] = 0.6;
				} else {
					signHeights[sign] = textureData.height;
					signWidths[sign] = textureData.width;
				}

			}

			/* position the post(s) */

			int numPosts = types.get(0).numPosts;

			List<VectorXYZ> positions = new ArrayList<>(numPosts);

			for (int i = 0; i < numPosts; i++) {

				double relativePosition = 0.5 - (i + 1) / (double) (numPosts + 1);

				positions.add(model.position.xyz(getBase().getY()).add(X_UNIT.mult(relativePosition * signWidths[0])));
			}

			/* create the front and back side of the sign */

			List<List<VectorXYZ>> signGeometries = new ArrayList<>();

			double distanceBetweenSigns = 0.1;
			double upperHeight = height;

			for (int sign = 0; sign < types.size(); sign++) {

				double signHeight = signHeights[sign];
				double signWidth = signWidths[sign];

				List<VectorXYZ> vs = asList(
						model.position.xyz(getBase().getY()).add(+signWidth / 2, upperHeight, postRadius),
						model.position.xyz(getBase().getY()).add(+signWidth / 2, upperHeight - signHeight, postRadius),
						model.position.xyz(getBase().getY()).add(-signWidth / 2, upperHeight, postRadius),
						model.position.xyz(getBase().getY()).add(-signWidth / 2, upperHeight - signHeight, postRadius));

				signGeometries.add(vs);

				upperHeight -= signHeight + distanceBetweenSigns;
			}

			/* rotate the sign around the base to match the direction */

			for (List<VectorXYZ> vs : signGeometries) {

				for (int i = 0; i < vs.size(); i++) {
					VectorXYZ v = vs.get(i);
					v = v.rotateVec(model.direction, model.position.xyz(getBase().getY()), VectorXYZ.Y_UNIT);
					vs.set(i, v);
				}

			}

			if (positions.size() > 1) { // if 1, the post is exactly on the base
				for (int i = 0; i < positions.size(); i++) {
					VectorXYZ v = positions.get(i);
					v = v.rotateVec(model.direction, model.position.xyz(getBase().getY()), VectorXYZ.Y_UNIT);
					positions.set(i, v);
				}
			}

			/* render the post(s) */

			for (VectorXYZ position : positions) {
				target.drawColumn(STEEL, null, position,
						height, postRadius, postRadius,
						false, true);
			}

			/* render the sign (front, then back) */

			for (int sign = 0; sign < types.size(); sign++) {

				TrafficSignTypeInstance type = types.get(sign);

				List<VectorXYZ> vs = signGeometries.get(sign);

				target.drawTriangleStrip(type.material, vs,
						texCoordLists(vs, type.material, STRIP_FIT));

				vs = asList(vs.get(2), vs.get(3), vs.get(0), vs.get(1));

				target.drawTriangleStrip(STEEL, vs,
						texCoordLists(vs, STEEL, STRIP_FIT));

			}
		}
	}
}
