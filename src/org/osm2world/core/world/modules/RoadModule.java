package org.osm2world.core.world.modules;

import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.world.modules.common.Materials.*;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseWidth;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.Material.Lighting;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleGeometryUtil;
import org.osm2world.core.world.modules.common.WorldModuleParseUtil;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.NetworkAreaWorldObject;
import org.osm2world.core.world.network.VisibleConnectorNodeWorldObject;

/**
 * adds roads to the world
 */
public class RoadModule extends ConfigurableWorldModule {

	public static final Material MARKING_MAT = new Material(Lighting.FLAT, new Color(1f, 1f, 1f));
	
	@Override
	public void applyTo(MapData grid) {
		
		for (MapWaySegment line : grid.getMapWaySegments()) {
			if (isRoad(line.getTags())) {
				line.addRepresentation(new Road(line, line.getTags()));
			}
		}

		for (MapArea area : grid.getMapAreas()) {
				
			if (isRoad(area.getTags())) {
				
				List<VectorXZ> coords = new ArrayList<VectorXZ>();
				for (MapNode node : area.getBoundaryNodes()) {
					coords.add(node.getPos());
				}
				coords.remove(coords.size()-1);
				
				area.addRepresentation(new RoadArea(area));
			}
			
		}

		for (MapNode node : grid.getMapNodes()) {

			TagGroup tags = node.getOsmNode().tags;
			
			//TODO: BEGIN COPY&PASTE
			
			List<MapWaySegment> inboundRoads = new ArrayList<MapWaySegment>();
			List<MapWaySegment> outboundRoads = new ArrayList<MapWaySegment>();
			
			for (MapWaySegment line : node.getInboundLines()) {
				if (line.getPrimaryRepresentation() instanceof Road) {
					inboundRoads.add(line);
				}
			}
			for (MapWaySegment line : node.getOutboundLines()) {
				if (line.getPrimaryRepresentation() instanceof Road) {
					outboundRoads.add(line);
				}
			}
			
			//END COPY&PASTE
			
			if (inboundRoads.size() > 0 || outboundRoads.size() > 0) {
				
				if (inboundRoads.size() + outboundRoads.size() > 2){
					node.addRepresentation(new RoadJunction(node));
				} else if (inboundRoads.size() + outboundRoads.size() == 2
						&& "crossing".equals(tags.getValue("highway"))) {
					node.addRepresentation(
						new RoadCrossingAtConnector(node));
				}
				
			}
			
		}
		
	}

	private static boolean isRoad(TagGroup tags) {
		return tags.containsKey("highway")
			|| tags.contains("railway", "platform")
			|| tags.contains("leisure", "track");
	}
	
	private static boolean isSteps(TagGroup tags) {
		return tags.contains(new Tag("highway","steps"));
	}

	//TODO: materials for junctions and crossings
	
	/**
	 * representation for junctions between roads.
	 */
	public static class RoadJunction
		extends JunctionNodeWorldObject
		implements NodeWorldObject, RenderableToAllTargets,
		TerrainBoundaryWorldObject {
						
		public RoadJunction(MapNode node) {
			super(node);
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			target.drawTriangles(ASPHALT, super.getTriangulation());
						
		}
		
		@Override
		public GroundState getGroundState() {
			GroundState currentGroundState = null;
			checkEachLine: {
				for (MapWaySegment line : this.node.getConnectedWaySegments()) {
					if (line.getPrimaryRepresentation() == null) continue;
					GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
					if (currentGroundState == null) {
						currentGroundState = lineGroundState;
					} else if (currentGroundState != lineGroundState) {
						currentGroundState = GroundState.ON;
						break checkEachLine;
					}
				}
			}
			return currentGroundState;
		}

	}
	
	/* TODO: crossings at junctions - when there is, e.g., a footway connecting to the road!
	 * (ideally, this would be implemented using more flexibly configurable
	 * junctions which can have "preferred" segments that influence
	 * the junction shape more/exclusively)
	 */
	
