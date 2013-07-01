package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class SetTerrainInterpolatorAction extends AbstractAction {

	Class<? extends TerrainInterpolator> interpolatorClass;
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public SetTerrainInterpolatorAction(
			Class<? extends TerrainInterpolator> interpolatorClass,
			ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {
		
		super(interpolatorClass.getSimpleName().replaceAll("Interpolator", ""));
		
		putValue(SELECTED_KEY, interpolatorClass.equals(
				renderOptions.getInterpolatorClass()));
		
		this.interpolatorClass = interpolatorClass;
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setInterpolatorClass(interpolatorClass);
		putValue(SELECTED_KEY,
				renderOptions.getInterpolatorClass().equals(interpolatorClass));
		
		if (data.getConversionResults() != null) {
			JOptionPane.showMessageDialog(viewerFrame, "You need to reload or" +
					" open a new OSM file for this option to have any effect!",
					"Reload required", JOptionPane.INFORMATION_MESSAGE);
		}
		
	}

}
