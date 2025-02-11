package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.Defaults;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ToggleOrthographicProjectionAction extends AbstractAction {

	private static final long serialVersionUID = 8546764815038965935L;
	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;

	public ToggleOrthographicProjectionAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions) {

		super("Orthographic projection");
		putValue(SHORT_DESCRIPTION, "Switches between orthographic and perspective projection");
		putValue(MNEMONIC_KEY, KeyEvent.VK_C);
		putValue(SELECTED_KEY, renderOptions.projection.orthographic());

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (renderOptions.projection.orthographic()) {
			renderOptions.projection = Defaults.PERSPECTIVE_PROJECTION;
		} else {
			renderOptions.projection = Defaults.ORTHOGRAPHIC_PROJECTION;
		}

		putValue(SELECTED_KEY, renderOptions.projection.orthographic());

	}

}
