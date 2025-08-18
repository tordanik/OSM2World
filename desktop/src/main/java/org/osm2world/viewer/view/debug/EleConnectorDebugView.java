package org.osm2world.viewer.view.debug;

import static java.awt.Color.BLUE;

import java.awt.*;

import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.world.data.WorldObject;

/**
 * shows all {@link EleConnector}s
 */
public class EleConnectorDebugView extends StaticDebugView {

	private static final Color CONNECTOR_COLOR = BLUE;
	private static final float CONNECTOR_HALF_WIDTH = 0.25f;

	public EleConnectorDebugView() {
		super("Elevation connectors", "shows all elevation connectors");
	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		for (WorldObject worldObject : scene.getWorldObjects()) {
			for (EleConnector eleConnector : worldObject.getEleConnectors()) {
				if (eleConnector.getPosXYZ() == null) {
					continue; //TODO shouldn't happen
				}
				drawBoxAround(output, eleConnector.getPosXYZ(),
						CONNECTOR_COLOR, CONNECTOR_HALF_WIDTH);
			}
		}

	}

}
