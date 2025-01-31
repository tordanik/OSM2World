package org.osm2world.core.world.modules.building;

import static java.lang.Double.POSITIVE_INFINITY;
import static org.osm2world.core.map_elevation.data.GroundState.ON;
import static org.osm2world.core.math.algorithms.CAGUtil.subtractPolygons;
import static org.osm2world.core.math.algorithms.GeometryUtil.roughlyContains;
import static org.osm2world.core.util.FaultTolerantIterationUtil.forEach;

import java.util.*;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.*;
import org.osm2world.core.map_data.data.MapRelation.Membership;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.shapes.LineSegmentXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonXZ;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.util.ConfigUtil;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.CachingProceduralWorldObject;
import org.osm2world.core.world.modules.building.indoor.IndoorWall;

/**
 * a building. Rendering a building is implemented as rendering all of its {@link BuildingPart}s.
 */
public class Building extends CachingProceduralWorldObject implements AreaWorldObject {

	/** a {@link MapArea} or {@link MapMultipolygonRelation} (for a multipolygon with multiple outer rings) */
	private final MapRelationElement element;

	private final Configuration config;

	private final List<BuildingPart> parts = new ArrayList<>();

	private final EleConnectorGroup outlineConnectors;

	private Map<NodeWithLevelAndHeights, List<LineSegmentXZ>> wallNodePolygonSegments = new HashMap<>();

	public Building(MapRelationElement element, Configuration config) {

		this.element = element;
		this.config = config;

		Optional<MapRelation> buildingRelation = element.getMemberships().stream()
				.filter(it -> "outline".equals(it.getRole()))
				.map(Membership::getRelation)
				.filter(it -> it.getTags().contains("type", "building"))
				.findAny();

		List<MapArea> areas = element instanceof MapMultipolygonRelation r
				? r.getAreas() : List.of((MapArea) element);

		if (buildingRelation.isPresent()) {

			/* find building parts based on the relation */

			for (Membership membership : buildingRelation.get().getMembers()) {
				if ("part".equals(membership.getRole()) && membership.getElement() instanceof MapArea) {
					parts.add(new BuildingPart(this, (MapArea) membership.getElement(), config));
				}
			}

		} else {

			/* find building part areas geometrically contained within the building outline */

			for (MapArea area : areas) {
				forEach(area.getOverlaps(), (MapOverlap<?, ?> overlap) -> {
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

		}

		/* use the building itself as a part if no parts exist, or in certain cases of non-standard mapping */

		boolean useBuildingAsPart = parts.isEmpty();

		String buildingPartValue = element.getTags().getValue("building:part");
		if (buildingPartValue != null && !"no".equals(buildingPartValue)) {
			// building is also tagged as a building part (non-standard mapping)
			useBuildingAsPart = true;
		}

		if (element instanceof MapArea area
				&& parts.stream().mapToDouble(p -> p.area.getPolygon().getArea()).sum() < 0.9 * area.getPolygon().getArea()) {
			var remainder = subtractPolygons(area.getPolygon(), parts.stream().map(p -> p.area.getPolygon()).toList());
			if (remainder.stream().mapToDouble(PolygonShapeXZ::getArea).sum() < 0.9 * area.getPolygon().getArea()) {
				// less than 90% of the building polygon is covered by building parts (non-standard mapping)
				useBuildingAsPart = true;
			}
		}

		if (useBuildingAsPart) {
			areas.forEach(area -> parts.add(new BuildingPart(this, area, config)));
		}

		/* create connectors along the outline.
		 * Because the ground around buildings is not necessarily plane,
		 * they aren't directly used for ele, but instead their minimum.
		 */

		outlineConnectors = new EleConnectorGroup();
		areas.forEach(area -> outlineConnectors.addConnectorsFor(area.getPolygon(), null, ON));

	}

	public List<BuildingPart> getParts() {
		return parts;
	}

	@Override
	public MapArea getPrimaryMapElement() {
		if (element instanceof MapArea a) {
			return a;
		} else if (element instanceof MapMultipolygonRelation r) {
			return r.getAreas().get(0);
		} else {
			throw new Error("unexpected element type: " + element.getClass().getSimpleName());
		}
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
		if (element instanceof MapArea area) {
			return area.getPolygon().getOuter().makeCounterclockwise();
		} else if (element instanceof MapMultipolygonRelation r) {
			var largestPolygon = r.getAreas().stream().map(MapArea::getPolygon).max(Comparator.comparingDouble(PolygonShapeXZ::getArea));
			assert largestPolygon.isPresent();
			return largestPolygon.get().getOuter().makeCounterclockwise();
		} else {
			throw new Error("unexpected element type: " + element.getClass().getSimpleName());
		}
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
	protected @Nullable LevelOfDetail getConfiguredLod() {
		return ConfigUtil.readLOD(config);
	}

	@Override
	public void buildMeshesAndModels(Target target) {
		forEach(parts, part -> part.buildMeshesAndModels(target));
		IndoorWall.renderNodePolygons(target, wallNodePolygonSegments);
	}

	@Override
	public Collection<AttachmentSurface> getAttachmentSurfaces() {
		List<AttachmentSurface> result = new ArrayList<>(super.getAttachmentSurfaces());
		forEach(parts, part -> result.addAll(part.getAttachmentSurfaces()));
		return result;
	}

	@Override
	public Collection<PolygonShapeXZ> getRawGroundFootprint() {
		return List.of(); // BuildingParts return their own footprint if necessary
	}

	public record NodeWithLevelAndHeights(
		MapNode node, Integer level, double heightAboveGround, double ceilingHeightAboveGround
	) {}

	public void addLineSegmentToPolygonMap(MapNode node, Integer level, LineSegmentXZ line, double heightAboveGround, double ceilingHeightAboveGround){
		if (wallNodePolygonSegments.get(new NodeWithLevelAndHeights(node, level, heightAboveGround, ceilingHeightAboveGround)) != null) {
			wallNodePolygonSegments.get(new NodeWithLevelAndHeights(node, level, heightAboveGround, ceilingHeightAboveGround)).add(line);
		} else {
			wallNodePolygonSegments.put(new NodeWithLevelAndHeights(node, level, heightAboveGround, ceilingHeightAboveGround), new ArrayList<>(Arrays.asList(line)));
		}
	}

}
