package org.osm2world.map_elevation.creation;

import javax.annotation.Nonnull;

import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.world.data.WorldObject;

/**
 * relies on tags that explicitly set elevation.
 * Subclasses determine the tag(s) to be used for this purpose.
 */
public abstract class TagEleCalculator implements EleCalculator {

	@Override
	public void calculateElevations(@Nonnull MapData mapData) {

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {

			for (EleConnector conn : worldObject.getEleConnectors()) {

				Double ele = null;

				if (conn.reference instanceof MapNode node) {
					// get ele from node
					ele = getEleForTags(node.getTags(), conn.getPosXYZ().y);
				}

				if (ele == null) {
					// get ele from Object's tags
					ele = getEleForTags(worldObject.getPrimaryMapElement().getTags(), conn.getPosXYZ().y);
				}

				if (ele != null) {
					conn.setPosXYZ(conn.pos.xyz(ele));
				}

			}

		});

	}

	/**
	 * returns the elevation as set explicitly by the tags
	 *
	 * @param terrainEle  initial elevation value derived from terrain data, or 0 if no terrain data is available
	 * @return  elevation; null if the tags don't define the elevation
	 */
	protected abstract Double getEleForTags(TagSet tags, double terrainEle);

}
