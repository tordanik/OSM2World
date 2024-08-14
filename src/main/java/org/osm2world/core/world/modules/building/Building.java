package org.osm2world.core.world.modules.building;

import static java.lang.Double.POSITIVE_INFINITY;
import static org.osm2world.core.map_elevation.data.GroundState.ON;
import static org.osm2world.core.math.GeometryUtil.roughlyContains;
import static org.osm2world.core.math.algorithms.CAGUtil.subtractPolygons;

import java.util.*;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.map_data.data.MapRelation.Membership;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.modules.building.indoor.IndoorWall;

/**
 * a building. Rendering a building is implemented as rendering all of its {@link BuildingPart}s.
 */
public class Building implements AreaWorldObject, LegacyWorldObject {

	private final MapArea area;

	private final List<BuildingPart> parts = new ArrayList<>();

	private final EleConnectorGroup outlineConnectors;

	private Map<NodeLevelPair, Boolean> windowNodes = new HashMap<>();
	private Map<NodeLevelPair, List<LineSegmentXZ>> wallNodePolygonSegments = new HashMap<>();

	private Collection<AttachmentSurface> attachmentSurfaces = null;

	public Building(MapArea area, Configuration config) {

		this.area = area;

		Optional<MapRelation> buildingRelation = area.getMemberships().stream()
				.filter(it -> "outline".equals(it.getRole()))
				.map(Membership::getRelation)
				.filter(it -> it.getTags().contains("type", "building"))
				.findAny();

		if (buildingRelation.isPresent()) {

			/* find building parts based on the relation */

			for (Membership membership : buildingRelation.get().getMemberships()) {
				if ("part".equals(membership.getRole()) && membership.getElement() instanceof MapArea) {
					parts.add(new BuildingPart(this, (MapArea) membership.getElement(), config));
				}
			}

		} else {

			/* find building part areas geometrically contained within the building outline */

			FaultTolerantIterationUtil.forEach(area.getOverlaps(), (MapOverlap<?,?> overlap) -> {
				MapElement other = overlap.getOther(area);
				if (other instanceof MapArea otherArea
						&& other.getTags().containsKey("building:part")) {

					if (otherArea.getMemberships().stream().anyMatch(m -> "part".equals(m.getRole())
							&& m.getRelation().getTags().contains("type", "building"))) {
						return; // belongs to another building's relation
					}

					if (roughlyContains(area.getPolygon(), otherArea.getPolygon())) {
						parts.add(new BuildingPart(this, otherArea, config));
					}

				}
			});

		}

		/* use the building itself as a part if no parts exist, or in certain cases of non-standard mapping */

		boolean useBuildingAsPart = parts.isEmpty();

		String buildingPartValue = area.getTags().getValue("building:part");
		if (buildingPartValue != null && !"no".equals(buildingPartValue)) {
			// building is also tagged as a building part (non-standard mapping)
			useBuildingAsPart = true;
		}

		if (parts.stream().mapToDouble(p -> p.area.getPolygon().getArea()).sum() < 0.9 * area.getPolygon().getArea()) {
			var remainder = subtractPolygons(area.getPolygon(), parts.stream().map(p -> p.area.getPolygon()).toList());
			if (remainder.stream().mapToDouble(PolygonShapeXZ::getArea).sum() < 0.9 * area.getPolygon().getArea()) {
				// less than 90% of the building polygon is covered by building parts (non-standard mapping)
				useBuildingAsPart = true;
			}
		}

		if (useBuildingAsPart) {
			parts.add(new BuildingPart(this, area, config));
		}

		/* create connectors along the outline.
		 * Because the ground around buildings is not necessarily plane,
		 * they aren't directly used for ele, but instead their minimum.
		 */

		outlineConnectors = new EleConnectorGroup();
		outlineConnectors.addConnectorsFor(area.getPolygon(), null, ON);

	}

	public List<BuildingPart> getParts() {
		return parts;
	}

	@Override
	public MapArea getPrimaryMapElement() {
		return area;
	}

