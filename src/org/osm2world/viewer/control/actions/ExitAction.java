package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/**
 * closes the application */
public class ExitAction extends AbstractAction {
		
	private static final long serialVersionUID = -7239839993534668987L; //generated serialVersionUID

	public ExitAction() {
		super("Exit");
		putValue(SHORT_DESCRIPTION, "Closes the application");
		putValue(MNEMONIC_KEY, KeyEvent.VK_X);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		System.exit(0);
	}
	
}
