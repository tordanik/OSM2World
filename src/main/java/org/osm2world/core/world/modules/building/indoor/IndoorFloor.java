package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

public class IndoorFloor {

    private final BuildingPart buildingPart;
    private final Material material;
    private final PolygonWithHolesXZ polygon;
    private final double floorHeight;
    private Boolean render;
    private final int level;

    private AttachmentSurface attachmentSurface;

    public IndoorFloor(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeightAboveBase, Boolean renderable, int level){
        this.buildingPart = buildingPart;
        this.material = material;
        this.polygon = polygon;
        this.floorHeight = floorHeightAboveBase;
        this.render = renderable;
        this.level = level;
    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {

        if (polygon == null) {
            return emptyList();
        }

        if (attachmentSurface == null) {
            AttachmentSurface.Builder builder = new AttachmentSurface.Builder("floor" + this.level);
            boolean tempRender = this.render;
            this.render = true;
            this.renderTo(builder);
            this.render = tempRender;
            attachmentSurface = builder.build();
        }

        return Collections.singleton(attachmentSurface);

    }

    public void renderTo(Target target) {

        if (render && polygon != null) {

            double floorEle = buildingPart.getBuildingPartBaseEle() + floorHeight + 0.0001;

            Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon);

            List<TriangleXYZ> trianglesXYZ = triangles.stream()
                    .map(t -> t.makeCounterclockwise().xyz(floorEle))
                    .collect(toList());

            target.drawTriangles(material, trianglesXYZ,
                    triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

        }
    }
}