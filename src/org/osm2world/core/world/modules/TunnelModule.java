package org.osm2world.core.world.modules;

import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createVectorsForTriangleStripBetween;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.VisibleConnectorNodeWorldObject;

/**
 * adds tunnels to the world.
 * 
 * Needs to be applied <em>after</em> all the modules that generate
 * whatever runs through the tunnels.
 */
public class TunnelModule extends AbstractModule {

	public static final boolean isTunnel(TagGroup tags) {
		return tags.containsKey("tunnel") && !"no".equals(tags.getValue("tunnel"));
	}

	public static final boolean isTunnel(MapSegment segment) {
		if (segment instanceof MapWaySegment) {
			return isTunnel(((MapWaySegment)segment).getTags());
		} else {
			return isTunnel(((MapAreaSegment)segment).getArea().getTags());
		}
	}
	
	@Override
	protected void applyToWaySegment(MapWaySegment segment) {
		
		WaySegmentWorldObject primaryRepresentation =
			segment.getPrimaryRepresentation();
		
		if (primaryRepresentation instanceof AbstractNetworkWaySegmentWorldObject
				&& isTunnel(segment)) {
			
			segment.addRepresentation(new Tunnel(segment,
					(AbstractNetworkWaySegmentWorldObject) primaryRepresentation));
			
		}
		
	}
	
	@Override
	protected void applyToNode(MapNode node) {
		
		/* entrances */
		
		if (node.getConnectedWaySegments().size() == 2) {
			
			MapWaySegment segmentA = node.getConnectedWaySegments().get(0);
			MapWaySegment segmentB = node.getConnectedWaySegments().get(1);
						
			if (isTunnel(segmentA) && !isTunnel(segmentB)
					&& segmentA.getPrimaryRepresentation() instanceof AbstractNetworkWaySegmentWorldObject) {
				node.addRepresentation(new TunnelEntrance(node, segmentA));
			} else if (isTunnel(segmentB) && !isTunnel(segmentA)
					&& segmentB.getPrimaryRepresentation() instanceof AbstractNetworkWaySegmentWorldObject) {
				node.addRepresentation(new TunnelEntrance(node, segmentB));
			}
			
		}
		
		/* tunnel nodes and junctions */
		
		boolean onlyTunnelConnected = true;
		for (MapWaySegment segment : node.getConnectedWaySegments()) {
			if (!isTunnel(segment)) {
				onlyTunnelConnected = false;
				break;
			}
		}
			
		if (onlyTunnelConnected) {

			if (node.getPrimaryRepresentation() instanceof VisibleConnectorNodeWorldObject) {
				//TODO: TunnelConnector
			} else if (node.getPrimaryRepresentation() instanceof JunctionNodeWorldObject) {
				node.addRepresentation(new TunnelJunction(node,
						(JunctionNodeWorldObject) node.getPrimaryRepresentation()));
			}
			
		}

		
	}
	
