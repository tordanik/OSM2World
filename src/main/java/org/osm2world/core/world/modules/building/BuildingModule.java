package org.osm2world.core.world.modules.building;

import static org.osm2world.core.util.FaultTolerantIterationUtil.forEach;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapMultipolygonRelation;
import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;

/**
 * adds buildings to the world
 */
public class BuildingModule extends ConfigurableWorldModule {

	@Override
	public void applyTo(MapData mapData) {

		forEach(mapData.getMapRelations(), (MapRelation relation) -> {
			if (relation instanceof MapMultipolygonRelation multipolygonRelation) {

				String buildingValue = relation.getTags().getValue("building");

				if (buildingValue != null && !buildingValue.equals("no")) {

					Building building = new Building(relation, config);
					multipolygonRelation.getAreas().get(0).addRepresentation(building);

				}

			}
		});

		forEach(mapData.getMapAreas(), (MapArea area) -> {

			if (!area.getRepresentations().isEmpty()) return;
			if (area.getMemberships().stream().anyMatch(m -> m.getRelation() instanceof MapMultipolygonRelation)) return;

			String buildingValue = area.getTags().getValue("building");

			if (buildingValue != null && !buildingValue.equals("no")) {

				Building building = new Building(area, config);
				area.addRepresentation(building);

			}

		});

	}

}
