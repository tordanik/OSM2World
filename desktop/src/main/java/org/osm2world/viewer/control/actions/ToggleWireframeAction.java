package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;


public class ToggleWireframeAction extends AbstractAction implements Observer {

	@Serial
	private static final long serialVersionUID = 6710342251037183143L;

	private final Data data;
	private final RenderOptions renderOptions;

	public ToggleWireframeAction(Data data, RenderOptions renderOptions) {

		super("Wireframe view");
		putValue(SHORT_DESCRIPTION, "Switches between wireframe and solid view");
		putValue(MNEMONIC_KEY, KeyEvent.VK_F);
		putValue(SELECTED_KEY, renderOptions.isWireframe());

		this.data = data;
		this.renderOptions = renderOptions;

		this.update(null, null);
		data.addObserver(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setWireframe(!renderOptions.isWireframe());
		putValue(SELECTED_KEY, renderOptions.isWireframe());

	}

	@Override
	public void update(Observable o, Object arg) {
		this.setEnabled(data.getConversionResults() != null);
	}

}
