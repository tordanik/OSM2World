package org.osm2world.world.modules.building.indoor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.map_data.data.MapElement;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.building.BuildingPart;

/**
 * the features (rooms, interior walls, etc.) inside a {@link BuildingPart}.
 * This could be merged into {@link BuildingPart}, but is kept separate for now
 * as it might be helpful to eventually allow interiors spanning entire buildings.
 */
public class BuildingPartInterior {

	private final List<IndoorWall> walls = new ArrayList<>();
	private final List<IndoorRoom> rooms = new ArrayList<>();
	private final List<IndoorArea> areas = new ArrayList<>();

	private List<AttachmentSurface> surfaces = null;

	public BuildingPartInterior(List<MapElement> elements, BuildingPart buildingPart) {

		for (MapElement element : elements) {

			var data = new IndoorObjectData(buildingPart, element);

			switch (element.getTags().getValue("indoor")) {
			case "wall" -> walls.add(new IndoorWall(data));
			case "room", "corridor" -> rooms.add(new IndoorRoom(data));
			case "area" -> areas.add(new IndoorArea(data));
			}

		}

	}

	public Collection<AttachmentSurface> getAttachmentSurfaces() {

		if (surfaces == null) {

			surfaces = new ArrayList<>();

			for (IndoorRoom room : rooms) {
				surfaces.addAll(room.getAttachmentSurfaces());
			}

			for (IndoorArea area : areas) {
				surfaces.addAll(area.getAttachmentSurfaces());
			}

		}

		return surfaces;

	}

	public void buildMeshesAndModels(ProceduralWorldObject.Target target) {

		IndoorWall.allRenderedWallSegments = new ArrayList<>(); //FIXME this is not thread-safe!

		walls.forEach(w -> w.renderTo(target));

		rooms.forEach(r -> r.buildMeshesAndModels(target));

		areas.forEach(a -> a.buildMeshesAndModels(target));

	}
}
