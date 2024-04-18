package org.osm2world.core.map_elevation.creation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.world.data.WorldObject;

/**
 * Attempts to improve elevations by applying an {@link EleConstraintEnforcer}
 */
public class ConstraintElevationCalculator implements ElevationCalculator {

	private final boolean DEBUG_CONSTRAINTS = false;

	private final EleConstraintEnforcer eleConstraintEnforcer;

	public ConstraintElevationCalculator(EleConstraintEnforcer eleConstraintEnforcer) {
		this.eleConstraintEnforcer = eleConstraintEnforcer;
	}

	@Override
	public void calculateElevations(@Nonnull MapData mapData) {

		final List<EleConnector> connectors = new ArrayList<>();

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {
			for (EleConnector conn : worldObject.getEleConnectors()) {
				connectors.add(conn);
			}
		});

		/* enforce constraints defined by WorldObjects */

		final EleConstraintEnforcer enforcer = DEBUG_CONSTRAINTS
				? new EleConstraintValidator(mapData, eleConstraintEnforcer)
				: eleConstraintEnforcer;

		enforcer.addConnectors(connectors);

		if (!(enforcer instanceof NoneEleConstraintEnforcer)) {

			FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(),
					(WorldObject o) -> o.defineEleConstraints(enforcer));

		}

		enforcer.enforceConstraints();

	}

}
