package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.core.map_elevation.creation.ElevationCalculator;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class SetElevationCalculatorAction extends AbstractAction {

	ElevationCalculator eleCalculator;
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public SetElevationCalculatorAction(ElevationCalculator eleCalculator,
			ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super(eleCalculator.getClass().getSimpleName());
		
		putValue(SELECTED_KEY, eleCalculator.getClass().equals(
				renderOptions.getEleCalculator().getClass()));
		
		this.eleCalculator = eleCalculator;
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setEleCalculator(eleCalculator);
		putValue(SELECTED_KEY,
				renderOptions.getEleCalculator() == eleCalculator);
		
		if (data.getConversionResults() != null) {
			JOptionPane.showMessageDialog(viewerFrame, "You need to reload or" +
					" open a new OSM file for this option to have any effect!",
					"Reload required", JOptionPane.INFORMATION_MESSAGE);
		}
		
	}

}
