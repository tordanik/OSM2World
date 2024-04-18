package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.*;

import org.osm2world.core.map_elevation.creation.EleCalculator;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class SetEleCalculatorAction extends AbstractAction {

	private static final long serialVersionUID = -5241031810160447221L; //generated serialVersionUID
	Class<? extends EleCalculator> eleCalculatorClass;
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public SetEleCalculatorAction(
			Class<? extends EleCalculator> eleCalculatorClass,
			ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super(eleCalculatorClass.getSimpleName().replace("EleCalculator", ""));

		putValue(SELECTED_KEY, eleCalculatorClass.equals(
				renderOptions.getEleCalculatorClass()));

		this.eleCalculatorClass = eleCalculatorClass;
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setEleCalculatorClass(eleCalculatorClass);
		putValue(SELECTED_KEY,
				renderOptions.getEleCalculatorClass().equals(eleCalculatorClass));

		if (data.getConversionResults() != null) {
			JOptionPane.showMessageDialog(viewerFrame, "You need to reload or" +
					" open a new OSM file for this option to have any effect!",
					"Reload required", JOptionPane.INFORMATION_MESSAGE);
		}

	}

}
