package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ToggleTerrainAction extends AbstractAction {

	private static final long serialVersionUID = 3659085143967426625L;
	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;

	public ToggleTerrainAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions) {

		super("Terrain");
		putValue(SHORT_DESCRIPTION, "Controls whether terrain is displayed");
		putValue(MNEMONIC_KEY, KeyEvent.VK_N);
		putValue(SELECTED_KEY, renderOptions.isShowTerrain());

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setShowTerrain(!renderOptions.isShowTerrain());
		putValue(SELECTED_KEY, renderOptions.isShowTerrain());

	}

}
