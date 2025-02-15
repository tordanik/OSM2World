package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.rendering.MutableCamera;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.Defaults;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ResetCameraAction extends AbstractAction implements Observer {

	private static final long serialVersionUID = 9163911330093863591L; //generated serialVersionUID
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public ResetCameraAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Reset Camera");
		putValue(SHORT_DESCRIPTION, "Use this if you have messed up the camera");

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

		setEnabled(false);
		data.addObserver(this);

	}

	@Override
	public void update(Observable o, Object arg) {
		setEnabled(data.getConversionResults() != null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		VectorXZ camLookAt = data.getConversionResults().getBoundary().center();

		renderOptions.camera = new MutableCamera();
		renderOptions.camera.setCamera(new VectorXYZ(camLookAt.x, 1000, camLookAt.z-1000),
                                       new VectorXYZ(camLookAt.x, 0, camLookAt.z));

		renderOptions.projection = Defaults.PERSPECTIVE_PROJECTION;

	}

}
