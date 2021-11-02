package org.osm2world.core.world.modules;

import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.model.ExternalResourceModel;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds parking spaces to the world
 */
public class ParkingModule extends AbstractModule {

	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("amenity","parking")) {

			String parkingValue = area.getTags().getValue("parking");

			if ("surface".equals(parkingValue) || "lane".equals(parkingValue)
					|| "street_side".equals(parkingValue) || parkingValue == null) {
				area.addRepresentation(new SurfaceParking(area));
			}

		}
	}

	private static class SurfaceParking extends AbstractAreaWorldObject implements TerrainBoundaryWorldObject {

		private final List<MapArea> parkingSpaces = new ArrayList<MapArea>();

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

			Model carModel = new ExternalResourceModel("car");

			for (MapArea parkingSpace : parkingSpaces) {

				SimplePolygonXZ bbox = parkingSpace.getOuterPolygon().minimumRotatedBoundingBox();

				// determine the car's facing direction based on  which side of the box is longer
				VectorXZ direction = bbox.getVertex(1).subtract(bbox.getVertex(0));
				if (bbox.getVertex(2).distanceTo(bbox.getVertex(1)) > direction.length()) {
					direction = bbox.getVertex(2).subtract(bbox.getVertex(1));
				}
				direction = direction.normalize();

				target.drawModel(carModel, bbox.getCenter().xyz(ele), direction.angle(), null, null, null);

			}

		}

	}

}
