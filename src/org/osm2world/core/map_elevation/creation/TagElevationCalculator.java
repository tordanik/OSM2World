package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.TerrainElevationData;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;

/**
 * relies on tags that explicitly set elevation.
 * Subclasses determine the tag(s) to be used for this purpose.
 */
public abstract class TagElevationCalculator implements ElevationCalculator {
	
	Double terrainElevation;
	boolean enableUnknownEleWarning;
	
	/**
	 * @param terrainElevation  elevation for the terrain
	 */
	public TagElevationCalculator(Double terrainElevation,
			boolean enableUnknownEleWarning) {
		this.terrainElevation = terrainElevation;
		this.enableUnknownEleWarning = enableUnknownEleWarning;
	}
	
	public TagElevationCalculator() {
		this(0.0, false);
	}
	
	@Override
	public void calculateElevations(MapData mapData,
			TerrainElevationData eleData) {

//		//TODO replace old ElevationProfile stuff
//
//		/* set nodes' elevation profiles */
//
//		for (MapNode node : mapData.getMapNodes()) {
//
//			Double ele = getEleForTags(node.getTags());
//
//			if (ele == null) {
//
//				/* use elevation information from nodes or areas containing
//				 * this node. If they have contradicting information, the
//				 * results will be unpredictable.
//				 */
//
//				for (MapSegment segment : node.getConnectedSegments()) {
//					if (ele == null) {
//						TagGroup tags;
//						if (segment instanceof MapWaySegment) {
//							tags = ((MapWaySegment) segment).getTags();
//						} else {
//							tags = ((MapAreaSegment) segment).getArea().getTags();
//						}
//						ele = getEleForTags(tags);
//					}
//				}
//
//			}
//
//			if (ele != null) {
//
//				NodeElevationProfile profile = new NodeElevationProfile(node);
//				profile.setEle(ele);
//				node.setElevationProfile(profile);
//
//			}
//
//		}
//
//		/* set elevation profiles for nodes without elevation profiles,
//		 * attempt interpolation the two closest connected nodes with ele */
//
//		for (MapNode node : mapData.getMapNodes()) {
//
//			if (node.getElevationProfile() == null) {
//
//				List<Connection> connections = new ArrayList<Connection>();
//
//				for (MapSegment segment : node.getConnectedSegments()) {
//
//					Connection connection =
//						findConnectionToNodeWithEle(node, segment);
//
//					if (connection != null) {
//						connections.add(connection);
//					}
//
//				}
//
//				// sort by length
//				Collections.sort(connections, new Comparator<Connection>() {
//					public int compare(Connection c1, Connection c2) {
//						return Double.compare(c1.getLength(), c2.getLength());
//					};
//				});
//
//				double ele = 0;
//
//				if (connections.size() < 2) {
//					if (enableUnknownEleWarning) {
//						System.err.println("node without ele information: " + node);
//					}
//				} else {
//
//					/* interpolate between 2 closest connected nodes */
//
//					Connection cA = connections.get(0);
//					Connection cB = connections.get(1);
//					double eleA = cA.endNode.getElevationProfile().getEle();
//					double eleB = cB.endNode.getElevationProfile().getEle();
//
//					ele = ((eleA * cB.getLength()) + (eleB * cA.getLength()))
//						/ (cA.getLength() + cB.getLength());
//
//				}
//
//				NodeElevationProfile profile = new NodeElevationProfile(node);
//				profile.setEle(ele);
//				node.setElevationProfile(profile);
//
//			}
//
//		}
//
//		/* set way segments' elevation profiles (based on nodes' elevations) */
//
//		for (MapWaySegment segment : mapData.getMapWaySegments()) {
//
//			if (segment.getPrimaryRepresentation() == null) continue;
//
//			WaySegmentElevationProfile profile =
//				new WaySegmentElevationProfile(segment);
//
//			profile.addPointWithEle(
//				segment.getStartNode().getElevationProfile().getPointWithEle());
//			profile.addPointWithEle(
//				segment.getEndNode().getElevationProfile().getPointWithEle());
//
//			segment.setElevationProfile(profile);
//
//		}
//
//		/* set areas' elevation profiles (based on nodes' elevations) */
//
//		for (MapArea area : mapData.getMapAreas()) {
//
//			if (area.getPrimaryRepresentation() == null) continue;
//
//			AreaElevationProfile profile =
//				new AreaElevationProfile(area);
//
//			for (MapNode node : area.getBoundaryNodes()) {
//				profile.addPointWithEle(
//					node.getElevationProfile().getPointWithEle());
//			}
//
//			for (List<MapNode> holeOutline : area.getHoles()) {
//				for (MapNode node : holeOutline) {
//					profile.addPointWithEle(
//						node.getElevationProfile().getPointWithEle());
//				}
//			}
//
//			area.setElevationProfile(profile);
//
//		}
		
	}
	
	/**
	 * a sequence of {@link MapSegment}s
	 * that leads to a {@link MapNode} at the end
	 */
	private static class Connection {
		
		public final List<MapSegment> segmentSequence;
		public final MapNode endNode;
		
		public Connection(List<MapSegment> segmentSequence, MapNode endNode) {
			this.segmentSequence = segmentSequence;
			this.endNode = endNode;
		}
		
		public double getLength() {
			double distance = 0.0;
			for (MapSegment s : segmentSequence) {
				distance += s.getLineSegment().getLength();
			}
			return distance;
		}
		
	}
	
	/**
	 * Tries to find the segment sequence to a node with elevation information.
	 * More precisely, this follows the sequence of segments started by
	 * the specified first segment until ... <ul>
	 * <li>... a node with elevation information is found and is returned.</li>
	 * <li>... the sequence segment branches, null is returned.</li>
	 * <li>... the sequence returns to the start, null is returned.</li>
	 * </ul>
	 */
	//TODO replace old ElevationProfile stuff
//	private static final Connection findConnectionToNodeWithEle(
//			MapNode node, MapSegment firstSegmentForSequence) {
//
//		List<MapSegment> segmentSequence = new ArrayList<MapSegment>();
//
//		MapNode currentNode = node;
//		MapSegment currentSegment = firstSegmentForSequence;
//
//		while (true) { //one of the return conditions will ultimately be true
//
//			segmentSequence.add(currentSegment);
//
//			currentNode = currentSegment.getOtherNode(currentNode);
//
//			if (currentNode.getElevationProfile() != null) {
//				return new Connection(segmentSequence, currentNode);
//			} else if (currentNode == node) {
//				return null;
//			} else if (currentNode.getConnectedSegments().size() > 2) {
//				return null;
//			}
//
//			// use the one connected segment that is not the current segment
//			// as the next current segment
//
//			for (MapSegment s : currentNode.getConnectedSegments()) {
//				if (s != currentSegment) {
//					currentSegment = s;
//					break;
//				}
//			}
//
//		}
//
//	}
	
	/**
	 * returns the elevation as set explicitly by the tags
	 * 
	 * @return  elevation; null if the tags don't define the elevation
	 */
	protected abstract Double getEleForTags(TagGroup tags);

}
