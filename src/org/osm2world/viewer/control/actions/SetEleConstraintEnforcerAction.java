package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class SetEleConstraintEnforcerAction extends AbstractAction {

	private static final long serialVersionUID = -5241031810160447221L; //generated serialVersionUID
	Class<? extends EleConstraintEnforcer> enforcerClass;
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public SetEleConstraintEnforcerAction(
			Class<? extends EleConstraintEnforcer> enforcerClass,
			ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {
		
		super(enforcerClass.getSimpleName().replace("EleConstraintEnforcer", ""));
		
		putValue(SELECTED_KEY, enforcerClass.equals(
				renderOptions.getEnforcerClass()));
		
		this.enforcerClass = enforcerClass;
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setEnforcerClass(enforcerClass);
		putValue(SELECTED_KEY,
				renderOptions.getEnforcerClass().equals(enforcerClass));
		
		if (data.getConversionResults() != null) {
			JOptionPane.showMessageDialog(viewerFrame, "You need to reload or" +
					" open a new OSM file for this option to have any effect!",
					"Reload required", JOptionPane.INFORMATION_MESSAGE);
		}
		
	}

}
