package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.math.*;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

public class Ceiling {

    private final BuildingPart buildingPart;
    private final Material material;
    private final PolygonWithHolesXZ polygon;
    private final double floorHeight;
    private Boolean render;
    private final int level;

    private AttachmentSurface attachmentSurface;

    public Ceiling(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeightAboveBase, Boolean renderable, int level){
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
            AttachmentSurface.Builder builder = new AttachmentSurface.Builder("ceiling" + buildingPart.levelReversion(this.level));
            this.renderSurface(builder, buildingPart.getBuildingPartBaseEle() + floorHeight);
            attachmentSurface = builder.build();
        }

        return Collections.singleton(attachmentSurface);

    }

    public void renderTo(Target target) {

        if (render && polygon != null) {

            double floorEle = buildingPart.getBuildingPartBaseEle() + floorHeight - 0.0001;

            List<ShapeXZ> sides  = new ArrayList<>(singleton(polygon.getOuter().makeCounterclockwise()));
            sides.addAll(polygon.getHoles().stream().map(SimplePolygonXZ::makeClockwise).collect(toList()));

            renderSides(target, sides, floorEle);

            renderSurface(target, floorEle);

        }
    }

    private void renderSurface(Target target, double floorEle){
        Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon);

        List<TriangleXYZ> trianglesXYZ = triangles.stream()
                .map(t -> t.makeClockwise().xyz(floorEle - 0.2))
                .collect(toList());

        target.drawTriangles(material, trianglesXYZ,
                triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));
    }

    private void renderSides(Target target, List<ShapeXZ> sides, double floorEle) {
        VectorXYZ bottom = new VectorXYZ(0, floorEle - 0.2, 0);
        VectorXYZ top = new VectorXYZ(0, floorEle, 0);

        List<VectorXYZ> path = new ArrayList<>();
        path.add(bottom);
        path.add(top);

        for (ShapeXZ side : sides) {
            target.drawExtrudedShape(material, side, path, nCopies(2, Z_UNIT), null, null, null);
        }
    }
}
