package org.osm2world.core.world.modules;

import static com.google.common.collect.Iterables.any;
import static java.util.Collections.nCopies;
import static org.osm2world.core.util.Predicates.hasType;
import static org.osm2world.core.world.modules.common.Materials.EMPTY_GROUND;
import static org.osm2world.core.world.modules.common.Materials.WATER;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createLineBetween;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createShapeExtrusionAlong;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createVectorsForTriangleStripBetween;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.povray.POVRayTarget;
import org.osm2world.core.target.povray.RenderableToPOVRay;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.Materials;
import org.osm2world.core.world.modules.common.WorldModuleGeometryUtil;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.NetworkAreaWorldObject;

/**
 * adds water bodies, streams, rivers and fountains to the world
 */
public class WaterModule implements WorldModule {

	//TODO: add canal, ditch, drain

	private static final Tag WATER_TAG = new Tag("natural", "water");
	private static final Tag RIVERBANK_TAG = new Tag("waterway", "riverbank");
	
	private static final Map<String, Float> WATERWAY_WIDTHS;
	
	static {
		WATERWAY_WIDTHS = new HashMap<String, Float>();
		WATERWAY_WIDTHS.put("river", 3f);
		WATERWAY_WIDTHS.put("stream", 0.5f);
		WATERWAY_WIDTHS.put("canal", 2f);
		WATERWAY_WIDTHS.put("ditch", 1f);
		WATERWAY_WIDTHS.put("drain", 1f);
	}
	
	//TODO: apply to is almost always the same! create a superclass handling this!
	
	@Override
	public void applyTo(MapData grid) {
		
		for (MapWaySegment line : grid.getMapWaySegments()) {
			for (String value : WATERWAY_WIDTHS.keySet()) {
				if (line.getTags().contains("waterway", value)) {
					line.addRepresentation(new Waterway(line));
				}
			}
		}

		for (MapNode node : grid.getMapNodes()) {
			
			int connectedRivers = 0;
			
			for (MapWaySegment line : node.getConnectedWaySegments()) {				
				if (any(line.getRepresentations(), hasType(Waterway.class))) {
					connectedRivers += 1;
				}				
			}			
						
			if (connectedRivers > 2) {				
				node.addRepresentation(new RiverJunction(node));				
			}
			
		}

		for (MapArea area : grid.getMapAreas()) {
			if (area.getTags().contains(WATER_TAG)
					|| area.getTags().contains(RIVERBANK_TAG)) {
				area.addRepresentation(new Water(area));
			}
			if (area.getTags().contains("amenity", "fountain")) {
				area.addRepresentation(new AreaFountain(area));
			}
		}
		
	}
	
	public static class Waterway extends AbstractNetworkWaySegmentWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
		
		public Waterway(MapWaySegment line) {
			super(line);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0.5;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			//TODO: copypaste from road module (same in railway module)
			if (BridgeModule.isBridge(line.getTags())) {
				return GroundState.ABOVE;
			} else if (TunnelModule.isTunnel(line.getTags())) {
				return GroundState.BELOW;
			} else {
				return GroundState.ON;
			}
		}
		
		@Override
		public float getWidth() {
			String widthValue = line.getTags().getValue("width");
			if (widthValue != null) {
				Float width = ValueStringParser.parseMeasure(widthValue);
				if (width != null) {
					return width;
				}
			}
			return WATERWAY_WIDTHS.get(line.getTags().getValue("waterway"));			
		}
		
		@Override
		public PolygonXYZ getOutlinePolygon() {
			if (isContainedWithinRiverbank()) {
				return null;
			} else {
				return super.getOutlinePolygon();
			}
		}
		
