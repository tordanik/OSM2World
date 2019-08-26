package org.osm2world.core.world.modules.common;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RoadModule.Road;

/**
 * A class containing all the necessary information
 * to render a traffic sign.
 *
 * @author Jason Manoloudis
 *
 */
public class TrafficSignModel {


	/** The {@link TrafficSignType}s on this sign */
	public List<TrafficSignType> types;

	/** The position this TrafficSignModel will be rendered on */
	public VectorXZ position;

	/** The direction the {@link TrafficSignModel#types} are facing */
	public double direction;

	/** The {@link org.osm2world.core.map_data.data.MapNode} the position of this model is relevant to */
	public MapNode node;

	public TrafficSignModel(MapNode node) {
		this.node = node;
	};

	/**
	 * Check if the top (bottom) of the given {@code way} is connected
	 * to another way that contains the same key-value tag. Applicable to
	 * traffic sign mapping cases where signs must be rendered both at
	 * the beginning and the end of the way they apply to
	 *
	 * @param topOrBottom A boolean value indicating whether to check the
	 * beginning or end of the {@code way}. True for beginning, false for end.
	 */
	public static boolean adjacentWayContains(boolean topOrBottom, MapWay way, String key, String value) {

		//decide whether to check beginning or end of the way
		int index = 0;
		MapNode wayNode;

		if(!topOrBottom) {

			//check the last node of the last segment

			index = way.getWaySegments().size()-1;

			wayNode = way.getWaySegments().get(index).getEndNode();

		}else {

			//check the first node of the first segment

			wayNode = way.getWaySegments().get(index).getStartNode();
		}

		//if the node is also part of another way
		if(wayNode.getConnectedSegments().size()==2) {

			for(MapWaySegment segment : wayNode.getConnectedWaySegments()) {

				//if current segment is part of this other way
				if(!segment.getWay().equals(way)) {

					//check if this other way also contains the key-value pair
					MapWay nextWay = segment.getWay();

					if(nextWay.getTags().contains(key, value)) {
						return true;
					}else {
						return false;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Calculate the position this model will be rendered to, based on the
	 * {@code side} of the road it is supposed to be and the width of the road
	 */
	public void calculatePosition(MapWaySegment segment, boolean side) {

		//if way is not a road
		if(!RoadModule.isRoad(segment.getTags())) {
			this.position = node.getPos();
			return;
		}

		//get the rendered segment's width
		Road r = (Road) segment.getPrimaryRepresentation();

		double roadWidth = r.getWidth();

		//rightNormal() vector will always be orthogonal to the segment, no matter its direction,
		//so we use that to place the signs to the left/right of the way
		VectorXZ rightNormal = segment.getDirection().rightNormal();

		if(side) {
			this.position = node.getPos().add(rightNormal.mult(-roadWidth));
			return;
		}else {
			this.position = node.getPos().add(rightNormal.mult(roadWidth));
			return;
		}
	}

	/**
	 * Calculate the rotation angle of the model based
	 * on the direction of {@code segment}. The segment
	 * will always be either the first one of it's way
	 * or the last one.
	 */
	public void calculateDirection(MapWaySegment segment) {

		//segments of the way this node is part of
		List<MapWaySegment> segments = node.getConnectedWaySegments().get(0).getWay().getWaySegments();

		VectorXZ wayDir = segment.getDirection();

		//handle ways with only 1 segment
		if(segments.size()==1) {

			//if the node is the segment's first node, face the incoming traffic
			if(node.equals(segments.get(0).getStartNode())) {
				wayDir = wayDir.invert();
			}

			this.direction = wayDir.angle();
			return;
		}

		//if the segment is the first one on it's way,
		//have the sign face the opposite direction (the incoming vehicles)
		if(segment.equals(segments.get(0))) {

			wayDir = wayDir.invert();
			this.direction = wayDir.angle();

		//if it is the last one, make the signs face the segment's direction,
		//for 2-way traffic
		}else if(segment.equals(segments.get(segments.size()-1))) {

			this.direction = wayDir.angle();
		}
	}

	/**
	 * Same as {@link #calculateDirection(MapWaySegment)} but also parses the node's
	 * tags for the direction specification and returns true if it is found. False otherwise.
	 * The segment to extract the direction from is the segment the node is part of.
	 */
	public void calculateDirection() {

		String regex = "traffic_sign:(forward|backward)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher;

		VectorXZ wayDir = node.getConnectedWaySegments().get(0).getDirection();

		for(Tag tag : node.getTags()) {

			matcher = pattern.matcher(tag.key);

			if(matcher.matches()) {

				if(matcher.group(1).equals("backward")) {
					this.direction = wayDir.angle();
					return;
				}
			}
		}

		//Either traffic_sign:forward is specified or no forward/backward is specified at all.
		//traffic_sign:forward affects vehicles moving in the same direction as
		//the way. Therefore they must face the sign so the way's direction is inverted.
		//Vehicles facing the sign is the most common case so it is set as the default case
		//as well
		wayDir = wayDir.invert();
		this.direction = wayDir.angle();
		return;
	}
}
