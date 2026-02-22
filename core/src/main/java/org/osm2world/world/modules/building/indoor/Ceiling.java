package org.osm2world.world.modules.building.indoor;

import static java.util.stream.Collectors.toList;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.scene.material.Material;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.building.BuildingPart;

public class Ceiling {

    private final BuildingPart buildingPart;
    private final Material material;
    private final PolygonWithHolesXZ polygon;
    private final double floorHeight;
    private Boolean render;
    final int level;

    Ceiling(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeightAboveBase, Boolean renderable, int level){
        this.buildingPart = buildingPart;
        this.material = material;
        this.polygon = polygon;
        this.floorHeight = floorHeightAboveBase;
        this.render = renderable;
        this.level = level;
    }

    void renderTo(ProceduralWorldObject.Target target) {

        if (render && polygon != null) {

            double floorEle = buildingPart.getBuilding().getGroundLevelEle() + floorHeight - 0.001;

            List<LineSegmentXZ> sides = new ArrayList<>(polygon.getOuter().makeCounterclockwise().getSegments());
            polygon.getHoles().forEach(p -> sides.addAll(p.makeClockwise().getSegments()));

            renderSides(target, sides, floorEle);

            renderSurface(target, floorEle);

        }
    }

    private void renderSurface(ProceduralWorldObject.Target target, double floorEle){
        Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon);

        List<TriangleXYZ> trianglesXYZ = triangles.stream()
                .map(t -> t.makeClockwise().xyz(floorEle - 0.2))
                .collect(toList());

        target.setCurrentAttachmentTypes("ceiling" + this.level);
        target.drawTriangles(material, trianglesXYZ,
                triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));
        target.setCurrentAttachmentTypes();
    }

    private void renderSides(CommonTarget target, List<LineSegmentXZ> sides, double floorEle) {

        VectorXYZ bottom = new VectorXYZ(0, floorEle - 0.2, 0);
        VectorXYZ top = new VectorXYZ(0, floorEle, 0);

        List<VectorXYZ> path = new ArrayList<>();
        path.add(bottom);
        path.add(top);

        for (LineSegmentXZ side : sides) {
            // do not render ceiling sides which overlap with the outer wall of a building part
            if (buildingPart.getPolygon().getRings().stream().noneMatch(
                    r -> r.distanceToSegments(side.p1) < 0.05 && r.distanceToSegments(side.p2) < 0.05)) {
                target.drawExtrudedShape(material, side, path, null, null, null);
            }
        }
    }
}
