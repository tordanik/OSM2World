package org.osm2world.world.modules;

import static org.osm2world.scene.color.Color.*;
import static org.osm2world.scene.material.DefaultMaterials.ASPHALT;
import static org.osm2world.scene.material.DefaultMaterials.getSurfaceMaterial;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD3;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD4;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_data.data.overlaps.MapOverlapType;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.mesh.LODRange;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * adds parking spaces to the world
 */
public class ParkingModule extends AbstractModule {

	private static final List<Color> CAR_COLORS = List.of(
			WHITE, WHITE, WHITE, WHITE, WHITE,
			BLACK, BLACK, BLACK, BLACK,
			GRAY, GRAY, GRAY, GRAY,
			LIGHT_GRAY, LIGHT_GRAY, // silver
			RED, GREEN, BLUE, YELLOW, CYAN);

	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("amenity","parking")) {

			String parkingValue = area.getTags().getValue("parking");

			if (parkingValue == null || List.of("surface", "lane", "street_side", "rooftop").contains(parkingValue)) {
				area.addRepresentation(new SurfaceParking(area));
			}

		}
	}

	class SurfaceParking extends AbstractAreaWorldObject
			implements ProceduralWorldObject {

		final List<MapArea> parkingSpaces = new ArrayList<>();

		public SurfaceParking(MapArea area) {

			super(area);

			/* find explicitly mapped parking spaces within */

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				if (overlap.getOther(area) instanceof MapArea otherArea
						&& otherArea.getTags().contains("amenity", "parking_space")
						&& (overlap.type == MapOverlapType.CONTAIN && overlap.e2 == area ||
							area.getPolygon().contains(otherArea.getOuterPolygon().getPointInside()))) {
					parkingSpaces.add(otherArea);
				}

			}

		}

		@Override
		public int getOverlapPriority() {
			return 30;
		}

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			String surface = area.getTags().getValue("surface");
			Material material = getSurfaceMaterial(surface, ASPHALT, config);

			List<TriangleXYZ> triangles = getTriangulation();

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

			/* draw cars on the parking spaces */

			double carDensity = config.getDouble("parkedVehicleDensity", 0.3);
			var random = new Random(area.getId());

			for (MapArea parkingSpace : parkingSpaces) {

				if (random.nextDouble() > carDensity) continue;

				Model carModel = config.mapStyle().getModel("CAR", random);
				Color carColor = CAR_COLORS.get(random.nextInt(CAR_COLORS.size()));

				if (carModel != null) {

					SimplePolygonXZ bbox = parkingSpace.getOuterPolygon().minimumRotatedBoundingBox();

					// determine the car's facing direction based on which side of the box is longer
					VectorXZ direction = bbox.getVertex(1).subtract(bbox.getVertex(0));
					if (bbox.getVertex(2).distanceTo(bbox.getVertex(1)) > direction.length()) {
						direction = bbox.getVertex(2).subtract(bbox.getVertex(1));
					}
					direction = direction.normalize();

					// determine direction (with some randomness)
					VectorXZ pos = bbox.getCenter().add(direction.mult(-0.2 + random.nextDouble(0.4)));

					// determine elevation
					double ele = getEleAt(pos);

					target.addSubModel(new ModelInstance(carModel, new InstanceParameters(
							pos.xyz(ele), direction.angle(), carColor, new LODRange(LOD3, LOD4))));

				}
			}

		}

	}

}
