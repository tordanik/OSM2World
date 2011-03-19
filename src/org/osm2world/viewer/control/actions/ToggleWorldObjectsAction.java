package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ToggleWorldObjectsAction extends AbstractAction {

	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;
		
	public ToggleWorldObjectsAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions) {
		
		super("World objects");
		putValue(SHORT_DESCRIPTION, "Controls whether world objects are displayed");
		putValue(MNEMONIC_KEY, KeyEvent.VK_W);
		putValue(SELECTED_KEY, renderOptions.isShowGrid());
		
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
				
		renderOptions.setShowGrid(!renderOptions.isShowGrid());
		putValue(SELECTED_KEY, renderOptions.isShowGrid());
				
	}

}
