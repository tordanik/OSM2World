package org.osm2world.core.world.modules;

import static java.lang.Math.toRadians;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseAngle;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
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
		if (node.getTags().contains("amenity", "waste_basket")) {
			node.addRepresentation(new WasteBasket(node));
		}
		if (node.getTags().contains("emergency", "fire_hydrant")
			&& node.getTags().contains("fire_hydrant:type", "pillar")) {
			node.addRepresentation(new FireHydrant(node));
		}
		if (node.getTags().contains("highway", "street_lamp")) {
			node.addRepresentation(new StreetLamp(node));
		}
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
		public MapElement getPrimaryMapElement() {
			return node;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			target.drawColumn(Materials.STEEL, null,
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
		public MapElement getPrimaryMapElement() {
			return node;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			float height = parseHeight(node.getTags(), 3f);
			
			/* draw socket, poster and cap */
			
			target.drawColumn(Materials.CONCRETE, null,
					node.getPos().xyz(ele),
					0.15 * height,
					0.5, 0.5, false, false);
			
			target.drawColumn(Materials.ADVERTISING_POSTER, null,
					node.getPos().xyz(ele),
					0.98 * height,
					0.48, 0.48, false, false);

			target.drawColumn(Materials.CONCRETE, null,
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
		public MapElement getPrimaryMapElement() {
			return node;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			
			float width = parseWidth(node.getTags(), 4);
			float height = parseHeight(node.getTags(), 3.5f);
			float minHeight = height / 5;
						
			Float directionAngle = 180f;
			if (node.getTags().containsKey("direction")) {
				directionAngle = parseAngle(node.getTags().getValue("direction"));
			}
			
			VectorXZ faceVector = VectorXZ.fromAngle(toRadians(directionAngle));
			VectorXZ boardVector = faceVector.rightNormal();
			
			/* draw board */
			
			VectorXYZ[] vs = {
				node.getPos().add(boardVector.mult(-width/2))
					.xyz(ele + minHeight),
				node.getPos().add(boardVector.mult(-width/2))
					.xyz(ele + height),
				node.getPos().add(boardVector.mult(+width/2))
					.xyz(ele + minHeight),
				node.getPos().add(boardVector.mult(+width/2))
					.xyz(ele + height)
			};
			
			target.drawTriangleStrip(Materials.ADVERTISING_POSTER, vs);
			
			VectorXYZ temp = vs[2];
			vs[2] = vs[0];
			vs[0] = temp;
			
			temp = vs[3];
			vs[3] = vs[1];
			vs[1] = temp;
			
			target.drawTriangleStrip(Materials.CONCRETE, vs);
			
						
			/* draw poles */
			
			VectorXZ[] poles = {
					node.getPos().add(boardVector.mult(-width/4)),
					node.getPos().add(boardVector.mult(+width/4))
			};
			
			for (VectorXZ pole : poles) {
				target.drawBox(Materials.CONCRETE,
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
		public MapElement getPrimaryMapElement() {
			return node;
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

			Float directionAngle = 180f;
			if (node.getTags().containsKey("direction")) {
				directionAngle = parseAngle(node.getTags().getValue("direction"));
			}
			
			VectorXZ faceVector = VectorXZ.fromAngle(toRadians(directionAngle));
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
		public MapElement getPrimaryMapElement() {
			return node;
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
						node.getTags().getValue("surface"), Materials.STEEL);
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
		public MapElement getPrimaryMapElement() {
			return node;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			float height = parseHeight(node.getTags(), 1f);
			
			/* draw main pole */
			target.drawColumn(Materials.FIREHYDRANT, null,
					node.getElevationProfile().getWithEle(node.getPos()),
					height,
					0.15, 0.15, false, true);
			
			/* draw two small and one large valve */
			VectorXYZ valveBaseVector = node.getPos().xyz(ele + height - 0.3);
			VectorXZ smallValveVector = new VectorXZ(1f, 0f);
			VectorXZ largeValveVector = new VectorXZ(0f, 1f);
			
			target.drawBox(Materials.FIREHYDRANT,
				valveBaseVector,
				smallValveVector, 0.1f, 0.5f, 0.1f);
			target.drawBox(Materials.FIREHYDRANT,
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
		public MapElement getPrimaryMapElement() {
			return node;
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
						node.getTags().getValue("surface"), Materials.STEEL);
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
			
			target.drawTriangleFan(material, vs);
			
			// upper part
			vs.clear();
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, -lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, -lampHalfWidth));
			vs.add(node.getPos().xyz(ele + poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			
			target.drawTriangleFan(material, vs);
		}
		
	}
	
}
