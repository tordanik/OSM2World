package org.osm2world.viewer.view.debug;

import static org.osm2world.math.VectorXZ.Z_UNIT;
import static org.osm2world.scene.color.Color.GREEN;
import static org.osm2world.scene.color.Color.RED;

import org.osm2world.math.VectorXYZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.data.WorldObject;

public class ConnectorAttachmentDebugView extends StaticDebugView {

	private final Color ATTACHED_COLOUR = GREEN;
	private final Color FAILED_COLOUR = RED;

	public ConnectorAttachmentDebugView() {
		super("Connector attachment", "shows connectors that attach objects to surfaces");
	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		for (WorldObject object : scene.getWorldObjects()) {
			for (Connector connector : object.getConnectors()) {

				if (connector.attachmentProperties == null) continue;

				if (connector.isAttached()) {

					VectorXYZ pos = connector.getPosXYZ();
					output.drawBox(new Material(Interpolation.FLAT, ATTACHED_COLOUR),
							pos.addY(-0.1), Z_UNIT, 0.2, 0.2, 0.2);
					drawArrow(output, ATTACHED_COLOUR, 0.2f, pos,
							pos.add(connector.getAttachedSurfaceNormal()));

				} else {

					output.drawBox(new Material(Interpolation.FLAT, FAILED_COLOUR),
							connector.originalPos.addY(-0.1), Z_UNIT, 0.2, 0.2, 0.2);

				}

			}
		}

	}

}
