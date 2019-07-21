package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class SetCameraToCoordinateAction extends AbstractAction implements Observer {

	private static final long serialVersionUID = 8587163475874776786L; //generated serialVersionUID
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public SetCameraToCoordinateAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Set camera to coordinate");
		putValue(SHORT_DESCRIPTION, "Precisely position the camera");

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

		double lat = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "lat"));
		double lon = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "lon"));
		double height = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "height"));

		MapProjection projection = data.getConversionResults().getMapProjection();

		VectorXZ newPosXZ = projection.calcPos(lat, lon);
		VectorXYZ newPos = newPosXZ.xyz(height);

		renderOptions.camera.move(newPos.subtract(renderOptions.camera.getPos()));

	}

}
