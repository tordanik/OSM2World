package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.wallTexCoordLists;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds various types of street furniture to the world
 */
public class StreetFurnitureModule extends AbstractModule {
	
	@Override
	protected void applyToNode(MapNode node) {
		if (node.getTags().contains("man_made", "flagpole")) {
			node.addRepresentation(new Flagpole(node));
		}
		if (node.getTags().contains("advertising", "column")) {
			node.addRepresentation(new AdvertisingColumn(node));
		}
		if (node.getTags().contains("advertising", "billboard")) {
			node.addRepresentation(new Billboard(node));
		}
		if (node.getTags().contains("amenity", "bench")) {
			node.addRepresentation(new Bench(node));
		}
		if (node.getTags().contains("highway", "bus_stop")) {
			node.addRepresentation(new BusStop(node));
		}
		
		if (node.getTags().contains("amenity", "waste_basket")) {
			node.addRepresentation(new WasteBasket(node));
		}
		if (node.getTags().contains("amenity", "grit_bin")) {
			node.addRepresentation(new GritBin(node));
		}
		if (node.getTags().contains("amenity", "post_box")
			&& (node.getTags().containsAnyKey(asList("operator", "brand")))) {
			node.addRepresentation(new PostBox(node));
		}
		if (node.getTags().contains("amenity", "telephone")
			&& (node.getTags().containsAnyKey(asList("operator", "brand")))) {
			node.addRepresentation(new Phone(node));
		}
		if (node.getTags().contains("amenity", "vending_machine")
			&& (node.getTags().containsAny("vending", asList("parcel_pickup;parcel_mail_in", "parcel_mail_in") ))) {
			node.addRepresentation(new ParcelMachine(node));
		}
		if (node.getTags().contains("amenity", "vending_machine")
				&& (node.getTags().containsAny("vending", asList("bicycle_tube", "cigarettes", "condoms")))) {
				node.addRepresentation(new VendingMachineVice(node));
		}
		if (node.getTags().contains("amenity", "recycling")
				&& (node.getTags().contains("recycling_type", "container"))) {
				node.addRepresentation(new RecyclingContainer(node));
		}
		if (node.getTags().contains("emergency", "fire_hydrant")
			&& node.getTags().contains("fire_hydrant:type", "pillar")) {
			node.addRepresentation(new FireHydrant(node));
		}
		if (node.getTags().contains("highway", "street_lamp")) {
			node.addRepresentation(new StreetLamp(node));
		}
	}
	
	private static boolean isInWall(MapNode node){
		if (node.getAdjacentAreas().size()>0){
			return true;
		} else {
			return false;
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
	
	private static final class Flagpole extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public Flagpole(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			target.drawColumn(STEEL, null,
					node.getElevationProfile().getWithEle(node.getPos()),
					parseHeight(node.getTags(), 10f),
					0.15, 0.15, false, true);
			
		}
		
	}
	
	private static final class AdvertisingColumn extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public AdvertisingColumn(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			float height = parseHeight(node.getTags(), 3f);
			
			/* draw socket, poster and cap */
			
			target.drawColumn(CONCRETE, null,
					node.getPos().xyz(ele),
					0.15 * height,
					0.5, 0.5, false, false);
			
			target.drawColumn(ADVERTISING_POSTER, null,
					node.getPos().xyz(ele),
					0.98 * height,
					0.48, 0.48, false, false);

			target.drawColumn(CONCRETE, null,
					node.getPos().xyz(ele + 0.95 * height),
					0.05 * height,
					0.5, 0.5, false, true);
						
		}
		
	}
	