		@Override
		public void renderTo(Target util) {
			
			//note: simply "extending" a river cannot work - unlike streets -
			//      because there can be islands within the riverbank polygon.
			//      That's why rivers will be *replaced* with Water areas instead.
			
			/* only draw the river if it doesn't have a riverbank */
			
			//TODO: handle case where a river is completely within riverbanks, but not a *single* riverbank
						
			if (! isContainedWithinRiverbank()) {
				
				List<VectorXYZ> leftOutline = getOutline(false);
				List<VectorXYZ> rightOutline = getOutline(true);
				
				List<VectorXYZ> leftWaterBorder = createLineBetween(
						leftOutline, rightOutline, 0.05f);
				List<VectorXYZ> rightWaterBorder = createLineBetween(
						leftOutline, rightOutline, 0.95f);
				
				modifyLineHeight(leftWaterBorder, -0.2f);
				modifyLineHeight(rightWaterBorder, -0.2f);
	
				List<VectorXYZ> leftGround = createLineBetween(
						leftOutline, rightOutline, 0.35f);
				List<VectorXYZ> rightGround = createLineBetween(
						leftOutline, rightOutline, 0.65f);
				
				modifyLineHeight(leftGround, -1);
				modifyLineHeight(rightGround, -1);
				
				/* render ground */
				
				util.drawTriangleStrip(EMPTY_GROUND, createVectorsForTriangleStripBetween(
						leftOutline, leftWaterBorder));
				util.drawTriangleStrip(EMPTY_GROUND, createVectorsForTriangleStripBetween(
						leftWaterBorder, leftGround));
				util.drawTriangleStrip(EMPTY_GROUND, createVectorsForTriangleStripBetween(
						leftGround, rightGround));
				util.drawTriangleStrip(EMPTY_GROUND, createVectorsForTriangleStripBetween(
						rightGround, rightWaterBorder));
				util.drawTriangleStrip(EMPTY_GROUND, createVectorsForTriangleStripBetween(
						rightWaterBorder, rightOutline));
	
				
				/* render water */
				
				VectorXYZ[] vs = WorldModuleGeometryUtil.createVectorsForTriangleStripBetween(
						leftWaterBorder, rightWaterBorder);
				
				util.drawTriangleStrip(WATER, vs);
				
			}
			
		}

		private boolean isContainedWithinRiverbank() {
			boolean containedWithinRiverbank = false; 
			
			for (MapOverlap<?,?> overlap : line.getOverlaps()) {				
				if (overlap.getOther(line) instanceof MapArea) {
					MapArea area = (MapArea)overlap.getOther(line);
					if (area.getPrimaryRepresentation() instanceof Water &&
							area.getPolygon().contains(line.getLineSegment())) {
						containedWithinRiverbank = true;
						break;
					}
				}				
			}
			return containedWithinRiverbank;
		}

		private static void modifyLineHeight(List<VectorXYZ> leftWaterBorder, float yMod) {
			for (int i = 0; i < leftWaterBorder.size(); i++) {
				VectorXYZ v = leftWaterBorder.get(i);
				leftWaterBorder.set(i, v.y(v.y+yMod));
			}
		}
		
	}

	public static class RiverJunction 
		extends JunctionNodeWorldObject
		implements NodeWorldObject, TerrainBoundaryWorldObject,
			RenderableToAllTargets {

		public RiverJunction(MapNode node) {
			super(node);
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0.5;
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
		public void renderTo(Target target) {
			
			//TODO: check whether it's within a riverbank (as with Waterway)
			
			List<VectorXYZ> vertices = getOutlinePolygon().getVertices();
			VectorXYZ[] vertexArray = vertices.toArray(new VectorXYZ[vertices.size()]);
			
			target.drawPolygon(WATER, vertexArray);
			
			//TODO: only cover with water to 0.95 * distance to center; add land below
			
		}
		
	}
	
	public static class Water extends NetworkAreaWorldObject 
		implements RenderableToAllTargets, RenderableToPOVRay,
		TerrainBoundaryWorldObject {
		
		//TODO: only cover with water to 0.95 * distance to center; add land below.
		// possible algorithm: for each node of the outer polygon, check whether it
		// connects to another water surface. If it doesn't move it inwards,
		// where "inwards" is calculated based on the two adjacent polygon segments.
		
		public Water(MapArea area) {
			super(area);
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0.5;
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
		public void renderTo(Target target) {						
			target.drawTriangles(WATER, getTriangulation());			
		}
		
		@Override
		public void renderTo(POVRayTarget target) {			
			renderTo((Target)target);
		}
		
	}
	
	private static class AreaFountain extends AbstractAreaWorldObject		
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {

		public AreaFountain(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
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
		public void renderTo(Target target) {

			/* render water */
				
			target.drawTriangles(WATER, getTriangulation());
			
			/* render walls */
			//note: mostly copy-pasted from BarrierModule
			
			double width=0.1;
			double height=0.5;
			
			VectorXYZ[] wallShape = {
					new VectorXYZ(-width/2, 0, 0),
					new VectorXYZ(-width/2, height, 0),
					new VectorXYZ(+width/2, height, 0),
					new VectorXYZ(+width/2, 0, 0)
				};
				
				List<VectorXYZ> path = 
					area.getElevationProfile().getWithEle(area.getOuterPolygon().getVertexLoop());
				
				List<VectorXYZ[]> strips = createShapeExtrusionAlong(wallShape,
						path, nCopies(path.size(), VectorXYZ.Y_UNIT));
				
				for (VectorXYZ[] strip : strips) {
					target.drawTriangleStrip(Materials.ASPHALT, strip);
				}
							
		}

	}
}
