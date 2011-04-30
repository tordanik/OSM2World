package org.osm2world.core.world.modules;

import static java.util.Collections.nCopies;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapElement;
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
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
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
		} else if (Fence.fits(tags)) {
			line.addRepresentation(new Fence(line, tags));
		}
			
	}
	
	@Override
	protected void applyToNode(MapNode node) {

		TagGroup tags = node.getTags();
		if (!tags.containsKey("barrier")) return; //fast exit for common case

		if (Bollard.fits(tags)) {
			node.addRepresentation(new Bollard(node, tags));
		}

		
	}
	
	private static abstract class LinearBarrier
		extends AbstractNetworkWaySegmentWorldObject
		implements WaySegmentWorldObject, RenderableToAllTargets {
				
		protected final float height;
		protected final float width;
		
		public LinearBarrier(MapWaySegment waySegment,
				float defaultHeight, float defaultWidth) {
			
			super(waySegment);
						
			height = parseHeight(waySegment.getOsmWay().tags, defaultHeight);
			width = parseWidth(waySegment.getOsmWay().tags, defaultWidth);
			
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return height;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public VectorXZ getStartPosition() {
			return line.getStartNode().getPos();
		}
		
		@Override
		public VectorXZ getEndPosition() {
			return line.getEndNode().getPos();
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
		private final float height;
		private final float width;
		
		public ColoredWall(Material material,
				MapWaySegment segment, float height, float width) {
			super(segment, 1, 0.5f);
			this.material = material;
			this.height = height;
			this.width = width;
		}
				
		@Override
		public void renderTo(Target<?> target) {
			
			//TODO: join ways back together to reduce the number of caps
			
			VectorXYZ[] wallShape = {
				new VectorXYZ(-width/2, 0, 0),
				new VectorXYZ(-width/2, height, 0),
				new VectorXYZ(+width/2, height, 0),
				new VectorXYZ(+width/2, 0, 0)
			};
			
			List<VectorXYZ> path =
				line.getElevationProfile().getPointsWithEle();
			
			List<VectorXYZ[]> strips = createShapeExtrusionAlong(wallShape,
					path, nCopies(path.size(), VectorXYZ.Y_UNIT));
			
			for (VectorXYZ[] strip : strips) {
				target.drawTriangleStrip(material, strip);
			}
			
			/* draw caps */
			
			VectorXYZ[] startCap = transformShape(wallShape,
					path.get(0),
					line.getDirection().xyz(0),
					VectorXYZ.Y_UNIT);
			VectorXYZ[] endCap = transformShape(wallShape,
					path.get(path.size()-1),
					line.getDirection().invert().xyz(0),
					VectorXYZ.Y_UNIT);
			
			target.drawPolygon(material, startCap);
			target.drawPolygon(material, endCap);
			
		}
		
	}
	
	private static class Wall extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return "wall".equals(tags.getValue("barrier"));
		}
		public Wall(MapWaySegment segment) {
			super(Materials.WALL_DEFAULT, segment, 1f, 0.25f);
		}
	}
	
	private static class CityWall extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return "city_wall".equals(tags.getValue("barrier"));
		}
		public CityWall(MapWaySegment segment) {
			super(Materials.WALL_DEFAULT, segment, 10, 2);
		}
	}
	
	private static class Hedge extends ColoredWall {
		public static boolean fits(TagGroup tags) {
			return "hedge".equals(tags.getValue("barrier"));
		}
		public Hedge(MapWaySegment segment) {
			super(Materials.HEDGE, segment, 1f, 0.5f);
		}
	}
	
	private static class Fence extends LinearBarrier {
		
		public static boolean fits(TagGroup tags) {
			return "fence".equals(tags.getValue("barrier"));
		}
		
		private static final Map<String, Material> MATERIAL_MAP;
		static {
			MATERIAL_MAP = new HashMap<String, Material>();
			MATERIAL_MAP.put("split_rail", Materials.SPLIT_RAIL_FENCE);
		}
		
		private final Material material;
		
		public Fence(MapWaySegment segment, TagGroup tags) {
			super(segment, 0.5f, 0.1f);
			
			Material materialFromMap = MATERIAL_MAP.get(tags.getValue("fence_type"));
			if (materialFromMap != null) {
				material = materialFromMap;
			} else {
				material = Materials.FENCE_DEFAULT;
			}
			
		}
		
		@Override
		public void renderTo(Target<?> util) {
			
			/* render bars */
			
			VectorXYZ[] vsLowFront = createVectorsForVerticalTriangleStrip(
					line.getElevationProfile().getPointsWithEle(),
					0.2f * height, 0.5f * height);
			VectorXYZ[] vsLowBack = createVectorsForVerticalTriangleStrip(
					line.getElevationProfile().getPointsWithEle(),
					0.5f * height, 0.2f * height);
			
			util.drawTriangleStrip(material, vsLowFront);
			util.drawTriangleStrip(material, vsLowBack);

			VectorXYZ[] vsHighFront = createVectorsForVerticalTriangleStrip(
					line.getElevationProfile().getPointsWithEle(),
					0.65f * height, 0.95f * height);
			VectorXYZ[] vsHighBack = createVectorsForVerticalTriangleStrip(
					line.getElevationProfile().getPointsWithEle(),
					0.95f * height, 0.65f * height);
			
			util.drawTriangleStrip(material, vsHighFront);
			util.drawTriangleStrip(material, vsHighBack);
			
			/* render poles */
			
			List<VectorXZ> polePositions = GeometryUtil.equallyDistributePointsAlong(1f, false,
					line.getStartNode().getPos(), line.getEndNode().getPos());
			
			for (VectorXZ polePosition : polePositions) {
			
				VectorXYZ base = polePosition.xyz(line.getElevationProfile().getEleAt(polePosition));
				util.drawColumn(material, null , base, height, width, width, false, true);
			
			}
			
		}
		
	}
	
	private static class Bollard implements NodeWorldObject, RenderableToAllTargets {

		private static final float DEFAULT_HEIGHT = 1;

		public static boolean fits(TagGroup tags) {
			return "bollard".equals(tags.getValue("barrier"));
		}
		
		private final MapNode node;
		private final float height;
		
		public Bollard(MapNode node, TagGroup tags) {
			
			this.node = node;

			height = parseHeight(tags, DEFAULT_HEIGHT);
			
			
		}

		@Override
		public MapElement getPrimaryMapElement() {
			return node;
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return height;
		}

		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON; //TODO: flexible ground states
		}

		@Override
		public void renderTo(Target<?> util) {
			VectorXYZ pos = node.getElevationProfile().getPointWithEle();
			util.drawColumn(Materials.BOLLARD_DEFAULT,
					null, pos, height, 0.15f, 0.15f, false, true);
		}
		
	}
	
	//TODO: bollard_count or similar tag exists? create "Bollards" rep.
	//just as lift gates etc, this should use the line.getRightNormal and the road width
	
}
