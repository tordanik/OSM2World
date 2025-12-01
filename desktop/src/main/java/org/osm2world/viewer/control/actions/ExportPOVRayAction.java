package org.osm2world.viewer.control.actions;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.output.Output;
import org.osm2world.output.common.rendering.MutableCamera;
import org.osm2world.output.povray.POVRayOutput;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ExportPOVRayAction extends AbstractExportAction {

	private static final long serialVersionUID = -6019526579344140850L; //generated serialVersionUID

	public ExportPOVRayAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export POVRay file", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes a source file for the POVRay raytracer");
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));

	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter("POVRay source files", "pov");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			/* create camera and perspective
			   compensating for left- vs. right-handed coords */

			MutableCamera povRayCamera = new MutableCamera();
			povRayCamera.setCamera(
					renderOptions.camera.pos(),
					renderOptions.camera.lookAt(),
					renderOptions.camera.up());

			/* write the file */
			Output output = new POVRayOutput(file, povRayCamera, renderOptions.projection);
			output.setConfiguration(data.getConfig());
			output.outputScene(data.getConversionResults());

			messageManager.addMessage("exported POVRay file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export POVRay file", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}
