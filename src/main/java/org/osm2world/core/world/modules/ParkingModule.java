package org.osm2world.core.world.modules;

import static java.awt.Color.*;
import static org.osm2world.core.target.common.material.Materials.ASPHALT;
import static org.osm2world.core.target.common.material.Materials.getSurfaceMaterial;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD3;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD4;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.LODRange;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.Models;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

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

	private class SurfaceParking extends AbstractAreaWorldObject
			implements LegacyWorldObject {

		private final List<MapArea> parkingSpaces = new ArrayList<>();

		public SurfaceParking(MapArea area) {

			super(area);

			/* find explicitly mapped parking spaces within */

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {

				if (overlap.type == MapOverlapType.CONTAIN
						&& overlap.e2 == area
						&& overlap.e1.getTags().contains("amenity", "parking_space")
						&& overlap.e1 instanceof MapArea) {
					parkingSpaces.add((MapArea)overlap.e1);
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
		public void renderTo(Target target) {

			String surface = area.getTags().getValue("surface");
			Material material = getSurfaceMaterial(surface, ASPHALT);

			List<TriangleXYZ> triangles = getTriangulation();

			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material, GLOBAL_X_Z));

			/* draw cars on the parking spaces */

			double ele = 0;

			if (getConnectorIfAttached() != null) {
				ele = getConnectorIfAttached().getAttachedPos().getY();
			} else {
				//TODO add elevation support
			}

			double carDensity = config.getDouble("carDensity", 0.3);
			var random = new Random(area.getId());

			Model carModel = Models.getModel("CAR");

			if (carModel != null) {
				for (MapArea parkingSpace : parkingSpaces) {

					if (random.nextDouble() > carDensity) continue;

					Color carColor = CAR_COLORS.get(random.nextInt(CAR_COLORS.size()));

					SimplePolygonXZ bbox = parkingSpace.getOuterPolygon().minimumRotatedBoundingBox();

					// determine the car's facing direction based on which side of the box is longer
					VectorXZ direction = bbox.getVertex(1).subtract(bbox.getVertex(0));
					if (bbox.getVertex(2).distanceTo(bbox.getVertex(1)) > direction.length()) {
						direction = bbox.getVertex(2).subtract(bbox.getVertex(1));
					}
					direction = direction.normalize();

					target.drawModel(carModel, new InstanceParameters(
							bbox.getCenter().xyz(ele), direction.angle(), carColor, new LODRange(LOD3, LOD4)));

				}
			}

		}

	}

}
