package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.osm2world.viewer.view.ViewerFrame;
import org.osm2world.viewer.view.DaylightDialog;

public class ChangeTimeAction extends AbstractAction
{
	private final ViewerFrame frame;

	public ChangeTimeAction(ViewerFrame frame) {
		super("Date and Time");
		this.frame = frame;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		DaylightDialog dialog = new DaylightDialog(frame);
	}
}
	

