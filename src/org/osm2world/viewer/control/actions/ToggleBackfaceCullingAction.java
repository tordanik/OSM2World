package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ToggleBackfaceCullingAction extends AbstractAction {

	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;
		
	public ToggleBackfaceCullingAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions) {
		
		super("Backface culling");
		putValue(SHORT_DESCRIPTION, "Switches backface culling on and off");	
		putValue(SELECTED_KEY, renderOptions.isBackfaceCulling());
		
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {
				
		renderOptions.setBackfaceCulling(!renderOptions.isBackfaceCulling());
		putValue(SELECTED_KEY, renderOptions.isBackfaceCulling());
				
	}

}
