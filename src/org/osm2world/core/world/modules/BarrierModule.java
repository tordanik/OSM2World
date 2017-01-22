package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.target.common.material.Materials.CHAIN_LINK_FENCE;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * adds barriers to the world
 */
public class BarrierModule extends AbstractModule {
	
	@Override
	protected void applyToWaySegment(MapWaySegment line) {

		TagGroup tags = line.getTags();
		if (!tags.containsKey("barrier")) return; //fast exit for common case
		
		if (Wall.fits(tags)) {
			line.addRepresentation(new Wall(line));
		} else if (CityWall.fits(tags)) {
			line.addRepresentation(new CityWall(line));
		} else if (Hedge.fits(tags)) {
			line.addRepresentation(new Hedge(line));
		} else if (ChainLinkFence.fits(tags)) {
			line.addRepresentation(new ChainLinkFence(line, tags));
		} else if (CableBarrier.fits(tags)) {
			line.addRepresentation(new CableBarrier(line, tags));
                } else if (HandRail.fits(tags)) {
			line.addRepresentation(new HandRail(line, tags));
		} else if (PoleFence.fits(tags)) {
			line.addRepresentation(new PoleFence(line, tags));
                }
			
	}
	
	@Override
	protected void applyToNode(MapNode node) {

		TagGroup tags = node.getTags();
		if (!tags.containsKey("barrier") && !tags.containsKey("power")) return; //fast exit for common case

		if (Bollard.fits(tags)) {
			node.addRepresentation(new Bollard(node, tags));
		}

		
	}
	
	private static abstract class LinearBarrier
		extends AbstractNetworkWaySegmentWorldObject
		implements RenderableToAllTargets {
				
		protected final float height;
		protected final float width;
		
		public LinearBarrier(MapWaySegment waySegment,
				float defaultHeight, float defaultWidth) {
			
			super(waySegment);
						
			height = parseHeight(waySegment.getOsmWay().tags, defaultHeight);
			width = parseWidth(waySegment.getOsmWay().tags, defaultWidth);
			
		}
		
		@Override
		public VectorXZ getStartPosition() {
			return segment.getStartNode().getPos();
		}
		
		@Override
		public VectorXZ getEndPosition() {
			return segment.getEndNode().getPos();
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON; //TODO: flexible ground states
		}
		
		@Override
		public float getWidth() {
			return width;
		}
				
	}
		
	/**
	 * superclass for linear barriers that are a simple colored "wall"
	 * (use width and height to create vertical sides and a flat top)
	 */
	private static abstract class ColoredWall extends LinearBarrier {
		
		private final Material material;
		
		public ColoredWall(Material material, MapWaySegment segment,
				float defaultHeight, float defaultWidth) {
			super(segment, 1, 0.5f);
			this.material = material;
		}
				
		@Override
		public void renderTo(Target<?> target) {
			
			//TODO: join ways back together to reduce the number of caps
			
			List<VectorXYZ> wallShape = asList(
				new VectorXYZ(-width/2, 0, 0),
				new VectorXYZ(-width/2, height, 0),
				new VectorXYZ(+width/2, height, 0),
				new VectorXYZ(+width/2, 0, 0)
			);
			
			List<VectorXYZ> path = getCenterline();
			
			List<List<VectorXYZ>> strips = createShapeExtrusionAlong(wallShape,
					path, nCopies(path.size(), VectorXYZ.Y_UNIT));
			
			for (List<VectorXYZ> strip : strips) {
				target.drawTriangleStrip(material, strip,
						texCoordLists(strip, material, STRIP_WALL));
			}
			
			/* draw caps */
			
			List<VectorXYZ> startCap = transformShape(wallShape,
					path.get(0),
					segment.getDirection().xyz(0),
					VectorXYZ.Y_UNIT);
			List<VectorXYZ> endCap = transformShape(wallShape,
					path.get(path.size()-1),
					segment.getDirection().invert().xyz(0),
					VectorXYZ.Y_UNIT);
			
			List<List<VectorXZ>> texCoordLists =
				texCoordLists(wallShape, material, GLOBAL_X_Y);
			
			target.drawConvexPolygon(material, startCap, texCoordLists);
			target.drawConvexPolygon(material, endCap, texCoordLists);
			
		}
		
	}
	
