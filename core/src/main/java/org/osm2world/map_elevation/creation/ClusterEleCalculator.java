package org.osm2world.map_elevation.creation;

import java.util.Collection;
import java.util.function.ToDoubleFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.MapData;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.connectors.LowestGroundContact;
import org.osm2world.world.data.WorldObject;

/**
 * calculates elevations based on terrain elevation and {@link ConnectorCluster}s.
 * Each cluster's base elevation is determined by the first of these rules which applies:
 * <ol>
 *     <li>an attached member: the cluster follows the attachment</li>
 *     <li>a {@link LowestGroundContact} relation: the lowest terrain elevation along the relation's outlines.
 *     If there are several, the highest result is used.</li>
 *     <li>all members are above the ground ({@link GroundState#ABOVE}, e.g. bridges):
 *     the cluster's initial base elevation plus a small offset</li>
 *     <li>otherwise: the cluster's initial base elevation, which is based on terrain elevation</li>
 * </ol>
 */
public class ClusterEleCalculator implements EleCalculator {

	private static final double ELE_OFFSET_ABOVE = 0.1;

	/** if ground contacts within a cluster suggest base elevations differing by more than this, a warning is logged */
	private static final double WARNING_THRESHOLD = 1.0;

	@Override
	public void calculateElevations(@Nonnull MapData mapData, @Nonnull TerrainInterpolator terrain) {
		calculateElevations(mapData.getWorldObjects(), pos -> terrain.interpolateEle(pos).y);
	}

	/**
	 * @param terrainEle  provides the terrain elevation at arbitrary positions
	 */
	void calculateElevations(Iterable<WorldObject> worldObjects, ToDoubleFunction<VectorXZ> terrainEle) {
		for (ConnectorCluster cluster : ConnectorCluster.fromWorldObjects(worldObjects)) {
			try {
				calculateClusterEle(cluster, terrainEle);
			} catch (Exception e) {
				ConversionLog.error(e, cluster.getElementForLogging());
			}
		}
	}

	private void calculateClusterEle(ConnectorCluster cluster, ToDoubleFunction<VectorXZ> terrainEle) {

		Double attachedBaseEle = cluster.getBaseEleFromAttachment();
		Double groundContactBaseEle = getBaseEleFromGroundContacts(cluster, terrainEle);

		if (attachedBaseEle != null) {
			cluster.setBaseEle(attachedBaseEle);
		} else if (groundContactBaseEle != null) {
			cluster.setBaseEle(groundContactBaseEle);
		} else if (cluster.getConnectors().stream().allMatch(c -> c.groundState == GroundState.ABOVE)) {
			cluster.setBaseEle(cluster.getBaseEle() + ELE_OFFSET_ABOVE);
		} else {
			cluster.setBaseEle(cluster.getBaseEle()); // applies the initial base elevation to the connectors
		}

	}

	/**
	 * returns the base elevation implied by the cluster's {@link LowestGroundContact} relations,
	 * or null if there are none
	 */
	private static @Nullable Double getBaseEleFromGroundContacts(ConnectorCluster cluster,
			ToDoubleFunction<VectorXZ> terrainEle) {

		if (cluster.getGroundContacts().isEmpty()) return null;

		double minBaseEle = Double.POSITIVE_INFINITY;
		double maxBaseEle = Double.NEGATIVE_INFINITY;

		for (LowestGroundContact groundContact : cluster.getGroundContacts()) {
			double value = getLowestTerrainEle(groundContact, terrainEle) - cluster.getOffset(groundContact.connector());
			minBaseEle = Math.min(minBaseEle, value);
			maxBaseEle = Math.max(maxBaseEle, value);
		}

		if (maxBaseEle - minBaseEle > WARNING_THRESHOLD) {
			ConversionLog.warn("Connected objects suggest different elevations", cluster.getElementForLogging());
		}

		return maxBaseEle;

	}

	/** returns the lowest terrain elevation at the vertices of a {@link LowestGroundContact}'s outlines */
	private static double getLowestTerrainEle(LowestGroundContact groundContact, ToDoubleFunction<VectorXZ> terrainEle) {
		return groundContact.outlines().stream()
				.flatMap(outline -> outline.getRings().stream())
				.map(SimplePolygonShapeXZ::vertices)
				.flatMap(Collection::stream)
				.mapToDouble(terrainEle)
				.min().orElseThrow();
	}

}
