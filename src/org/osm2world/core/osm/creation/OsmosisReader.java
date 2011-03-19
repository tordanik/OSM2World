package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import static org.openstreetmap.josm.plugins.graphview.core.data.EmptyTagGroup.*;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.osm.data.OSMMember;
import org.osm2world.core.osm.data.OSMNode;
import org.osm2world.core.osm.data.OSMRelation;
import org.osm2world.core.osm.data.OSMWay;

/**
 * DataSource providing information from a single .osm file. The file is read
 * during the constructor call, there will be no updates when the file is
 * changed later. This class uses osmosis to read the file.
 */
public class OsmosisReader implements OSMDataReader {
	
	private boolean complete = false;
	
	private synchronized boolean isComplete() {
		return complete;
	}
	
	private synchronized void setCompleteTrue() {
		this.complete = true;
	}
	
	private Map<Long, Node> nodesById = new HashMap<Long, Node>();
	private Map<Long, Way> waysById = new HashMap<Long, Way>();
	private Map<Long, Relation> relationsById = new HashMap<Long, Relation>();
	
	private Collection<OSMNode> ownNodes;
	private Collection<OSMWay> ownWays;
	private Collection<OSMRelation> ownRelations;
	
	private final Sink sinkImplementation = new Sink() {
		public void release() {
			/* do nothing */
		}		
		public void complete() {
			setCompleteTrue();
		}		
		public void process(EntityContainer entityContainer) {
			Entity entity = entityContainer.getEntity();
			if (entity instanceof Node) {
				nodesById.put(entity.getId(), ((Node) entity));
			} else if (entity instanceof Way) {
				waysById.put(entity.getId(), ((Way) entity));
			} else if (entity instanceof Relation) {
				relationsById.put(entity.getId(), ((Relation) entity));
			}
		}
	};
	
	public OsmosisReader(File file) throws IOException {
		
		CompressionMethod compression = CompressionMethod.None;
				
		if (file.getName().endsWith(".gz")) {
			compression = CompressionMethod.GZip;
		} else if (file.getName().endsWith(".bz2")) {
			compression = CompressionMethod.BZip2;
		}
		
		XmlReader reader = new XmlReader(file, false, compression);
		reader.setSink(sinkImplementation);
		
		Thread readerThread = new Thread(reader);
		readerThread.start();
		
		while (readerThread.isAlive()) {
			try {
				readerThread.join();
			} catch (InterruptedException e) { /* do nothing */
			}
		}
		
		if (!isComplete()) {
			throw new IOException("couldn't read from file");
		}
		
		convertToOwnRepresentation();
		
	}
	
	private void convertToOwnRepresentation() {
		
		ownNodes = new ArrayList<OSMNode>(nodesById.size());
		ownWays = new ArrayList<OSMWay>(waysById.size());
		ownRelations = new ArrayList<OSMRelation>(relationsById.size());
		
		Map<Node, OSMNode> nodeMap = new HashMap<Node, OSMNode>();
		Map<Way, OSMWay> wayMap = new HashMap<Way, OSMWay>();
		Map<Relation, OSMRelation> relationMap = new HashMap<Relation, OSMRelation>();
		
		for (Node node : nodesById.values()) {
			
			OSMNode ownNode = new OSMNode(node.getLatitude(), node
					.getLongitude(), tagGroupForEntity(node), node.getId());
			
			ownNodes.add(ownNode);
			nodeMap.put(node, ownNode);
			
		}
		
		for (Way way : waysById.values()) {
			
			List<WayNode> origWayNodes = way.getWayNodes();
			List<OSMNode> wayNodes = new ArrayList<OSMNode>(origWayNodes.size());
			for (WayNode origWayNode : origWayNodes) {
				Node origNode = nodesById.get(origWayNode.getNodeId());
				if (origNode != null) {
					wayNodes.add(nodeMap.get(origNode));
				}
			}
			
			OSMWay ownWay = new OSMWay(tagGroupForEntity(way),
					way.getId(), wayNodes);
			
			ownWays.add(ownWay);
			wayMap.put(way, ownWay);
			
		}
		
		for (Relation relation : relationsById.values()) {
			
			OSMRelation ownRelation = new OSMRelation(
					tagGroupForEntity(relation), relation.getId(),
					relation.getMembers().size());
			
			ownRelations.add(ownRelation);
			relationMap.put(relation, ownRelation);
			
		}
		
		// add relation members
		// (needs to be done *after* creation because relations can be members
		// of other relations)
		
		for (Relation relation : relationMap.keySet()) {
			
			OSMRelation ownRelation = relationMap.get(relation);
			
			for (org.openstreetmap.osmosis.core.domain.v0_6.RelationMember member : relation
					.getMembers()) {
								
				OSMElement memberObject = null;
				if (member.getMemberType() == EntityType.Node) {
					memberObject = nodeMap.get(nodesById.get(member
							.getMemberId()));
				} else if (member.getMemberType() == EntityType.Way) {
					memberObject = wayMap.get(waysById
							.get(member.getMemberId()));
				} else if (member.getMemberType() == EntityType.Relation) {
					memberObject = relationMap.get(relationsById.get(member
							.getMemberId()));
				} else {
					continue;
				}
				
				if (memberObject != null) {
					
					OSMMember ownMember = new OSMMember(member
							.getMemberRole(), memberObject);
					
					ownRelation.relationMembers.add(ownMember);
					
				}
				
			}
			
		}
		
		// give up references to original collections
		
		nodesById = null;
		waysById = null;
		relationsById = null;
		
	}
	
	private TagGroup tagGroupForEntity(Entity entity) {
		if (entity.getTags().isEmpty()) {
			return EMPTY_TAG_GROUP;
		} else {
			Map<String, String> tagMap = new HashMap<String, String>(entity.getTags().size());
			for (Tag tag : entity.getTags()) {
				tagMap.put(tag.getKey(), tag.getValue());
			}
			return new MapBasedTagGroup(tagMap);
		}
	}
	
	@Override
	public OSMData getData() {		
		return new OSMData(ownNodes, ownWays, ownRelations);
	}
	
}
