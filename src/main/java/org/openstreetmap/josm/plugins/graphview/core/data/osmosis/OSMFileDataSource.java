package org.openstreetmap.josm.plugins.graphview.core.data.osmosis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.plugins.graphview.core.data.DataSource;
import org.openstreetmap.josm.plugins.graphview.core.data.DataSourceObserver;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
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

/**
 * DataSource providing information from a single .osm file. The file is read
 * during the constructor call, there will be no updates when the file is
 * changed later. This class uses osmosis to read the file.
 */
public class OSMFileDataSource implements
		DataSource<OSMFileDataSource.OwnNode, OSMFileDataSource.OwnWay,
		           OSMFileDataSource.OwnRelation, OSMFileDataSource.OwnMember> {
	
	private static final boolean useDebugLabels = true;
	
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
	
	private Collection<OwnNode> ownNodes;
	private Collection<OwnWay> ownWays;
	private Collection<OwnRelation> ownRelations;
	
	private final Sink sinkImplementation = new Sink() {
		@Override public void initialize(Map<String, Object> arg0) {
			/* do nothing */
		}
		@Override public void close() {
			/* do nothing */
		}
		@Override public void complete() {
			setCompleteTrue();
		}
		@Override public void process(EntityContainer entityContainer) {
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
	
	public OSMFileDataSource(File file) throws IOException {
		
		XmlReader reader = new XmlReader(file, false, CompressionMethod.None);
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
		
		ownNodes = new ArrayList<OwnNode>(nodesById.size());
		ownWays = new ArrayList<OwnWay>(waysById.size());
		ownRelations = new ArrayList<OwnRelation>(relationsById.size());
		
		Map<Node, OwnNode> nodeMap = new HashMap<Node, OwnNode>();
		Map<Way, OwnWay> wayMap = new HashMap<Way, OwnWay>();
		Map<Relation, OwnRelation> relationMap = new HashMap<Relation, OwnRelation>();
		
		for (Node node : nodesById.values()) {
			
			OwnNode ownNode = new OwnNode(node.getLatitude(), node
					.getLongitude(), tagGroupForEntity(node));
			
			ownNodes.add(ownNode);
			nodeMap.put(node, ownNode);
			
		}
		
		for (Way way : waysById.values()) {
			
			List<WayNode> origWayNodes = way.getWayNodes();
			List<OwnNode> wayNodes = new ArrayList<OwnNode>(origWayNodes.size());
			for (WayNode origWayNode : origWayNodes) {
				Node origNode = nodesById.get(origWayNode.getNodeId());
				wayNodes.add(nodeMap.get(origNode));
			}
			
			OwnWay ownWay = new OwnWay(tagGroupForEntity(way), wayNodes);
			
			ownWays.add(ownWay);
			wayMap.put(way, ownWay);
			
		}
		
		for (Relation relation : relationsById.values()) {
			
			OwnRelation ownRelation = new OwnRelation(
					tagGroupForEntity(relation), relation.getMembers().size());
			
			ownRelations.add(ownRelation);
			relationMap.put(relation, ownRelation);
			
		}
		
		// add relation members
		// (needs to be done *after* creation because relations can be members
		// of other relations)
		
		for (Relation relation : relationMap.keySet()) {
			
			OwnRelation ownRelation = relationMap.get(relation);
			
			for (org.openstreetmap.osmosis.core.domain.v0_6.RelationMember member : relation
					.getMembers()) {
				
				Object memberObject = null;
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
				
				OwnMember ownMember = new OwnMember(member
						.getMemberRole(), memberObject);
				
				ownRelation.relationMembers.add(ownMember);
			}
			
		}
		
		// give up references to original collections
		
		nodesById = null;
		waysById = null;
		relationsById = null;
		
	}
	
	private TagGroup tagGroupForEntity(Entity entity) {
		Map<String, String> tagMap = new HashMap<String, String>(entity.getTags().size());
		for (Tag tag : entity.getTags()) {
			tagMap.put(tag.getKey(), tag.getValue());
		}
		return new MapBasedTagGroup(tagMap);
	}
	
	public void addObserver(DataSourceObserver observer) {
		// OSMFileDataSource doesn't check for updates
	}
	
	public void deleteObserver(DataSourceObserver observer) {
		// OSMFileDataSource doesn't check for updates
	}
	
	public double getLat(OwnNode node) {
		return node.lat;
	}
	
	public double getLon(OwnNode node) {
		return node.lon;
	}
	
	public List<OwnMember> getMembers(OwnRelation relation) {
		return relation.relationMembers;
	}
	
	public Collection<OwnNode> getNodes() {
		return ownNodes;
	}
	
	public Collection<OwnWay> getWays() {
		return ownWays;
	}
	
	public Collection<OwnRelation> getRelations() {
		return ownRelations;
	}
	
	public List<OwnNode> getNodes(OwnWay way) {
		return way.nodes;
	}
	
	public TagGroup getTagsN(OwnNode node) {
		return node.tags;
	}
	
	public TagGroup getTagsR(OwnRelation relation) {
		return relation.tags;
	}
	
	public TagGroup getTagsW(OwnWay way) {
		return way.tags;
	}
	
	public Object getMember(OwnMember member) {
		return member.member;
	}
	
	public String getRole(OwnMember member) {
		return member.role;
	}
	
	public boolean isNMember(OwnMember member) {
		return member.member instanceof OwnNode;
	};
	
	public boolean isWMember(OwnMember member) {
		return member.member instanceof OwnWay;
	};
	
	public boolean isRMember(OwnMember member) {
		return member.member instanceof OwnRelation;
	};
	
	public class OwnNode {
		private final double lat;
		private final double lon;
		private final TagGroup tags;
		
		public OwnNode(double lat, double lon, TagGroup tags) {
			this.lat = lat;
			this.lon = lon;
			this.tags = tags;
		}
		
		@Override
		public String toString() {
			if (useDebugLabels && tags.containsKey("debug:label")) {
				return tags.getValue("debug:label");
			}
			return "(" + lat + ", " + lon + ", " + tags + ")";
		}
		
	}
	
	public class OwnWay {
		private final TagGroup tags;
		private final List<OwnNode> nodes;
		
		public OwnWay(TagGroup tags, List<OwnNode> nodes) {
			this.tags = tags;
			this.nodes = nodes;
		}
		
		@Override
		public String toString() {
			
			if (useDebugLabels && tags.containsKey("debug:label")) {
				return tags.getValue("debug:label");
			}
			
			return nodes.get(0)
				+ "->[" + (nodes.size() - 2) + "]->"
				+ nodes.get(nodes.size()-1);
			
		}
		
	}
	
	public class OwnRelation {
		
		private final TagGroup tags;
		
		private final List<OwnMember> relationMembers;
			// content added after constructor call
		
		public OwnRelation(TagGroup tags, int initialMemberSize) {
			this.tags = tags;
			this.relationMembers =
				new ArrayList<OwnMember>(initialMemberSize);
		}
	}
	
	public static class OwnMember {
		private final String role;
		private final Object member;
		
		public OwnMember(String role, Object member) {
			this.role = role;
			this.member = member;
		}
	}
	
}
