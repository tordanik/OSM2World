package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;

public class ToggleBackfaceCullingAction extends AbstractAction implements Observer {

	@Serial
	private static final long serialVersionUID = 3993313015641228064L;

	private final Data data;
	private final RenderOptions renderOptions;

	public ToggleBackfaceCullingAction(Data data, RenderOptions renderOptions) {

		super("Backface culling");
		putValue(SHORT_DESCRIPTION, "Switches backface culling on and off");
		putValue(SELECTED_KEY, renderOptions.isBackfaceCulling());

		this.data = data;
		this.renderOptions = renderOptions;

		this.update(null, null);
		data.addObserver(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setBackfaceCulling(!renderOptions.isBackfaceCulling());
		putValue(SELECTED_KEY, renderOptions.isBackfaceCulling());

	}

	@Override
	public void update(Observable o, Object arg) {
		this.setEnabled(data.getConversionResults() != null);
	}

}
