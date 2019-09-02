package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXYZ.X_UNIT;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.target.common.material.Materials.getMaterial;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseDirection;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.MapRelation.Membership;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextTextureData;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.modules.common.TrafficSignModel;
import org.osm2world.core.world.modules.common.TrafficSignType;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RoadModule.Road;

/**
 * adds traffic signs to the world
 */
public class TrafficSignModule extends AbstractModule {

	/**
	 * Checks if {@code node} is part of any type=destination_sign relationships.
	 * Adds destination sign world representation if it is
	 */
	private void createDestinationSign(MapNode node) {

		//collection with the relations this node is part of
		Collection<Membership> membList = node.getMemberships();

		for(Membership m : membList) {

			if(m.getRelation().getTags().contains("type", "destination_sign") && m.getRole().equals("sign")) {

				node.addRepresentation(new DestinationSign(node));
				return;
			}
		}

		return;
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
		TrafficSignType attributes;

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
		TagGroup wayTags = way.getTags();

		if(wayTags.containsAnyKey(asList("maxspeed", "overtaking", "maxwidth", "maxheight", "maxweight", "traffic_sign", "traffic_sign:backward", "traffic_sign:forward"))) {

			List<String> containedKeys = wayTags.containsWhichKeys(asList("maxspeed", "overtaking", "maxwidth", "maxheight", "maxweight", "traffic_sign", "traffic_sign:backward", "traffic_sign:forward"));
			List<String> keysForFirstModel = new ArrayList<>();
			List<String> keysForSecondModel = new ArrayList<>();

			if(!containedKeys.isEmpty()) {

				String tempKey = "";

				//determine rendering positions
				for(String key : containedKeys) {

					//if a way adjacent to the beginning of this way does not contain the same traffic sign
					if(!TrafficSignModel.adjacentWayContains(true, way, key, wayTags.getValue(key)) && !key.contains("backward")) {

						boolean renderMe = true;

						//when at the beginning of the way, handle the sign
						//as it was mapped as sign:forward (affect vehicles moving at the same
						//direction as the way)
						if(!key.contains("forward")) {
							tempKey = key + ":forward";

							//check the way adjacent to the beginning of this way for sign mapped with forward direction
							if(TrafficSignModel.adjacentWayContains(true, way, tempKey, wayTags.getValue(key))){

								renderMe = false;
							}

						}else {

							//check the way adjacent to the beginning of this way for sign mapped without direction specified
							tempKey = key.substring(0, key.indexOf(':'));

							if(TrafficSignModel.adjacentWayContains(true, way, tempKey, wayTags.getValue(key))){

								renderMe = false;
							}

							tempKey = key;
						}

						if(renderMe) {

							//get first way segment
							MapWaySegment topSegment = way.getWaySegments().get(0);

							//new TrafficSignModel at the beginning of the way
							firstModel = new TrafficSignModel(topSegment.getStartNode());

							//get segment's driving side
							side = RoadModule.hasRightHandTraffic(topSegment);

							//set the model's facing direction
							firstModel.calculateDirection(topSegment, tempKey);

							//Avoid the case where the first node of the way may be a junction
							if(topSegment.getStartNode().getConnectedWaySegments().size()<3) {
								firstModel.calculatePosition(topSegment, side, tempKey, false);
							}else {

								if(RoadModule.isRoad(wayTags)) {
									VectorXZ offset = ((Road)topSegment.getPrimaryRepresentation()).getStartOffset();
									firstModel.position = firstModel.node.getPos().add(offset);
								}else {
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
					if(!TrafficSignModel.adjacentWayContains(false, way, key, wayTags.getValue(key)) && !RoadModule.isOneway(way.getTags())
							&& !state.equalsIgnoreCase("limited") && !key.contains("forward")) {

						boolean renderMe = true;

						//when at the end of the way, handle traffic sign
						//as it was mapped as sign:backward
						if(!key.contains("backward")) {
							tempKey = key + ":backward";

							//check the way adjacent to the beginning of this way for sign mapped with forward direction
							if(TrafficSignModel.adjacentWayContains(false, way, tempKey, wayTags.getValue(key))){
								//continue;
								renderMe = false;
							}

						}else {

							//check the way adjacent to the beginning of this way for sign mapped without direction specified
							tempKey = key.substring(0, key.indexOf(':'));

							if(TrafficSignModel.adjacentWayContains(false, way, tempKey, wayTags.getValue(key))){
								//continue;
								renderMe = false;
							}

							tempKey = key;
						}

						if(renderMe) {

							//get number of segments from the way this node is part of
							int numOfSegments = way.getWaySegments().size();

							//get last way segment
							MapWaySegment bottomSegment = way.getWaySegments().get(numOfSegments-1);

							secondModel = new TrafficSignModel(bottomSegment.getEndNode());

							//get segment's driving side
							side = RoadModule.hasRightHandTraffic(bottomSegment);

							secondModel.calculateDirection(bottomSegment, tempKey);

							/*
							 * new TrafficSignModel at the end of the way.
							 * Avoid the case where the last node of the way may be a junction
							 */
							if(bottomSegment.getEndNode().getConnectedWaySegments().size()<3) {

								secondModel.calculatePosition(bottomSegment, side, tempKey, false);

							}else {

								if(RoadModule.isRoad(wayTags)) {
									VectorXZ offset = ((Road)bottomSegment.getPrimaryRepresentation()).getEndOffset();
									secondModel.position = secondModel.node.getPos().add(offset);
								}else {
									secondModel.position = secondModel.node.getPos().add(DEFAULT_OFFSET_VECTOR);
								}
							}

							keysForSecondModel.add(key);
						}
					}
				}

				signs = keysForFirstModel.toArray(new String[0]);
				signsForSecondModel = keysForSecondModel.toArray(new String[0]);

				List<TrafficSignType> types = new ArrayList<>(containedKeys.size());
				List<TrafficSignType> typesForSecondSign = new ArrayList<>(keysForSecondModel.size());

				//configure the traffic sign types

				for (String sign : signs) {

					/* only take into account overtaking=no tags for "overtaking" key */
					if(sign.equals("overtaking") && !wayTags.getValue(sign).equals("no")) {
						continue;
					}

					//distinguish between human-readable values and traffic sign IDs
					tagValue = wayTags.getValue(sign);

					if(tagValue.contains(":")) {

						String[] countryAndSigns = tagValue.split(":", 2);
						if(countryAndSigns.length !=2) continue;
						country = countryAndSigns[0];

						sign = countryAndSigns[1];

					}else {
						country = "";
					}

					attributes = configureSignType(country, sign, wayTags);

					types.add(attributes);

				}

				for(String sign : signsForSecondModel) {

					/* only take into account overtaking=no tags for "overtaking" key */
					if(sign.equals("overtaking") && !wayTags.getValue(sign).equals("no")) {
						continue;
					}

					//distinguish between human-readable values and traffic sign IDs
					tagValue = wayTags.getValue(sign);

					if(tagValue.contains(":")) {

						String[] countryAndSigns = tagValue.split(":", 2);
						if(countryAndSigns.length !=2) continue;
						country = countryAndSigns[0];

						sign = countryAndSigns[1];

					}else {
						country = "";
					}

					attributes = configureSignType(country, sign, wayTags);

					typesForSecondSign.add(attributes);
				}

				if (types.size() > 0) {

					if(firstModel!=null) {

						firstModel.types = types;
						node.addRepresentation(new TrafficSign(firstModel, config));
					}
				}
				if(typesForSecondSign.size() > 0) {

					if(secondModel!=null) {

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

		if (!node.getTags().containsAnyKey(asList("traffic_sign", "traffic_sign:forward", "traffic_sign:backward")) &&
				!node.getTags().containsAny("highway", asList("give_way", "stop"))) return;

		String[] signs = {};
		String country = "";
		String tagValue = "";

		//a TrafficSignType instance to hold the sign's attributes
		TrafficSignType attributes;

		// Value to hold the angle the sign is facing
		Double direction;

		//The (potential) signs to be rendered
		TrafficSignModel firstModel = null;
		TrafficSignModel secondModel = null;

		//this variable is used because isInHighway needs to be called more than once
		boolean inHighway = isInHighway(node);

		//if node is part of a way
		if(inHighway) {

			String tempKey = "";

			//one rendering is required
			firstModel = new TrafficSignModel(node);

			if(node.getTags().containsKey("traffic_sign:forward")) {
				tagValue = node.getTags().getValue("traffic_sign:forward");
				tempKey = "traffic_sign:forward";
			}else if(node.getTags().containsKey("traffic_sign:backward")){
				tagValue = node.getTags().getValue("traffic_sign:backward");
				tempKey = "traffic_sign:backward";
			}else if(node.getTags().containsKey("traffic_sign")){
				tagValue = node.getTags().getValue("traffic_sign");
				tempKey = "traffic_sign:forward";
			}else {
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

			if(node.getTags().contains("side", "both")) {

				//if side=both , create another model instance and place them to the left & right of the road

				secondModel = new TrafficSignModel(node);

				secondModel.direction = firstModel.direction;
				secondModel.calculatePosition(segment, !side, tempKey, true);
			}

		}else {

			//if sign is explicitly mapped on node next to a way

			if(node.getTags().containsKey("traffic_sign")) tagValue = node.getTags().getValue("traffic_sign");

			firstModel = new TrafficSignModel(node);
			firstModel.position = node.getPos();

			direction = parseDirection(node.getTags(), PI);
			firstModel.direction = direction;
		}

		/* split the traffic sign value into its components */

		if(!tagValue.isEmpty()) {

			if(tagValue.contains(":")) {

				//if country prefix is used
				String[] countryAndSigns = tagValue.split(":", 2);
				if(countryAndSigns.length !=2) return;
				country = countryAndSigns[0];
				signs = countryAndSigns[1].split("[;,]");

			} else {

				//human-readable value
				signs = tagValue.split("[;,]");
			}
		}

		//handle the special cases highway=give_way/stop
		String[] specialCaseSigns = null;

		if(node.getTags().containsAny("highway", asList("give_way", "stop"))) {

			String value = node.getTags().getValue("highway");

			//check if value is already contained in signs[]
			boolean contains = Arrays.stream(signs).anyMatch(value::equals);

			if(!contains) {

				specialCaseSigns = new String[] {value};

				//concatenate the two arrays
				signs = (String[]) ArrayUtils.addAll(signs, specialCaseSigns);
			}
		}

		List<TrafficSignType> types = new ArrayList<TrafficSignType>(signs.length);

		for (String sign : signs) {

			attributes = configureSignType(country, sign, node.getTags());

			types.add(attributes);
		}

		/* create a visual representation for the traffic sign */

		if (types.size() > 0) {

			if(firstModel!=null) {
				firstModel.types = types;
				node.addRepresentation(new TrafficSign(firstModel, config));
			}

			if(secondModel!=null) {
				secondModel.types = types;
				node.addRepresentation(new TrafficSign(secondModel, config));
			}
		}
	}

	@Override
	protected void applyToNode(MapNode node) {

		String signsOnWaysState = config.getString("deduceTrafficSignsFromWayTags", "yes");

		/* if sign is a destination sign */

		if(!node.getMemberships().isEmpty()) {
			createDestinationSign(node);
		}

		/* if "traffic_sign = *" or any of the human-readable signs (maxspeed|overtaking|maxwidth|maxweight|maxheight) are mapped as a way tag */

		if(!node.getConnectedWaySegments().isEmpty() && !signsOnWaysState.equalsIgnoreCase("no")) {
			createSignFromTagOnWay(node, signsOnWaysState);
		}

		/* if sign is a simple traffic sign mapped on a node (not a destination sign or a human-readable value mapped on a way) */

		createSignFromNode(node);

	}

	private static boolean isInHighway(MapNode node){
		if (node.getConnectedWaySegments().size()>0){
			for(MapWaySegment way: node.getConnectedWaySegments()){
				if( way.getTags().containsKey("highway") && !way.getTags().containsAny("highway", asList("path", "footway", "platform") ) ){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Prepare a {@link TrafficSignType} object to (usually) be added in a
	 * {@link TrafficSignModel}
	 */
	private TrafficSignType configureSignType(String country, String sign, /*MapElement elementToGetTagsFrom*/ TagGroup elementToGetTagsFrom) {

		sign.trim();
		sign = sign.replace('-', '_');

		/* extract subtype and/or brackettext values */

		Map<String, String> map = new HashMap<>();

		String regex = "[A-Za-z0-9]*\\.?\\d*_?(\\d+)?[A-Za-z]*(?:\\[(.*)\\])?"; //match every traffic sign

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher1 = pattern.matcher(sign);

		if(matcher1.matches()) {

			if(matcher1.group(1)!=null) {
				map.put("traffic_sign.subtype", matcher1.group(1));
			}

			if(matcher1.group(2)!=null) {

				String brackettext = matcher1.group(2).replace('_', '-'); //revert the previous replacement
				map.put("traffic_sign.brackettext", brackettext);

				//cut off value to get the actual sign name
				sign = sign.replace("["+matcher1.group(2)+"]", "");
			}
		}

		sign = sign.toUpperCase();

		ConfMaterial originalMaterial;

		String signName = generateSignName(country, sign);

		TrafficSignType attributes = mapSignAttributes(signName);

		/* "Fallback" functionality in case no attributes and material are defined for this sign name
		 *
		 * attributes.materialName.isEmpty() -> no "trafficSign_name_material = " defined for this sign
		 * getMaterial(signName)==null -> no material defined for this sign name
		 * map.containsKey("traffic_sign.subtype") -> sign name contains a subtype value (e.g. 50 in 274-50)
		 */
		if(attributes.materialName.isEmpty() && getMaterial(signName)==null && map.containsKey("traffic_sign.subtype")) {

			sign = sign.replace("_"+matcher1.group(1), ""); //cut off subtype value

			signName = generateSignName(country, sign);

			attributes = mapSignAttributes(signName); //retry with new sign name
		}

		if(!attributes.materialName.isEmpty()) {
			originalMaterial = getMaterial(attributes.materialName);
			attributes.material = configureMaterial(originalMaterial, map, elementToGetTagsFrom);
		}

		if(attributes.material==null) {

			//if there is no material configured for the sign, try predefined ones
			originalMaterial = getMaterial(signName);
			attributes.material = configureMaterial(originalMaterial, map, elementToGetTagsFrom);

			//if there is no material defined for the sign, create simple white sign
			if(attributes.material==null) {
				attributes.material = new ConfMaterial(Interpolation.FLAT, Color.white);
			}
		}

		if(attributes.defaultHeight==0) attributes.defaultHeight = config.getFloat("defaultTrafficSignHeight", 2);

		return attributes;
	}

	private String generateSignName(String country, String sign) {

		String signName = "";

		if(!country.isEmpty()) {
			signName = "SIGN_"+country+"_"+sign;
		}else {
			signName = "SIGN_"+sign;
		}

		return signName;
	}

	/**
	 * Parses configuration files for the traffic sign-specific keys
	 * trafficSign_NAME_numPosts|defaultHeight|material
	 *
	 * @param signName The sign name (country prefix and subtype/brackettext value)
	 * @return A TrafficSignType instance of the the parsed values
	 */
	private TrafficSignType mapSignAttributes(String signName) {

		String regex = "trafficSign_"+signName+"_(numPosts|defaultHeight|material)";

		String originalMaterial = "";
		int numPosts = 1;
		double defaultHeight = 0;

		Matcher matcher = null;

		@SuppressWarnings("unchecked")
		Iterator<String> keyIterator = config.getKeys();

		//parse traffic sign specific configuration values
		while (keyIterator.hasNext()) {

			String key = keyIterator.next();
			matcher = Pattern.compile(regex).matcher(key);

			if (matcher.matches()) {

				String attribute = matcher.group(1);

				if("material".equals(attribute)) {

					originalMaterial = config.getString(key, "").toUpperCase();

				} else if("numPosts".equals(attribute)) {

					numPosts = config.getInt(key, 1);

				} else if("defaultHeight".equals(attribute)) {

					defaultHeight = config.getDouble(key, 0);
				}
			}
		}

		return new TrafficSignType(originalMaterial, numPosts, defaultHeight);
	}

	/**
	 * Creates a replica of originalMaterial with a new textureDataList.
	 * The new list is a copy of the old one with its TextTextureData layers
	 * replaced by a new TextTextureData instance of different text.
	 * Returns the new ConfMaterial created.
	 *
	 * @param originalMaterial The ConfMaterial to replicate
	 * @param map A HashMap used to map each of traffic_sign.subtype / traffic_sign.brackettext
	 * to their corresponding values
	 * @param tags The MapElement object to extract values from
	 * @return a ConfMaterial identical to originalMaterial with its textureDataList altered
	 */
	public static Material configureMaterial(ConfMaterial originalMaterial, Map<String, String> map, TagGroup tags) {

		if(originalMaterial == null) return null;

		Material newMaterial = null;
		List<TextureData> newList = new ArrayList<TextureData>(originalMaterial.getTextureDataList());

		String regex = ".*(%\\{(.+)\\}).*";
		Pattern pattern = Pattern.compile(regex);

		for(TextureData layer : originalMaterial.getTextureDataList()) {

			if(layer instanceof TextTextureData) {

				String newText = "";
				Matcher matcher = pattern.matcher(((TextTextureData) layer).text);
				int index = originalMaterial.getTextureDataList().indexOf(layer);

				if( matcher.matches() && !((TextTextureData) layer).text.isEmpty()) {

					newText = ((TextTextureData)layer).text;

					while(matcher.matches()) {

						if(tags.containsKey(matcher.group(2))) {

							newText = newText.replace(matcher.group(1), tags.getValue(matcher.group(2)));

						} else if(!map.isEmpty()) {

							//TODO: implement that with streams
							boolean matched = false;

							for (String key : map.keySet()) {
								if(key.equals(matcher.group(2))) {
									newText = newText.replace(matcher.group(1), map.get(matcher.group(2)));
									matched = true;
								}
							}

							if(!matched) newText = newText.replace(matcher.group(1), "");

						} else {
							System.err.println("Unknown attribute: "+matcher.group(2));
							newText = newText.replace(matcher.group(1), "");
						}

						matcher = pattern.matcher(newText);
					}

					TextTextureData textData = new TextTextureData(newText, ((TextTextureData)layer).font, layer.width,
							layer.height, ((TextTextureData)layer).topOffset, ((TextTextureData)layer).leftOffset,
							((TextTextureData)layer).textColor, ((TextTextureData) layer).relativeFontSize,
							layer.wrap, layer.coordFunction, layer.colorable, layer.isBumpMap);

					newList.set(index, textData);
				}
			}
		}

		newMaterial = new ConfMaterial(originalMaterial.getInterpolation(),originalMaterial.getColor(),
						originalMaterial.getAmbientFactor(),originalMaterial.getDiffuseFactor(),originalMaterial.getSpecularFactor(),
						originalMaterial.getShininess(),originalMaterial.getTransparency(),originalMaterial.getShadow(),
						originalMaterial.getAmbientOcclusion(),newList);

		return newMaterial;
	}

	/**
	 * Finds the junction closest to {@code node}.
	 * A node with 3 or more roads connected to it is considered
	 * a junction.
	 */
	public static MapNode findClosestJunction(MapNode node) {

		int numOfConnectedRoads = RoadModule.getConnectedRoads(node, false).size();

		if(numOfConnectedRoads>2) {
			throw new IllegalArgumentException("Node "+node.getOsmElement().getId()+" itself is a junction");
		}

		//hold the junctions found
		List<MapNode> foundJunctions = new ArrayList<>();

		//the way segment we are currently "working with"
		MapWaySegment currentSegment = RoadModule.getConnectedRoads(node, false).get(0).segment;

		//the node we are currently "working with"
		MapNode currentNode = node;

		boolean otherWayChecked = false;

		while(numOfConnectedRoads<=2) {

			currentNode = currentSegment.getOtherNode(currentNode);

			//get the number of roads connected to the current node
			numOfConnectedRoads = RoadModule.getConnectedRoads(currentNode, false).size();

			if(numOfConnectedRoads==1) {

				//dead end, try the other way (if it exists and is not already checked)

				if(RoadModule.getConnectedRoads(node, false).size()>1 && !otherWayChecked) {

					currentSegment = RoadModule.getConnectedRoads(node, false).get(1).segment;
					otherWayChecked=true;
					currentNode = node;
					numOfConnectedRoads = RoadModule.getConnectedRoads(currentNode, false).size();

				}else {

					break;
				}

			}else if(numOfConnectedRoads>=3){

				//junction found, try the other way (if it exists and is not already checked)

				foundJunctions.add(currentNode);

				if(RoadModule.getConnectedRoads(node, false).size()>1 && !otherWayChecked) {

					currentSegment = RoadModule.getConnectedRoads(node, false).get(1).segment;
					otherWayChecked=true;
					currentNode = node;
					numOfConnectedRoads = RoadModule.getConnectedRoads(currentNode, false).size();

				}else{
					break;
				}

			}else {

				if(currentSegment.equals(RoadModule.getConnectedRoads(currentNode, false).get(1).segment)) {
					currentSegment = RoadModule.getConnectedRoads(currentNode, false).get(0).segment;
				}else {
					currentSegment = RoadModule.getConnectedRoads(currentNode, false).get(1).segment;
				}
			}
		}

		if(foundJunctions.size()==0) {
			return null;
		}

		return foundJunctions.stream().min((Comparator.comparingDouble(j -> j.getPos().distanceTo(node.getPos())))).get();
	}

	/**
	 * find the segment of {@code way} whose start (or end) node
	 * is the {@code intersection} . Returns the found segment,
	 * null if not found.
	 */
	public static MapWaySegment getAdjacentSegment(MapWay way, MapNode intersection) {

		List<MapWaySegment> waySegments = way.getWaySegments(); //get the way's segments

		for(MapWaySegment segment : asList(waySegments.get(0), waySegments.get(waySegments.size()-1))) {

			if(segment.getStartNode().equals(intersection) || segment.getEndNode().equals(intersection)) {
				return segment;
			}
		}

		return null;
	}

	private final class DestinationSign extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {

		//relation attributes
		private Color backgroundColor;
		private Color textColor;

		//calculated value
		private double rotation;

		private MapRelation relation;

		private ArrayList<TrafficSignType> types = new ArrayList<TrafficSignType>();
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

			for(Membership m : membList) {

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
				ConfMaterial originalMat = null;

				destination = relation.getTags().getValue("destination");
				if(destination==null) {
					System.err.println("Destination can not be null. Destination sign rendering ommited for relation "+relation);
					continue;
				}
				distance = relation.getTags().getValue("distance");

				//parse background color
				if(relation.getTags().getValue("colour:back")!=null) {

					Field field;

					try {
						field = Color.class.getField(relation.getTags().getValue("colour:back"));
						this.backgroundColor = (Color) field.get(null);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				//parse text color
				if(relation.getTags().getValue("colour:text")!=null) {

					Field field;

					try {
						field = Color.class.getField(relation.getTags().getValue("colour:text"));
						this.textColor = (Color) field.get(null);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				arrowColour = relation.getTags().getValue("colour:arrow");
				if(arrowColour==null) arrowColour = "BLACK";

				//map to use in configureMaterial
				HashMap<String, String> map = new HashMap<String, String>();

				map.put("destination", destination);
				if(distance!=null) map.put("distance", distance);

				//List to hold all "from" members of the relation (if multiple)
				List<MapWay> fromMembers = new ArrayList<>();

				//variable to avoid calculating 'from' using vector from "sign" to "intersection"
				//if fromMembers size is 0 because of not acceptable mapping
				boolean wrongFrom = false;

				for(Membership member : relation.getMemberships()) {

					if(member.getRole().equals("from")) {

						if(member.getElement() instanceof MapWay) {
							fromMembers.add((MapWay)member.getElement());
						}else {
							System.err.println("'from' member of relation "+
									this.relation.getOsmElement().getId()+" is not a way. It is not being considered for rendering this relation's destination sign");
							wrongFrom = true;
							continue;
						}

					}else if(member.getRole().equals("to")) {

						if(member.getElement() instanceof MapWay) {
							to = (MapWay)member.getElement();
						}else {
							System.err.println("'to' member of relation "+
									this.relation.getOsmElement().getId()+" is not a way. It is not being considered for rendering this relation's destination sign");
							continue;
						}

					}else if(member.getRole().equals("intersection")) {

						if(!(member.getElement() instanceof MapNode)) {
							System.err.println("'intersection' member of relation "+
									this.relation.getOsmElement().getId()+" is not a node. It is not being considered for rendering this relation's destination sign");
							continue;
						}

						intersection = (MapNode) member.getElement();
					}
				}

				//check intersection first as it is being used in 'from' calculation below
				if(intersection==null) {
					System.err.println("Member 'intersection' was not defined in relation "+relation.getOsmElement().getId()+
							". Destination sign rendering is ommited for this relation.");
					continue;
				}

				/* if there are multiple "from" members or none at all,
				use the vector from "sign" to "intersection" instead */

				if(fromMembers.size() > 1 || (fromMembers.size() == 0 && !wrongFrom)) {
					List<MapNode> signAndIntersection = asList(
							node,
							intersection);

					//create a new MapWay instance from sign node to intersection node
					MapWay signToIntersection = new MapWay(null, signAndIntersection);
					from = signToIntersection;

				}else if(fromMembers.size()==1) {

					from = fromMembers.get(0);
				}

				//check if the rest of the "vital" relation members where defined
				if(from==null || to==null) {
					System.err.println("not all members of relation "+
							relation.getOsmElement().getId()+" where defined. Destination sign rendering is ommited for this relation.");
					continue;
				}

				//get facing direction

				boolean mustBeInverted = false;

				//'from' member is a way
				fromSegment = getAdjacentSegment(from, intersection);

				if(fromSegment!=null) {
					if(fromSegment.getEndNode().equals(intersection)) {
						mustBeInverted = true;
					}
				}else {
					System.err.println("Way "+from+" is not connected to intersection "+intersection);
					continue;
				}

				VectorXZ facingDir = fromSegment.getDirection();

				//if the segment is facing towards the intersection, invert its direction vector
				if(mustBeInverted) facingDir = facingDir.invert();

				this.rotation = facingDir.angle();

				//'to' member is also a way
				toSegment = getAdjacentSegment(to, intersection);

				if(toSegment==null) {
					System.err.println("Way "+to+" is not connected to intersection "+intersection+". Returning");
					continue;
				}

				//explicit direction definition overwrites from-derived facing direction
				if(node.getTags().containsKey("direction")) {

					this.rotation = parseDirection(node.getTags(), PI);
				}

				//get the other ends of the segments
				MapNode otherToSegmentNode = toSegment.getOtherNode(intersection);
				MapNode otherFromSegmentNode = fromSegment.getOtherNode(intersection);

				boolean isRightOf = GeometryUtil.isRightOf(otherToSegmentNode.getPos(), otherFromSegmentNode.getPos(), intersection.getPos());

				if(isRightOf) {
					pointingDirection = "RIGHT";
				}else {
					pointingDirection = "LEFT";
				}

				//parse traffic sign specific configurable values: material,numPosts,defaultHeight
				String signName = "SIGN_DESTINATION_SIGN_"+arrowColour.toUpperCase()+"_"+pointingDirection.toUpperCase();

				TrafficSignType attributes = mapSignAttributes(signName);

				//if trafficSign_signName_material = * is defined
				if(!attributes.materialName.isEmpty()) {

					originalMat = getMaterial(attributes.materialName);
					attributes.material = configureMaterial(originalMat, map, relation.getTags());
				}

				if(attributes.material==null) {

					//if there is no material configured for the sign, try predefined ones
					originalMat = getMaterial(signName);

					attributes.material = configureMaterial(originalMat, map, relation.getTags());

					//if there is no material defined for the sign, create simple white sign
					if(attributes.material==null) {

						attributes.material = new ConfMaterial(Interpolation.FLAT, Color.white);
					}
				}

				//set material background color
				attributes.material = attributes.material.withColor(this.backgroundColor);

				//set material text color
				attributes.material = attributes.material.withTextColor(this.textColor, 1);

				//if trafficSign_signName_defaultHeight = *  was not defined
				if(attributes.defaultHeight==0) attributes.defaultHeight = config.getFloat("defaultTrafficSignHeight", 2);

				attributes.rotation = this.rotation;

				types.add(attributes);

			}
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

			System.out.println("DestinationSign rendered!!");

			/* get basic parameters */

			if(types.size()==0) return;

			double height = parseHeight(node.getTags(), (float)types.get(0).defaultHeight);

			double[] signHeights = new double[types.size()];
			double[] signWidths = new double[types.size()];

			for (int sign = 0; sign < types.size(); sign++) {

				TextureData textureData = null;

				if (types.get(sign).material.getNumTextureLayers() != 0) {
					textureData = types.get(sign).material.getTextureDataList().get(0);
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

			List<VectorXYZ> positions = new ArrayList<VectorXYZ>(numPosts);

			for (int i = 0; i < numPosts; i++) {
				double relativePosition = 0.5 - (i+1)/(double)(numPosts+1);
				positions.add(getBase().add(X_UNIT.mult(relativePosition * signWidths[0])));
			}

			/* create the front and back side of the sign */

			List<List<VectorXYZ>> signGeometries = new ArrayList<List<VectorXYZ>>();

			double distanceBetweenSigns = 0.1;
			double upperHeight = height;

			for (int sign = 0; sign < types.size(); sign++) {

				double signHeight = signHeights[sign];
				double signWidth = signWidths[sign];

				List<VectorXYZ> vs = asList(
						getBase().add(+signWidth/2, upperHeight, postRadius),
						getBase().add(+signWidth/2, upperHeight-signHeight, postRadius),
						getBase().add(-signWidth/2, upperHeight, postRadius),
						getBase().add(-signWidth/2, upperHeight-signHeight, postRadius)
						);

				signGeometries.add(vs);

				upperHeight -= signHeight + distanceBetweenSigns;
			}

			/* rotate the sign around the base to match the direction value */

			for (List<VectorXYZ> vs : signGeometries) {

				int index = signGeometries.indexOf(vs);
				double rotation = types.get(index).rotation;

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

			for (int sign = 0; sign < types.size(); sign++) {

				TrafficSignType type = types.get(sign);
				List<VectorXYZ> vs = signGeometries.get(sign);

				target.drawTriangleStrip(type.material, vs,
						texCoordLists(vs, type.material, STRIP_FIT));

				vs = asList(vs.get(2), vs.get(3), vs.get(0), vs.get(1));

				target.drawTriangleStrip(STEEL, vs,
						texCoordLists(vs, STEEL, STRIP_FIT));

			}
		}
	}

	private static final class TrafficSign extends NoOutlineNodeWorldObject
				implements RenderableToAllTargets {

		private final List<TrafficSignType> types;
		private final Configuration config;
		private final double postRadius ;
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
		public void renderTo(Target<?> target) {

			/* get basic parameters */

			double height = parseHeight(node.getTags(), (float)types.get(0).defaultHeight);

			double[] signHeights = new double[types.size()];
			double[] signWidths = new double[types.size()];

			for (int sign = 0; sign < types.size(); sign++) {

				TextureData textureData = null;

				if (types.get(sign).material.getNumTextureLayers() != 0) {
					textureData = types.get(sign).material.getTextureDataList().get(0);
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

			List<VectorXYZ> positions = new ArrayList<VectorXYZ>(numPosts);

			for (int i = 0; i < numPosts; i++) {

				double relativePosition = 0.5 - (i+1)/(double)(numPosts+1);

				positions.add(model.position.xyz(getBase().getY()).add(X_UNIT.mult(relativePosition * signWidths[0])));
			}

			/* create the front and back side of the sign */

			List<List<VectorXYZ>> signGeometries = new ArrayList<List<VectorXYZ>>();

			double distanceBetweenSigns = 0.1;
			double upperHeight = height;

			for (int sign = 0; sign < types.size(); sign++) {

				double signHeight = signHeights[sign];
				double signWidth = signWidths[sign];

				List<VectorXYZ> vs = asList(
						model.position.xyz(getBase().getY()).add(+signWidth/2, upperHeight, postRadius),
						model.position.xyz(getBase().getY()).add(+signWidth/2, upperHeight-signHeight, postRadius),
						model.position.xyz(getBase().getY()).add(-signWidth/2, upperHeight, postRadius),
						model.position.xyz(getBase().getY()).add(-signWidth/2, upperHeight-signHeight, postRadius)
				);

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

				TrafficSignType type = types.get(sign);

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