	/**
	 * representation for crossings (zebra crossing etc.) on roads
	 */
	public static class RoadCrossingAtConnector
		extends VisibleConnectorNodeWorldObject
		implements NodeWorldObject, RenderableToAllTargets,
		TerrainBoundaryWorldObject {
		
		private static final float CROSSING_WIDTH = 3f;
				
		public RoadCrossingAtConnector(MapNode node) {
			super(node);
		}
		
		@Override
		public float getLength() {
			return CROSSING_WIDTH;
		}
		
		@Override
		public void renderTo(Target<?> target) {
							
			NodeElevationProfile eleProfile = node.getElevationProfile();
			
			VectorXYZ start = eleProfile.getWithEle(startPos);
			VectorXYZ end = eleProfile.getWithEle(endPos);
			
			VectorXYZ startLines1 = eleProfile.getWithEle(
					interpolateBetween(startPos, endPos, 0.1f));
			VectorXYZ endLines1 = eleProfile.getWithEle(
					interpolateBetween(startPos, endPos, 0.2f));
			VectorXYZ startLines2 = eleProfile.getWithEle(
					interpolateBetween(startPos, endPos, 0.8f));
			VectorXYZ endLines2 = eleProfile.getWithEle(
					interpolateBetween(startPos, endPos, 0.9f));

			double halfStartWidth = startWidth * 0.5;
			double halfEndWidth = endWidth * 0.5;
			double halfStartLines1Width = interpolateValue(startLines1.xz(),
					startPos, halfStartWidth, endPos, halfEndWidth);
			double halfEndLines1Width = interpolateValue(endLines1.xz(),
					startPos, halfStartWidth, endPos, halfEndWidth);
			double halfStartLines2Width = interpolateValue(startLines2.xz(),
					startPos, halfStartWidth, endPos, halfEndWidth);
			double halfEndLines2Width = interpolateValue(endLines2.xz(),
					startPos, halfStartWidth, endPos, halfEndWidth);

			//TODO: don't always use halfStart/EndWith - you need to interpolate!
			
			/** area outside and inside lines */
			target.drawTriangleStrip(ASPHALT,
					start.subtract(cutVector.mult(halfStartWidth)),
					start.add(cutVector.mult(halfStartWidth)),
					startLines1.subtract(cutVector.mult(halfStartLines1Width)),
					startLines1.add(cutVector.mult(halfStartLines1Width)));
			target.drawTriangleStrip(ASPHALT,
					endLines1.subtract(cutVector.mult(halfEndLines1Width)),
					endLines1.add(cutVector.mult(halfEndLines1Width)),
					startLines2.subtract(cutVector.mult(halfStartLines2Width)),
					startLines2.add(cutVector.mult(halfStartLines2Width)));
			target.drawTriangleStrip(ASPHALT,
					endLines2.subtract(cutVector.mult(halfEndLines2Width)),
					endLines2.add(cutVector.mult(halfEndLines2Width)),
					end.subtract(cutVector.mult(halfEndWidth)),
					end.add(cutVector.mult(halfEndWidth)));

			/** lines across road */
			target.drawTriangleStrip(MARKING_MAT,
					startLines1.subtract(cutVector.mult(halfStartLines1Width)),
					startLines1.add(cutVector.mult(halfStartLines1Width)),
					endLines1.subtract(cutVector.mult(halfEndLines1Width)),
					endLines1.add(cutVector.mult(halfEndLines1Width)));
			target.drawTriangleStrip(MARKING_MAT,
					startLines2.subtract(cutVector.mult(halfStartLines2Width)),
					startLines2.add(cutVector.mult(halfStartLines2Width)),
					endLines2.subtract(cutVector.mult(halfEndLines2Width)),
					endLines2.add(cutVector.mult(halfEndLines2Width)));
			
		}
		
		@Override
		public GroundState getGroundState() {
			GroundState currentGroundState = null;
			checkEachLine: {
				for (MapWaySegment line : this.node.getConnectedWaySegments()) {
					if (line.getPrimaryRepresentation() == null) continue;
					GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
					if (currentGroundState == null) {
						currentGroundState = lineGroundState;
					} else if (currentGroundState != lineGroundState) {
						currentGroundState = GroundState.ON;
						break checkEachLine;
					}
				}
			}
			return currentGroundState;
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}

	}
		
