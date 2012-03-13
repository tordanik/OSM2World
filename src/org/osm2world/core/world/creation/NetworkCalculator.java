package org.osm2world.core.world.creation;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject;
import org.osm2world.core.world.network.NetworkWaySegmentWorldObject;
import org.osm2world.core.world.network.VisibleConnectorNodeWorldObject;

/**
 * class that will calculate the information for those {@link MapElement}s
 * that implement an interface from org.o3dw.representation.network and set it.
 * Calling this is necessary for those representations to work properly!
 */
public class NetworkCalculator {
	
	private NetworkCalculator() {}
	
	
	
	//FIXME: must be able to handle connectors with lines of opposite direction!
	
	
	
	private static final float ROAD_PUSHING_STEP = 0.01f;
	
	
	/**
	 * calculates cut and offset information for all
	 * NetworkNode/Line/AreaRepresentations of elements in a grid.
	 */
	public static void calculateNetworkInformationInGrid(MapData grid) {
		
		for (MapNode node : grid.getMapNodes()) {
			
			
			//TODO: also work with nodes that aren't Network*NodeRepresentations,
			//  but connect two NetworkWaySegmentRep.s (invisible connectors)
			
			final List<MapWaySegment> inboundNLines = new ArrayList<MapWaySegment>();
			List<MapWaySegment> outboundNLines = new ArrayList<MapWaySegment>();
			
			for (MapWaySegment line : node.getInboundLines()) {
				if (line.getPrimaryRepresentation() instanceof NetworkWaySegmentWorldObject) {
					inboundNLines.add(line);
				}
			}
			
			for (MapWaySegment line : node.getOutboundLines()) {
				if (line.getPrimaryRepresentation() instanceof NetworkWaySegmentWorldObject) {
					outboundNLines.add(line);
				}
			}

			
			if (node.getPrimaryRepresentation() instanceof JunctionNodeWorldObject) {

				/* junctions */
				
				calculateJunctionNodeEffects(node,
						(JunctionNodeWorldObject)node.getPrimaryRepresentation(),
						inboundNLines, outboundNLines);
				
			} else if (inboundNLines.size() + outboundNLines.size() == 2) {
				
				/* visible or invisible connectors */
				
				List<MapWaySegment> connectedNLines = new ArrayList<MapWaySegment>(2);
				connectedNLines.addAll(inboundNLines);
				connectedNLines.addAll(outboundNLines);
				
				MapWaySegment line1 = connectedNLines.get(0);
				MapWaySegment line2 = connectedNLines.get(1);
				
				calculateConnectorNodeEffects(node.getPrimaryRepresentation(),
						line1, line2,
						inboundNLines.contains(line1),
						inboundNLines.contains(line2));
				
			} else {
				
				for (MapWaySegment outboundNLine : outboundNLines) {
					setOrthogonalCutVector(outboundNLine, true);
				}

				for (MapWaySegment inboundNLine : inboundNLines) {
					setOrthogonalCutVector(inboundNLine, false);
				}
				
			}
			
		}
		
	}
	
