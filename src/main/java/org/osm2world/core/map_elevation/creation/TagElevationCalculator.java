package org.osm2world.core.map_elevation.creation;

import javax.annotation.Nonnull;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.world.data.WorldObject;

/**
 * relies on tags that explicitly set elevation.
 * Subclasses determine the tag(s) to be used for this purpose.
 */
public abstract class TagElevationCalculator implements ElevationCalculator {

	@Override
	public void calculateElevations(@Nonnull MapData mapData) {

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {

			for (EleConnector conn : worldObject.getEleConnectors()) {
				Double ele = getEleForTags(worldObject.getPrimaryMapElement().getTags());
				if (ele != null) {
					conn.setPosXYZ(conn.pos.xyz(ele));
				}
			}

		});

	}

	/**
	 * returns the elevation as set explicitly by the tags
	 *
	 * @return  elevation; null if the tags don't define the elevation
	 */
	protected abstract Double getEleForTags(TagSet tags);

}
