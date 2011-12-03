package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class OrthoBoundsAction extends AbstractAction implements Observer {

	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public OrthoBoundsAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Ortho bounds");
		putValue(SHORT_DESCRIPTION, "Switch to orthographic view of "
				+ "the entire map data from south.");

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

		AxisAlignedBoundingBoxXZ bounds =
			data.getConversionResults().getMapData().getDataBoundary();
		
		renderOptions.camera =
			OrthoTilesUtil.cameraForBounds(bounds, 30, CardinalDirection.S);

		renderOptions.projection =
			OrthoTilesUtil.projectionForBounds(bounds, 30, CardinalDirection.S);
		
	}
	
}
