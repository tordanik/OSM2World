package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.common.rendering.TileNumber;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class OrthoTileAction extends AbstractAction implements Observer {

	private static final long serialVersionUID = 4494511160535468154L; //generated serialVersionUID
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public OrthoTileAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Ortho tile");
		putValue(SHORT_DESCRIPTION, "Switch to orthographic view of a tile.");

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

		try {

			int zoom = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "zoom level"));
			int tileX = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "tile x"));
			int tileY = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "tile y"));
			int angle = Integer.parseInt(
					JOptionPane.showInputDialog(viewerFrame, "view angle"));
			CardinalDirection from = CardinalDirection.valueOf(
					JOptionPane.showInputDialog(viewerFrame, "from cardinal direction"));

			AxisAlignedRectangleXZ tileBounds =
				data.getConversionResults().getMapData().getDataBoundary();

			renderOptions.camera = OrthoTilesUtil.cameraForTile(
					data.getConversionResults().getMapProjection(),
					new TileNumber(zoom, tileX, tileY), angle, from);

			renderOptions.projection = OrthoTilesUtil.projectionForTile(
					data.getConversionResults().getMapProjection(),
					new TileNumber(zoom, tileX, tileY), angle, from);

		} catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(viewerFrame, "invalid input");
		}

	}

}
