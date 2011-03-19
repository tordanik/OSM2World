package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.TileNumber;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class OrthoTileAction extends AbstractAction {

	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public OrthoTileAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Ortho tile");
		putValue(SHORT_DESCRIPTION, "Switch to orthographic view of a tile.");

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		try {
			
			int tileX = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "tile x"));
			int tileY = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "tile y"));
			int angle = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "view angle"));
						
			AxisAlignedBoundingBoxXZ tileBounds =
				data.getConversionResults().getMapData().getBoundary();
			
			renderOptions.camera = OrthoTilesUtil.cameraForTile(
					data.getConversionResults().getMapProjection(),
					new TileNumber(12, tileX, tileY), angle);

			renderOptions.projection = OrthoTilesUtil.projectionForTile(
					data.getConversionResults().getMapProjection(),
					new TileNumber(12, tileX, tileY), angle);
						
		} catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(viewerFrame, "invalid input");
		}
		
	}
	
}