	/**
	 * calculates the effects of both visible and invisible connector nodes.
	 */
	private static void calculateConnectorNodeEffects(
			NodeWorldObject nodeRepresentation,
			MapWaySegment line1, MapWaySegment line2,
			boolean inbound1, boolean inbound2) {
					
		NetworkWaySegmentWorldObject renderable1 =
			((NetworkWaySegmentWorldObject)line1.getPrimaryRepresentation());
		NetworkWaySegmentWorldObject renderable2 =
			((NetworkWaySegmentWorldObject)line2.getPrimaryRepresentation());
		
		VisibleConnectorNodeWorldObject visibleConnectorRep = null;
		
		if (nodeRepresentation instanceof VisibleConnectorNodeWorldObject) {
			visibleConnectorRep =
				(VisibleConnectorNodeWorldObject)nodeRepresentation;
		}
		
		/* calculate cut as angle bisector between the two lines */
		
		VectorXZ inVector = line1.getDirection();
		VectorXZ outVector = line2.getDirection();

		if (!inbound1) { inVector = inVector.invert(); }
		if (inbound2) { outVector = outVector.invert(); }
		
		VectorXZ cutVector;
		
		if (inVector.equals(outVector)) { //TODO: allow for some small difference?
			cutVector = outVector.rightNormal();
		} else {
			cutVector = outVector.subtract(inVector);
			cutVector = cutVector.normalize();
		}
		
		//make sure that cutVector points to the right, which is equivalent to:
		//y component of the cross product (inVector x cutVector) is positive.
		//If this isn't the case, invert the cut vector.
		if (inVector.z * cutVector.x - inVector.x * cutVector.z <= 0) {
			cutVector = cutVector.invert();
		}
		
		/* set calculated cut vector */
		
		if (inbound1) {
			renderable1.setEndCutVector(cutVector);
		} else {
			renderable1.setStartCutVector(cutVector.invert());
		}
		
		if (inbound2) {
			renderable2.setEndCutVector(cutVector.invert());
		} else {
			renderable2.setStartCutVector(cutVector);
		}
		
		/* perform calculations necessary for connectors
		 * whose representation requires space */
		
		double connectorLength = 0;
		
		if (visibleConnectorRep != null) {
			connectorLength = visibleConnectorRep.getLength();
		}
		
		if (connectorLength > 0) {
									
			/* move connected lines to make room for the node's representation */
			
			//connected node of line1 is moved orthogonally to the cut vector
			VectorXZ offset1 = cutVector.rightNormal();
			offset1 = offset1.mult(connectorLength / 2);
			if (inbound1) {
				renderable1.setEndOffset(offset1);
			} else {
				renderable1.setStartOffset(offset1);
			}
			
			//node of line2 is moved into the opposite direction
			VectorXZ offset2 = offset1.invert();
			if (inbound2) {
				renderable2.setEndOffset(offset2);
			} else {
				renderable2.setStartOffset(offset2);
			}
								
			/* provide information to node's representation */
			
			if (nodeRepresentation instanceof VisibleConnectorNodeWorldObject) {
				
				VisibleConnectorNodeWorldObject connectorRep =
					(VisibleConnectorNodeWorldObject)nodeRepresentation;
				
				VectorXZ connectedPos1;
				VectorXZ connectedPos2;
				
				if (inbound1) {
					connectedPos1 = line1.getEndNode().getPos();
				} else {
					connectedPos1 = line1.getStartNode().getPos();
				}
				
				if (inbound2) {
					connectedPos2 = line2.getEndNode().getPos();
				} else {
					connectedPos2 = line2.getStartNode().getPos();
				}
				
				connectorRep.setInformation(
						cutVector,
						connectedPos1.add(offset1),
						connectedPos2.add(offset2),
						renderable1.getWidth(),
						renderable2.getWidth());
				
			}
			
			//TODO: if done properly, this might affect NOT ONLY the directly adjacent lines
			
		}
		
	}

