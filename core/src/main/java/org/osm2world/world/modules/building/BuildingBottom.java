package org.osm2world.world.modules.building;

import static java.util.stream.Collectors.toList;
import static org.osm2world.output.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.output.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.Collection;
import java.util.List;

import org.osm2world.math.algorithms.TriangulationUtil;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.common.material.Material;

/** the underside of a {@link BuildingPart} */
class BuildingBottom {

	private final BuildingPart buildingPart;
	private final Material material;
	private final PolygonWithHolesXZ polygon;
	private final double floorHeight;

	public BuildingBottom(BuildingPart buildingPart, Material material, PolygonWithHolesXZ polygon, double floorHeight) {
		this.buildingPart = buildingPart;
		this.material = material;
		this.polygon = polygon;
		this.floorHeight = floorHeight;
	}

	public void renderTo(CommonTarget target) {

		double floorEle = buildingPart.building.getGroundLevelEle() + floorHeight - 0.01;

		Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon);

		List<TriangleXYZ> trianglesXYZ = triangles.stream()
				.map(t -> t.makeClockwise().xyz(floorEle))
				.collect(toList());

		target.drawTriangles(material, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

	}

}