	public static class Tunnel implements WaySegmentWorldObject,
		RenderableToAllTargets {
		
		private final MapWaySegment segment;
		private final AbstractNetworkWaySegmentWorldObject primaryRep;
		
		public Tunnel(MapWaySegment segment,
				AbstractNetworkWaySegmentWorldObject primaryRepresentation) {
			this.segment = segment;
			this.primaryRep = primaryRepresentation;
		}
		
		@Override
		public MapElement getPrimaryMapElement() {
			return segment;
		}

		@Override
		public VectorXZ getEndPosition() {
			return primaryRep.getEndPosition();
		}

		@Override
		public VectorXZ getStartPosition() {
			return primaryRep.getStartPosition();
		}

		@Override
		public double getClearingAbove(VectorXZ pos) {
			return primaryRep.getClearingAbove(pos) + 1;
		}

		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			List<VectorXYZ> leftOutline = primaryRep.getOutline(false);
			List<VectorXYZ> rightOutline = primaryRep.getOutline(true);
			
			List<VectorXYZ> aboveLeftOutline =
				new ArrayList<VectorXYZ>(leftOutline.size());
			List<VectorXYZ> aboveRightOutline =
				new ArrayList<VectorXYZ>(rightOutline.size());
			
			for (int i=0; i < leftOutline.size(); i++) {
			
				VectorXYZ clearingOffset = VectorXYZ.Y_UNIT.mult(
						primaryRep.getClearingAbove(leftOutline.get(i).xz()));
				
				aboveLeftOutline.add(leftOutline.get(i).add(clearingOffset));
				aboveRightOutline.add(rightOutline.get(i).add(clearingOffset));
				
			}
			
			VectorXYZ[] strip1 = createVectorsForTriangleStripBetween(
					rightOutline, aboveRightOutline);
			VectorXYZ[] strip2 = createVectorsForTriangleStripBetween(
					aboveRightOutline, aboveLeftOutline);
			VectorXYZ[] strip3 = createVectorsForTriangleStripBetween(
					aboveLeftOutline, leftOutline);
			
			target.drawTriangleStrip(Materials.TUNNEL_DEFAULT, strip1);
			target.drawTriangleStrip(Materials.TUNNEL_DEFAULT, strip2);
			target.drawTriangleStrip(Materials.TUNNEL_DEFAULT, strip3);
					
		}
		
	}
	
	public static class TunnelEntrance implements NodeWorldObject,
		TerrainBoundaryWorldObject {
		
		private final MapNode node;
		private final MapWaySegment tunnelSegment;
		
		public TunnelEntrance(MapNode node, MapWaySegment tunnelSegment) {
			this.node = node;
			this.tunnelSegment = tunnelSegment;
		}
		
		@Override
		public MapElement getPrimaryMapElement() {
			return node;
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
		public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
			return new AxisAlignedBoundingBoxXZ(getOutlinePolygon().getVertices());
		}

		@Override
		public PolygonXYZ getOutlinePolygon() {
			
			AbstractNetworkWaySegmentWorldObject tunnelPrimaryRep =
				(AbstractNetworkWaySegmentWorldObject)tunnelSegment.getPrimaryRepresentation();
			
			VectorXZ cutVector = tunnelPrimaryRep.getCutVectorAt(node)
				.mult(tunnelPrimaryRep.getWidth() * 0.5f);
			
			VectorXZ directionIntoTunnel = tunnelSegment.getDirection();
			if (tunnelSegment.getEndNode() == node) {
				directionIntoTunnel = directionIntoTunnel.invert();
			}
			
			VectorXYZ toUpperVertices = directionIntoTunnel.mult(0.1).xyz(
					tunnelPrimaryRep.getClearingAbove(node.getPos()));
			
			List<VectorXYZ> vertexLoop = new ArrayList<VectorXYZ>(5);
			
			VectorXYZ lowerRight = node.getPos().add(cutVector).xyz(node.getElevationProfile().getMinEle());
			VectorXYZ lowerLeft = node.getPos().subtract(cutVector).xyz(node.getElevationProfile().getMinEle());
			
			vertexLoop.add(lowerRight);
			vertexLoop.add(lowerLeft);
			vertexLoop.add(lowerLeft.add(toUpperVertices));
			vertexLoop.add(lowerRight.add(toUpperVertices));
			vertexLoop.add(lowerRight);
			
			return new PolygonXYZ(vertexLoop);
			
		}
		
	}
	
	public static class TunnelJunction implements NodeWorldObject,
		RenderableToAllTargets {
	
		private final MapNode node;
		private final JunctionNodeWorldObject primaryRep;
		
		public TunnelJunction(MapNode node, JunctionNodeWorldObject primaryRep) {
			this.node = node;
			this.primaryRep = primaryRep;
		}
		
		@Override
		public MapElement getPrimaryMapElement() {
			return node;
		}
	
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return primaryRep.getClearingAbove(pos) + 1;
		}
	
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
	
		@Override
		public GroundState getGroundState() {
			return GroundState.BELOW;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			List<VectorXYZ> topOutline = new ArrayList<VectorXYZ>();
			
			int segCount = node.getConnectedSegments().size();
			for (int i=0; i<segCount; i++) {
				
				List<VectorXYZ> line = primaryRep.getOutline((i+1)%segCount, i);
				
				List<VectorXYZ> lineTop =
					new ArrayList<VectorXYZ>(line.size());
				
				for (VectorXYZ lineV : line) {
				
					double clearing;
					
					if (line.indexOf(lineV) == 0) {
						MapSegment segment = node.getConnectedSegments().get((i+1)%segCount);
						clearing = clearingAboveMapSegment(lineV, segment);
					} else {
						MapSegment segment = node.getConnectedSegments().get(i);
						clearing = clearingAboveMapSegment(lineV, segment);
					}
					
					lineTop.add(lineV.y(lineV.y + clearing));
					
				}
				
				// draw wall
				
				target.drawTriangleStrip(Materials.TUNNEL_DEFAULT,
						createVectorsForTriangleStripBetween(line, lineTop));
				
				//collect nodes for top outline
				
				topOutline.addAll(lineTop);
				
			}
			
			// draw top
			
			target.drawPolygon(Materials.TUNNEL_DEFAULT,
					topOutline.toArray(new VectorXYZ[0]));
			
		}

		private static double clearingAboveMapSegment(VectorXYZ lineV, MapSegment segment) {
			
			WorldObject segmentRep;
			if (segment instanceof MapWaySegment) {
				segmentRep = ((MapWaySegment)segment)
					.getPrimaryRepresentation();
			} else {
				segmentRep = ((MapAreaSegment)segment)
					.getArea().getPrimaryRepresentation();
			}
			
			if (segmentRep != null) {
				return segmentRep.getClearingAbove(lineV.xz());
			} else {
				return 0;
			}
			
		}
		
	}
	
}
