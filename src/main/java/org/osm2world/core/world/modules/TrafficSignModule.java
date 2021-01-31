package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXYZ.X_UNIT;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.util.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.util.ValueParseUtil.parseColor;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.TextTexture;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.modules.common.TrafficSignType;


/**
 * adds traffic signs to the world
 */
public class TrafficSignModule extends AbstractModule {

	@Override
	protected void applyToNode(MapNode node) {

		/* if sign is a destination sign */

		if(!node.getMemberships().isEmpty()) {

			//for each relation this node is part of

			for(Membership m : node.getMemberships()) {

				if(m.getRelation().getTags().contains("type", "destination_sign") && m.getRole().equals("sign")) {

					node.addRepresentation(new DestinationSign(node));
					return;
				}
			}
		}

		if (!node.getTags().containsKey("traffic_sign")) return;

		if (isInHighway(node)) return; //only exact positions (next to highway)

		String tagValue = node.getTags().getValue("traffic_sign");

		String country = "";
		String signs[];
		TrafficSignType attributes;

		/* split the traffic sign value into its components */

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

		List<TrafficSignType> types = new ArrayList<TrafficSignType>(signs.length);

		String regex = null;
		Pattern pattern = null;
		Matcher matcher1 = null; //the matcher to match traffic sign values in .osm file

		for (String sign : signs) {

			//re-initialize the values below for every sign
			ConfMaterial originalMaterial = null;
			HashMap<String, String> map = new HashMap<String, String>();

			sign.trim();
			sign = sign.replace('-', '_');

			/* extract subtype and/or brackettext values */

			regex = "[A-Za-z0-9]*\\.?\\d*_?(\\d+)?[A-Za-z]*(?:\\[(.*)\\])?"; //match every traffic sign
			pattern = Pattern.compile(regex);
			matcher1 = pattern.matcher(sign);

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

			String signName = generateSignName(country, sign);

			attributes = mapSignAttributes(signName);

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
				attributes.material = configureMaterial(originalMaterial, map, node.getTags());
			}

			if(attributes.material==null) {

				//if there is no material configured for the sign, try predefined ones
				originalMaterial = getMaterial(signName);
				attributes.material = configureMaterial(originalMaterial, map, node.getTags());

				//if there is no material defined for the sign, create simple white sign
				if(attributes.material==null) {
					attributes.material = new ConfMaterial(Interpolation.FLAT, Color.white);
				}
			}

			if(attributes.defaultHeight==0) attributes.defaultHeight = config.getFloat("defaultTrafficSignHeight", 2);

			types.add(attributes);
		}

		/* create a visual representation for the traffic sign */

