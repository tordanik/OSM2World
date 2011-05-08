package org.osm2world.core.world.modules;

import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
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
	
}
