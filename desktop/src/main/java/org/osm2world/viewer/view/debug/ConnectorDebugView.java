package org.osm2world.viewer.view.debug;

import static org.osm2world.scene.color.Color.BLUE;

import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.data.WorldObject;

/**
 * A debug view which shows all {@link Connector}s
 */
public class ConnectorDebugView extends StaticDebugView {

	private static final Color CONNECTOR_COLOR = BLUE;
	private static final float CONNECTOR_HALF_WIDTH = 0.25f;

	public ConnectorDebugView() {
		super("Connectors", "shows all connectors");
	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		for (WorldObject worldObject : scene.getWorldObjects()) {
			for (Connector connector : worldObject.getConnectors()) {
				if (connector.getPosXYZ() == null) {
					continue; //TODO shouldn't happen
				}
				drawBoxAround(output, connector.getPosXYZ(),
						CONNECTOR_COLOR, CONNECTOR_HALF_WIDTH);
			}
		}

	}

}
