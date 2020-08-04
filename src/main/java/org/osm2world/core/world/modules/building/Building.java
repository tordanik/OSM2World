package org.osm2world.core.world.modules.building;

import static java.lang.Double.POSITIVE_INFINITY;
import static org.osm2world.core.map_elevation.data.GroundState.ON;
import static org.osm2world.core.math.GeometryUtil.roughlyContains;

import java.util.*;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;

/**
 * a building. Rendering a building is implemented as rendering all of its {@link BuildingPart}s.
 */
public class Building implements AreaWorldObject, TerrainBoundaryWorldObject {

	private final MapArea area;

	private final List<BuildingPart> parts = new ArrayList<>();

	private final EleConnectorGroup outlineConnectors;

	private Map<NodeLevelPair, Boolean> windowNodes = new HashMap<>();

	public Building(MapArea area, Configuration config) {

		this.area = area;

		for (MapOverlap<?,?> overlap : area.getOverlaps()) {
			MapElement other = overlap.getOther(area);
			if (other instanceof MapArea
					&& other.getTags().containsKey("building:part")) {

				MapArea otherArea = (MapArea)other;

				if (roughlyContains(area.getPolygon(), otherArea.getPolygon().getOuter())) {
					parts.add(new BuildingPart(this, otherArea, config));
				}

			}
		}

		/* use the building itself as a part if no parts exist,
		 * or if it's explicitly tagged as a building part at the same time (non-standard mapping) */

		String buildingPartValue = area.getTags().getValue("building:part");

		if (parts.isEmpty() || buildingPartValue != null && !"no".equals(buildingPartValue)) {
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
	public PolygonXYZ getOutlinePolygon() {
		return getOutlinePolygonXZ().xyz(getGroundLevelEle());
	}

	@Override
	public void renderTo(Target target) {
		for (BuildingPart part : parts) {
			part.renderTo(target);
		}
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox(){
		return getOutlinePolygonXZ().boundingBox();
	}

	@Override
	public Collection<PolygonShapeXZ> getTerrainBoundariesXZ(){
		Collection<PolygonShapeXZ> shapes = new ArrayList<>();

		for (BuildingPart part : parts) {

			if (part.getMinLevel() < 1 && part.getIndoor() != null){
				shapes.add(part.getPolygon().getOuter());
			}

		}

		return shapes;
	}

	private class NodeLevelPair{

		private final MapNode node;
		private final Integer level;

		NodeLevelPair(MapNode node, Integer level) {
			this.node = node;
			this.level = level;
		}

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
		if (windowNodes.get(new NodeLevelPair(node, level)) != null) {
			windowNodes.replace(new NodeLevelPair(node, level), true);
		} else {
			windowNodes.put(new NodeLevelPair(node, level), false);
		}
	}

	public void addListWindowNodes(List<MapNode> nodes, Integer level) {
		nodes.forEach(n -> addWindowNode(n, level));
	}

	public Boolean queryWindowSegments(MapNode node, Integer level){
		return Boolean.TRUE.equals(windowNodes.get(new NodeLevelPair(node, level)));
	}

}
