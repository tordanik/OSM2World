package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class OrthoBoundsAction extends AbstractAction {

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

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		AxisAlignedBoundingBoxXZ bounds =
			data.getConversionResults().getMapData().getBoundary();
		
		renderOptions.camera =
			OrthoTilesUtil.cameraForBounds(bounds, 30, CardinalDirection.S);

		renderOptions.projection =
			OrthoTilesUtil.projectionForBounds(bounds, 30, CardinalDirection.S);
		
	}
	
}