	private static class Wall extends ColoredWall {
		
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "wall");
		}
		
		private static Material getMaterial(TagGroup tags) {
			
			Material material = null;
			
			if ("gabion".equals(tags.getValue("wall"))) {
				material = Materials.WALL_GABION;
			} else if ( tags.containsKey("material") ) {
				material = Materials.getMaterial(tags.getValue("material").toUpperCase());
			}
			
			if (material != null) {
				return material;
			} else {
				return Materials.WALL_DEFAULT;
			}
			
		}
		
		public Wall(MapWaySegment segment) {
			super(getMaterial(segment.getTags()), segment, 1f, 0.25f);
		}
		
	}
	
	private static class CityWall extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "city_wall");
		}
		public CityWall(MapWaySegment segment) {
			super(Materials.WALL_DEFAULT, segment, 10, 2);
		}
	}
	
	private static class Hedge extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "hedge");
		}
		public Hedge(MapWaySegment segment) {
			super(Materials.HEDGE, segment, 1f, 0.5f);
		}
	}
	
	private static class ChainLinkFence extends LinearBarrier {
		
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "fence")
					&& (tags.contains("fence_type", "chain_link") || (tags.contains("fence_type", "metal") || (tags.contains("fence_type", "railing") ) ) );
		}
		
		public ChainLinkFence(MapWaySegment segment, TagGroup tags) {
			super(segment, 1f, 0.02f);
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			/* render fence */
			
			List<VectorXYZ> pointsWithEle = getCenterline();
			
			List<VectorXYZ> vsFence = createVerticalTriangleStrip(
					pointsWithEle, 0, height);
			List<List<VectorXZ>> texCoordListsFence = texCoordLists(
					vsFence, CHAIN_LINK_FENCE, STRIP_WALL);
			
			target.drawTriangleStrip(CHAIN_LINK_FENCE, vsFence, texCoordListsFence);

			List<VectorXYZ> pointsWithEleBack =
					new ArrayList<VectorXYZ>(pointsWithEle);
			Collections.reverse(pointsWithEleBack);
			
			List<VectorXYZ> vsFenceBack = createVerticalTriangleStrip(
					pointsWithEleBack, 0, height);
			List<List<VectorXZ>> texCoordListsFenceBack = texCoordLists(
					vsFenceBack, CHAIN_LINK_FENCE, STRIP_WALL);
			
			target.drawTriangleStrip(CHAIN_LINK_FENCE, vsFenceBack,
					texCoordListsFenceBack);
						
			/* render poles */
			
			//TODO connect the poles to the ground independently
			
			List<VectorXZ> polePositions = GeometryUtil.equallyDistributePointsAlong(2f, false,
					segment.getStartNode().getPos(), segment.getEndNode().getPos());
			
                        
			for (VectorXZ pos : polePositions) {
                                
                                VectorXYZ base = null;
                                
                                for (int i = 0; i + 1 < pointsWithEle.size(); i++) {
				
                                    if (isBetween(pos, pointsWithEle.get(i).xz(), pointsWithEle.get(i+1).xz())) {
    					base = interpolateElevation(pos, pointsWithEle.get(i), pointsWithEle.get(i+1));
                                        break;
                                    }
                                }
				target.drawColumn(Materials.METAL_FENCE_POST, null, base,
						height, width, width, false, true);
			}
			
		}
	}
        
        private static class PoleFence extends LinearBarrier {
		private Material material;
		protected float barWidth;
		protected float barGap;
                protected float barOffset;
		protected int bars;
                protected Material defaultFenceMaterial = Materials.WOOD;
                protected Material defaultPoleMaterial = Materials.WOOD;
                protected Material poleMaterial;
                
		public static boolean fits(TagGroup tags) {
                        return tags.contains("barrier", "fence");
		}
		
		public PoleFence(MapWaySegment segment, TagGroup tags) {
			super(segment, 1f, 0.02f);
			if ( tags.containsKey("material") ){
                                material = Materials.getMaterial(tags.getValue("material").toUpperCase());
                                poleMaterial = material;
                        }
                        
			this.barWidth = 0.1f;
			this.barGap = 0.2f;
			this.bars = 10;
                        this.barOffset = barGap/2;
		}
		
		@Override
		public void renderTo(Target<?> target) {

			if ( material == null ) {
                                material = defaultFenceMaterial;
                                poleMaterial = defaultPoleMaterial;
			}
                                        
			List<VectorXYZ> baseline = getCenterline();
                        
			/* render fence */
			for(int i=0; i<bars; i++){
                                float barEndHeight = height - (i * barGap) - barOffset;
                                float barStartHeight = barEndHeight - barWidth;
                    
                                if ( barStartHeight > 0 ){
    					List<VectorXYZ> vsLowFront = createVerticalTriangleStrip(baseline, barStartHeight, barEndHeight );
    					List<VectorXYZ> vsLowBack = createVerticalTriangleStrip(baseline, barEndHeight, barStartHeight);
			
    					target.drawTriangleStrip(material, vsLowFront, null);
    					target.drawTriangleStrip(material, vsLowBack, null);
                                }
			}

                        
			/* render poles */
			List<VectorXZ> polePositions = GeometryUtil.equallyDistributePointsAlong(2f, false,
					segment.getStartNode().getPos(), segment.getEndNode().getPos());
			
                        
			for (VectorXZ pos : polePositions) {
                                
                                VectorXYZ base = null;
                                
                                for (int i = 0; i + 1 < baseline.size(); i++) {
				
                                    if (isBetween(pos, baseline.get(i).xz(), baseline.get(i+1).xz())) {
    					base = interpolateElevation(pos, baseline.get(i), baseline.get(i+1));
                                        break;
                                    }
                                }
				target.drawColumn(poleMaterial, null, base,
						height, width, width, false, true);
			}
			
		}
	}
        
        private static class CableBarrier extends PoleFence {
                
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "cable_barrier");
		}
		
		public CableBarrier(MapWaySegment segment, TagGroup tags) {
			super(segment, tags);
                        
			this.barWidth = 0.03f;
			this.barGap = 0.1f;
			this.bars = 4;
			this.barOffset = barGap/2;
                 
                        this.defaultFenceMaterial = Materials.METAL_FENCE;
                        this.defaultPoleMaterial = Materials.METAL_FENCE_POST;
		}
	}
        
        private static class HandRail extends PoleFence {
		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "handrail");
		}
		
		public HandRail(MapWaySegment segment, TagGroup tags) {
			super(segment, tags);
                        
			this.barWidth = 0.05f;
			this.barGap = 0f;
			this.bars = 1;
			this.barOffset = 0;
                        
                        this.defaultFenceMaterial = Materials.HANDRAIL_DEFAULT;
                        this.defaultPoleMaterial = Materials.HANDRAIL_DEFAULT;
		}
        }
	
	private static class Bollard extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {

		private static final float DEFAULT_HEIGHT = 1;

		public static boolean fits(TagGroup tags) {
			return tags.contains("barrier", "bollard");
		}
		
		private final float height;
		
		public Bollard(MapNode node, TagGroup tags) {
			
			super(node);
			
			height = parseHeight(tags, DEFAULT_HEIGHT);
						
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON; //TODO: flexible ground states
		}

		@Override
		public void renderTo(Target<?> target) {
			target.drawColumn(Materials.CONCRETE,
					null, getBase(), height, 0.15f, 0.15f, false, true);
		}
		
	}
	
	//TODO: bollard_count or similar tag exists? create "Bollards" rep.
	//just as lift gates etc, this should use the line.getRightNormal and the road width
	
}
