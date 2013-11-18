package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ToggleWireframeAction extends AbstractAction {

	private static final long serialVersionUID = 6710342251037183143L;
	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;
		
	public ToggleWireframeAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions) {
		
		super("Wireframe view");
		putValue(SHORT_DESCRIPTION, "Switches between wireframe and solid view");
		putValue(MNEMONIC_KEY, KeyEvent.VK_F);		
		putValue(SELECTED_KEY, renderOptions.isWireframe());
		
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {
				
		renderOptions.setWireframe(!renderOptions.isWireframe());
		putValue(SELECTED_KEY, renderOptions.isWireframe());
				
	}

}
