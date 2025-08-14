package org.osm2world.world.modules.building.roof;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.scene.material.Materials.VOID;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.Collection;
import java.util.List;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.output.CommonTarget;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.model.ExternalResourceModel;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.scene.texcoord.NamedTexCoordFunction;

/** the top of a chimney, modeled as a special kind of "roof" */
public class ChimneyRoof extends Roof {

	public ChimneyRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public Double calculatePreliminaryHeight() {
		return 0.0;
	}

	@Override
	public double getRoofHeightAt(VectorXZ coord) {
		return 0;
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return emptyList();
	}

	@Override
	public void renderTo(CommonTarget target, double baseEle) {

		double chimneyHoleEle = baseEle - 3.0;

		SimplePolygonXZ outerPolygon = originalPolygon.getOuter();
		SimplePolygonXZ chimneyHole = asSimplePolygon(outerPolygon.scale(0.8)).makeCounterclockwise();

		/* draw the area around the hole */

		List<TriangleXZ> trianglesXZ = new PolygonWithHolesXZ(outerPolygon, asList(chimneyHole)).getTriangulation();
		List<TriangleXYZ> triangles = trianglesXZ.stream().map(t -> t.xyz(baseEle)).collect(toList());

		target.drawTriangles(material, triangles,
				triangleTexCoordLists(triangles, material, NamedTexCoordFunction.GLOBAL_X_Z));

		/* draw the walls and bottom of the hole */

		List<VectorXYZ> chimneyHolePath = asList(new VectorXYZ(0, chimneyHoleEle, 0), new VectorXYZ(0, baseEle, 0));
		target.drawExtrudedShape(material, new PolylineXZ(chimneyHole.vertices()).reverse(),
				chimneyHolePath, null, null, null);

		List<TriangleXZ> holeTrianglesXZ = chimneyHole.getTriangulation();
		List<TriangleXYZ> holeTriangles = holeTrianglesXZ.stream().map(t -> t.xyz(chimneyHoleEle)).collect(toList());

		target.drawTriangles(VOID, holeTriangles,
				triangleTexCoordLists(holeTriangles, VOID, NamedTexCoordFunction.GLOBAL_X_Z));

		/* mark the location where a client might want to emit a smoke effect */

		var smokeEmitterModel = new ExternalResourceModel("smokeEmitter");
		target.drawModel(new ModelInstance(smokeEmitterModel,
				new InstanceParameters(chimneyHole.getCenter().xyz(chimneyHoleEle), 0)));

	}

}
