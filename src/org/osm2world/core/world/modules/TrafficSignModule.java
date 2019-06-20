package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXYZ.X_UNIT;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextTextureData;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.modules.common.TrafficSignType;


/**
 * adds traffic signs to the world
 */
public class TrafficSignModule extends AbstractModule {
	
	@Override
	protected void applyToNode(MapNode node) {
		
		if (!node.getTags().containsKey("traffic_sign")) return;
		
		if (isInHighway(node)) return; //only exact positions (next to highway)
		
		/* split the traffic sign value into its components */
		
		String tagValue = node.getTags().getValue("traffic_sign");
		String[] countryAndSigns = tagValue.split(":", 2);
		
		if (countryAndSigns.length != 2) return;
		
		String country = countryAndSigns[0];
		
		String[] signs = countryAndSigns[1].split("[;,]");
		
		/* match each individual sign to the list of supported types */
		
		List<TrafficSignType> types = new ArrayList<TrafficSignType>(signs.length);
		
		String regex = null;
		Pattern pattern = null;
		Matcher matcher = null;
		
		for (String sign : signs) {
			
			Material signMaterial = null;
			
			sign.trim();
			sign = sign.replace('-', '_');
			
			regex = "274_(\\d+)";
			pattern = Pattern.compile(regex);		
			matcher = pattern.matcher(sign);
			
			if(matcher.matches()) { //if the sign is of DE:274-... type
				
				String speedLimit = matcher.group(1);
				ConfMaterial originalMaterial = SIGN_DE_274;
				TextTextureData layer = null;
				
				for(TextureData td : originalMaterial.getTextureDataList()) {
					if(td instanceof TextTextureData) {
						layer = (TextTextureData) td;
						break;
					}
				}
				
				signMaterial = configureMaterial(originalMaterial, speedLimit, layer);
			}
			
			regex = "1001_31\\[(\\d+.*)\\]";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(sign);
			
			if(matcher.matches()) { //if sign is of DE:1001-31[...] type
				
				String value = matcher.group(1);
				if(!value.contains("km")) {
					value = value+"km";
				}
				
				value = value.replaceAll("\\s", ""); //remove all spaces
				
				ConfMaterial originalMaterial = SIGN_DE_1001_31;
				TextTextureData layer = null;
				
				//TODO make this a separate method
				for(TextureData td : originalMaterial.getTextureDataList()) {
					if(td instanceof TextTextureData) {
						layer = (TextTextureData) td;
						break;
					}
				}
				
				signMaterial = configureMaterial(originalMaterial, value, layer);
			}
			
			if(signMaterial == null) {
				
				signMaterial = getMaterial("SIGN_"+country+"_"+sign);
				
				if(signMaterial==null) {
					//if there is no texture for the sign, create simple white sign
					signMaterial = new ConfMaterial(Interpolation.FLAT, Color.white);
				}
			}
			types.add(new TrafficSignType(signMaterial,1,2));	
		}
		
		/* create a visual representation for the traffic sign */
		
		if (types.size() > 0) {
			node.addRepresentation(new TrafficSign(node, types));
		}
				
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
	 * Creates a replica of originalMaterial with a new textureDataList. 
	 * The new list is a copy of the old one with the parameterized layer 
	 * replaced by a new TextTextureData instance of different text.
	 * Returns the new ConfMaterial created.
	 * 
	 * @param originalMaterial The ConfMaterial to replicate
	 * @param value The text of the new TextTextureData layer object
	 * @param layer The textureDataList layer to be replaced 
	 * @return a ConfMaterial identical to originalMaterial with its textureDataList altered
	 */
	private Material configureMaterial(ConfMaterial originalMaterial, String value, TextTextureData layer) {
		
		if(layer == null) return originalMaterial;
		
		Material newMaterial = null;
		int index = originalMaterial.getTextureDataList().indexOf(layer);
				
		TextTextureData textData = new TextTextureData(value, layer.font, layer.width, 
						layer.height, layer.topOffset, layer.leftOffset, layer.wrap, 
						layer.coordFunction, layer.colorable, layer.isBumpMap);
				
		List<TextureData> newList = new ArrayList<TextureData>(originalMaterial.getTextureDataList());
				
		newList.set(index, textData);
				
		newMaterial = new ConfMaterial(originalMaterial.getInterpolation(),originalMaterial.getColor(),
						originalMaterial.getAmbientFactor(),originalMaterial.getDiffuseFactor(),originalMaterial.getSpecularFactor(),
						originalMaterial.getShininess(),originalMaterial.getTransparency(),originalMaterial.getShadow(),
						originalMaterial.getAmbientOcclusion(),newList);
			
			
		return newMaterial;
	}
	
	private static final class TrafficSign extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
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
		public void renderTo(Target<?> target) {

			/* get basic parameters */

			double height = parseHeight(node.getTags(), (float)types.get(0).defaultHeight);
			double postRadius = 0.05;
			
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