	@Override
	public GroundState getGroundState() {
		return GroundState.ON;
	}

	@Override
	public EleConnectorGroup getEleConnectors() {
		return outlineConnectors;
	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) { }

	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		return area.getPolygon().getOuter().makeCounterclockwise();
	}

	public double getGroundLevelEle() {

		double minEle = POSITIVE_INFINITY;

		for (EleConnector c : outlineConnectors) {
			if (c.getPosXYZ().y < minEle) {
				minEle = c.getPosXYZ().y;
			}
		}

		return minEle;

	}

	@Override
	public void renderTo(Target target) {
		FaultTolerantIterationUtil.forEach(parts, part -> part.renderTo(target));
		IndoorWall.renderNodePolygons(target, wallNodePolygonSegments);
	}

	@Override
	public Collection<PolygonShapeXZ> getRawGroundFootprint(){
		Collection<PolygonShapeXZ> shapes = new ArrayList<>();

		for (BuildingPart part : parts) {

			if (part.levelStructure.bottomHeight() <= 0 && part.getIndoor() != null) {
				shapes.add(part.getPolygon());
			}

		}

		return shapes;
	}

	@Override
	public Collection<AttachmentSurface> getAttachmentSurfaces() {
		if (attachmentSurfaces == null) {
			attachmentSurfaces = new ArrayList<>();
			for (BuildingPart part : parts) {
				attachmentSurfaces.addAll(part.getAttachmentSurfaces());
			}
		}
		return attachmentSurfaces;
	}

	public class NodeLevelPair{

		private final MapNode node;
		private final Integer level;
		private final double heightAboveGround;
		private final double ceilingHeightAboveGround;

		NodeLevelPair(MapNode node, Integer level, double heightAboveGround, double ceilingHeightAboveGround) {
			this.node = node;
			this.level = level;
			this.heightAboveGround = heightAboveGround;
			this.ceilingHeightAboveGround = ceilingHeightAboveGround;
		}

		public double getHeightAboveGround() { return heightAboveGround; }

		public double getCeilingHeightAboveGround() { return ceilingHeightAboveGround; }

		@Override
		public boolean equals(Object anObject){
			if (anObject instanceof NodeLevelPair) {
				NodeLevelPair temp = (NodeLevelPair) anObject;
				if (temp.level.equals(this.level) && temp.node.equals(this.node)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public int hashCode() {
			return  Long.hashCode(node.getId()) / ((level * 2) + 1);
		}

	}

	public void addWindowNode(MapNode node, Integer level){
		if (windowNodes.get(new NodeLevelPair(node, level, 0, 0)) != null) {
			windowNodes.replace(new NodeLevelPair(node, level, 0, 0), true);
		} else {
			windowNodes.put(new NodeLevelPair(node, level, 0, 0), false);
		}
	}

	public void addListWindowNodes(List<MapNode> nodes, Integer level) {
		nodes.forEach(n -> addWindowNode(n, level));
	}

	public Boolean queryWindowSegments(MapNode node, Integer level){
		return Boolean.TRUE.equals(windowNodes.get(new NodeLevelPair(node, level, 0, 0)));
	}


	public void addLineSegmentToPolygonMap(MapNode node, Integer level, LineSegmentXZ line, double heightAboveGround, double ceilingHeightAboveGround){
		if (wallNodePolygonSegments.get(new NodeLevelPair(node, level, heightAboveGround, ceilingHeightAboveGround)) != null) {
			wallNodePolygonSegments.get(new NodeLevelPair(node, level, heightAboveGround, ceilingHeightAboveGround)).add(line);
		} else {
			wallNodePolygonSegments.put(new NodeLevelPair(node, level, heightAboveGround, ceilingHeightAboveGround), new ArrayList<>(Arrays.asList(line)));
		}
	}

	public List<LineSegmentXZ> queryPolygonMap(MapNode node, Integer level){
		return wallNodePolygonSegments.get(new NodeLevelPair(node, level, 0, 0));
	}

}
