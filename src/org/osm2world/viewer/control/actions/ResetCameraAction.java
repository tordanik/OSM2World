package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.Defaults;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ResetCameraAction extends AbstractAction {

	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public ResetCameraAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Reset Camera");
		putValue(SHORT_DESCRIPTION, "Use this if you have messed up the camera");

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		MapData grid = data.getConversionResults().getMapData();

		VectorXZ camLookAt = new VectorXZ(
				grid.getCenter().x, grid.getCenter().z);

		renderOptions.camera = new Camera();
		renderOptions.camera.setLookAt(camLookAt.x, 0, camLookAt.z);
		renderOptions.camera.setPos(camLookAt.x, 1000, camLookAt.z-1000);
		
		renderOptions.projection = Defaults.PERSPECTIVE_PROJECTION;

	}

}
