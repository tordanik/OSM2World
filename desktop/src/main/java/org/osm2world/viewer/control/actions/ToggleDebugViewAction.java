package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;
import org.osm2world.viewer.view.debug.DebugView;



public class ToggleDebugViewAction extends AbstractAction implements Observer {

	private static final long serialVersionUID = -7415950981091744016L;

	private final DebugView debugView;

	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;

	private boolean enabled = false;

	public ToggleDebugViewAction(DebugView debugView, int mnemonicKey,
			boolean enabled, ViewerFrame viewerFrame,
			Data data, RenderOptions renderOptions) {

		super(debugView.label);

		putValue(SHORT_DESCRIPTION, debugView.description);
		putValue(MNEMONIC_KEY, mnemonicKey);
		putValue(SELECTED_KEY, enabled);

		this.debugView = debugView;
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;
		this.enabled = enabled;

		if (enabled) {
			renderOptions.activeDebugViews.add(debugView);
		}

		this.setEnabled(debugView.canBeUsed());

		data.addObserver(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (enabled) {
			renderOptions.activeDebugViews.remove(debugView);
		} else {
			renderOptions.activeDebugViews.add(debugView);
		}

		enabled = !enabled;
		putValue(SELECTED_KEY, enabled);

	}

	@Override
	public void update(Observable o, Object arg) {

		viewerFrame.setConfiguration(data.getConfig());

		debugView.setConfiguration(data.getConfig());

		debugView.setConversionResults(data.getConversionResults());

		this.setEnabled(debugView.canBeUsed());

	}

}
