package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

public class Ceiling {

    private final BuildingPart buildingPart;
    private final Material material;
    private final PolygonWithHolesXZ polygon;
    private final double floorHeight;

    public Ceiling(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeightAboveBase){
        this.buildingPart = buildingPart;
        this.material = material;
        this.polygon = polygon;
        this.floorHeight = floorHeightAboveBase;
    }

    public void renderTo(Target target) {
        double floorEle = buildingPart.getBuilding().getGroundLevelEle() + floorHeight - 0.01;

        Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon);

        List<TriangleXYZ> trianglesXYZ = triangles.stream()
                .map(t -> t.makeClockwise().xyz(floorEle))
                .collect(toList());

        target.drawTriangles(material, trianglesXYZ,
                triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

    }
}
