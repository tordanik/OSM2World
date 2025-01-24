package org.osm2world.core.world.modules.building.indoor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.ProceduralWorldObject;
import org.osm2world.core.world.modules.building.BuildingPart;

public class IndoorFloor {

    private final BuildingPart buildingPart;
    private final Material material;
    private final PolygonWithHolesXZ polygon;
    private final double floorHeight;
    private Boolean render;
    final int level;
    private final Ceiling ceiling;

    private AttachmentSurface attachmentSurface;

    IndoorFloor(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeightAboveBase, Boolean renderable, int level){
        this.buildingPart = buildingPart;
        this.material = material;
        this.polygon = polygon;
        this.floorHeight = floorHeightAboveBase;
        this.render = renderable;
        this.level = level;
        this.ceiling = new Ceiling(buildingPart, material, polygon, floorHeightAboveBase, renderable, level - 1);
    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {

        if (polygon == null) {
            return emptyList();
        }

        if (attachmentSurface == null) {
			List<TriangleXYZ> triangles = triangulateFloorPolygons(singleton(polygon));
			attachmentSurface = new AttachmentSurface(List.of("floor" + this.level), triangles);
        }

		return List.of(attachmentSurface);

    }

    void renderTo(ProceduralWorldObject.Target target) {

        if (level != buildingPart.levelStructure.levels.get(0).level) {
            ceiling.renderTo(target);
        }

        if (render && polygon != null) {

    		/* subtract attached areas from the floor polygon */

    		List<PolygonShapeXZ> subtractPolys = new ArrayList<>();

    		if (attachmentSurface != null) {
    			for (AttachmentConnector connector : attachmentSurface.getAttachedConnectors()) {
					if (connector.object != null) {
    					subtractPolys.addAll(connector.object.getRawGroundFootprint());
    				}
    			}
    		}

    		Collection<? extends PolygonShapeXZ> polygons;

    		if (subtractPolys.isEmpty()) {
    			polygons = singleton(polygon);
    		} else {
    			subtractPolys.addAll(polygon.getHoles());
    			polygons = CAGUtil.subtractPolygons(polygon.getOuter(), subtractPolys);
    		}

    		/* triangulate and render the (remaining) floor polygons */

    		List<TriangleXYZ> trianglesXYZ = triangulateFloorPolygons(polygons);

            target.drawTriangles(material, trianglesXYZ,
                    triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

        }
    }

	private List<TriangleXYZ> triangulateFloorPolygons(Collection<? extends PolygonShapeXZ> polygons) {
		double floorEle = buildingPart.getBuilding().getGroundLevelEle() + floorHeight + 0.0001;
		return polygons.stream()
				.flatMap(p -> p.getTriangulation().stream())
				.map(t -> t.makeCounterclockwise().xyz(floorEle))
				.toList();
	}

}