	private static void calculateJunctionNodeEffects(
			MapNode node, JunctionNodeWorldObject nodeRepresentation,
			final List<MapWaySegment> inboundNLines, List<MapWaySegment> outboundNLines) {
		
		/* create list of all connected roads.
		 * Order of adds is important, it needs to match
		 * the order of cutVectors, coords and widths adds. */
		
		List<MapWaySegment> connectedNSegments = new ArrayList<MapWaySegment>();
		connectedNSegments.addAll(inboundNLines);
		connectedNSegments.addAll(outboundNLines);
				
		//all cut vectors in here will point to the right from the junctions pov!
		List<VectorXZ> cutVectors = new ArrayList<VectorXZ>(connectedNSegments.size());
		List<VectorXZ> coords = new ArrayList<VectorXZ>(connectedNSegments.size());
		List<Float> widths = new ArrayList<Float>(connectedNSegments.size());
		
		/* determine cut angles:
		 * always orthogonal to the connected line */
				
		for (MapWaySegment in : inboundNLines) {
			
			NetworkWaySegmentWorldObject inRenderable =
				((NetworkWaySegmentWorldObject)in.getPrimaryRepresentation());
			
			VectorXZ cutVector = in.getRightNormal();
			inRenderable.setEndCutVector(cutVector);
			cutVectors.add(cutVector.invert());
			
			coords.add(in.getEndNode().getPos());
			widths.add(inRenderable.getWidth());
			
		}

		for (MapWaySegment out : outboundNLines) {
			
			NetworkWaySegmentWorldObject outRenderable =
				((NetworkWaySegmentWorldObject)out.getPrimaryRepresentation());
			
			VectorXZ cutVector = out.getRightNormal();
			outRenderable.setStartCutVector(cutVector);
			cutVectors.add(cutVector);
			
			coords.add(out.getStartNode().getPos());
			widths.add(outRenderable.getWidth());
			
		}
		
		/* move roads away from the intersection until they cannot overlap anymore,
		 * this is certain if the distance between their ends' center points
		 * is greater than the sum of their half-widths */
		
		//TODO (performance) if roads were ordered by angle here already, this would be much faster -> only neighbors checked
		
		boolean overlapPossible;
		
		do {
			
			overlapPossible = false;
			
			overlapCheck:
			for (int r1=0; r1 < coords.size(); r1++) {
				for (int r2=r1+1; r2 < coords.size(); r2++) {
					
					/* ignore overlapping (or almost overlapping) way segments
					 * as no reasonable amount of pushing would separate these */
					if (VectorXZ.distance(connectedNSegments.get(r1).getDirection(),
							connectedNSegments.get(r2).getDirection()) < 0.1
						||	VectorXZ.distance(connectedNSegments.get(r1).getDirection(),
								connectedNSegments.get(r2).getDirection().invert()) < 0.1) {
						continue;
					}
					
					double distance = Math.abs(coords.get(r1).subtract(coords.get(r2)).length());
					
					if (distance > 200) {
						//TODO: proper error handling
						System.err.println("distance has exceeded 200 at node " + node
								+ "\n (representation: " + nodeRepresentation + ")");
						// overlapCheck will remain false, no further size increase
						break overlapCheck;
					}
					
					if (distance <= widths.get(r1)*0.5 + widths.get(r2)*0.5) {
						overlapPossible = true;
						break overlapCheck;
					}
					
				}
			}
		
			if (overlapPossible) {
				
				/* push outwards */
				
				coords.clear();
				
				for (MapWaySegment in : inboundNLines) {
					
					NetworkWaySegmentWorldObject inRenderable =
						((NetworkWaySegmentWorldObject)in.getPrimaryRepresentation());
					VectorXZ inVector = in.getDirection();
					
					VectorXZ offsetModification = inVector.mult(-ROAD_PUSHING_STEP);
					
					VectorXZ newEndOffset = inRenderable.getEndOffset().add(offsetModification);
					inRenderable.setEndOffset(newEndOffset);
					coords.add(in.getEndNode().getPos().add(newEndOffset));
					
				}

				for (MapWaySegment out : outboundNLines) {
					
					NetworkWaySegmentWorldObject outRenderable =
						((NetworkWaySegmentWorldObject)out.getPrimaryRepresentation());
					VectorXZ outVector = out.getDirection();
					
					VectorXZ offsetModification = outVector.mult(ROAD_PUSHING_STEP);
					
					VectorXZ newStartOffset = outRenderable.getStartOffset().add(offsetModification);
					outRenderable.setStartOffset(newStartOffset);
					coords.add(out.getStartNode().getPos().add(newStartOffset));
					
				}
				
			}
			
		} while(overlapPossible);

		/* set calculated information using the correct order */
		
		List<MapSegment> segments = node.getConnectedSegments();
				
		ArrayList<VectorXZ> junctionCutCenters = new ArrayList<VectorXZ>(segments.size());
		ArrayList<VectorXZ> junctionCutVectors = new ArrayList<VectorXZ>(segments.size());
		ArrayList<Float> junctionWidths = new ArrayList<Float>(segments.size());
		
		for (MapSegment segment : segments) {
			
			if (connectedNSegments.contains(segment)) {

				int index = connectedNSegments.indexOf(segment);
				
				junctionCutCenters.add(coords.get(index));
				junctionCutVectors.add(cutVectors.get(index));
				junctionWidths.add(widths.get(index));
				
			} else {
				
				junctionCutCenters.add(null);
				junctionCutVectors.add(null);
				junctionWidths.add(null);
				
			}
			
		}
		
		nodeRepresentation.setInformation(
				junctionCutCenters, junctionCutVectors, junctionWidths);
		
	}

	/**
	 * @param l  line with {@link NetworkWaySegmentWorldObject} as representation
	 */
	private static void setOrthogonalCutVector(MapWaySegment l, boolean setStartVector) {

		VectorXZ cutVector = l.getRightNormal();
		
		NetworkWaySegmentWorldObject lRepresentation =
			(NetworkWaySegmentWorldObject)l.getPrimaryRepresentation();
		
		if (setStartVector) {
			lRepresentation.setStartCutVector(cutVector);
		} else {
			lRepresentation.setEndCutVector(cutVector);
		}
		
	}
	
}