		if (types.size() > 0) {
			node.addRepresentation(new TrafficSign(node, types));
		}

	}

	private static boolean isInHighway(MapNode node){
		if (node.getConnectedWaySegments().size()>0){
			for(MapWaySegment way: node.getConnectedWaySegments()){
				if (way.getTags().containsKey("highway")
						&& !asList("path", "footway", "platform").contains(way.getTags().getValue("highway"))) {
					return true;
				}
			}
		}
		return false;
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
	 * Returns the new Material created.
	 *
	 * @param originalMaterial The ConfMaterial to replicate
	 * @param map A HashMap used to map each of traffic_sign.subtype / traffic_sign.brackettext
	 * to their corresponding values
	 * @param tags The tag group to extract values from
	 * @return a ConfMaterial identical to originalMaterial with its textureDataList altered
	 */
	public static Material configureMaterial(ConfMaterial originalMaterial, Map<String, String> map, TagSet tags) {

		if(originalMaterial == null) return null;

		Material newMaterial = null;
		List<TextureLayer> newList = new ArrayList<>(originalMaterial.getTextureLayers());

		String regex = ".*(%\\{(.+)\\}).*";
		Pattern pattern = Pattern.compile(regex);

		for(TextureLayer layer : originalMaterial.getTextureLayers()) {

			if(layer.baseColorTexture instanceof TextTexture) {

				TextTexture textTexture = (TextTexture) layer.baseColorTexture;

				String newText = "";
				Matcher matcher = pattern.matcher(textTexture.text);
				int index = originalMaterial.getTextureLayers().indexOf(layer);

				if( matcher.matches() ) {

					newText = textTexture.text;

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

					TextTexture newTextTexture = new TextTexture(newText, textTexture.font, textTexture.width,
							textTexture.height, textTexture.topOffset, textTexture.leftOffset,
							textTexture.textColor, textTexture.relativeFontSize,
							textTexture.wrap, textTexture.coordFunction);

					newList.set(index, new TextureLayer(newTextTexture,
							layer.normalTexture, layer.ormTexture, layer.displacementTexture, layer.colorable));
				}
			}
		}

		newMaterial = new ConfMaterial(originalMaterial.getInterpolation(),originalMaterial.getColor(),
						originalMaterial.isDoubleSided(), originalMaterial.getTransparency(),
						originalMaterial.getShadow(), originalMaterial.getAmbientOcclusion(), newList);

		return newMaterial;
	}

	public static MapWaySegment getAdjacentSegment(MapWay way, MapNode intersection) {

			List<MapWaySegment> waySegments = way.getWaySegments(); //get the way's segments

			for(MapWaySegment segment : asList(waySegments.get(0), waySegments.get(waySegments.size()-1))) {

				if(segment.getStartNode().equals(intersection) || segment.getEndNode().equals(intersection)) {
					return segment;
				}
			}

			return null;
		}

	private final class DestinationSign extends NoOutlineNodeWorldObject {

		//relation attributes
		private Color backgroundColor;
		private Color textColor;

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

			for(Membership m : node.getMemberships()) {

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
				if (relation.getTags().containsKey("colour:back")) {
					this.backgroundColor = parseColor(relation.getTags().getValue("colour:back"), CSS_COLORS);
					if (this.backgroundColor==null) this.backgroundColor = Color.WHITE;
				}

				//parse text color
				if (relation.getTags().containsKey("colour:text")) {
					this.textColor = parseColor(relation.getTags().getValue("colour:text"), CSS_COLORS);
					if(this.textColor==null) this.textColor = Color.BLACK;
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
							System.err.println("'from' member of relation " + this.relation + " is not a way."
									+ " It is not being considered for rendering this relation's destination sign.");
							wrongFrom = true;
							continue;
						}

					}else if(member.getRole().equals("to")) {

						if(member.getElement() instanceof MapWay) {
							to = (MapWay)member.getElement();
						}else {
							System.err.println("'to' member of relation " + this.relation + " is not a way. "
									+ "It is not being considered for rendering this relation's destination sign");
							continue;
						}

					}else if(member.getRole().equals("intersection")) {

						if(!(member.getElement() instanceof MapNode)) {
							System.err.println("'intersection' member of relation " + this.relation + " is not a node."
									+ " It is not being considered for rendering this relation's destination sign");
							continue;
						}

						intersection = (MapNode) member.getElement();
					}
				}


				//check intersection first as it is being used in 'from' calculation below
				if(intersection==null) {
					System.err.println("Member 'intersection' was not defined in relation " + relation
							+ ". Destination sign rendering is omitted for this relation.");
					continue;
				}

				/* if there are multiple "from" members or none at all,
				use the vector from "sign" to "intersection" instead */

				if(fromMembers.size() > 1 || (fromMembers.size() == 0 && !wrongFrom)) {
					List<MapNode> signAndIntersection = asList(
							node,
							intersection);

					//create a new MapWay instance from sign node to intersection node
					MapWay signToIntersection = new MapWay(-1, TagSet.of(), signAndIntersection);
					from = signToIntersection;

				}else if(fromMembers.size()==1) {

					from = fromMembers.get(0);
				}

				//check if the rest of the "vital" relation members where defined
				if(from==null || to==null) {
					System.err.println("not all members of relation " + relation
							+" were defined. Destination sign rendering is ommited for this relation.");
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
					return;
				}

				VectorXZ facingDir = fromSegment.getDirection();

				//if the segment is facing towards the intersection, invert its direction vector
				if(mustBeInverted) facingDir = facingDir.invert();

				this.rotation = facingDir.angle();

				//'to' member is also a way
				toSegment = getAdjacentSegment(to, intersection);

				if(toSegment==null) {
					System.err.println("Way "+to+" is not connected to intersection "+intersection+". Returning");
					return;
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
		public void renderTo(Target target) {

			/* get basic parameters */

			double height = parseHeight(node.getTags(), (float)types.get(0).defaultHeight);

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

	private static final class TrafficSign extends NoOutlineNodeWorldObject {

		private final List<TrafficSignType> types;

		public TrafficSign(MapNode node, List<TrafficSignType> types) {

			super(node);

			this.types = types;

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target target) {

			/* get basic parameters */

			double height = parseHeight(node.getTags(), (float)types.get(0).defaultHeight);
			double postRadius = 0.05;

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

			/* rotate the sign around the base to match the direction tag */

			double direction = parseDirection(node.getTags(), PI);

			for (List<VectorXYZ> vs : signGeometries) {

				for (int i = 0; i < vs.size(); i++) {
					VectorXYZ v = vs.get(i);
					v = v.rotateVec(direction, getBase(), VectorXYZ.Y_UNIT);
					vs.set(i, v);
				}

			}

			if (positions.size() > 1) { // if 1, the post is exactly on the base
				for (int i = 0; i < positions.size(); i++) {
					VectorXYZ v = positions.get(i);
					v = v.rotateVec(direction, getBase(), VectorXYZ.Y_UNIT);
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