	/** representation of a road */
	public static class Road
		extends AbstractNetworkWaySegmentWorldObject
		implements WaySegmentWorldObject, RenderableToAllTargets,
		TerrainBoundaryWorldObject {
			
		protected static final float DEFAULT_LANE_WIDTH = 3.5f;
		
		protected static final float DEFAULT_ROAD_CLEARING = 5;
		protected static final float DEFAULT_PATH_CLEARING = 2;
		
		public final float width;
		
		final private TagGroup tags;
		final public VectorXZ startCoord, endCoord;
	
		final private boolean steps;
						
		public Road(MapWaySegment line, TagGroup tags) {
			
			super(line);
			
			this.tags = tags;
			this.startCoord = line.getStartNode().getPos();
			this.endCoord = line.getEndNode().getPos();
			
			float fallbackWidth = getDefaultWidth(tags);
			if (tags.containsKey("lanes")) {
				try {
					fallbackWidth = Integer.parseInt(tags.getValue("lanes")) * DEFAULT_LANE_WIDTH;
				} catch (NumberFormatException e) {}
			}
			
			this.width = parseWidth(tags, fallbackWidth);
			
			steps = isSteps(tags);
			
		}

		private static float getDefaultWidth (TagGroup tags) {
			
			String highwayValue = tags.getValue("highway");
			
			if (isPath(tags)) {
				return 1f;
			}
			
			if ("service".equals(highwayValue)
					|| "track".equals(highwayValue)) {
				if (tags.contains("service", "parking_aisle")) {
					return DEFAULT_LANE_WIDTH * 0.8f;
				} else {
					return DEFAULT_LANE_WIDTH;
				}
			}
			
			if (tags.containsKey("oneway") && !tags.getValue("oneway").equals("no")) {
				return DEFAULT_LANE_WIDTH;
			}
			
			else {
				return 4f;
			}
			
		}

		private static boolean isPath(TagGroup tags) {
			String highwayValue = tags.getValue("highway");
			return "path".equals(highwayValue)
				|| "footway".equals(highwayValue)
				|| "cycleway".equals(highwayValue)
				|| "bridleway".equals(highwayValue)
				|| "steps".equals(highwayValue);
		}

		@Override
		public float getWidth() {
			return width;
		}
		
		private void renderStepsUsing(Target<?> target) {

			WaySegmentElevationProfile elevationProfile = line.getElevationProfile();
			
			final VectorXZ startWithOffset = getStartPosition();
			final VectorXZ endWithOffset = getEndPosition();
		
			double lineLength = VectorXZ.distance (
					line.getStartNode().getPos(), line.getEndNode().getPos());
			
			/* render ground first (so gaps between the steps look better) */
			
			VectorXYZ[] vs = WorldModuleGeometryUtil.createVectorsForTriangleStripBetween(
					getOutline(false), getOutline(true));
		
			target.drawTriangleStrip(ASPHALT, vs);
			
			/* determine the length of each individual step */
			
			float stepLength = 0.3f;
			
			if (tags.containsKey("step_count")) {
				try {
					int stepCount = Integer.parseInt(tags.getValue("step_count"));
					stepLength = (float)lineLength / stepCount;
				} catch (NumberFormatException e) { /* don't overwrite default length */ }
			}
			
			/* locate the position on the line at the beginning/end of each step
			 * (positions on the line spaced by step length),
			 * interpolate heights between adjacent points with elevation */
			
			List<VectorXZ> stepBorderPositionsXZ =
				GeometryUtil.equallyDistributePointsAlong(
					stepLength, true, startWithOffset, endWithOffset);
			
			List<VectorXYZ> stepBorderPositions = new ArrayList<VectorXYZ>();
			for (VectorXZ posXZ : stepBorderPositionsXZ) {
				VectorXYZ posXYZ = posXZ.xyz(elevationProfile.getEleAt(posXZ));
				stepBorderPositions.add(posXYZ);
			}
			
			/* draw steps */
			
			VectorXYZ fullRightVector = line.getRightNormal().mult(width).xyz(0);
			VectorXYZ halfLeftVector = fullRightVector.mult(-0.5f);
			
			for (int step = 0; step < stepBorderPositions.size() - 1; step++) {
				
				VectorXYZ frontCenter = stepBorderPositions.get(step);
				VectorXYZ backCenter = stepBorderPositions.get(step+1);
				
				VectorXYZ frontLowerCenter;
				if (frontCenter.y > backCenter.y /* segment goes downwards */) {
					frontLowerCenter = frontCenter.y(backCenter.y);
				} else {
					frontLowerCenter = frontCenter;
				}
				
				VectorXYZ upVector = new VectorXYZ(
						0, Math.abs(frontCenter.y - backCenter.y), 0);
				
				target.drawBox(new Material(Lighting.FLAT, Color.DARK_GRAY),
						frontLowerCenter.add(halfLeftVector),
						fullRightVector,
						upVector,
						backCenter.subtract(frontCenter).y(0));
				
			}
			
		}

		@Override
		public void renderTo(Target<?> target) {
		
			if (!steps) {
				
				VectorXYZ[] vs = WorldModuleGeometryUtil.createVectorsForTriangleStripBetween(
							getOutline(false), getOutline(true));
				
				String surface = tags.getValue("surface");
				target.drawTriangleStrip(getMaterial(surface, ASPHALT), vs);
				
						
			} else {
				renderStepsUsing(target);
			}
			
		}
		
		@Override
		public GroundState getGroundState() {
			if (BridgeModule.isBridge(tags)) {
				return GroundState.ABOVE;
			} else if (TunnelModule.isTunnel(tags)) {
				return GroundState.BELOW;
			} else {
				return GroundState.ON;
			}
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return WorldModuleParseUtil.parseClearing(tags,
					isPath(tags) ? DEFAULT_PATH_CLEARING : DEFAULT_ROAD_CLEARING);
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
	}
	
	public static class RoadArea extends NetworkAreaWorldObject
		implements  RenderableToAllTargets, TerrainBoundaryWorldObject {

		private static final float DEFAULT_CLEARING = 5f;
		
		public RoadArea(MapArea area) {
			super(area);
		}
		
		@Override
		public void renderTo(Target<?> target) {
			String surface = area.getTags().getValue("surface");
			target.drawTriangles(getMaterial(surface, ASPHALT), getTriangulation());
		}
		
		@Override
		public GroundState getGroundState() {
			if (BridgeModule.isBridge(area.getTags())) {
				return GroundState.ABOVE;
			} else if (TunnelModule.isTunnel(area.getTags())) {
				return GroundState.BELOW;
			} else {
				return GroundState.ON;
			}
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return WorldModuleParseUtil.parseClearing(
					area.getTags(), DEFAULT_CLEARING);
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
	}
	
}