	private static final class Billboard extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public Billboard(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			
			float width = parseWidth(node.getTags(), 4);
			float height = parseHeight(node.getTags(), 3.5f);
			float minHeight = height / 5;
						
			double directionAngle = parseDirection(node.getTags(), PI);
						
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();
			
			
			/* draw board */
			
			VectorXYZ[] vsPoster = {
				node.getPos().add(boardVector.mult(width/2))
					.xyz(ele + height),
				node.getPos().add(boardVector.mult(width/2))
					.xyz(ele + minHeight),
				node.getPos().add(boardVector.mult(-width/2))
					.xyz(ele + height),
				node.getPos().add(boardVector.mult(-width/2))
					.xyz(ele + minHeight)
			};
			
			List<VectorXYZ> vsListPoster = asList(vsPoster);
			
			target.drawTriangleStrip(ADVERTISING_POSTER, vsListPoster,
					wallTexCoordLists(vsListPoster, ADVERTISING_POSTER));
			
			VectorXYZ[] vsBoard = {
					vsPoster[2],
					vsPoster[3],
					vsPoster[0],
					vsPoster[1]
			};
			
			List<VectorXYZ> vsListBoard = asList(vsBoard);
									
			target.drawTriangleStrip(CONCRETE, vsListBoard,
					wallTexCoordLists(vsListBoard, CONCRETE));
			
			
			/* draw poles */
			
			VectorXZ[] poles = {
					node.getPos().add(boardVector.mult(-width/4)),
					node.getPos().add(boardVector.mult(+width/4))
			};
			
			for (VectorXZ pole : poles) {
				target.drawBox(CONCRETE,
						node.getElevationProfile().getWithEle(pole),
						faceVector, minHeight, 0.2, 0.1);
			}
			
		}
		
	}
	
	private static final class Bench extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public Bench(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			
			float width = parseWidth(node.getTags(), 1.5f);
			
			/* determine material */
			
			Material material = null;
			
			//TODO parse color
			
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}
				
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), Materials.WOOD);
			}
			
			/* calculate vectors and corners */

			double directionAngle = parseDirection(node.getTags(), PI);
			
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();
			
			List<VectorXZ> cornerOffsets = new ArrayList<VectorXZ>(4);
			cornerOffsets.add(faceVector.mult(+0.25).add(boardVector.mult(+width/2)));
			cornerOffsets.add(faceVector.mult(+0.25).add(boardVector.mult(-width/2)));
			cornerOffsets.add(faceVector.mult(-0.25).add(boardVector.mult(+width/2)));
			cornerOffsets.add(faceVector.mult(-0.25).add(boardVector.mult(-width/2)));
			
			/* draw seat and backrest */
			
			target.drawBox(material, node.getPos().xyz(ele + 0.5),
					faceVector, 0.05, width, 0.5);
			
			if (!node.getTags().contains("backrest", "no")) {
				
				target.drawBox(material,
						node.getPos().add(faceVector.mult(-0.23)).xyz(ele + 0.5),
						faceVector, 0.5, width, 0.04);
				
			}
						
			/* draw poles */
						
			for (VectorXZ cornerOffset : cornerOffsets) {
				VectorXZ polePos = node.getPos().add(cornerOffset.mult(0.8));
				target.drawBox(material,
						node.getElevationProfile().getWithEle(polePos),
						faceVector, 0.5, 0.08, 0.08);
			}
			
		}
		
	}
	
	private static final class RecyclingContainer extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		double directionAngle = parseDirection(node.getTags(), PI);
		VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
		
		public RecyclingContainer(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			float distanceX = 3f;
			float distanceZ = 1.6f;
			
			int n = -1;
			int m = 0;
			
			if (node.getTags().containsAny( asList("recycling:glass_bottles","recycling:glass"), "yes")){
				n++;
			}
			if (node.getTags().contains( "recycling:paper", "yes")){
				n++;
			}
			if (node.getTags().contains( "recycling:clothes", "yes")){
				n++;
			}
			
			if (node.getTags().contains( "recycling:paper", "yes")){
				drawContainer(target, "paper", node.getPos().xyz(ele).add(new VectorXYZ((distanceX*(-n/2 + m) ), 0f,  (distanceZ/2)).rotateY(faceVector.angle())));
				drawContainer(target, "paper", node.getPos().xyz(ele).add(new VectorXYZ((distanceX*(-n/2 + m) ), 0f,  -(distanceZ/2)).rotateY(faceVector.angle())));
				m++;
			}
			if (node.getTags().containsAny( asList("recycling:glass_bottles","recycling:glass" ), "yes")){
				drawContainer(target, "white_glass", node.getPos().xyz(ele).add(new VectorXYZ((distanceX*(-n/2 + m) ), 0f,  (distanceZ/2)).rotateY(faceVector.angle())));
				drawContainer(target, "coloured_glass", node.getPos().xyz(ele).add(new VectorXYZ((distanceX*(-n/2 + m) ), 0f,  -(distanceZ/2)).rotateY(faceVector.angle())));
				m++;
			}
			if (node.getTags().contains( "recycling:clothes", "yes")){
				drawContainer(target, "clothes", node.getPos().xyz(ele).add(new VectorXYZ((distanceX*(-n/2 + m) ), 0f,  0).rotateY(faceVector.angle())));
			}
			

			
		}
		
		private void drawContainer(Target<?> target, String trash, VectorXYZ pos){
			
			if (trash == "clothes") {
				target.drawBox(new ConfMaterial(Lighting.FLAT, new Color(0.82f, 0.784f, 0.75f)),
						pos,
						faceVector, 2, 1, 1);
			} else {//if(trash == "paper" || trash == "white_glass" || trash == "coloured_glass"){
				float width = 1.5f;
				float height = 1.6f;
				
				Material colourFront = null;
				Material colourBack = null;
				
				if (trash == "paper") {
					colourFront = new ConfMaterial(Lighting.FLAT, Color.BLUE);
					colourBack = new ConfMaterial(Lighting.FLAT, Color.BLUE);
				} else if (trash == "white_glass") {
					colourFront = new ConfMaterial(Lighting.FLAT, Color.WHITE);
					colourBack = new ConfMaterial(Lighting.FLAT, Color.WHITE);
				} else { // if trash == "coloured_glass"){
					colourFront = new ConfMaterial(Lighting.FLAT, new Color(0.18f, 0.32f, 0.14f));
					colourBack = new ConfMaterial(Lighting.FLAT, new Color(0.39f, 0.15f, 0.11f));
				}
			
				target.drawBox(STEEL,
						pos,
						faceVector, height, width, width);
				target.drawBox(colourFront,
						pos.add(new VectorXYZ((width/2-0.10),0.1f, (width/2-0.1)).rotateY(directionAngle )),
						faceVector, height-0.2, 0.202, 0.202);
				target.drawBox(colourBack,
						pos.add(new VectorXYZ(-(width/2-0.10),0.1f, (width/2-0.1)).rotateY(directionAngle )),
						faceVector, height-0.2, 0.202, 0.202);
				target.drawBox(colourFront,
						pos.add(new VectorXYZ((width/2-0.10),0.1f, -(width/2-0.1)).rotateY(directionAngle )),
						faceVector, height-0.2, 0.202, 0.202);
				target.drawBox(colourBack,
						pos.add(new VectorXYZ(-(width/2-0.10),0.1f, -(width/2-0.1)).rotateY(directionAngle )),
						faceVector, height-0.2, 0.202, 0.202);
			}
		}

		
	}
	
	private static final class WasteBasket extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public WasteBasket(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			
			/* determine material */
			
			Material material = null;
			
			//TODO parse color
			
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}
				
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), STEEL);
			}
			
			/* draw pole */
			target.drawColumn(material, null,
					node.getElevationProfile().getWithEle(node.getPos()),
					1.2, 0.06, 0.06, false, true);
			
			/* draw basket */
			target.drawColumn(material, null,
					node.getPos().xyz(ele + 0.5).add(0.25, 0f, 0f),
					0.5, 0.2, 0.2, true, true);
		}
		
	}

	private static final class GritBin extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
	
		public GritBin(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			
			float height = parseHeight(node.getTags(), 0.5f);
			float width = parseWidth(node.getTags(), 1);
			float depth = width / 2f;
			
			/* determine material */
			
			Material material = null;
			
			//TODO parse color
			
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}
				
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), Materials.GRITBIN_DEFAULT);
			}
			
			double directionAngle = parseDirection(node.getTags(), PI);
						
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();
			
			/* draw box */
			target.drawBox(material,
					node.getElevationProfile().getWithEle(node.getPos()),
					faceVector, height, width, depth);
			
			/* draw lid */
			List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
			vs.add(node.getPos().xyz(ele + height + 0.2));
			vs.add(node.getPos().add(boardVector.mult(width/2)).add(faceVector.mult(depth/2)).xyz(ele + height));
			vs.add(node.getPos().add(boardVector.mult(-width/2)).add(faceVector.mult(depth/2)).xyz(ele + height));
			vs.add(node.getPos().add(boardVector.mult(-width/2)).add(faceVector.mult(-depth/2)).xyz(ele + height));
			vs.add(node.getPos().add(boardVector.mult(width/2)).add(faceVector.mult(-depth/2)).xyz(ele + height));
			vs.add(node.getPos().add(boardVector.mult(width/2)).add(faceVector.mult(depth/2)).xyz(ele + height));
			
			target.drawTriangleFan(material.brighter(), vs, null);
			
		}
	
	}
	
	private static final class Phone extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		private static enum Type {WALL, PILLAR, CELL, HALFCELL};
		
		public Phone(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			double ele = node.getElevationProfile().getEle();
			
			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			
			Material roofMaterial = null;
			Material poleMaterial = null;
			Type type = null;
			
			// get Type of Phone
			if(isInWall(node)){
				type = Type.WALL;
			} else {
				type =  Type.CELL;
			}
			
			// Phones differ widely in appearance, hence we draw them only for known operators or brands
			if (node.getTags().containsAny(asList("operator", "brand"), asList("Deutsche Telekom AG", "Deutsche Telekom", "Telekom") )) {
				roofMaterial = TELEKOM_MANGENTA;
				poleMaterial = STEEL;
			} else if (node.getTags().containsAny(asList("operator", "brand"), "British Telecom")) {
				roofMaterial = POSTBOX_ROYALMAIL;
				poleMaterial = POSTBOX_ROYALMAIL;
			} else {
				//no rendering, unknown operator or brand //TODO log info
				return;
			}

			
			// default dimensions may differ depending on the phone type
			float height = 0f;
			float width = 0f;
			
			switch(type) {
				case WALL:
					
					break;
				case CELL:
					height = parseHeight(node.getTags(), 2.1f);
					width = parseWidth(node.getTags(), 0.8f);
	
					target.drawBox(GLASS,
							node.getPos().xyz(ele),
							faceVector, height-0.2, width-0.06, width-0.06);
					target.drawBox(roofMaterial,
							node.getPos().xyz(ele + height-0.2),
							faceVector, 0.2, width, width);
					target.drawBox(poleMaterial,
							node.getPos().xyz(ele).add(new VectorXYZ((width/2-0.05),0, (width/2-0.05) ).rotateY( directionAngle )),
							faceVector, height-0.2, 0.1, 0.1);
					target.drawBox(poleMaterial,
							node.getPos().xyz(ele).add(new VectorXYZ(-(width/2-0.05),0, (width/2-0.05) ).rotateY( directionAngle )),
							faceVector, height-0.2, 0.1, 0.1);
					target.drawBox(poleMaterial,
							node.getPos().xyz(ele).add(new VectorXYZ(0,0, -(width/2-0.05) ).rotateY( directionAngle )),
							faceVector, height-0.2, width, 0.1);
					
					break;
				default:
					assert false : "unknown or unsupported phone type";
			}
			
		}
		
	}
	
	private static final class VendingMachineVice extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {

		private static enum Type {WALL, PILLAR};
		
		public VendingMachineVice(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			double ele = node.getElevationProfile().getEle();
			
			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			
			Material machineMaterial = null;
			Material poleMaterial = STEEL;
			Type type = null;
			
			if(node.getTags().contains("vending", "bicycle_tube") && node.getTags().containsAny("operator", asList("Continental", "continental"))){
				machineMaterial=new ConfMaterial(Lighting.FLAT, Color.ORANGE );
			}else if(node.getTags().contains("vending", "bicycle_tube")){
					machineMaterial=new ConfMaterial(Lighting.FLAT, Color.BLUE );
			} else if(node.getTags().contains("vending", "cigarettes")){
				machineMaterial=new ConfMaterial(Lighting.FLAT, new Color(0.8f, 0.73f, 0.5f));
			} else if(node.getTags().contains("vending", "condoms")){
				machineMaterial=new ConfMaterial(Lighting.FLAT, new Color(0.39f, 0.15f, 0.11f) );
			}
			
			// get Type of vending machine
			if(isInWall(node)){
				type = Type.WALL;
			} else {
				type =  Type.PILLAR;
			}
			
			// default dimensions will differ depending on the post box type
			float height = 0f;
			
			switch(type) {
				case WALL:
					
					break;
				case PILLAR:
					height = parseHeight(node.getTags(), 1.8f);
					
					target.drawBox(poleMaterial,
							node.getPos().xyz(ele).add(new VectorXYZ(0,0, -0.05).rotateY(faceVector.angle())),
							faceVector, height-0.3, 0.1, 0.1);
					target.drawBox(machineMaterial,
							node.getPos().xyz(ele+height-1).add(new VectorXYZ(0,0, 0.1 ).rotateY(directionAngle)),
							faceVector, 1, 1, 0.2);
					
					break;
				default:
					assert false : "unknown or unsupported Vending machine Type";
			}
			
		}
		
	}
	
	private static final class PostBox extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		private static enum Type {WALL, PILLAR};
	
		public PostBox(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			double ele = node.getElevationProfile().getEle();
			
			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			
			Material boxMaterial = null;
			Material poleMaterial = null;
			Type type = null;
			
			
			// post boxes differ widely in appearance, hence we draw them only for known operators or brands
			if (node.getTags().containsAny(asList("operator", "brand"), asList("Deutsche Post AG", "Deutsche Post"))) {
				boxMaterial = POSTBOX_DEUTSCHEPOST;
				poleMaterial = STEEL;
				type = Type.WALL;
			} else if (node.getTags().contains("operator", "Royal Mail")) {
				boxMaterial = POSTBOX_ROYALMAIL;
				type = Type.PILLAR;
			} else {
				//no rendering, unknown operator or brand for post box //TODO log info
				return;
			}
			
			assert (type != Type.WALL || poleMaterial != null) : "post box of type wall requires a pole material";
			
			// default dimensions will differ depending on the post box type
			float height = 0f;
			float width = 0f;
			
			switch(type) {
				case WALL:
					height = parseHeight(node.getTags(), 0.8f);
					width = parseWidth(node.getTags(), 0.3f);
					
					target.drawBox(poleMaterial,
						node.getPos().xyz(ele),
						faceVector, height, 0.08, 0.08);
					
					target.drawBox(boxMaterial,
						node.getPos().add(faceVector.mult(width/2 - 0.08/2)).xyz(ele + height),
						faceVector, width, width, width);
					break;
				case PILLAR:
					height = parseHeight(node.getTags(), 2f);
					width = parseWidth(node.getTags(), 0.5f);
					
					target.drawColumn(boxMaterial, null,
						node.getPos().xyz(ele),
						height - 0.1, width, width, false, false);
					target.drawColumn(boxMaterial, null,
						node.getPos().xyz(ele + height - 0.1),
						0.1, width + 0.1, 0, true, true);
					break;
				default:
					assert false : "unknown post box type";
			}
			
		}
	
	}
	
	private static final class BusStop extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public BusStop(MapNode node) {
			super(node);
			
			if(node.getTags().contains("bin", "yes")){
				node.addRepresentation(new WasteBasket(node));
			}
			
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			if(!isInHighway(node)){
				float height = parseHeight(node.getTags(), 3f);
				float signHeight = 0.7f;
				float signWidth = 0.4f;
				
				Material poleMaterial = STEEL;
				
				double ele = node.getElevationProfile().getEle();
				
				double directionAngle = parseDirection(node.getTags(), PI);
				
				VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
				
				target.drawColumn(poleMaterial, null,
					node.getPos().xyz(ele),
					height-signHeight, 0.05, 0.05, true, true);
				/* draw sign */
				target.drawBox(BUS_STOP_SIGN,
					node.getPos().xyz(ele + height - signHeight),
					faceVector, signHeight, signWidth, 0.02);
				/*  draw timetable */
				target.drawBox(poleMaterial,
					node.getPos().xyz(ele + 1.2f).add(new VectorXYZ(0.055f,0,0f).rotateY(directionAngle)),
					faceVector, 0.31, 0.01, 0.43);
								
				//TODO Add Shelter and bench
			}
		}
		
	}
	
	
	private static final class ParcelMachine extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
	
		public ParcelMachine(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			VectorXYZ posWithEle = node.getElevationProfile().getPointWithEle();
			
			double directionAngle = parseDirection(node.getTags(), PI);
			
			Material boxMaterial = POSTBOX_DEUTSCHEPOST;
			Material otherMaterial = STEEL;
			
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ rightVector = faceVector.rightNormal();
			
			// shape depends on type
			if (node.getTags().contains("type", "Rondell")) {
				
				float height = parseHeight(node.getTags(), 2.2f);
				float width = parseWidth(node.getTags(), 3f);
				float rondelWidth  = width * 2/3;
				float boxWidth     = width * 1/3;
				float roofOverhang = 0.3f;
				
				/* draw rondel */
				target.drawColumn(boxMaterial, null,
					node.getPos().add(rightVector.mult(-rondelWidth/2)).xyz(ele),
					height,	rondelWidth/2, rondelWidth/2, false, true);
				/* draw box */
				target.drawBox(boxMaterial,
					node.getPos().add(rightVector.mult(boxWidth/2)).add(faceVector.mult(-boxWidth/2)).xyz(ele),
					faceVector, height, boxWidth, boxWidth);
				/* draw roof */
				target.drawColumn(otherMaterial, null,
					node.getPos().xyz(ele + height),
					0.1, rondelWidth/2 + roofOverhang/2, rondelWidth/2 + roofOverhang/2, true, true);
				
			} else if (node.getTags().contains("type", "Paketbox")) {
				
				float height = parseHeight(node.getTags(), 1.5f);
				float width = parseHeight(node.getTags(), 1.0f);
				float depth = width;
				
				target.drawBox(boxMaterial,	posWithEle,
						faceVector, height, width * 2, depth * 2);
				
			} else { // type=Schrank or type=24/7 Station (they look roughly the same) or no type (fallback)
				
				float height = parseHeight(node.getTags(), 2.2f);
				float width = parseWidth(node.getTags(), 3.5f);
				float depth = width/3;
				float roofOverhang = 0.3f;
				
				/* draw box */
				target.drawBox(boxMaterial,
					node.getPos().xyz(ele),
					faceVector, height, width, depth);
				/*  draw small roof */
				target.drawBox(otherMaterial,
					node.getPos().add(faceVector.mult(roofOverhang)).xyz(ele + height),
					faceVector, 0.1, width, depth + roofOverhang*2);
				
			}
			
		}
	
	}
	
	private static final class FireHydrant extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public FireHydrant(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			float height = parseHeight(node.getTags(), 1f);
			
			/* draw main pole */
			target.drawColumn(FIREHYDRANT, null,
					node.getElevationProfile().getWithEle(node.getPos()),
					height,
					0.15, 0.15, false, true);
			
			/* draw two small and one large valve */
			VectorXYZ valveBaseVector = node.getPos().xyz(ele + height - 0.3);
			VectorXZ smallValveVector = VectorXZ.X_UNIT;
			VectorXZ largeValveVector = VectorXZ.Z_UNIT;
			
			target.drawBox(FIREHYDRANT,
				valveBaseVector,
				smallValveVector, 0.1f, 0.5f, 0.1f);
			target.drawBox(FIREHYDRANT,
				valveBaseVector.add(0.2f, -0.1f, 0f),
				largeValveVector, 0.15f, 0.15f, 0.15f);
		}
		
	}
	
	private static final class StreetLamp extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public StreetLamp(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			float lampHeight = 0.8f;
			float lampHalfWidth = 0.4f;
			float poleHeight = parseHeight(node.getTags(), 5f) - lampHeight;
			
			/* determine material */
			
			Material material = null;
			
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}
			
			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), STEEL);
			}
			
			/* draw pole */
			target.drawColumn(material, null,
					node.getPos().xyz(ele),
					0.5, 0.16, 0.08, false, false);
			target.drawColumn(material, null,
					node.getPos().xyz(ele + 0.5),
					poleHeight, 0.08, 0.08, false, false);
			
			/* draw lamp */
					
			// lower part
			List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
			vs.add(node.getPos().xyz(ele + poleHeight));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, -lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, -lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			
			target.drawTriangleFan(material, vs, null);
			
			// upper part
			vs.clear();
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, -lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, -lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			
			target.drawTriangleFan(material, vs, null);
		}
		
	}
	
}
