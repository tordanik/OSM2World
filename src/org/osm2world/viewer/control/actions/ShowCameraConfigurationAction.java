package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;

public class ShowCameraConfigurationAction extends AbstractAction {
	
	private static final long serialVersionUID = -3461617949419339009L;
	private final Data data;
	private final RenderOptions renderOptions;
	
	public ShowCameraConfigurationAction(Data data, RenderOptions renderOptions) {
		super("Show current camera configuration");
		this.data = data;
		this.renderOptions = renderOptions;
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
				"posLat = " + mapProjection.calcLat(pos.xz())
				+ "\nposLon = " + mapProjection.calcLon(pos.xz())
				+ "\nposEle = " + pos.y
				+ "\nlookAtLat = " + mapProjection.calcLat(lookAt.xz())
				+ "\nlookAtLon = " + mapProjection.calcLon(lookAt.xz())
				+ "\nlookAtEle = " + lookAt.y,
				"Current camera configuration", JOptionPane.INFORMATION_MESSAGE);
	}
	
}
