package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.geo.MapProjection;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;

public class ShowCameraConfigurationAction
	extends AbstractAction implements Observer {

	private static final long serialVersionUID = -3461617949419339009L;
	private final Data data;
	private final RenderOptions renderOptions;

	public ShowCameraConfigurationAction(Data data, RenderOptions renderOptions) {

		super("Show current camera configuration");
		this.data = data;
		this.renderOptions = renderOptions;

		setEnabled(false);
		data.addObserver(this);

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Results r = data.getConversionResults();

		if (r == null) {
			JOptionPane.showMessageDialog(null, "no Camera defined");
			return;
		}

		MapProjection mapProjection = data.getConversionResults().getMapProjection();

		VectorXYZ pos = renderOptions.camera.getPos();
		VectorXYZ lookAt = renderOptions.camera.getLookAt();

		JOptionPane.showMessageDialog(null,
				"posLat = " + mapProjection.toLat(pos.xz())
				+ "\nposLon = " + mapProjection.toLon(pos.xz())
				+ "\nposEle = " + pos.y
				+ "\nlookAtLat = " + mapProjection.toLat(lookAt.xz())
				+ "\nlookAtLon = " + mapProjection.toLon(lookAt.xz())
				+ "\nlookAtEle = " + lookAt.y,
				"Current camera configuration", JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void update(Observable o, Object arg) {
		setEnabled(data.getConversionResults() != null);
	}

}
